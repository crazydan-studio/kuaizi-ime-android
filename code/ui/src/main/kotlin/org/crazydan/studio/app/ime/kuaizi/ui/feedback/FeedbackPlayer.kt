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

package org.crazydan.studio.app.ime.kuaizi.ui.feedback

/**
 * 感官反馈播放器接口。
 *
 * 定义播放音效和触觉振动的通用契约。
 * 平台实现由 `:app` 模块提供。
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

enum class AudioType {
    /** 按键音：用户点击按键时播放 */
    KeyPress,

    /** 候选选择音：用户从候选列表选择候选词时播放 */
    CandidateSelect,

    /** 滑行输入音：用户滑行输入识别完成时播放 */
    Slip,

    /** 翻页音：候选列表翻页时播放 */
    PageFlip,
}

enum class HapticType {
    /** 轻触反馈：按键点击、候选选择等 */
    LightTap,

    /** 中等反馈：滑行识别完成、翻页等 */
    MediumTap,

    /** 重触反馈：长按触发上下文菜单等 */
    HeavyTap,
}
