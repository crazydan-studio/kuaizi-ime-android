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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;
import org.crazydan.studio.app.ime.kuaizi.core.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;

/**
 * {@link State.Type#Editor_Edit_Doing}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-19
 */
public class EditorEditDoingStateData implements State.Data {

    public static Motion createAnchor(Motion motion) {
        // 根据屏幕移动距离得出光标移动字符数
        float distance = motion.distance > 0 ? Math.max(1, motion.distance / ScreenUtils.dpToPx(16f)) : 0;

        return new Motion(motion.direction, distance, motion.timestamp);
    }
}
