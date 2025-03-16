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

import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType;

/**
 * {@link UserInputMsgType#SingleTap_Btn_Paste_InputFavorite} 的消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-14
 */
public class UserInputFavoriteMsgData extends UserInputMsgData {
    /** 触发消息的数据项位置 */
    public final int position;
    /** 与消息相关的 {@link InputFavorite} */
    public final InputFavorite favorite;

    public UserInputFavoriteMsgData(int position, InputFavorite favorite) {
        this.position = position;
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{text=" + this.favorite.text + '}';
    }
}
