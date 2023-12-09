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

package org.crazydan.studio.app.ime.kuaizi.core.msg;

import org.crazydan.studio.app.ime.kuaizi.core.InputList;

/** 输入消息 */
public enum InputMsg {
    /** 输入音效播放中 */
    InputAudio_Play_Doing,

    /** 字符输入中 */
    InputChars_Input_Doing,
    /** 输入提示气泡显示中 */
    InputChars_Input_Popup_Show_Doing,
    /** 输入提示气泡隐藏中 */
    InputChars_Input_Popup_Hide_Doing,
    /** 字符输入已结束 */
    InputChars_Input_Done,

    /** 输入候选字选择中 */
    InputCandidate_Choose_Doing,
    /** 输入候选字已选择 */
    InputCandidate_Choose_Done,

    /** 输入列表已更新 */
    InputList_Update_Done,
    /** 输入列表中的输入已选择 */
    InputList_Input_Choose_Done,
    /** 输入补全已更新 */
    InputList_Input_Completion_Update_Done,
    /** 输入补全已应用 */
    InputList_Input_Completion_Apply_Done,
    /** 输入列表的{@link InputList#getPending 待输入}已丢弃 */
    InputList_Pending_Drop_Done,
    /** 输入列表的{@link InputList#getSelected 当前选中的输入}已删除 */
    InputList_Selected_Delete_Done,
    /** 输入列表提交中：将输入内容写入到 目标编辑器 中 */
    InputList_Commit_Doing,
    /** 已提交输入列表撤回中 */
    InputList_Committed_Revoke_Doing,
    /** 输入列表中的 配对符号 提交中：将输入内容写入到 目标编辑器 中 */
    InputList_PairSymbol_Commit_Doing,

    /** 定位 目标编辑器 的光标 */
    Editor_Cursor_Move_Doing,
    /** 选择 目标编辑器 的内容 */
    Editor_Range_Select_Doing,
    /** 编辑 目标编辑器 */
    Editor_Edit_Doing,

    /** 键盘状态已更新 */
    Keyboard_State_Change_Done,
    /** 键盘配置已更新 */
    Keyboard_Config_Update_Done,
    /** 键盘主题样式已更新 */
    Keyboard_Theme_Update_Done,
    /** 键盘左右手模式切换中 */
    Keyboard_HandMode_Switch_Done,
    /** 键盘切换中 */
    Keyboard_Switch_Doing,
    /** 键盘已切换 */
    Keyboard_Switch_Done,
    /** X 型输入键盘的演示已被终止：仅用于发送演示终止消息 */
    Keyboard_XPad_Simulation_Terminated,

    /** 输入法切换中 */
    IME_Switch_Doing,

    /** 表情符号选择中 */
    Emoji_Choose_Doing,
    /** 标点符号选择中 */
    Symbol_Choose_Doing,
}
