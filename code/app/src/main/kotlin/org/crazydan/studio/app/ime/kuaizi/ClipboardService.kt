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

import android.content.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputClip
import org.crazydan.studio.app.ime.kuaizi.engine.domain.InputTextType

class ClipboardService(
    private val clipboardManager: ClipboardManager,
    private val scope: CoroutineScope,
) {
    private val _clip = MutableStateFlow<InputClip?>(null)
    val clip: StateFlow<InputClip?> = _clip.asStateFlow()

    private val _showTip = MutableStateFlow(false)
    val showTip: StateFlow<Boolean> = _showTip.asStateFlow()

    init {
        scope.launch {
            monitorClipboard()
        }
    }

    private suspend fun monitorClipboard() {
        callbackFlow {
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val clip = clipboardManager.primaryClip
                    ?.getItemAt(0)?.text?.toString()
                if (clip != null) {
                    scope.launch(Dispatchers.Default) {
                        val inputClip = InputClip.from(clip)
                        trySend(inputClip)
                    }
                }
            }
            clipboardManager.addPrimaryClipChangedListener(listener)
            awaitClose {
                clipboardManager.removePrimaryClipChangedListener(listener)
            }
        }.collect { clip ->
            _clip.value = clip
            _showTip.value = clip != null
        }
    }

    fun pasteClip(): String? = _clip.value?.text

    fun extractType(type: InputTextType): String? {
        val clip = _clip.value ?: return null
        return when (type) {
            InputTextType.Captcha -> CAPTCHA_REGEX.find(clip.text)?.groupValues?.get(1)
            InputTextType.Url -> URL_REGEX.find(clip.text)?.value
            InputTextType.Email -> EMAIL_REGEX.find(clip.text)?.value
            InputTextType.Phone -> PHONE_REGEX.find(clip.text)?.value
            InputTextType.IdCard -> ID_CARD_REGEX.find(clip.text)?.value
            InputTextType.CreditCard -> CREDIT_CARD_REGEX.find(clip.text)?.value
            InputTextType.Address -> clip.text
            InputTextType.Html -> clip.text
            InputTextType.Text -> clip.text
        }
    }

    fun dismissTip() {
        _showTip.value = false
    }

    companion object {
        private val URL_REGEX = Regex("""https?://[^\s]+""")
        private val EMAIL_REGEX = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
        private val PHONE_REGEX = Regex("""1[3-9]\d{9}""")
        private val CAPTCHA_REGEX = Regex(
            """(?:验证码|code|码)[^\d]*(\d{4,6})""",
            RegexOption.IGNORE_CASE,
        )
        private val ID_CARD_REGEX = Regex("""\d{17}[\dXx]""")
        private val CREDIT_CARD_REGEX = Regex("""\d{16,19}""")
    }
}
