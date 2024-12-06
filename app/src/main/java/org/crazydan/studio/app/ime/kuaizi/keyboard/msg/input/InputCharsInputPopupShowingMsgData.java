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

package org.crazydan.studio.app.ime.kuaizi.keyboard.msg.input;

import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsg;

/**
 * {@link KeyboardMsg#InputChars_Input_Popup_Show_Doing} 的消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-28
 */
public class InputCharsInputPopupShowingMsgData extends InputCommonMsgData {
    /** 气泡内容 */
    public final String text;
    /** 是否延迟隐藏 */
    public final boolean hideDelayed;

    public InputCharsInputPopupShowingMsgData(String text, boolean hideDelayed) {
        this.text = text;
        this.hideDelayed = hideDelayed;
    }
}
