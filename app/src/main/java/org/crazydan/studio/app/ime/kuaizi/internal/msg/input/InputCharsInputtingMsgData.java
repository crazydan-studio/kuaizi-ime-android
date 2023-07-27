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

package org.crazydan.studio.app.ime.kuaizi.internal.msg.input;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;

/**
 * {@link InputMsg#InputChars_Inputting}消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class InputCharsInputtingMsgData extends InputCommonMsgData {
    /** 当前已输入按键列表 */
    public final List<Key<?>> inputs;
    /** 当前按键 */
    public final Key<?> current;
    /** 靠近的按键 */
    public final Key<?> closed;

    public InputCharsInputtingMsgData(
            Keyboard.KeyFactory keyFactory, List<Key<?>> inputs, Key<?> current, Key<?> closed
    ) {
        super(keyFactory);

        this.inputs = inputs;
        this.current = current;
        this.closed = closed;
    }
}
