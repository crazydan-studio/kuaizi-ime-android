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

package org.crazydan.studio.app.ime.kuaizi.engine.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Clipboard(
    val currentText: String? = null,
    val showTip: Boolean = false,
    val clips: List<InputClip> = emptyList(),
    val disabled: Boolean = false,
)

data class InputClip(
    val text: String,
    val type: InputTextType? = null,
    val code: String = "",
) {
    companion object {
        suspend fun from(text: String): InputClip = withContext(Dispatchers.Default) {
            InputClip(text = text, type = InputTextType.detect(text))
        }
    }
}

enum class InputTextType {
    Text,
    Url,
    Email,
    Phone,
    Captcha,
    IdCard,
    CreditCard,
    Address,
    Html,;

    companion object {
        fun detect(text: String): InputTextType? {
            return when {
                CAPTCHA_REGEX.matches(text) -> Captcha
                CREDIT_CARD_REGEX.matches(text) -> CreditCard
                ID_CARD_REGEX.matches(text) -> IdCard
                PHONE_REGEX.matches(text) -> Phone
                EMAIL_REGEX.matches(text) -> Email
                URL_REGEX.matches(text) -> Url
                ADDRESS_KEYWORD_REGEX.containsMatchIn(text) -> Address
                HTML_TAG_REGEX.containsMatchIn(text) -> Html
                else -> null
            }
        }

        private val URL_REGEX = Regex("""https?://[^\s]+""")
        private val EMAIL_REGEX = Regex("""[\w.+-]+@[\w-]+\.[\w.-]+""")
        private val PHONE_REGEX = Regex("""1[3-9]\d{9}""")
        private val CAPTCHA_REGEX = Regex(
            """(?:验证码|code|码)[^\d]*(\d{4,6})""",
            RegexOption.IGNORE_CASE,
        )
        private val ID_CARD_REGEX = Regex("""\d{17}[\dXx]""")
        private val CREDIT_CARD_REGEX = Regex("""\d{16,19}""")
        private val ADDRESS_KEYWORD_REGEX = Regex(
            """(?:省|市|区|县|镇|乡|村|路|街|道|号|栋|楼|室|小区|花园|广场)""",
        )
        private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    }
}
