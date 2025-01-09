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

package org.crazydan.studio.app.ime.kuaizi.core.msg.user;

import org.crazydan.studio.app.ime.kuaizi.core.InputList;
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
     * 在 {@link InputList} 嵌套时，当前输入所在的输入列表在上层输入列表中的位置
     * <p/>
     * 若为 <code>-1</code>，则表示当前输入所在的输入列表未被嵌套
     */
    public final int positionInParent;
    /**
     * 目标输入所在的位置：<ul>
     * <li>- <code>-1</code>: 在 {@link InputList} 列表中的末尾；</li>
     * <li>- 其他: 在 {@link InputList} 列表中的实际序号；</li>
     * </ul>
     */
    public final int position;

    public UserInputSingleTapMsgData(int positionInParent, int position) {
        this.positionInParent = positionInParent;
        this.position = position;
    }
}
