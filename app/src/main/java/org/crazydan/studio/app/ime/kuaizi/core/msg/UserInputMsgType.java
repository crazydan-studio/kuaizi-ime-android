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
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
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
    /** 单击 {@link InputClip} */
    SingleTap_InputClip,
    /** 双击 {@link InputFavorite} */
    DoubleTap_InputFavorite,

    /** 单击 输入列表清空 的按钮 */
    SingleTap_Btn_Clean_InputList,
    /** 单击 撤销 输入列表清空 的按钮 */
    SingleTap_Btn_Cancel_Clean_InputList,

    /** 单击 关闭键盘 的按钮 */
    SingleTap_Btn_Close_Keyboard,
    /** 单击 打开已收藏 的按钮 */
    SingleTap_Btn_Open_Favoriteboard,
    /** 单击 关闭已收藏 的按钮 */
    SingleTap_Btn_Close_Favoriteboard,

    /** 单击 选中 {@link InputFavorite} 的按钮 */
    SingleTap_Btn_Select_InputFavorite,
    /** 单击 粘贴 {@link InputFavorite} 的按钮 */
    SingleTap_Btn_Paste_InputFavorite,
    /** 单击 删除已选中的 {@link InputFavorite} 的按钮 */
    SingleTap_Btn_Delete_Selected_InputFavorite,
    /** 单击 清空全部 {@link InputFavorite} 的按钮 */
    SingleTap_Btn_Clear_All_InputFavorite,
    /** 单击 收藏数据 的按钮 */
    SingleTap_Btn_Save_As_Favorite,
}
