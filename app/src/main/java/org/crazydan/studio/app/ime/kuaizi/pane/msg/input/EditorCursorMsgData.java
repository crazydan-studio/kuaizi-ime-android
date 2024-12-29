/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane.msg.input;

import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.Motion;

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
}
