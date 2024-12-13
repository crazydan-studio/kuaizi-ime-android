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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state;

import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.State;

/**
 * {@link State.Type#InputList_Committing_Option_Choose_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-10
 */
public class CommittingOptionChooseStateData implements State.Data {
    private Input.Option option;
    private boolean hasSpell;
    private boolean hasVariant;

    public CommittingOptionChooseStateData(InputList inputList) {
        update(inputList);
    }

    public void update(InputList inputList) {
        this.option = inputList.getOption();
        this.hasSpell = false;
        this.hasVariant = false;

        for (CharInput input : inputList.getCharInputs()) {
            InputWord word = input.getWord();
            if (word == null) {
                continue;
            }

            if (word.hasSpell()) {
                this.hasSpell = true;
            }
            if (word.hasVariant()) {
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
