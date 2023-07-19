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
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Motion;

/**
 * {@link InputMsg#SelectingInputText}消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-19
 */
public class InputTextSelectingMsgData extends CommonInputMsgData {
    /** 1 号锚点信息 */
    public final Motion anchor1;
    /** 2 号锚点信息 */
    public final Motion anchor2;

    /** 触发消息的按键 */
    public final Key<?> key;

    public InputTextSelectingMsgData(Keyboard.KeyFactory keyFactory, Key<?> key, Motion anchor1, Motion anchor2) {
        super(keyFactory);
        this.key = key;
        this.anchor1 = anchor1;
        this.anchor2 = anchor2;
    }

    public InputTextSelectingMsgData(Key<?> key, Motion anchor1, Motion anchor2) {
        this(null, key, anchor1, anchor2);
    }
}
