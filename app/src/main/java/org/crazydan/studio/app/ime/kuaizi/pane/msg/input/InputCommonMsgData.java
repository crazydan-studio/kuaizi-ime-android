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

import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.KeyboardMsgData;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputCommonMsgData implements KeyboardMsgData {
    private final KeyFactory keyFactory;
    /** 触发按键 */
    private final Key<?> key;

    public InputCommonMsgData() {
        this(null, null);
    }

    public InputCommonMsgData(KeyFactory keyFactory) {
        this(keyFactory, null);
    }

    public InputCommonMsgData(Key<?> key) {
        this(null, key);
    }

    public InputCommonMsgData(KeyFactory keyFactory, Key<?> key) {
        this.keyFactory = keyFactory;
        this.key = key;
    }

    @Override
    public KeyFactory getKeyFactory() {
        return this.keyFactory;
    }

    @Override
    public Key<?> getKey() {
        return this.key;
    }
}
