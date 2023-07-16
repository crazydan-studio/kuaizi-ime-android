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

/** 键盘输入消息 */
public enum InputMsg {
    /** 字符输入中 */
    InputtingChars,
    /** 选择输入候选字中 */
    ChoosingInputCandidate,
    /** 字符已输入结束 */
    InputtingCharsDone,
    /** 提交输入内容到输入目标 */
    InputCommitting,
    /** 向后删除输入目标中的输入内容 */
    InputBackwardDeleting,
    /** 切换输入法 */
    SwitchingIME,
}
