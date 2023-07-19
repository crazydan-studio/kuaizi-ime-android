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

package org.crazydan.studio.app.ime.kuaizi.internal.msg.data;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserMsgData;

/**
 * {@link UserMsg#FingerSlipping}消息数据
 * <p/>
 * 注：<ul>
 * <li>{@link #target} 为滑动开始所按下的按键；</li>
 * </ul>
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class UserFingerSlippingMsgData extends UserMsgData {
    /** 运动信息 */
    public final Motion motion;

    public UserFingerSlippingMsgData(Key<?> target, Motion motion) {
        super(target);
        this.motion = motion;
    }
}
