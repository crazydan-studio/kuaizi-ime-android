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

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;

/**
 * 用户对 {@link Input} 操作的消息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-07
 */
public enum UserInputMsgType {
    /** 单击 {@link Input} */
    SingleTap_Input,

    /** 单击 {@link InputCompletion} */
    SingleTap_InputCompletion,

    /** 单击 输入列表清空 的按钮 */
    SingleTap_Btn_Clean_InputList,
    /** 单击 撤销 输入列表清空 的按钮 */
    SingleTap_Btn_Cancel_Clean_InputList,

    /** 单击 隐藏键盘 的按钮 */
    SingleTap_Btn_Hide_Keyboard,
}
