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

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;

/**
 * {@link InputMsgType#InputClip_Apply_Done}、
 * {@link InputMsgType#InputClip_CanBe_Favorite}
 * 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-12
 */
public class InputClipMsgData extends InputMsgData {
    public final ClipSourceType source;
    /** 当前处理的剪贴数据 */
    public final InputClip clip;

    public InputClipMsgData(InputClip clip) {
        this(null, clip);
    }

    public InputClipMsgData(ClipSourceType source, InputClip clip) {
        this.source = source;
        this.clip = clip;
    }

    public enum ClipSourceType {
        /** 来自粘贴操作 */
        paste(R.string.text_confirm_save_pasted_content),
        /** 来自复制/剪贴 */
        copy_cut(R.string.text_confirm_save_copied_content),
        /** 来自用户输入 */
        user_input(R.string.text_confirm_save_input_content),
        ;

        public final int confirmResId;

        ClipSourceType(int confirmResId) {
            this.confirmResId = confirmResId;
        }
    }
}
