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

import java.util.List;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;

/**
 * {@link Keyboard.Type#Pinyin_Candidate} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class PinyinCandidateKeyTable extends KeyTable {

    protected PinyinCandidateKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static PinyinCandidateKeyTable create(KeyTableConfig config) {
        return new PinyinCandidateKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 候选字按键的分页大小 */
    public int getKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 在键盘上可显示的最佳候选字的数量 */
    public int getBestCandidatesCount() {
        return 17;
    }

    /** 创建输入候选字按键 */
    public Key[][] createKeys(
            PinyinCharsTree charsTree, CharInput input,//
            List<PinyinWord.Spell> spells, List<InputWord> words, //
            int startIndex, PinyinWord.Filter wordFilter
    ) {
        Key[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();
        int index_mid = getGridMiddleColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);

        boolean isAdvanceFilter = !wordFilter.isEmpty(true);
        if (totalPage > 2 || isAdvanceFilter) {
            CtrlKey key = ctrlKey(CtrlKey.Type.Filter_PinyinCandidate_advance, (b) -> {
                if (isAdvanceFilter) {
                    b.icon(R.drawable.ic_filter_filled);
                }
            });

            gridKeys[2][0] = key;
        }

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.DropInput);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        // 声调过滤按键
        GridCoord[] spellKeyCorrds = getStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < spellKeyCorrds.length && j < spells.size(); i++, j++) {
            GridCoord keyCoord = spellKeyCorrds[i];
            PinyinWord.Spell spell = spells.get(j);

            boolean disabled = wordFilter.spells.contains(spell);
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinCandidate_by_Spell;

            CtrlKey key = advanceFilterKey(type, spell.value, spell, (b) -> b.disabled(disabled));
            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }

        // 拼音变换按键
        CharInput startingToggle = (CharInput) input.copy();
        if (input.is_Pinyin_SCZ_Starting()) {
            String s = input.getKeyChars().get(0).substring(0, 1);

            String label = s + "," + s + "h";
            CtrlKey.Type type = CtrlKey.Type.Toggle_Pinyin_Spell;
            CtrlKey.Option<CtrlKey.PinyinToggleMode> option = new CtrlKey.Option<>(CtrlKey.PinyinToggleMode.zcs_start);

            CtrlKey key = ctrlKey(type, (b) -> b.option(option).label(label));
            gridKeys[0][index_end] = key;

            startingToggle.toggle_Pinyin_SCZ_Starting();
        } else if (input.is_Pinyin_NL_Starting()) {
            // Note: 第二个右侧添加占位空格，以让字母能够对齐切换箭头
            String label = "n,l  ";
            CtrlKey.Type type = CtrlKey.Type.Toggle_Pinyin_Spell;
            CtrlKey.Option<CtrlKey.PinyinToggleMode> option = new CtrlKey.Option<>(CtrlKey.PinyinToggleMode.nl_start);

            CtrlKey key = ctrlKey(type, (b) -> b.option(option).label(label));
            gridKeys[0][index_end] = key;

            startingToggle.toggle_Pinyin_NL_Starting();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!startingToggle.getKeyChars().equals(input.getKeyChars()) //
            && !charsTree.isPinyinCharsInput(startingToggle)) {
            gridKeys[0][index_end] = noopCtrlKey();
        }

        CharInput endingToggle = (CharInput) input.copy();
        if (input.is_Pinyin_NG_Ending()) {
            String s = input.getKeyChars().get(input.getKeyChars().size() - 1);
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);

            String label = tail + "," + tail + "g";
            CtrlKey.Type type = CtrlKey.Type.Toggle_Pinyin_Spell;
            CtrlKey.Option<CtrlKey.PinyinToggleMode> option = new CtrlKey.Option<>(CtrlKey.PinyinToggleMode.ng_end);

            CtrlKey key = ctrlKey(type, (b) -> b.option(option).label(label));
            gridKeys[1][index_end] = key;

            endingToggle.toggle_Pinyin_NG_Ending();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!endingToggle.getKeyChars().equals(input.getKeyChars()) //
            && !charsTree.isPinyinCharsInput(endingToggle)) {
            gridKeys[1][index_end] = noopCtrlKey();
        }

        // 候选字按键
        GridCoord[][] levelKeyCoords = getLevelKeyCoords();
        fillGridLevelKeysByCoord(gridKeys,
                                 levelKeyCoords,
                                 words,
                                 startIndex,
                                 (word, level) -> inputWordKey(word, level, (b) -> {
                                     // 禁用已被选中的候选字按键
                                     b.disabled(word.equals(input.getWord()));
                                 }));

        return gridKeys;
    }

    /** 候选字高级过滤按键的分页大小 */
    public int getAdvanceFilterKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建输入候选字高级过滤按键 */
    public Key[][] createAdvanceFilterKeys(
            List<PinyinWord.Spell> spells, List<PinyinWord.Radical> radicals, //
            int startIndex, PinyinWord.Filter wordFilter
    ) {
        Key[][] gridKeys = createEmptyGrid();

        int dataSize = radicals.size();
        int pageSize = getAdvanceFilterKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Confirm_PinyinCandidate_Filter);

        // 声调过滤按键
        GridCoord[] spellKeyCorrds = getStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < spellKeyCorrds.length && j < spells.size(); i++, j++) {
            GridCoord keyCoord = spellKeyCorrds[i];
            PinyinWord.Spell spell = spells.get(j);

            boolean disabled = wordFilter.spells.contains(spell);
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinCandidate_by_Spell;

            CtrlKey key = advanceFilterKey(type, spell.value, spell, (b) -> b.disabled(disabled));
            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }

        // 部首过滤按键
        GridCoord[][] levelKeyCoords = getLevelKeyCoords(true);
        fillGridLevelKeysByCoord(gridKeys, levelKeyCoords, radicals, startIndex, (radical, level) -> {
            Key.Color color = key_input_word_level_colors[level];
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinCandidate_by_Radical;

            boolean disabled = wordFilter.radicals.contains(radical);
            return advanceFilterKey(type, radical.value, radical, (b) -> b.color(color).disabled(disabled));
        });

        return gridKeys;
    }

    public CtrlKey advanceFilterKey(CtrlKey.Type type, String label) {
        return advanceFilterKey(type, label, null, CtrlKey.Builder.noop);
    }

    public CtrlKey advanceFilterKey(CtrlKey.Type type, String label, Object value, Consumer<CtrlKey.Builder> c) {
        CtrlKey.Option<?> option = new CtrlKey.Option<>(value);

        return ctrlKey(type, (b) -> {
            b.option(option).label(label);
            c.accept(b);
        });
    }

    /** 获取候选字的笔画过滤按键坐标 */
    private GridCoord[] getStrokeFilterKeyCoords() {
        return new GridCoord[] {
                coord(0, 6), coord(0, 5),
                //
                coord(0, 4), coord(0, 3),
                //
                coord(0, 2),
                };
    }
}
