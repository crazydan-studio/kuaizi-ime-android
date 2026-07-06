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

package org.crazydan.studio.app.ime.kuaizi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.ToolListState
import org.crazydan.studio.app.ime.kuaizi.engine.input.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.Keyboard
import org.crazydan.studio.app.ime.kuaizi.engine.keyboard.KeyboardType
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableContext
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableGenerator
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyboardLayoutMode
import org.crazydan.studio.app.ime.kuaizi.ui.panel.CandidateListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.GestureFeedbackPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.GestureInputPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.InputListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.KeyLayoutPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.PopupTipPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.ToolListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.theme.KeyboardTheme
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.PopupTipState

/**
 * 键盘宿主组件，顶层集成组件。
 *
 * 通过 [derivedStateOf] 分别订阅 [ImeState] 的各个子字段，
 * 任一子字段的变化仅触发依赖该字段的面板重组，避免整个键盘树因任何微小状态变更而重组。
 * 例如键盘按键面板仅订阅 keyboard 字段，候选列表面板仅订阅 candidateList 字段。
 *
 * 通过 [KeyboardLayoutMode] 参数统一 [StackedLayout] 和 [SeparatedLayout] 两种布局入口，
 * 支持运行时动态切换布局模式。
 *
 * @param viewModel 键盘视图模型
 * @param showIndicator 是否显示播放器指示器
 * @param modifier 修饰符
 */
@Composable
fun KeyboardHost(
    viewModel: KeyboardViewModel,
    showIndicator: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state

    // derivedStateOf：从 ImeState 中提取各子字段的独立快照
    // 任一子字段变化仅触发依赖该字段的 Composable 重组
    val keyboard by remember { derivedStateOf { state.value.keyboard } }
    val inputList by remember { derivedStateOf { state.value.inputList } }
    val candidateList by remember { derivedStateOf { state.value.candidateList } }
    val config by remember { derivedStateOf { state.value.config } }

    // 通过 collectAsState 订阅 ViewModel 的独立 StateFlow
    val layoutMode by viewModel.layoutMode.collectAsState()
    val popupTipState by viewModel.popupTipState.collectAsState()
    val toolListState by viewModel.toolListState.collectAsState()
    val feedbackState = viewModel.feedbackState

    // 构建按键生成上下文，当任一依赖变化时重新创建
    val keyTableContext = remember(keyboard, inputList, candidateList, config) {
        KeyTableContext(
            config = config,
            keyboard = keyboard,
            inputList = inputList,
            candidateList = candidateList,
        )
    }
    // 创建 KeyTableGenerator 实例（此处使用内联实现 generateBasicLayout）
    val generator = remember {
        KeyTableGenerator { context ->
            generateBasicLayout(context)
        }
    }
    // 根据上下文生成按键表
    val keyTable = remember(keyTableContext) { generator.generate(keyTableContext) }

    // 通过 KeyboardTheme 提供主题色彩上下文
    KeyboardTheme(config.ui) {
        // 根据布局模式选择不同的组件部署方式
        when (layoutMode) {
            KeyboardLayoutMode.Stacked -> StackedLayout(
                keyboard = keyboard,
                inputList = inputList,
                candidateList = candidateList,
                config = config,
                toolListState = toolListState,
                popupTipState = popupTipState,
                feedbackState = feedbackState,
                viewModel = viewModel,
                showIndicator = showIndicator,
                keyTable = keyTable,
                keyTableGenerator = generator,
                keyTableContext = keyTableContext,
                modifier = modifier,
            )

            is KeyboardLayoutMode.Separated -> SeparatedLayout(
                keyboard = keyboard,
                inputList = inputList,
                candidateList = candidateList,
                config = config,
                toolListState = toolListState,
                popupTipState = popupTipState,
                feedbackState = feedbackState,
                viewModel = viewModel,
                showIndicator = showIndicator,
                keyTable = keyTable,
                keyTableGenerator = generator,
                keyTableContext = keyTableContext,
                modifier = modifier,
            )
        }
    }
}

/**
 * 堆叠布局：所有组件集中在 Zone B。
 *
 * 布局结构（从上到下）：
 * - Row 1：CandidateListPanel + PopupTipPanel（叠加）
 * - Row 2：ToolListPanel ↔ InputListPanel（互斥，由 isInputting 控制）
 * - Row 3：KeyLayoutPanel + GestureFeedbackPanel + GestureInputPanel（三层叠加）
 */
@Composable
private fun StackedLayout(
    keyboard: Keyboard,
    inputList: InputList,
    candidateList: CandidateList,
    config: ImeConfig,
    toolListState: ToolListState,
    popupTipState: PopupTipState?,
    feedbackState: GestureFeedbackState,
    viewModel: KeyboardViewModel,
    showIndicator: Boolean,
    keyTable: List<List<InputKey>>,
    keyTableGenerator: KeyTableGenerator,
    keyTableContext: KeyTableContext,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Row 1：候选栏 + 弹出提示叠加
        Box {
            CandidateListPanel(
                candidates = candidateList.currentPage,
                selectedIndex = 0,
                layoutState = CandidateListLayoutState(
                    candidatePositions = TODO(),
                    panelSize = TODO()
                ),
                showIndicator = showIndicator,
            )
            PopupTipPanel(
                tipState = popupTipState,
                onAction = { viewModel.handleIntent(it) },
            )
        }
        // Row 2：输入栏与工具列表互斥切换
        if (inputList.hasPending) {
            InputListPanel(
//                items = inputList.inputs,
//                cursorIndex = inputList.gapIndex,
                layoutState = InputListLayoutState(
                    itemPositions = TODO(),
                    panelSize = TODO()
                ),
                showIndicator = showIndicator,
                inputList = TODO(),
                onLayoutStateChanged = TODO(),
                indicatorState = TODO(),
                modifier = TODO(),
            )
        } else {
            ToolListPanel(
//                toolListState = toolListState,
                layoutState = KeyLayoutState(),
                showIndicator = showIndicator,
                toolList = TODO(),
                onLayoutStateChanged = TODO(),
                indicatorState = TODO(),
                onToolClick = TODO(),
                modifier = TODO(),
            )
        }
        // Row 3：按键面板 + 反馈面板 + 输入面板 三层叠加
        Box {
            KeyLayoutPanel(
                keyTable = keyTable,
//                generator = keyTableGenerator,
//                context = keyTableContext,
                keyboardInputMode = config.ui.keyboardInputMode,
                keyLayoutState = KeyLayoutState(),
                onLayoutStateChanged = { viewModel.updateKeyLayoutState(it) },
            )
            GestureFeedbackPanel(
                feedbackState = feedbackState,
                keyLayoutState = KeyLayoutState(),
            )
            GestureInputPanel(
                keyLayoutState = KeyLayoutState(),
                onGesture = { viewModel.handleGesture(it) },
            )
        }
    }
}

/**
 * 分离布局：Zone A 展示按键布局，Zone B 承载输入区域。
 *
 * 布局结构（从上到下）：
 * - Zone A：KeyLayoutPanel + GestureFeedbackPanel（叠加）
 * - Row 1：CandidateListPanel
 * - Row 2：ToolListPanel ↔ InputListPanel（互斥）
 * - Row 3：GestureFeedbackPanel + GestureInputPanel（叠加）
 *
 * 分离模式下手指在 Zone B 输入时不会被自身遮挡，
 * 按键在 Zone A 中展示，缩短视觉搜索路径。
 */
@Composable
private fun SeparatedLayout(
    keyboard: Keyboard,
    inputList: InputList,
    candidateList: CandidateList,
    config: ImeConfig,
    toolListState: ToolListState,
    popupTipState: PopupTipState?,
    feedbackState: GestureFeedbackState,
    viewModel: KeyboardViewModel,
    showIndicator: Boolean,
    keyTable: List<List<InputKey>>,
    keyTableGenerator: KeyTableGenerator,
    keyTableContext: KeyTableContext,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Zone A：按键布局 + 手势反馈
        Box {
            KeyLayoutPanel(
                keyTable = keyTable,
//                generator = keyTableGenerator,
//                context = keyTableContext,
                keyboardInputMode = config.ui.keyboardInputMode,
                keyLayoutState = KeyLayoutState(),
                onLayoutStateChanged = { viewModel.updateKeyLayoutState(it) },
            )
            GestureFeedbackPanel(
                feedbackState = feedbackState,
                keyLayoutState = KeyLayoutState(),
            )
        }
        // Row 1：候选栏
        CandidateListPanel(
            candidates = candidateList.currentPage,
            selectedIndex = 0,
            layoutState = CandidateListLayoutState(
                candidatePositions = TODO(),
                panelSize = TODO()
            ),
            showIndicator = showIndicator,
        )
        // Row 2：输入栏与工具列表互斥切换
        if (inputList.hasPending) {
            InputListPanel(
//                items = inputList.inputs,
//                cursorIndex = inputList.gapIndex,
                layoutState = InputListLayoutState(
                    itemPositions = TODO(),
                    panelSize = TODO()
                ),
                showIndicator = showIndicator,
                inputList = TODO(),
                onLayoutStateChanged = TODO(),
                indicatorState = TODO(),
                modifier = TODO(),
            )
        } else {
            ToolListPanel(
//                toolListState = toolListState,
                layoutState = KeyLayoutState(),
                showIndicator = showIndicator,
                toolList = TODO(),
                onLayoutStateChanged = TODO(),
                indicatorState = TODO(),
                onToolClick = TODO(),
                modifier = TODO(),
            )
        }
        // Row 3：反馈面板 + 输入面板 两层叠加
        Box {
            GestureFeedbackPanel(
                feedbackState = feedbackState,
                keyLayoutState = KeyLayoutState(),
            )
            GestureInputPanel(
                keyLayoutState = KeyLayoutState(),
                onGesture = { viewModel.handleGesture(it) },
            )
        }
    }
}

/** 生成基本的按键布局：根据键盘类型返回对应的 QWERTY/数字/默认布局 */
private fun generateBasicLayout(context: KeyTableContext): List<List<InputKey>> {
    return when (context.keyboard.type) {
        KeyboardType.Pinyin, KeyboardType.Latin -> listOf(
            // 第一行
            listOf(
                InputKey.Char.Alphabet(value = "q"),
                InputKey.Char.Alphabet(value = "w"),
                InputKey.Char.Alphabet(value = "e"),
                InputKey.Char.Alphabet(value = "r"),
                InputKey.Char.Alphabet(value = "t"),
                InputKey.Char.Alphabet(value = "y"),
                InputKey.Char.Alphabet(value = "u"),
                InputKey.Char.Alphabet(value = "i"),
                InputKey.Char.Alphabet(value = "o"),
                InputKey.Char.Alphabet(value = "p"),
            ),
            // 第二行
            listOf(
                InputKey.Char.Alphabet(value = "a"),
                InputKey.Char.Alphabet(value = "s"),
                InputKey.Char.Alphabet(value = "d"),
                InputKey.Char.Alphabet(value = "f"),
                InputKey.Char.Alphabet(value = "g"),
                InputKey.Char.Alphabet(value = "h"),
                InputKey.Char.Alphabet(value = "j"),
                InputKey.Char.Alphabet(value = "k"),
                InputKey.Char.Alphabet(value = "l"),
            ),
            // 第三行（含 Ctrl 功能键）
            listOf(
                InputKey.Ctrl(type = ""),
                InputKey.Char.Alphabet(value = "z"),
                InputKey.Char.Alphabet(value = "x"),
                InputKey.Char.Alphabet(value = "c"),
                InputKey.Char.Alphabet(value = "v"),
                InputKey.Char.Alphabet(value = "b"),
                InputKey.Char.Alphabet(value = "n"),
                InputKey.Char.Alphabet(value = "m"),
                InputKey.Ctrl(type = ""),
            ),
        )

        KeyboardType.Number -> listOf(
            listOf(
                InputKey.Char.Number(actual = 1),
                InputKey.Char.Number(actual = 2),
                InputKey.Char.Number(actual = 3)
            ),
            listOf(
                InputKey.Char.Number(actual = 4),
                InputKey.Char.Number(actual = 5),
                InputKey.Char.Number(actual = 6)
            ),
            listOf(
                InputKey.Char.Number(actual = 7),
                InputKey.Char.Number(actual = 8),
                InputKey.Char.Number(actual = 9)
            ),
            listOf(InputKey.Ctrl(type = ""), InputKey.Char.Number(actual = 0), InputKey.Ctrl(type = "")),
        )

        else -> listOf(
            listOf(
                InputKey.Char.Alphabet(value = "a"),
                InputKey.Char.Alphabet(value = "b"),
                InputKey.Char.Alphabet(value = "c")
            ),
        )
    }
}
