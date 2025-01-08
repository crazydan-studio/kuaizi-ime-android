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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;

/**
 * {@link Keyboard.Type#InputList_Commit_Option} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class InputListCommitOptionKeyTable extends KeyTable {

    protected InputListCommitOptionKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static InputListCommitOptionKeyTable create(KeyTableConfig config) {
        return new InputListCommitOptionKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建定位按键 */
    public Key[][] createKeys(Input.Option currentOption, boolean hasSpell, boolean hasVariant) {
        Key[][] gridKeys = createEmptyGrid();

        boolean isOnlyPinyin = currentOption.wordSpellUsedMode == PinyinWord.SpellUsedMode.replacing;
        boolean isWithPinyin = currentOption.wordSpellUsedMode == PinyinWord.SpellUsedMode.following;
        boolean isVariantUsed = currentOption.wordVariantUsed;
        int index_end = getGridLastColumnIndex();

        gridKeys[1][index_end] = wordCommitModeKey(CtrlKey.InputWordCommitMode.only_pinyin,
                                                   (b) -> b.disabled(!hasSpell || isOnlyPinyin));

        gridKeys[2][index_end] = wordCommitModeKey(CtrlKey.InputWordCommitMode.with_pinyin,
                                                   (b) -> b.disabled(!hasSpell || isWithPinyin));

        gridKeys[4][index_end] = wordCommitModeKey(isVariantUsed
                                                   ? CtrlKey.InputWordCommitMode.trad_to_simple
                                                   : CtrlKey.InputWordCommitMode.simple_to_trad,
                                                   (b) -> b.disabled(!hasVariant || isOnlyPinyin));

        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        return gridKeys;
    }

    public CtrlKey wordCommitModeKey(CtrlKey.InputWordCommitMode mode) {
        return wordCommitModeKey(mode, CtrlKey.Builder.noop);
    }

    public CtrlKey wordCommitModeKey(CtrlKey.InputWordCommitMode mode, Consumer<CtrlKey.Builder> c) {
        String label = mode.label;
        CtrlKey.Option<CtrlKey.InputWordCommitMode> option = new CtrlKey.Option<>(mode);

        return ctrlKey(CtrlKey.Type.Commit_InputList_Option, (b) -> {
            b.option(option).label(label);
            c.accept(b);
        });
    }
}
