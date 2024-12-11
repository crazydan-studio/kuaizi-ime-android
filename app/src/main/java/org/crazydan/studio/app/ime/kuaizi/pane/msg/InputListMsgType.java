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

import org.crazydan.studio.app.ime.kuaizi.pane.InputList;

/**
 * {@link InputListMsg} 消息的类型
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-21
 */
public enum InputListMsgType {
    /** 输入选择中 */
    Input_Choose_Doing,
    /** 输入已选中 */
    Input_Choose_Done,

    /** {@link InputList#getPending 待输入}已丢弃 */
    Input_Pending_Drop_Done,
    /** {@link InputList#getSelected 当前选中的输入}已删除 */
    Input_Selected_Delete_Done,

    /** 输入已清空 */
    Inputs_Clean_Done,
    /** 已撤销对输入的清空操作 */
    Inputs_Cleaned_Cancel_Done,

    /** 输入补全已更新 */
    Input_Completion_Update_Done,
    /** 输入补全已清除 */
    Input_Completion_Clean_Done,
    /** 输入补全已应用 */
    Input_Completion_Apply_Done,
}
