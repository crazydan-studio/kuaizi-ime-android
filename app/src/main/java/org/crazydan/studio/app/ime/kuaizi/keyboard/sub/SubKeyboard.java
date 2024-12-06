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

package org.crazydan.studio.app.ime.kuaizi.keyboard.sub;

import java.util.function.Supplier;

import org.crazydan.studio.app.ime.kuaizi.keyboard.InputList;
import org.crazydan.studio.app.ime.kuaizi.keyboard.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.keyboard.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.UserKeyMsgData;

/**
 * 子键盘
 * <p/>
 * 处理不同输入的子键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface SubKeyboard extends UserInputMsgListener {

    Keyboard.Subtype getType();

    Configuration getConfig();

    void setConfig(Supplier<Configuration> getter);

    KeyFactory getKeyFactory();

    void setInputList(Supplier<InputList> getter);

    /** 启动 */
    void start();

    /** 重置状态 */
    void reset();

    /** 销毁 */
    void destroy();

    /** 处理{@link UserKeyMsg 按键消息} */
    void onMsg(UserKeyMsg msg, UserKeyMsgData data);
}
