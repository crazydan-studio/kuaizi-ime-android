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

package org.crazydan.studio.app.ime.kuaizi.engine


/** 屏幕方向 */
enum class ScreenOrientation {
    /** 横屏 */
    Landscape,

    /** 竖屏 */
    Portrait
}

/**
 * 编辑器输入类型。
 *
 * 决定输入法启动时的键盘类型以及 Enter 按键的图标样式。
 */
enum class EditorInputType {
    /** 搜索框输入 */
    Filter,

    /** 自动填充 */
    AutoComplete,

    /** 数字输入 */
    Number,

    /** 日期输入 */
    Datetime,

    /** 电话输入 */
    Phone,

    /** 密码输入 */
    Password,

    /** 邮件输入 */
    Email,

    /** url 地址输入 */
    URI,

    /** 普通文本输入，在无法精确识别输入类型时，均采用该类型 */
    Text
}

/** 系统输入法子类型（Input Method Subtype）。 */
enum class InputMethodSubtype {
    /** 英文 */
    Latin,

    /** 中文 */
    Hans
}
