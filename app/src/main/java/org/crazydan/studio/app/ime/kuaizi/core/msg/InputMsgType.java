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

import org.crazydan.studio.app.ime.kuaizi.core.InputList;

/** {@link InputMsg} 消息的类型 */
public enum InputMsgType {
    /** 输入音效播放中 */
    InputAudio_Play_Doing,

    /** 字符输入中 */
    InputChars_Input_Doing,
    /** 字符输入已结束 */
    InputChars_Input_Done,
    /** 输入提示气泡显示中 */
    InputChars_Input_Popup_Show_Doing,
    /** 输入提示气泡隐藏中 */
    InputChars_Input_Popup_Hide_Doing,

    /** 输入候选字选择中 */
    InputCandidate_Choose_Doing,
    /** 输入候选字已选择 */
    InputCandidate_Choose_Done,

    /** 输入选择中 */
    Input_Choose_Doing,
    /** 输入已选中 */
    Input_Choose_Done,
    /** {@link InputList#getPending 待输入}已丢弃 */
    Input_Pending_Drop_Done,
    /** {@link InputList#getSelected 当前已选中输入}已删除 */
    Input_Selected_Delete_Done,

    /** 输入补全已生成 */
    InputCompletion_Create_Done,
    /** 输入补全已应用 */
    InputCompletion_Apply_Done,

    /** 输入已清空 */
    InputList_Clean_Done,
    /** 已撤销对输入的清空操作 */
    InputList_Cleaned_Cancel_Done,
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
    /** 键盘左右手模式切换中 */
    Keyboard_HandMode_Switch_Doing,
    /** 键盘左右手模式已切换 */
    Keyboard_HandMode_Switch_Done,
    /** 键盘主题已切换 */
    Keyboard_Theme_Switch_Done,
    /** 键盘切换中 */
    Keyboard_Switch_Doing,
    /** 键盘已切换 */
    Keyboard_Switch_Done,
    /** 键盘启动中 */
    Keyboard_Start_Doing,
    /** 键盘已启动 */
    Keyboard_Start_Done,
    /** 键盘关闭中 */
    Keyboard_Close_Doing,
    /** 键盘已关闭 */
    Keyboard_Close_Done,
    /** 键盘已退出 */
    Keyboard_Exit_Done,
    /** X 型输入键盘的演示已被终止：仅用于发送演示终止消息 */
    Keyboard_XPad_Simulation_Terminated,

    /** 输入法切换中 */
    IME_Switch_Doing,
    /**
     * （系统）配置已更新
     * <p/>
     * - 对 {@link org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey#theme theme}
     * 的变更将转换为消息 {@link #Keyboard_Theme_Switch_Done}；
     * - 对 {@link org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey#hand_mode hand_mode}
     * 的变更将转换为消息 {@link #Keyboard_HandMode_Switch_Done}；
     */
    Config_Update_Done,

    /** 剪贴数据已生成 */
    InputClip_Create_Done,
    /** 剪贴数据已应用 */
    InputClip_Apply_Done,
    /** 剪贴数据已废弃 */
    InputClip_Discard_Done,
    /** 剪贴数据可以被收藏 */
    InputClip_CanBe_Favorite,
    /** 剪贴文本提交中：将文本写入到 目标编辑器 中 */
    InputClip_Text_Commit_Doing,

    /** 已收藏输入已就绪 */
    InputFavorite_Be_Ready,
    /** 已收藏输入已粘贴 */
    InputFavorite_Paste_Done,
    /** 已收藏输入已删除 */
    InputFavorite_Delete_Done,
    /** 已收藏输入已保存 */
    InputFavorite_Save_Done,
    /** 已收藏输入文本提交中：将文本写入到 目标编辑器 中 */
    InputFavorite_Text_Commit_Doing,
}
