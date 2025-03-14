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

package org.crazydan.studio.app.ime.kuaizi.core.msg.input;

import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;

/**
 * {@link InputMsgType#InputClip_Text_Commit_Doing}、
 * {@link InputMsgType#InputFavorite_Text_Commit_Doing}
 * 的消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-10
 */
public class InputTextCommitMsgData extends InputMsgData {
    /** 待提交的文本内容 */
    public final CharSequence text;
    /** 是否逐字输入 */
    public final boolean oneByOne;

    public InputTextCommitMsgData(CharSequence text) {
        this(text, false);
    }

    public InputTextCommitMsgData(CharSequence text, boolean oneByOne) {
        this.text = text;
        this.oneByOne = oneByOne;
    }
}
