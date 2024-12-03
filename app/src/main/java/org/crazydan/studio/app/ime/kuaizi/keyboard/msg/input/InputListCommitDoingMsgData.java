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

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.keyboard.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.InputMsg;

/**
 * {@link InputMsg#InputList_Commit_Doing} 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-15
 */
public class InputListCommitDoingMsgData extends InputCommonMsgData {
    public final CharSequence text;
    public final List<String> replacements;

    public InputListCommitDoingMsgData(
            KeyFactory keyFactory, CharSequence text, List<String> replacements
    ) {
        super(keyFactory);
        this.text = text;
        this.replacements = replacements != null ? replacements : new ArrayList<>();
    }
}
