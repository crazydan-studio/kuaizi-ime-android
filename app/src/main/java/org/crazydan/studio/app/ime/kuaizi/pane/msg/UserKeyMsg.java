/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.crazydan.studio.app.ime.kuaizi.pane.msg;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 用户对 {@link Key} 操作的消息
 * <p/>
 * 此类消息与视图做一对一绑定，再通过视图转发消息给与其关联的键盘对象等处理，
 * 故而，不需要做全局消息管理
 */
public enum UserKeyMsg {
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
