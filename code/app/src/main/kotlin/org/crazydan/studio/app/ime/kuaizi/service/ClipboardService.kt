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

package org.crazydan.studio.app.ime.kuaizi.service

import android.content.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputClip
import org.crazydan.studio.app.ime.kuaizi.engine.input.InputTextType

/**
 * 剪贴板服务。
 *
 * 监听系统剪贴板变更，维护当前剪贴内容状态，提供类型化提取能力。
 * 使用 [callbackFlow] 将 Android 的 [ClipboardManager.OnPrimaryClipChangedListener]
 * 转换为 Kotlin Flow，在注入的协程作用域中持续监听。
 *
 * 通过 [clip] 和 [showTip] 两个 StateFlow 对外暴露状态。
 */
class ClipboardService(
    /** 系统剪贴板管理器 */
    private val clipboardManager: ClipboardManager,
    /** 协程作用域，控制监听生命周期 */
    private val scope: CoroutineScope,
) {
    // 当前剪贴内容，null 表示无内容
    private val _clip = MutableStateFlow<InputClip?>(null)
    val clip: StateFlow<InputClip?> = _clip.asStateFlow()

    // 是否显示剪贴板提示
    private val _showTip = MutableStateFlow(false)
    val showTip: StateFlow<Boolean> = _showTip.asStateFlow()

    init {
        // 启动剪贴板监听协程
        scope.launch {
            monitorClipboard()
        }
    }

    /**
     * 监听系统剪贴板变更。
     *
     * 通过 [callbackFlow] 将监听器回调转换为 Flow，
     * 每当剪贴板内容变化时，创建 [InputClip] 实例并发射。
     */
    private suspend fun monitorClipboard() {
        callbackFlow {
            // 注册系统剪贴板变更监听器
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                val clip = clipboardManager.primaryClip
                    ?.getItemAt(0)?.text?.toString()
                if (clip != null) {
                    // 在 Default 调度器上执行类型检测，避免阻塞主线程
                    scope.launch(Dispatchers.Default) {
                        val inputClip = InputClip.from(clip)
                        trySend(inputClip)
                    }
                }
            }
            clipboardManager.addPrimaryClipChangedListener(listener)
            // 协程取消时移除监听器，避免内存泄漏
//            awaitClose {
//                clipboardManager.removePrimaryClipChangedListener(listener)
//            }
        }.collect { clip ->
            // 更新状态并显示提示
            _clip.value = clip
            _showTip.value = clip != null
        }
    }

    /** 获取当前剪贴文本内容，用于粘贴操作。 */
    fun pasteClip(): String? = _clip.value?.text

    /**
     * 从当前剪贴内容中提取指定类型的结构化数据。
     *
     * 根据 [InputTextType] 从剪贴文本中提取对应的数据片段，
     * 例如从验证码文本中提取数字部分，从 URL 中提取完整链接。
     * 类型不匹配或无剪贴内容时返回 null。
     */
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

    /** 关闭剪贴板提示。 */
    fun dismissTip() {
        _showTip.value = false
    }

    companion object {
        // 各文本类型的正则表达式，用于类型检测和提取
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
