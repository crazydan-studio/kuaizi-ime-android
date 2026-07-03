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

package org.crazydan.studio.app.ime.kuaizi.ui

import org.crazydan.studio.app.ime.kuaizi.ui.effect.AudioType
import org.crazydan.studio.app.ime.kuaizi.ui.effect.HapticType

/**
 * 感官反馈播放器接口。
 *
 * 定义播放音效和触觉振动的通用契约。
 * 平台实现（如 [AndroidAudioPlayer]、[AndroidHapticPlayer]）由 `:app` 模块提供。
 *
 * @param T 播放类型（[AudioType] 或 [HapticType]）
 */
interface FeedbackPlayer<T> {
    /** 播放指定类型的反馈 */
    fun play(type: T)
}

/** 音频播放器类型别名 */
typealias AudioPlayer = FeedbackPlayer<AudioType>

/** 触觉振动播放器类型别名 */
typealias HapticPlayer = FeedbackPlayer<HapticType>
