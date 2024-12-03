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

package org.crazydan.studio.app.ime.kuaizi.keyboard.msg.user;

import org.crazydan.studio.app.ime.kuaizi.keyboard.Key;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsgData;

/**
 * {@link UserKeyMsg#FingerMoving} 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class UserFingerMovingMsgData extends UserKeyMsgData {
    /** 运动信息 */
    public final Motion motion;

    public UserFingerMovingMsgData(Key<?> target, Motion motion) {
        super(target);
        this.motion = motion;
    }
}
