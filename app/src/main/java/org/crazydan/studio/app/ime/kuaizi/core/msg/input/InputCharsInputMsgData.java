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

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;

/**
 * {@link InputMsgType#InputChars_Input_Doing}
 * 和 {@link InputMsgType#InputChars_Input_Done}
 * 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class InputCharsInputMsgData extends InputMsgData {
    public final InputMode inputMode;

    public InputCharsInputMsgData(Key key, Input input) {
        this(key, input, null);
    }

    public InputCharsInputMsgData(Key key, Input input, InputMode inputMode) {
        super(key, input);
        this.inputMode = inputMode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + '{'
               + "inputMode="
               + this.inputMode
               + ", key="
               + this.key
               + ", input="
               + this.input
               + '}';
    }

    /** 输入方式 */
    public enum InputMode {
        /** 点击输入 */
        tap,
        /** 滑动输入 */
        slip,
        /** 翻动输入 */
        flip,
        /** X 型输入中的画圈输入 */
        circle,
    }
}
