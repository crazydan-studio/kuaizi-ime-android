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

/** 输入消息 */
public enum InputMsg {
    /** 字符输入进行中 */
    InputChars_Inputting,
    /** 字符输入已结束 */
    InputChars_InputtingEnd,

    /** 输入候选字选择中 */
    InputCandidate_Choosing,
    /** 输入音效播放中 */
    InputAudio_Playing,

    /** 输入列表提交中：录入到目标输入组件中 */
    InputList_Committing,

    /** 定位输入目标的光标 */
    InputTarget_Cursor_Locating,
    /** 回删输入目标中的内容 */
    InputTarget_Backspacing,
    /** 选择输入目标的内容 */
    InputTarget_Selecting,
    /** 复制输入目标的内容 */
    InputTarget_Copying,
    /** 粘贴内容到输入目标 */
    InputTarget_Pasting,
    /** 剪切输入目标的内容 */
    InputTarget_Cutting,
    /** 撤销对输入目标的变更 */
    InputTarget_Undoing,
    /** 重做对输入目标的变更 */
    InputTarget_Redoing,

    /** 输入法切换中 */
    IME_Switching,
    /** 键盘切换中 */
    Keyboard_Switching,
    /** 左右手模式切换中 */
    HandMode_Switching,
}
