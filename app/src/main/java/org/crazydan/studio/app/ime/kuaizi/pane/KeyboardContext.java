/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane;

import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;

/**
 * {@link Keyboard} 的上下文，以参数形式向键盘传递上下文信息
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-23
 */
public class KeyboardContext {
    public final InputList inputList;
    public final InputMsgListener listener;

    private final Key<?> key;

    public KeyboardContext(InputList inputList, InputMsgListener listener) {
        this(inputList, listener, null);
    }

    KeyboardContext(InputList inputList, InputMsgListener listener, Key<?> key) {
        this.inputList = inputList;
        this.listener = listener;
        this.key = key;
    }

    /** 根据 {@link Key} 新建实例，以使其携带该 {@link #key()} */
    public KeyboardContext newWithKey(Key<?> key) {
        return new KeyboardContext(this.inputList, this.listener, key);
    }

    /** 获取与当前上下文直接关联的 {@link Key}，一般为触发 {@link UserKeyMsg} 消息所对应的按键 */
    public <T extends Key<?>> T key() {
        return (T) this.key;
    }
}
