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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

/**
 * {@link State.Type#InputList_Commit_Option_Choose_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class InputListCommitOptionChooseStateData implements State.Data {
    private Input.Option option;
    private boolean hasSpell;
    private boolean hasVariant;

    public void update(InputList inputList) {
        this.option = inputList.getInputOption();
        this.hasSpell = false;
        this.hasVariant = false;

        for (CharInput input : inputList.getCharInputs()) {
            InputWord word = input.getWord();
            if (!(word instanceof PinyinWord)) {
                continue;
            }

            if (((PinyinWord) word).spell != null) {
                this.hasSpell = true;
            }
            if (((PinyinWord) word).variant != null) {
                this.hasVariant = true;
            }
        }
    }

    public Input.Option getOption() {
        return this.option;
    }

    public boolean hasSpell() {
        return this.hasSpell;
    }

    public boolean hasVariant() {
        return this.hasVariant;
    }
}
