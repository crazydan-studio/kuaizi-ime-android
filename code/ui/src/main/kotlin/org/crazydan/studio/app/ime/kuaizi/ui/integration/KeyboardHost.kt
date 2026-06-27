package org.crazydan.studio.app.ime.kuaizi.ui.integration

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
import org.crazydan.studio.app.ime.kuaizi.engine.domain.CandidateList
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputKey
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputList
import org.crazydan.studio.app.ime.kuaizi.engine.domain.Keyboard
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.CandidateListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.InputListLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyLayoutState
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableContext
import org.crazydan.studio.app.ime.kuaizi.ui.keyboard.KeyTableGenerator
import org.crazydan.studio.app.ime.kuaizi.ui.panel.CandidateListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.GestureFeedbackPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.GestureInputPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.InputListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.KeyLayoutPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.PopupTipPanel
import org.crazydan.studio.app.ime.kuaizi.ui.panel.ToolListPanel
import org.crazydan.studio.app.ime.kuaizi.ui.theme.KeyboardTheme
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.GestureFeedbackState
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardLayoutMode
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.PopupTipState

@Composable
fun KeyboardHost(
    viewModel: KeyboardViewModel,
    showIndicator: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state

    val keyboard by remember { derivedStateOf { state.value.keyboard } }
    val inputList by remember { derivedStateOf { state.value.inputList } }
    val candidateList by remember { derivedStateOf { state.value.candidateList } }
    val config by remember { derivedStateOf { state.value.config } }

    val layoutMode by viewModel.layoutMode.collectAsState()
    val popupTipState by viewModel.popupTipState.collectAsState()
    val toolListState by viewModel.toolListState.collectAsState()
    val feedbackState = viewModel.feedbackState

    val keyTableContext = remember(keyboard, inputList, candidateList, config) {
        KeyTableContext(
            config = config,
            keyboard = keyboard,
            inputList = inputList,
            candidateList = candidateList,
        )
    }
    val keyTableGenerator = remember {
        object : KeyTableGenerator {
            override fun generate(context: KeyTableContext): List<List<InputKey>> {
                return emptyList()
            }
        }
    }
    val keyTable = remember(keyTableContext, keyTableGenerator) {
        keyTableGenerator.generate(keyTableContext)
    }

    KeyboardTheme(config.ui) {
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
                keyTableGenerator = keyTableGenerator,
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
                keyTableGenerator = keyTableGenerator,
                keyTableContext = keyTableContext,
                modifier = modifier,
            )
        }
    }
}

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
        Box {
            CandidateListPanel(
                candidates = candidateList.currentPage,
                selectedIndex = 0,
                layoutState = CandidateListLayoutState(),
                showIndicator = showIndicator,
            )
            PopupTipPanel(
                tipState = popupTipState,
                onAction = { viewModel.handleIntent(it) },
            )
        }
        if (inputList.hasPending) {
            InputListPanel(
                items = inputList.inputs,
                cursorIndex = inputList.gapIndex,
                layoutState = InputListLayoutState(),
                showIndicator = showIndicator,
            )
        } else {
            ToolListPanel(
                toolListState = toolListState,
                layoutState = KeyLayoutState(),
                showIndicator = showIndicator,
            )
        }
        Box {
            KeyLayoutPanel(
                keyTable = keyTable,
                generator = keyTableGenerator,
                context = keyTableContext,
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
        Box {
            KeyLayoutPanel(
                keyTable = keyTable,
                generator = keyTableGenerator,
                context = keyTableContext,
                keyboardInputMode = config.ui.keyboardInputMode,
                keyLayoutState = KeyLayoutState(),
                onLayoutStateChanged = { viewModel.updateKeyLayoutState(it) },
            )
            GestureFeedbackPanel(
                feedbackState = feedbackState,
                keyLayoutState = KeyLayoutState(),
            )
        }
        CandidateListPanel(
            candidates = candidateList.currentPage,
            selectedIndex = 0,
            layoutState = CandidateListLayoutState(),
            showIndicator = showIndicator,
        )
        if (inputList.hasPending) {
            InputListPanel(
                items = inputList.inputs,
                cursorIndex = inputList.gapIndex,
                layoutState = InputListLayoutState(),
                showIndicator = showIndicator,
            )
        } else {
            ToolListPanel(
                toolListState = toolListState,
                layoutState = KeyLayoutState(),
                showIndicator = showIndicator,
            )
        }
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
