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
