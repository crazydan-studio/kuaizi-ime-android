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

import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;

/**
 * {@link InputMsgType#Editor_Range_Select_Doing}
 * 和 {@link InputMsgType#Editor_Cursor_Move_Doing}
 * 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-18
 */
public class EditorCursorMsgData extends InputMsgData {
    /** 锚点信息 */
    public final Motion anchor;

    public EditorCursorMsgData(Key key, Motion motion) {
        super(key);
        this.anchor = createAnchor(motion);
    }

    private static Motion createAnchor(Motion motion) {
        // 根据屏幕移动距离得出光标移动字符数
        float distance = motion.distance > 0 ? Math.max(1, motion.distance / ScreenUtils.dpToPx(16f)) : 0;

        return new Motion(motion.direction, distance, motion.timestamp);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + "anchor=" + this.anchor + ", key=" + this.key + '}';
    }
}
