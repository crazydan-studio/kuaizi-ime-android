/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
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
    public final Input.Option oldOption;

    private Input.Option option;
    private boolean hasSpell;
    private boolean hasVariant;

    public InputListCommitOptionChooseStateData(Input.Option oldOption) {
        this.oldOption = oldOption;
    }

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
