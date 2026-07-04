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

package org.crazydan.studio.app.ime.kuaizi.ui.domain

enum class AudioType {
    /** 单击音 */
    SingleTick,

    /** 双击音 */
    DoubleTick,

    /** 时钟走时音 */
    ClockTick,

    /** 敲击音 */
    KnockTick,

    /** 翻页音 */
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
