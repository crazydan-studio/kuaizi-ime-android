/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.engine

import org.crazydan.studio.app.ime.kuaizi.engine.backup.ImportStrategy
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorEditAction
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Motion
import org.crazydan.studio.app.ime.kuaizi.engine.input.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.input.CommonInput
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputFavorite
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputTextOption
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputWord
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType

/**
 * 用户意图 sealed class，MVI 架构中的输入通道。
 *
 * 所有用户操作统一编码为 [ImeIntent]，由 [ImeEngine.handleIntent] 接收并处理。
 * 遵循「意图与手势分离」原则：表达业务语义（如「提交输入」），而非底层手势细节。
 * 手势到意图的映射由 [KeyboardViewModel] 完成，引擎只消费意图，不感知手势。
 *
 * @see ImeEngine.handleIntent
 */
sealed class ImeIntent {

    /** 针对发生在键盘上的意图：按压、点击、滑行等交互操作 */
    sealed class OnKeyboard : ImeIntent() {
        /** 与意图关联的按键，若其为 `null`，则表示无关联的按键 */
        abstract val key: InputKey?

        // ---------------------------------------
        /** 按压 */
        sealed class Press : OnKeyboard() {

            /** 按压开始 */
            data class Begin(
                override val key: InputKey? = null,
            ) : Press()

            /** 按压结束 */
            data class End(
                override val key: InputKey? = null,
            ) : Press()
        }

        /** 长按。如果长按开始后直接移动手指，将自动结束长按并生成滑行意图 */
        sealed class LongPress : OnKeyboard() {

            /** 长按开始 */
            data class Begin(
                override val key: InputKey? = null,
            ) : LongPress()

            /** 长按结束 */
            data class End(
                override val key: InputKey? = null,
            ) : LongPress()

            /**
             * 长按停留中
             * @property tick 停留的滴答数。始终大于 0
             */
            data class Hold(
                override val key: InputKey? = null,
                val tick: Int = 1,
            ) : LongPress() {
                init {
                    require(tick > 0)
                }
            }
        }

        /**
         * 点击。
         * @property tick 点击的滴答数。`0` 表示单击，`1` 表示双击，`2` 表示三击
         */
        sealed class Tap(val tick: Int) : OnKeyboard() {

            /** 单击 */
            data class Single(
                override val key: InputKey? = null,
            ) : Tap(tick = 0)

            /** 双击 */
            data class Double(
                override val key: InputKey? = null,
            ) : Tap(tick = 1)

            /** 三击 */
            data class Triple(
                override val key: InputKey? = null,
            ) : Tap(tick = 2)
        }

        /** 滑行 */
        sealed class Swipe : OnKeyboard() {

            /** 滑行开始 */
            data class Begin(
                override val key: InputKey? = null,
            ) : Swipe()

            /** 滑行结束 */
            data class End(
                override val key: InputKey? = null,
            ) : Swipe()

            /** 滑行进行中 */
            data class Moving(
                override val key: InputKey? = null,
                val motion: Motion,
            ) : Swipe()

            /**
             * 滑行停留
             * @property tick 停留的滴答数。始终大于 0
             */
            data class Hold(
                override val key: InputKey? = null,
                val tick: Int = 1,
            ) : Swipe() {
                init {
                    require(tick > 0)
                }
            }
        }

        /**
         * 翻动。其发生在 [Swipe.Begin] 与 [Swipe.End] 之间。
         */
        data class Flip(
            override val key: InputKey? = null,
            val motion: Motion,
        ) : OnKeyboard()
    }

    // ---------------------------------------------------------------------

    /** 针对输入列表的意图 */
    sealed class InputList : ImeIntent() {

        /** 提交输入列表 */
        data object Commit : InputList()

        /** 撤回已提交的输入列表：将已提交到编辑器的输入撤回，以重新编辑 */
        data object Revoke : InputList()

        /**
         * 添加输入项：拼音、字母、数字、符号、表情等
         * @property replacements 替换字符列表（必须为包含按键字符在内的完整列表）。
         * 在添加时，首先判断前序字符是否在该替换列表内，
         * 若存在，则将前序替换为 [item]，否则，不做替换，直接追加。
         * 在发送该意图前，需由发送方判断是否需要尝试替换前序输入，
         * 若不满足替换条件，则需要将 [replacements] 置为 `null`
         */
        data class AddItem(
            val item: CommonInput.Item,
            val replacements: List<String>? = null,
        ) : InputList()

        /** 回删：在输入列表为空或已被冻结时，回删编辑器内的内容 */
        data object DeleteBackward : InputList()

        /** 删除当前已选中的输入项 */
        data object RemoveSelected : InputList()

        // TODO 支持选中算术输入列表中的输入项
        /** 选中指定位置的输入项 */
        data class SelectAt(val index: Int) : InputList()

        /** 确认待输入 */
        data object ConfirmPending : InputList()

        /** 丢弃待输入 */
        data object DropPending : InputList()

        /** 更新 [InputTextOption] */
        data class UpdateTextOption(val textOption: InputTextOption) : InputList()
    }

    // -----------------------------------------------------------------------

    /** 针对键盘的意图 */
    sealed class Keyboard : ImeIntent() {

        /** 关闭键盘。 */
        data object Close : Keyboard()

        /** 切换到指定类型的键盘。 */
        data class SwitchTo(val type: KeyboardType) : Keyboard()

        /** 反转键盘左右手模式。 */
        data object ToggleHandMode : Keyboard()
    }

    // -----------------------------------------------------------------------

    /** 针对编辑器的意图 */
    sealed class Editor : ImeIntent() {

        /** 编辑操作：对编辑器执行指定的编辑动作。 */
        data class PerformEdit(val action: EditorEditAction) : Editor()
    }

    // -----------------------------------------------------------------------

    /** 切换输入法：告知应用层切换输入法。 */
    data object SwitchIme : ImeIntent()

    // ---------------------------------------------------------------------

    /** 候选选择：用户选中指定候选词。 */
    data class SelectCandidate(val candidate: InputWord) : ImeIntent()

    /** 候选翻页：用户向指定方向翻页候选列表。 */
    data class PageCandidate(val direction: PageDirection) : ImeIntent()

    /** 粘贴剪贴板内容：将指定文本粘贴到编辑器。 */
    data class PasteClip(val text: String) : ImeIntent()

    /** 保存收藏：收藏指定的输入内容。 */
    data class SaveFavorite(val favorite: InputFavorite) : ImeIntent()

    /** 显示剪贴板列表。 */
    data object ShowClipList : ImeIntent()

    /** 显示收藏列表。 */
    data object ShowFavoriteList : ImeIntent()

    /** 关闭剪贴板列表。 */
    data object CloseClipList : ImeIntent()

    /** 关闭收藏列表。 */
    data object CloseFavoriteList : ImeIntent()

    /** 加载候选词意图（异步 sideEffect）：根据拼音查询字典。 */
    data class LoadCandidates(val pinyin: String) : ImeIntent()

    /** 设置候选词：将字典查询结果设置到状态中。 */
    data class SetCandidates(val candidates: CandidateList) : ImeIntent()

    // -----------------------------------------------------------------------

    /** 更新配置：使用新配置替换当前配置。 */
    data class UpdateConfig(val config: ImeConfig) : ImeIntent()

    /** 导出用户数据：将用户数据导出到指定文件。 */
    data class ExportUserData(val filePath: String) : ImeIntent()

    /** 导入用户数据：从指定文件导入用户数据。 */
    data class ImportUserData(val filePath: String, val strategy: ImportStrategy) : ImeIntent()
}

/** 候选翻页方向。 */
enum class PageDirection { Next, Previous }
