/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import org.crazydan.studio.app.ime.kuaizi.core.Key;

/**
 * {@link UserKeyMsg} 消息的类型
 */
public enum UserKeyMsgType {
    /** 按压 {@link Key} 开始 */
    Press_Key_Start,
    /** 按压 {@link Key} 结束 */
    Press_Key_Stop,

    /** 长按 {@link Key} 开始 */
    LongPress_Key_Start,
    /** 长按 {@link Key} 的 tick */
    LongPress_Key_Tick,
    /** 长按 {@link Key} 结束 */
    LongPress_Key_Stop,

    /** 单击 {@link Key} */
    SingleTap_Key,
    /** 双击 {@link Key} */
    DoubleTap_Key,

    /** 手指移动开始 */
    FingerMoving_Start,
    /** 手指移动 */
    FingerMoving,
    /** 手指移动结束 */
    FingerMoving_Stop,
    /** 手指翻动 */
    FingerFlipping,
}
