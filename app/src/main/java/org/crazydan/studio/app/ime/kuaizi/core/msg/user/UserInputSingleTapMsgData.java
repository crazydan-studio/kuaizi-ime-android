/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.core.msg.user;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType;

/**
 * {@link UserInputMsgType#SingleTap_Input} 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-05
 */
public class UserInputSingleTapMsgData extends UserInputMsgData {
    /**
     * 目标输入所在的位置：<ul>
     * <li>- <code>-1</code>: 在 {@link InputList} 列表中的末尾；</li>
     * <li>- 其他: 在 {@link InputList} 列表中的实际序号；</li>
     * </ul>
     */
    public final int position;

    public UserInputSingleTapMsgData(int position) {
        this.position = position;
    }
}
