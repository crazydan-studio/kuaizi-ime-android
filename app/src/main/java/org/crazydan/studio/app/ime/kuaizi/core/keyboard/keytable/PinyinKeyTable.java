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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinKeyboard;

/**
 * {@link PinyinKeyboard 拼音键盘}按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-30
 */
public class PinyinKeyTable extends KeyTable {

    protected PinyinKeyTable(Config config) {
        super(config);
    }

    public static PinyinKeyTable create(Config config) {
        return new PinyinKeyTable(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link PinyinKeyboard 拼音键盘}按键 */
    public Key<?>[][] createKeys() {
        if (this.config.isXInputPadEnabled()) {
            return createKeysForXPad();
        }

        return (Key<?>[][]) new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.Switch_HandMode),
                        // 😂
                        emojiKey("\uD83D\uDE02"),
                        symbolKey("！").withReplacements("!"),
                        level0CharKey("ü").withReplacements("v", "V"),
                        level0CharKey("i").withReplacements("I"),
                        level0CharKey("u").withReplacements("U"),
                        level0CharKey("o").withReplacements("O"),
                        level0CharKey("j").withReplacements("J"),
                        } //
                , new Key[] {
                keyboardSwitchKey(Keyboard.Type.Math),
                symbolKey("？").withReplacements("?"),
                level0CharKey("d").withReplacements("D"),
                level0CharKey("b").withReplacements("B"),
                level0CharKey("x").withReplacements("X"),
                level0CharKey("q").withReplacements("Q"),
                level0CharKey("a").withReplacements("A"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                keyboardSwitchKey(Keyboard.Type.Latin),
                // 😄
                emojiKey("\uD83D\uDE04"),
                symbolKey("；").withReplacements(";"),
                level0CharKey("m").withReplacements("M"),
                level0CharKey("l").withReplacements("L"),
                level0CharKey("y").withReplacements("Y"),
                level0CharKey("p").withReplacements("P"),
                level0CharKey("e").withReplacements("E"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.Toggle_Emoji_Keyboard),
                symbolKey("：").withReplacements(":"),
                level0CharKey("s").withReplacements("S"),
                level0CharKey("t").withReplacements("T"),
                ctrlKey(CtrlKey.Type.Editor_Cursor_Locator),
                level0CharKey("r").withReplacements("R"),
                level0CharKey("h").withReplacements("H"),
                this.config.hasInputs() ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.Toggle_Symbol_Keyboard),
                // 😉
                emojiKey("\uD83D\uDE09"),
                symbolKey("。").withReplacements("."),
                level0CharKey("c").withReplacements("C"),
                level0CharKey("z").withReplacements("Z"),
                level0CharKey("f").withReplacements("F"),
                level0CharKey("n").withReplacements("N"),
                level0CharKey("k").withReplacements("K"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput).setDisabled(!this.config.hasRevokingInputs()),
                symbolKey("，").withReplacements(","),
                level0CharKey("sh").withReplacements("Sh", "SH"),
                level0CharKey("ch").withReplacements("Ch", "CH"),
                level0CharKey("zh").withReplacements("Zh", "ZH"),
                level0CharKey("g").withReplacements("G"),
                level0CharKey("w").withReplacements("W"),
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    @Override
    protected XPadKey createXPadKey() {
        // 声母频率: https://www.zhihu.com/question/23111438/answer/559582999
        return xPadKey(Keyboard.Type.Pinyin, new Key[][][] {
                new Key[][] {
                        new Key[] { level0CharKey("h"), level0CharKey("k"), symbolKey("："), }, //
                        new Key[] {
                                symbolKey("。"), ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                },
                        }, //
                new Key[][] {
                        new Key[] { symbolKey("，"), symbolKey("？"), symbolKey("！"), }, //
                        new Key[] { level0CharKey("zh"), level0CharKey("z"), level0CharKey("l"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("d"), level0CharKey("b"), level0CharKey("t") }, //
                        new Key[] { level0CharKey("y"), level0CharKey("f"), level0CharKey("p"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("g"), level0CharKey("r"), level0CharKey("w"), }, //
                        new Key[] { level0CharKey("ch"), level0CharKey("c"), level0CharKey("m"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("e"), level0CharKey("a"), level0CharKey("o"), }, //
                        new Key[] { level0CharKey("i"), level0CharKey("u"), level0CharKey("ü"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("sh"), level0CharKey("s"), level0CharKey("n"), }, //
                        new Key[] { level0CharKey("j"), level0CharKey("q"), level0CharKey("x"), }, //
                }, //
        });
    }

    /** 创建拼音后继字母第 1/2 级按键 */
    public Key<?>[][] createNextCharKeys(
            String level0Char, String level1Char, String level2Char, Collection<String> level1NextChars,
            Map<Integer, List<String>> level2NextChars
    ) {
        // 在初始键盘上显隐按键
        Key<?>[][] gridKeys = createKeys();

        // Note: 第 1 级后继按键与键盘初始按键位置保持一致
        for (int i = 0; i < gridKeys.length; i++) {
            for (int j = 0; j < gridKeys[i].length; j++) {
                Key<?> key = gridKeys[i][j];

                gridKeys[i][j] = noopCtrlKey();
                if (!(key instanceof CharKey)) {
                    continue;
                }

                for (String nextChar : level1NextChars) {
                    if (nextChar.equals(key.getText()) //
                        || (nextChar.length() > key.getText().length() //
                            // Note: hng 中的第 1 级按键 ng 使用 n 所在键位
                            && nextChar.startsWith(key.getText()))) {
                        key = gridKeys[i][j] = level1CharKey(nextChar);
                        break;
                    }
                }

                boolean disabled = key.getText() != null && key.getText().equals(level1Char);
                key.setDisabled(disabled);
            }
        }

        // 统计按键总数，小于一定数量的，单独布局
        if (level2NextChars.size() <= 2) {
            List<String> keys = new ArrayList<>();
            level2NextChars.keySet()
                           .stream()
                           .sorted(Integer::compare)
                           .forEach((keyLength) -> keys.addAll(level2NextChars.get(keyLength)));

            GridCoord[] keyAmountCoords = getLevel2KeyCoordsByKeyAmount(keys.size());

            if (keyAmountCoords != null) {
                fillNextCharGridKeys(gridKeys, keyAmountCoords, keys, level0Char, level2Char);
                return gridKeys;
            }
        }

        // 在指定可用位置创建第 2 级字母按键
        level2NextChars.forEach((keyLength, keys) -> {
            GridCoord[] keyCoords = getLevel2KeyCoords(keyLength);
            fillNextCharGridKeys(gridKeys, keyCoords, keys, level0Char, level2Char);
        });

        return gridKeys;
    }

    private void fillNextCharGridKeys(
            Key<?>[][] gridKeys, GridCoord[] keyCoords, List<String> keys, String level0Char, String level2Char
    ) {
        int diff = keyCoords.length - keys.size();

        for (int i = 0; i < keys.size(); i++) {
            String text = keys.get(i);
            // 确保按键靠底部进行放置
            GridCoord keyCoord = keyCoords[i + diff];
            int row = keyCoord.row;
            int column = keyCoord.column;

            gridKeys[row][column] = level2CharKey(level0Char, text);

            boolean disabled = text != null && text.equals(level2Char);
            gridKeys[row][column].setDisabled(disabled);
        }
    }

    /** 按韵母起始字母以此按行创建按键 */
    public Key<?>[][] createFullCharKeys(String startChar, Map<String, List<String>> restChars) {
        Key<?>[][] gridKeys = createEmptyGrid();

        String[] charOrders = new String[] { "m", "n", "g", "a", "o", "e", "i", "u", "ü" };
        GridCoord[] gridCoords = getFullCharKeyCoords();

        List<String> restCharList = new ArrayList<>();
        for (String order : charOrders) {
            List<String> list = restChars.get(order);
            if (list != null) {
                restCharList.addAll(list);
            }
        }
        // 再按字符长度升序排列
        restCharList.sort(Comparator.comparing(String::length));

        for (int i = 0; i < restCharList.size(); i++) {
            String restChar = restCharList.get(i);
            GridCoord keyCoord = gridCoords[i];

            int row = keyCoord.row;
            int column = keyCoord.column;

            gridKeys[row][column] = level2CharKey(startChar, restChar);
        }

        return gridKeys;
    }

    /** 创建 X 型输入的拼音后继字母第 1/2 级按键 */
    public Key<?>[][] createXPadNextCharKeys(
            String level0Char, String level1Char, String level2Char, Collection<String> level1NextChars,
            Map<Integer, List<String>> level2NextChars
    ) {
        XPadKey xPadKey = createXPadKey();
        // 在初始键盘上显隐按键
        Key<?>[][] gridKeys = createKeysForXPad(xPadKey);

        if (level1Char == null) {
            for (Key<?>[][] zone_2_key : xPadKey.zone_2_keys) {
                // Note: 第 1 级后继按键与键盘初始按键位置保持一致
                for (Key<?>[] keys : zone_2_key) {
                    for (int j = 0; j < keys.length; j++) {
                        Key<?> key = keys[j];

                        if (!(key instanceof CharKey)) {
                            keys[j] = null;
                            continue;
                        }

                        for (String nextChar : level1NextChars) {
                            if (nextChar.equals(key.getText()) //
                                || (nextChar.length() > key.getText().length() //
                                    // Note: hng 中的第 1 级按键 ng 使用 n 所在键位
                                    && nextChar.startsWith(key.getText()))) {
                                key = keys[j] = level1CharKey(nextChar);
                                break;
                            }
                        }

                        if (key.getLevel() != Key.Level.level_1) {
                            keys[j] = null;
                        }
                    }

                    // 字母按键向靠近中心的方向排列
                    Arrays.sort(keys, (a, b) -> a != null && b != null ? 0 : a != null ? -1 : 1);
                }
            }
        } else {
            for (Key<?>[][] zone_2_key : xPadKey.zone_2_keys) {
                for (Key<?>[] keys : zone_2_key) {
                    Arrays.fill(keys, null);
                }
            }

            // 在指定可用位置创建第 2 级字母按键
            level2NextChars.forEach((keyLength, keys) -> {
                GridCoord[] keyCoords = getXPadLevel2KeyCoords(keyLength);
                for (int i = 0; i < keys.size(); i++) {
                    String text = keys.get(i);
                    GridCoord keyCoord = keyCoords[i];
                    int row = keyCoord.row;
                    int column = keyCoord.column;
                    int layer = keyCoord.layer;

                    xPadKey.zone_2_keys[layer][row][column] = level2CharKey("", text);
                }
            });
        }

        // Note：仅待输入 1/2 级字母时才需要提供拼音结束按键，且可通过结束按键去掉首字母选错的拼音
        xPadKey.zone_2_keys[5][1][2] = ctrlKey(CtrlKey.Type.Pinyin_End);

        xPadKey.zone_2_keys[0][1][1] = ctrlKey(CtrlKey.Type.Space);
        xPadKey.zone_2_keys[0][1][2] = ctrlKey(CtrlKey.Type.Backspace);

        return gridKeys;
    }

    /** 候选字按键的分页大小 */
    public int getInputCandidateKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建输入候选字按键 */
    public Key<?>[][] createInputCandidateKeys(
            CharInput input, List<InputWord> words, Map<String, Integer> strokes, //
            int startIndex, boolean hasAdvanceFilter
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getInputCandidateKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();
        int index_mid = getGridMiddleColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        if (totalPage > 2 || hasAdvanceFilter) {
            CtrlKey key = ctrlKey(CtrlKey.Type.Filter_PinyinInputCandidate_advance);
            gridKeys[2][0] = key;

            if (hasAdvanceFilter) {
                key.setIconResId(R.drawable.ic_filter_filled);
            }
        }

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.DropInput);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        // 笔画过滤按键
        String[] filterStrokes = PinyinInputWord.getStrokeNames();
        GridCoord[] filterStrokeKeyCorrds = getInputCandidateStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < filterStrokeKeyCorrds.length && j < filterStrokes.length; i++, j++) {
            GridCoord keyCoord = filterStrokeKeyCorrds[i];
            String stroke = filterStrokes[j];

            int row = keyCoord.row;
            int column = keyCoord.column;

            String strokeCode = PinyinInputWord.getStrokeCode(stroke);
            Integer strokeCount = strokes.get(strokeCode);

            gridKeys[row][column] = strokeFilterKey(stroke, strokeCount);
        }

        // 拼音变换按键
        CharInput startingToggle = input.copy();
        if (input.is_Pinyin_SCZ_Starting()) {
            String s = input.getChars().get(0).substring(0, 1);

            String label = s + "," + s + "h";
            CtrlKey.Type type = CtrlKey.Type.Toggle_PinyinInput_spell;
            CtrlKey.Option<?> option
                    = new CtrlKey.PinyinSpellToggleOption(CtrlKey.PinyinSpellToggleOption.Toggle.zcs_h);

            gridKeys[0][index_end] = ctrlKey(type).setOption(option).setLabel(label);

            startingToggle.toggle_Pinyin_SCZ_Starting();
        } else if (input.is_Pinyin_NL_Starting()) {
            // Note: 第二个右侧添加占位空格，以让字母能够对齐切换箭头
            String label = "n,l  ";
            CtrlKey.Type type = CtrlKey.Type.Toggle_PinyinInput_spell;
            CtrlKey.Option<?> option = new CtrlKey.PinyinSpellToggleOption(CtrlKey.PinyinSpellToggleOption.Toggle.nl);

            gridKeys[0][index_end] = ctrlKey(type).setOption(option).setLabel(label);

            startingToggle.toggle_Pinyin_NL_Starting();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!startingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(startingToggle)) {
            gridKeys[0][index_end] = noopCtrlKey();
        }

        CharInput endingToggle = input.copy();
        if (input.is_Pinyin_NG_Ending()) {
            String s = input.getChars().get(input.getChars().size() - 1);
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);

            String label = tail + "," + tail + "g";
            CtrlKey.Type type = CtrlKey.Type.Toggle_PinyinInput_spell;
            CtrlKey.Option<?> option = new CtrlKey.PinyinSpellToggleOption(CtrlKey.PinyinSpellToggleOption.Toggle.ng);

            gridKeys[1][index_end] = ctrlKey(type).setOption(option).setLabel(label);

            endingToggle.toggle_Pinyin_NG_Ending();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!endingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(endingToggle)) {
            gridKeys[1][index_end] = noopCtrlKey();
        }

        // 候选字按键
        int dataIndex = startIndex;
        GridCoord[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            GridCoord[] keyCoords = levelKeyCoords[level];

            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                if (dataIndex >= dataSize) {
                    break;
                }

                InputWord word = words.get(dataIndex++);
                if (word == null) {
                    continue;
                }

                InputWordKey key = inputWordKey(word, level);
                // 禁用已被选中的候选字按键
                if (word.equals(input.getWord())) {
                    key.setDisabled(true);
                }
                gridKeys[row][column] = key;
            }
        }

        return gridKeys;
    }

    /** 候选字高级过滤按键的分页大小 */
    public int getInputCandidateAdvanceFilterKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建输入候选字高级过滤按键 */
    public Key<?>[][] createInputCandidateAdvanceFilterKeys(
            List<PinyinInputWord.Spell> spells, List<PinyinInputWord.Spell> selectedSpells, //
            List<PinyinInputWord.Radical> radicals, List<PinyinInputWord.Radical> selectedRadicals, //
            int startIndex
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = radicals.size();
        int pageSize = getInputCandidateAdvanceFilterKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Confirm_PinyinInputCandidate_Filters);

        // 声调过滤按键
        GridCoord[] spellKeyCorrds = getInputCandidateStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < spellKeyCorrds.length && j < spells.size(); i++, j++) {
            GridCoord keyCoord = spellKeyCorrds[i];
            PinyinInputWord.Spell spell = spells.get(j);

            int row = keyCoord.row;
            int column = keyCoord.column;

            boolean disabled = selectedSpells.contains(spell);
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinInputCandidate_by_Spell;

            gridKeys[row][column] = advanceFilterKey(type, spell.value, spell).setDisabled(disabled);
        }

        // 部首过滤按键
        int dataIndex = startIndex;
        GridCoord[][] levelKeyCoords = getLevelKeyCoords(true);

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            GridCoord[] keyCoords = levelKeyCoords[level];

            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                if (dataIndex >= dataSize) {
                    break;
                }

                PinyinInputWord.Radical radical = radicals.get(dataIndex++);
                boolean disabled = selectedRadicals.contains(radical);
                KeyColor color = key_input_word_level_colors[level];
                CtrlKey.Type type = CtrlKey.Type.Filter_PinyinInputCandidate_by_Radical;

                gridKeys[row][column] = advanceFilterKey(type, radical.value, radical).setColor(color)
                                                                                      .setDisabled(disabled);
            }
        }

        return gridKeys;
    }

    /** 创建 输入列表 提交选项 按键 */
    public Key<?>[][] createInputListCommittingOptionKeys(
            Input.Option currentOption, boolean hasNotation, boolean hasVariant
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        boolean isOnlyPinyin = currentOption != null
                               && currentOption.wordNotationType == InputWord.NotationType.replacing;
        int index_end = getGridLastColumnIndex();

        gridKeys[1][index_end]
                = commitOptionKey(CtrlKey.InputListCommitOption.Option.only_pinyin).setDisabled(!hasNotation);
        gridKeys[2][index_end]
                = commitOptionKey(CtrlKey.InputListCommitOption.Option.with_pinyin).setDisabled(!hasNotation);
        gridKeys[4][index_end]
                = commitOptionKey(CtrlKey.InputListCommitOption.Option.switch_simple_trad).setDisabled(!hasVariant
                                                                                                       || isOnlyPinyin);

        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);

        return gridKeys;
    }

    public CharKey level0CharKey(String level0Char) {
        return alphabetKey(level0Char);
    }

    public CharKey level1CharKey(String level1Char) {
        KeyColor color = key_char_special_color;

        return alphabetKey(level1Char).setLevel(Key.Level.level_1).setColor(color);
    }

    public CharKey level2CharKey(String level0Char, String level2Char) {
        String text = level2Char;
        String label = level0Char + text;
        KeyColor color = key_char_color;

        int labelDimension = R.dimen.char_key_text_size_3d;
        if (label.length() > 5) {
            labelDimension = R.dimen.char_key_text_size_4d;
        }

        return alphabetKey(text).setLevel(Key.Level.level_2)
                                .setLabel(label)
                                .setColor(color)
                                .setLabelDimensionId(labelDimension);
    }

    public InputWordKey inputWordKey(InputWord word, int level) {
        KeyColor color = key_input_word_level_colors[level];

        return InputWordKey.create(word).setColor(color);
    }

    public CtrlKey strokeFilterKey(String stroke, Integer strokeCount) {
        String strokeCode = PinyinInputWord.getStrokeCode(stroke);
        String label = strokeCount != null ? stroke + "/" + strokeCount : stroke;

        CtrlKey.Option<?> option = new CtrlKey.CodeOption(strokeCode, stroke);

        return ctrlKey(CtrlKey.Type.Filter_PinyinInputCandidate_by_Stroke).setOption(option).setLabel(label);
    }

    public CtrlKey advanceFilterKey(CtrlKey.Type type, String label, Object value) {
        CtrlKey.Option<?> option = new CtrlKey.ValueOption(value);

        return ctrlKey(type).setOption(option).setLabel(label);
    }

    public CtrlKey commitOptionKey(CtrlKey.InputListCommitOption.Option opt) {
        CtrlKey.Option<?> option = new CtrlKey.InputListCommitOption(opt);
        String label = null;
        switch (opt) {
            case only_pinyin:
                label = "仅拼音";
                break;
            case with_pinyin:
                label = "带拼音";
                break;
            case switch_simple_trad:
                label = "简/繁";
                break;
        }

        return ctrlKey(CtrlKey.Type.Commit_InputList_Option).setOption(option).setLabel(label);
    }

    /** 获取候选字的笔画过滤按键坐标 */
    private GridCoord[] getInputCandidateStrokeFilterKeyCoords() {
        return new GridCoord[] {
                coord(0, 6), coord(0, 5),
                //
                coord(0, 4), coord(0, 3),
                //
                coord(0, 2),
                };
    }

    /** 获取拼音{@link Key.Level#level_2 第二级}按键坐标 */
    private GridCoord[] getLevel2KeyCoords(int keyLength) {
        if (keyLength <= 2) {
            // Note：特定韵母组成的 2 个字母的音节数不会超过 4 个
            return new GridCoord[] {
                    //
                    coord(2, 2), coord(3, 2),
                    //
                    coord(4, 3), coord(5, 3),
                    };
        } else if (keyLength == 3) {
            // Note：特定韵母组成的 3 个字母的音节数不会超过 3 个
            return new GridCoord[] {
                    //
                    coord(3, 1), coord(4, 2),
                    //
                    coord(5, 2),
                    };
        }
        return new GridCoord[] {
                coord(3, 3), coord(4, 4),
                };
    }

    /** 获取拼音{@link Key.Level#level_2 第二级}按键坐标 */
    private GridCoord[] getLevel2KeyCoordsByKeyAmount(int keyAmount) {
        if (keyAmount <= 2) {
            return new GridCoord[] {
                    coord(3, 3), coord(4, 4),
                    };
        } else {
            switch (keyAmount) {
                case 3: {
                    return new GridCoord[] {
                            coord(3, 3), coord(4, 4),
                            //
                            coord(4, 3),
                            };
                }
                case 4: {
                    return new GridCoord[] {
                            //
                            coord(3, 3), coord(4, 4),
                            //
                            coord(4, 3), coord(5, 3),
                            };
                }
                case 5: {
                    return new GridCoord[] {
                            //
                            coord(3, 3), coord(4, 4),
                            //
                            coord(4, 3), coord(5, 3),
                            //
                            coord(3, 2),
                            };
                }
            }
        }
        return null;
    }

    private GridCoord[] getFullCharKeyCoords() {
        return new GridCoord[] {
                // row 0
                coord(0, 7), coord(0, 6),
                //
                coord(0, 5), coord(0, 4),
                //
                coord(0, 3),
                // row 1
                coord(1, 7), coord(1, 6),
                //
                coord(1, 5), coord(1, 4),
                //
                coord(1, 3),
                // row 2
                coord(2, 7), coord(2, 6),
                //
                coord(2, 5), coord(2, 4),
                //
                coord(2, 3),
                // row 3
                coord(3, 7), coord(3, 6),
                //
                coord(3, 5), coord(3, 4),
                //
                coord(3, 3),
                // row 4
                coord(4, 7), coord(4, 6),
                //
                coord(4, 5), coord(4, 4),
                //
                coord(4, 3),
                // row 5
                coord(5, 7), coord(5, 6),
                //
                coord(5, 5), coord(5, 4),
                //
                coord(5, 3),
                };
    }

    /** 获取 X 型输入的拼音{@link Key.Level#level_2 第二级}按键坐标 */
    private GridCoord[] getXPadLevel2KeyCoords(int keyLength) {
        if (keyLength <= 2) {
            // Note：特定韵母组成的 2 个字母的音节数不会超过 4 个
            return new GridCoord[] {
                    coord(0, 0, 4), coord(1, 0, 4), //
                    coord(0, 1, 4), coord(1, 1, 4), //
                    coord(0, 2, 4), coord(1, 2, 4), //
            };
        } else if (keyLength == 3) {
            // Note：特定韵母组成的 3 个字母的音节数不会超过 3 个
            return new GridCoord[] {
                    coord(1, 0, 3), coord(1, 1, 3), coord(1, 2, 3), //
            };
        }
        return new GridCoord[] {
                coord(0, 0, 5), coord(0, 1, 5), coord(0, 2, 5), //
        };
    }
}
