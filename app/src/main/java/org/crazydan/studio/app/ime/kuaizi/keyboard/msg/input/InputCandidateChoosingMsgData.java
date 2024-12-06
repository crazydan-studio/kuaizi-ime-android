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

import org.crazydan.studio.app.ime.kuaizi.keyboard.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.keyboard.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.KeyboardMsg;

/**
 * {@link KeyboardMsg#InputCandidate_Choose_Doing}
 * 和 {@link KeyboardMsg#InputCandidate_Choose_Done}
 * 消息数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-23
 */
public class InputCandidateChoosingMsgData extends InputCommonMsgData {
    public final CharInput target;

    public InputCandidateChoosingMsgData(KeyFactory keyFactory, CharInput target) {
        super(keyFactory);

        this.target = target;
    }
}
