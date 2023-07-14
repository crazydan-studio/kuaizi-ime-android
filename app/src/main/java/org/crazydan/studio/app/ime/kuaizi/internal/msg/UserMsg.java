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

package org.crazydan.studio.app.ime.kuaizi.internal.msg;

/** 用户操作消息 */
public enum UserMsg {
    /** 按键长按开始 */
    KeyLongPressStart,
    /** 按键长按结束 */
    KeyLongPressEnd,
    /** 单击按键 */
    KeySingleTap,
    /** 手指移动 */
    FingerMoving,
    /** 手指滑动 */
    FingerSlipping,
}
