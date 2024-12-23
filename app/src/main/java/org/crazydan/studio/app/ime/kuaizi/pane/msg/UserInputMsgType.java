/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;

/**
 * 用户对 {@link Input} 操作的消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-07
 */
public enum UserInputMsgType {
    /** 单击 {@link Input} */
    SingleTap_Input,

    /** 单击 {@link CompletionInput} */
    SingleTap_CompletionInput,

    /** 单击 输入列表清空 的按钮 */
    SingleTap_Btn_Clean_InputList,

    /** 单击 撤销 输入列表清空 的按钮 */
    SingleTap_Btn_Cancel_Clean_InputList,
}