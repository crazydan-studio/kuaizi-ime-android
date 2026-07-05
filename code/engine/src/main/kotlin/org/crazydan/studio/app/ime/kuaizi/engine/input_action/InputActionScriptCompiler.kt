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

package org.crazydan.studio.app.ime.kuaizi.engine.input_action

import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey

/**
 * 输入动作脚本编译器：将待输入文本编译为 [InputActionScript]。
 *
 * 编译器是纯函数式组件——无副作用、无外部依赖，接收文本和模式参数，产出脚本或抛出异常。
 * 编译过程涉及文本分词、拼音拆分、按键映射和时间计算四个步骤。
 *
 * 遵循 Fail Fast 原则：字符无法映射到按键时立即抛出 [IllegalArgumentException]，
 * 避免回放时在位置解析阶段才发现错误。
 */
class InputActionScriptCompiler {

    /**
     * 将输入文本编译为动作脚本。
     *
     * @param text 待输入的文本内容
     * @param mode 输入交互模式（点击 / 滑行）
     * @return 编译完成的动作脚本
     * @throws IllegalArgumentException 文本包含无法映射为拼音或不支持的字符时抛出
     */
    fun compile(
        text: String,
        mode: InputActionMode,
    ): InputActionScript {
        val pinyinSegments = textToPinyinSegments(text)
        val actions = when (mode) {
            InputActionMode.Tap -> compileTapActions(pinyinSegments)
            InputActionMode.Swipe -> compileSwipeActions(pinyinSegments)
        }
        // 计算总时长：取所有动作的 startTime + 持续时长的最大值
        val totalDuration = actions.maxOfOrNull { a ->
            a.startTime + when (a) {
                is InputAction.SwipeTo -> a.duration
                is InputAction.Wait -> a.duration
                else -> 0L
            }
        } ?: 0L

        return InputActionScript(
            name = "$text-${mode.name}",
            description = "输入「$text」的${mode.name}模式脚本，共 ${actions.size} 个动作",
            inputActionMode = mode,
            actions = actions,
            totalDuration = totalDuration,
        )
    }

    /**
     * 编译点击模式的动作列表。
     * 每个字母映射为 [InputAction.KeyDown] + [InputAction.KeyUp] 对，
     * 相邻字母间插入 [InputAction.Wait]，每个音节结束后插入 [InputAction.SelectCandidate]。
     */
    private fun compileTapActions(segments: List<PinyinSegment>): List<InputAction> {
        val actions = mutableListOf<InputAction>()
        var currentTime = 0L

        for ((segmentIndex, segment) in segments.withIndex()) {
            for ((charIndex, char) in segment.letters.withIndex()) {
                val key = requireKeyForChar(char)
                actions.add(InputAction.KeyDown(currentTime, key))
                currentTime += KEY_DOWN_DURATION
                actions.add(InputAction.KeyUp(currentTime, key))
                currentTime += KEY_UP_DURATION

                val isLastChar = charIndex == segment.letters.lastIndex
                if (!isLastChar) {
                    actions.add(InputAction.Wait(currentTime, TAP_KEY_INTERVAL))
                    currentTime += TAP_KEY_INTERVAL
                }
            }

            // 每个音节结束后选择首个候选词
            actions.add(InputAction.SelectCandidate(currentTime, 0))
            currentTime += CANDIDATE_SELECT_DURATION

            val isLastSegment = segmentIndex == segments.lastIndex
            if (!isLastSegment) {
                actions.add(InputAction.Wait(currentTime, TAP_SYLLABLE_INTERVAL))
                currentTime += TAP_SYLLABLE_INTERVAL
            }
        }

        return actions
    }

    /**
     * 编译滑行模式的动作列表。
     * 音节首字母为 [InputAction.KeyDown]，后续字母通过 [InputAction.SwipeTo] 连接，
     * 音节末尾 [InputAction.KeyUp] 完成滑行，每个音节结束后选择首个候选词。
     */
    private fun compileSwipeActions(segments: List<PinyinSegment>): List<InputAction> {
        val actions = mutableListOf<InputAction>()
        var currentTime = 0L

        for ((segmentIndex, segment) in segments.withIndex()) {
            val keys = segment.letters.map { requireKeyForChar(it) }

            actions.add(InputAction.KeyDown(currentTime, keys.first()))
            currentTime += KEY_DOWN_DURATION

            for (i in 1 until keys.size) {
                val fromKey = keys[i - 1]
                val toKey = keys[i]
                val duration = calculateSwipeDuration(fromKey, toKey)
                actions.add(InputAction.SwipeTo(currentTime, fromKey, toKey, duration))
                currentTime += duration
            }

            actions.add(InputAction.KeyUp(currentTime, keys.last()))
            currentTime += KEY_UP_DURATION

            actions.add(InputAction.SelectCandidate(currentTime, 0))
            currentTime += CANDIDATE_SELECT_DURATION

            val isLastSegment = segmentIndex == segments.lastIndex
            if (!isLastSegment) {
                actions.add(InputAction.Wait(currentTime, SWIPE_SYLLABLE_INTERVAL))
                currentTime += SWIPE_SYLLABLE_INTERVAL
            }
        }

        return actions
    }

    /**
     * 根据按键距离计算滑行时长。
     * 基础时长 + 距离系数，结果限定在 [SWIPE_MIN_DURATION, SWIPE_MAX_DURATION] 范围内。
     */
    private fun calculateSwipeDuration(fromKey: InputKey, toKey: InputKey): Long {
        val distance = estimateKeyDistance(fromKey, toKey)
        return (SWIPE_BASE_DURATION + (distance * SWIPE_DISTANCE_FACTOR).toLong())
            .coerceIn(SWIPE_MIN_DURATION, SWIPE_MAX_DURATION)
    }

    /** 估算两按键之间的距离（归一化值），目前简化实现返回固定值。 */
    private fun estimateKeyDistance(fromKey: InputKey, toKey: InputKey): Float {
        if (fromKey == toKey) return 0.0f
        return 1.0f
    }

    /** 将字符映射为按键，若无法映射则抛出异常。 */
    private fun requireKeyForChar(char: String): InputKey {
        return InputKey.Char.Alphabet(value = char)
    }

    /**
     * 将输入文本拆分为拼音音节序列。
     *
     * - 中文字符：通过 [pinyinForChar] 转换为拼音字母列表
     * - 拉丁字母：拆分为单字母音节
     * - 其他字符：抛出异常
     */
    private fun textToPinyinSegments(text: String): List<PinyinSegment> {
        val segments = mutableListOf<PinyinSegment>()

        for (char in text) {
            when {
                char.isCJKCharacter() -> {
                    val pinyin = pinyinForChar(char)
                        ?: throw IllegalArgumentException(
                            "无法将字符「$char」转换为拼音"
                        )
                    segments.add(
                        PinyinSegment(
                            originalText = char.toString(),
                            letters = pinyin.toList().map { it.toString() },
                        )
                    )
                }

                char.isLetter() && char.code < 128 -> {
                    segments.add(
                        PinyinSegment(
                            originalText = char.toString(),
                            letters = listOf(char.lowercaseChar().toString()),
                        )
                    )
                }

                else -> {
                    throw IllegalArgumentException(
                        "不支持的字符「$char」，仅支持中文和拉丁字母"
                    )
                }
            }
        }

        return segments
    }

    /** 获取汉字的拼音映射（简化版本，仅包含基础演示用汉字）。 */
    private fun pinyinForChar(char: Char): String? {
        val code = char.code
        val basicPinyins = mapOf(
            0x4E2D to "zhong",
            0x56FD to "guo",
            0x4EBA to "ren",
            0x5927 to "da",
            0x5C0F to "xiao",
            0x4E00 to "yi",
            0x4E8C to "er",
            0x4E09 to "san",
        )
        return basicPinyins[code]
    }

    /** 判断字符是否为 CJK（中日韩统一表意文字）字符。 */
    private fun Char.isCJKCharacter(): Boolean {
        val code = this.code
        return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF
    }

    /**
     * 拼音音节：表达一个音节的原始文本和对应的拼音字母列表。
     *
     * @property originalText 原始文本（汉字或拉丁字母）
     * @property letters 拼音字母列表，如 "zhong" → ["z", "h", "o", "n", "g"]
     */
    private data class PinyinSegment(
        val originalText: String,
        val letters: List<String>,
    )

    companion object {
        // 按键按下持续时长（毫秒）
        private const val KEY_DOWN_DURATION = 50L

        // 按键抬起持续时长（毫秒）
        private const val KEY_UP_DURATION = 30L

        // 同音节内按键间隔（毫秒）
        private const val TAP_KEY_INTERVAL = 80L

        // 音节间间隔（毫秒）
        private const val TAP_SYLLABLE_INTERVAL = 200L

        // 滑行模式音节间间隔（毫秒）
        private const val SWIPE_SYLLABLE_INTERVAL = 300L

        // 候选词选择持续时长（毫秒）
        private const val CANDIDATE_SELECT_DURATION = 100L

        // 滑行基础时长（毫秒）
        private const val SWIPE_BASE_DURATION = 100L

        // 滑行距离系数（毫秒/单位距离）
        private const val SWIPE_DISTANCE_FACTOR = 50f

        // 滑行最小时长（毫秒）
        private const val SWIPE_MIN_DURATION = 60L

        // 滑行最大时长（毫秒）
        private const val SWIPE_MAX_DURATION = 300L
    }
}
