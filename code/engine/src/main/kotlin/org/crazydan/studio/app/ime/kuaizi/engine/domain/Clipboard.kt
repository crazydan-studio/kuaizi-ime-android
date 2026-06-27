package org.crazydan.studio.app.ime.kuaizi.engine.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Clipboard(
    val currentText: String? = null,
    val inputTextType: InputTextType? = null,
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
