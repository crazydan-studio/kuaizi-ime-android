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

package org.crazydan.studio.app.ime.kuaizi

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.device.AudioPlayer
import org.crazydan.studio.app.ime.kuaizi.device.HapticPlayer
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEngine
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.dict.provider.InMemoryDictProvider
import org.crazydan.studio.app.ime.kuaizi.engine.domain.EditorInputType
import org.crazydan.studio.app.ime.kuaizi.ui.KeyboardHost
import org.crazydan.studio.app.ime.kuaizi.ui.KeyboardViewModel
import org.crazydan.studio.app.ime.kuaizi.ui.theme.KeyboardTheme
import org.crazydan.studio.app.ime.kuaizi.util.SystemHelper

/**
 * 系统输入法服务入口。
 *
 * 继承 Android 的 [InputMethodService]，作为系统 IME 的薄壳层：
 * - 在 [onCreate] 中初始化引擎、配置存储和输入连接桥接
 * - 在 [onCreateInputView] 中创建 Compose UI 层
 * - 在 [onStartInputView] 中根据编辑器类型启动引擎
 * - 在 [onFinishInputView] 和 [onDestroy] 中清理资源
 *
 * 职责仅限于生命周期管理和组件组装，不包含业务逻辑。
 */
class IMEService : InputMethodService() {
    /** IMEService 级别的协程作用域，用于初始化等异步操作 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 引擎实例，负责所有输入法核心逻辑
    private var engine: ImeEngine? = null

    // 键盘视图模型，连接引擎状态与 Compose UI
    private var viewModel: KeyboardViewModel? = null

    // 输入连接桥接，将引擎输出转发到系统 InputConnection
    private var inputConnectionBridge: ImeEditorBridge? = null

    private var audioPlayer: AudioPlayer? = null
    private var hapticPlayer: HapticPlayer? = null

    // -------------------------------------------------------

    /** 服务创建时初始化引擎、配置存储和桥接。 */
    override fun onCreate() {
        super.onCreate()

        val context = this

        audioPlayer = AudioPlayer(context)
        hapticPlayer = HapticPlayer(context)

        // ------------------
        val configDataStore = initConfigDataStore(context)
        scope.launch {
            initLog(configDataStore, context.filesDir)
        }

        // -------------------
        inputConnectionBridge = InputConnectionBridge(
            { currentInputConnection },
            { ch -> sendKeyChar(ch) }
        )
        scope.launch {
            initEngine(configDataStore, inputConnectionBridge!!)
        }
    }

    /**
     * 创建键盘输入视图，返回 ComposeView 作为 UI 根节点。
     *
     * 注意，输入视图只创建一次。
     */
    override fun onCreateInputView(): View {
        val engine = engine ?: return super.onCreateInputView()

        viewModel = createViewModel(engine)

        // 返回 ComposeView，包裹键盘主题和 UI 宿主
        return ComposeView(this).apply {
            setContent {
                val vm = viewModel ?: return@setContent
                KeyboardTheme(config = vm.config.ui) {
                    KeyboardHost(viewModel = vm)
                }
            }
        }
    }

    // -------------------------------------------------------

    /**
     * 编辑器输入启动时，根据编辑器输入类型配置引擎。
     *
     * 注意，每次弹出键盘时均会被调用。
     */
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)

        val inputType = resolveEditorInputType(editorInfo)

        startInput(inputType)
    }

    /** 切换系统输入法子类型时，重新启动引擎（注意，引擎可能还未创建）。 */
    override fun onCurrentInputMethodSubtypeChanged(newSubtype: android.view.inputmethod.InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)

        // Note：编辑器输入类型始终为 null，以确保不覆盖已识别到的目标类型
        startInput(null)
    }

    // -------------------------------------------------------

    /** 关闭输入：暂时退出编辑，但会恢复编辑。 */
    override fun onFinishInputView(finishingInput: Boolean) {
        engine?.close()

        super.onFinishInputView(finishingInput)
    }

    /**
     * 输入结束：彻底退出编辑。
     *
     * 注意，在 [onCreateInputView] 之前，且熄屏/亮屏也会调用该接口。
     */
    override fun onFinishInput() {
        engine?.exit()

        super.onFinishInput()
    }

    /** 服务销毁时释放引擎资源。 */
    override fun onDestroy() {
        scope.cancel()   // ← 必须取消，否则协程泄漏

        audioPlayer?.release()
        audioPlayer = null
        hapticPlayer?.release()
        hapticPlayer = null

        inputConnectionBridge?.let { engine?.detachEditorBridge(it) }
        inputConnectionBridge = null

        engine?.destroy()
        engine = null

        super.onDestroy()
    }

    // ----------------------------------------------------

    private suspend fun initEngine(
        configDataStore: ConfigDataStore,
        editorBridge: ImeEditorBridge
    ) {
        engine = ImeEngine.create(
            config = configDataStore.getConfig(),
            dictProvider = InMemoryDictProvider(),
        )
        engine!!.attachEditorBridge(editorBridge)

        updateConfigDataStoreWhenEngineConfigUpdated(configDataStore, engine!!)
    }

    private fun createViewModel(engine: ImeEngine): KeyboardViewModel =
        KeyboardViewModel.Option(
            engine = engine,
            //
            playAudio = { type -> audioPlayer?.play(type) },
            playHaptic = { type -> hapticPlayer?.play(type) },
            //
            switchIme = { SystemHelper.switchIme(this) },
            closeKeyboard = {
                // Note：
                // - 在高版本 Android 中，hideWindow 可能无法隐藏窗口，原因未知
                // - 若发送退出按键（KeyEvent.KEYCODE_BACK）消息，其行为由编辑器决定，可能会导致已输入内容丢失
                // - hideWindow 将自动调用当前 class 重载的 onFinishInputView 接口，并进而 close engine
                hideWindow()
            },
        ).let {
            KeyboardViewModel(it)
        }

    private fun startInput(inputType: EditorInputType?) {
        val subtype = SystemHelper.getInputMethodSubtype(this)
        val orientation = SystemHelper.getScreenOrientation(this)

        ImeConfig.Startup(
            inputMethodSubtype = subtype,
            screenOrientation = orientation,
            editorInputType = inputType,
        ).also {
            engine?.start(it)
        }
    }

    // ----------------------------------------------------

    /**
     * 从 [EditorInfo] 解析 [EditorInputType]。
     *
     * 根据 Android 输入类型掩码判断目标编辑器的输入类型，
     * 用于决定键盘布局（如密码框显示拉丁键盘、数字框显示数字键盘）。
     */
    private fun resolveEditorInputType(editorInfo: EditorInfo?): EditorInputType? {
        if (editorInfo == null) return null

        return when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            // 数字/电话/日期类输入
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_PHONE ->
                EditorInputType.Number

            EditorInfo.TYPE_CLASS_DATETIME ->
                EditorInputType.Datetime

            // 文本类输入，进一步按变体区分
            EditorInfo.TYPE_CLASS_TEXT -> {
                when (editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION) {
                    // 密码框：禁用按键提示，强制拉丁键盘
                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ->
                        EditorInputType.Password

                    // 邮箱输入
                    EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ->
                        EditorInputType.Email

                    // URI 输入
                    EditorInfo.TYPE_TEXT_VARIATION_URI ->
                        EditorInputType.URI

                    // 搜索过滤输入
                    EditorInfo.TYPE_TEXT_VARIATION_FILTER ->
                        EditorInputType.Filter

                    // 普通文本兜底
                    else ->
                        if (editorInfo.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE != 0) {
                            EditorInputType.AutoComplete
                        } else {
                            EditorInputType.Text
                        }
                }
            }

            else -> EditorInputType.Text
        }
    }
}
