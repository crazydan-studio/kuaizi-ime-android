package org.crazydan.studio.app.ime.kuaizi

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import org.crazydan.studio.app.ime.kuaizi.config.ConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.*
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogcatWriter
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel

class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var viewModel: KeyboardViewModel? = null
    private var inputConnectionBridge: ImeEditorBridge? = null
    private var configDataStore: ConfigDataStore? = null

    override fun onCreate() {
        super.onCreate()

        ImeLog.init(
            level = LogLevel.DEBUG,
            writers = listOf(LogcatWriter()),
        )

        configDataStore = ConfigDataStore(this)
        inputConnectionBridge = InputConnectionBridge { currentInputConnection }

        engine = ImeEngine.create(
            config = ImeConfig(),
            dictProvider = InMemoryDictProvider(),
        )
        engine?.attachEditorBridge(inputConnectionBridge!!)
    }

    override fun onCreateInputView(): View {
        val engine = engine ?: return super.onCreateInputView()

        viewModel = KeyboardViewModel(engine)

        return ComposeView(this).apply {
            setContent {
                // TODO: Mount KeyboardHost with viewModel
            }
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)

        val subtype = when (val inputType = editorInfo?.inputType) {
            // Determine from input type
            else -> IMESubtype.Hans
        }

        engine?.start(
            StartupConfig(
                imeSubtype = subtype,
                screenOrientation = ScreenOrientation.Portrait,
                editorInputType = resolveEditorInputType(editorInfo),
            )
        )
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        engine?.close()
    }

    override fun onDestroy() {
        engine?.destroy()
        super.onDestroy()
    }

    private fun resolveEditorInputType(editorInfo: EditorInfo?): EditorInputType? {
        if (editorInfo == null) return null
        return when (editorInfo.inputType and EditorInfo.TYPE_MASK_CLASS) {
            EditorInfo.TYPE_CLASS_NUMBER, EditorInfo.TYPE_CLASS_PHONE -> EditorInputType.Number
            EditorInfo.TYPE_CLASS_DATETIME -> EditorInputType.Datetime
            EditorInfo.TYPE_CLASS_TEXT -> {
                when (editorInfo.inputType and EditorInfo.TYPE_MASK_VARIATION) {
                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD -> EditorInputType.Password
                    EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> EditorInputType.Email
                    EditorInfo.TYPE_TEXT_VARIATION_URI -> EditorInputType.URI
                    EditorInfo.TYPE_TEXT_VARIATION_FILTER -> EditorInputType.Filter
                    else -> EditorInputType.Text
                }
            }
            else -> EditorInputType.Text
        }
    }
}
