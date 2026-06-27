package org.crazydan.studio.app.ime.kuaizi.engine.domain

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
)

enum class InputTextType {
    Captcha, CreditCard, IdCard, Phone, Email, Url, Address, Html, Text;

    companion object {
        fun detect(text: String): InputTextType {
            return entries.firstOrNull { type ->
                when (type) {
                    Captcha -> text.matches(Regex("^\\d{4,6}$"))
                    CreditCard -> text.matches(Regex("^\\d{13,19}$"))
                    IdCard -> text.matches(Regex("^\\d{17}[\\dXx]$"))
                    Phone -> text.matches(Regex("^1[3-9]\\d{9}$"))
                    Email -> text.contains("@")
                    Url -> text.matches(Regex("^https?://", RegexOption.IGNORE_CASE))
                    Address -> text.contains(Regex("省|市|区|路|号"))
                    Html -> text.matches(Regex("^\\s*<", RegexOption.IGNORE_CASE))
                    Text -> true
                }
            } ?: Text
        }
    }
}
