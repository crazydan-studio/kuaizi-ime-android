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

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import org.crazydan.studio.app.ime.kuaizi.config.ConfigDataStore
import org.crazydan.studio.app.ime.kuaizi.engine.EditorInputType
import org.crazydan.studio.app.ime.kuaizi.engine.IMESubtype
import org.crazydan.studio.app.ime.kuaizi.engine.ImeConfig
import org.crazydan.studio.app.ime.kuaizi.engine.ImeEngine
import org.crazydan.studio.app.ime.kuaizi.engine.ScreenOrientation
import org.crazydan.studio.app.ime.kuaizi.engine.StartupConfig
import org.crazydan.studio.app.ime.kuaizi.engine.bridge.ImeEditorBridge
import org.crazydan.studio.app.ime.kuaizi.engine.logging.ImeLog
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogLevel
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogStorage
import org.crazydan.studio.app.ime.kuaizi.engine.logging.LogWriter
import org.crazydan.studio.app.ime.kuaizi.engine.logging.writer.FileLogWriter
import org.crazydan.studio.app.ime.kuaizi.engine.logging.writer.LogcatWriter
import org.crazydan.studio.app.ime.kuaizi.ui.integration.KeyboardHost
import org.crazydan.studio.app.ime.kuaizi.ui.theme.KeyboardTheme
import org.crazydan.studio.app.ime.kuaizi.ui.viewmodel.KeyboardViewModel

class IMEService : InputMethodService() {
    private var engine: ImeEngine? = null
    private var viewModel: KeyboardViewModel? = null
    private var inputConnectionBridge: ImeEditorBridge? = null
    private var configDataStore: ConfigDataStore? = null

    override fun onCreate() {
        super.onCreate()

        initLog(this)

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
                val vm = viewModel ?: return@setContent
                KeyboardTheme(config = vm.config.ui) {
                    KeyboardHost(viewModel = vm)
                }
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

    private fun initLog(context: Context) {
        var level = LogLevel.ERROR
        val writers = mutableListOf<LogWriter>()

        if (BuildConfig.DEBUG) {
            level = LogLevel.DEBUG
            writers.add(LogcatWriter())
        } else {
            val dir = context.filesDir.resolve("logs")
            val storage = LogStorage(dir)
            writers.add(FileLogWriter(storage))

            ImeLog.CrashInterceptor(storage).install()
        }

        ImeLog.init(
            level = level,
            writers = writers,
        )
    }
}
