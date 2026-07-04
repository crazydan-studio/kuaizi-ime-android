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

package org.crazydan.studio.app.ime.kuaizi.engine.input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 剪贴板状态的不可变数据模型。
 * 管理当前剪贴板文本、提示显示状态、历史剪贴条目列表和功能开关。
 *
 * @param currentText 当前系统剪贴板文本，null 表示无内容
 * @param showTip 是否在 UI 上显示剪贴板提示
 * @param clips 最近的剪贴条目列表
 * @param disabled 功能是否禁用，由配置门控决定
 */
data class Clipboard(
    val currentText: String? = null,
    val showTip: Boolean = false,
    val clips: List<InputClip> = emptyList(),
    val disabled: Boolean = false,
)

/**
 * 剪贴板条目的数据模型
 * @param text 剪贴文本内容
 * @param type 自动检测的文本类型
 * @param code 条目唯一标识码，用于去重
 */
data class InputClip(
    val text: String,
    val type: InputTextType? = null,
    val code: String = "",
) {
    companion object {
        /**
         * 根据文本内容自动检测类型并创建 InputClip 实例
         * @param text 待检测的文本
         * @return 带类型标注的 InputClip 实例
         */
        suspend fun from(text: String): InputClip = withContext(Dispatchers.Default) {
            InputClip(text = text, type = InputTextType.detect(text))
        }
    }
}

/**
 * 文本语义类型枚举，定义了剪贴板文本的语义类型体系。
 * detect() 方法通过正则表达式优先级链实现自动类型检测。
 */
enum class InputTextType {
    /** 普通文本 */
    Text,

    /** URL 链接 */
    Url,

    /** 邮箱地址 */
    Email,

    /** 手机号码 */
    Phone,

    /** 验证码 */
    Captcha,

    /** 身份证号 */
    IdCard,

    /** 银行卡号 */
    CreditCard,

    /** 地址信息 */
    Address,

    /** HTML 内容 */
    Html, ;

    companion object {
        /**
         * 检测文本类型，按优先级链匹配
         * @param text 待检测文本
         * @return 匹配的文本类型，无法匹配时返回 null
         */
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
