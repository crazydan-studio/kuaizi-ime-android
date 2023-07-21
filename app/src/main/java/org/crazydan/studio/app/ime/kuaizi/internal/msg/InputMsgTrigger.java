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

package org.crazydan.studio.app.ime.kuaizi.internal.msg;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link InputMsg} 消息的触发器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-21
 */
public class InputMsgTrigger {
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.add(listener);
    }

    /** 触发 {@link InputMsg} 消息 */
    public void fireInputMsg(InputMsg msg, InputMsgData data) {
        this.inputMsgListeners.forEach(listener -> listener.onInputMsg(msg, data));
    }
}
