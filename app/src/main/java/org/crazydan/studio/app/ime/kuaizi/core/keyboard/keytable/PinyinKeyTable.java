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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinCharsTree;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinWord;
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
                level0CharKey("m").withReplacements("M"),
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
                level0CharKey("b").withReplacements("B"),
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
                        new Key[] { level0CharKey("g"), level0CharKey("f"), level0CharKey("p"), }, //
                        new Key[] {
                                symbolKey("。"), ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                symbolKey("，").withReplacements("；"),
                                symbolKey("？").withReplacements("："),
                                symbolKey("！").withReplacements("、"),
                                }, //
                        new Key[] { level0CharKey("d"), level0CharKey("b"), level0CharKey("t"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("y"), level0CharKey("h"), level0CharKey("r") }, //
                        new Key[] { level0CharKey("l"), level0CharKey("m"), level0CharKey("n"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("z"), level0CharKey("s"), level0CharKey("c"), }, //
                        new Key[] { level0CharKey("sh"), level0CharKey("zh"), level0CharKey("ch"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("e"), level0CharKey("a"), level0CharKey("o"), }, //
                        new Key[] { level0CharKey("i"), level0CharKey("u"), level0CharKey("ü"), }, //
                }, //
                new Key[][] {
                        new Key[] { level0CharKey("j"), level0CharKey("w"), symbolKey("～"), }, //
                        new Key[] { level0CharKey("x"), level0CharKey("q"), level0CharKey("k"), }, //
                }, //
        });
    }

    /** 创建拼音后继字母第 1/2 级按键 */
    public Key<?>[][] createNextCharKeys(
            PinyinCharsTree charsTree, //
            String level0Char, String level1Char, String level2Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
        if (level0CharsTree == null) {
            return createEmptyGrid();
        }

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

                String nextChar = key.getText();
                PinyinCharsTree child = level0CharsTree.getChild(nextChar);
                if (child == null) {
                    continue;
                }

                if (!child.isPinyin() && child.countChild() == 1) {
                    nextChar = child.getNextChars().get(0);
                }
                gridKeys[i][j] = key = level1CharKey(nextChar);

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
    public Key<?>[][] createFullCharKeys(PinyinCharsTree charsTree, String level0Char) {
        Key<?>[][] gridKeys = createEmptyGrid();

        String[] charOrders = new String[] { "m", "n", "g", "a", "o", "e", "i", "u", "ü" };
        GridCoord[] gridCoords = getFullCharKeyCoords();

        PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
        if (level0CharsTree == null) {
            return createKeys();
        }

        List<String> restCharList = new ArrayList<>();
        if (level0CharsTree.isPinyin()) {
            // 单音节拼音的后继始终为空字符
            restCharList.add("");
        }

        for (String order : charOrders) {
            PinyinCharsTree child = level0CharsTree.getChild(order);
            if (child == null) {
                continue;
            }

            restCharList.addAll(child.getAllPinyinChars());
        }
        // 再按字符长度升序排列
        restCharList.sort(Comparator.comparing(String::length));

        for (int i = 0; i < restCharList.size(); i++) {
            String restChar = restCharList.get(i);
            GridCoord keyCoord = gridCoords[i];

            int row = keyCoord.row;
            int column = keyCoord.column;

            gridKeys[row][column] = level2CharKey(level0Char, restChar);
        }

        return gridKeys;
    }

    /** 创建 X 型输入的拼音后继字母第 1/2 级按键 */
    public Key<?>[][] createXPadNextCharKeys(
            PinyinCharsTree charsTree, //
            String level0Char, String level1Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        XPadKey xPadKey = createXPadKey();
        // 在初始键盘上显隐按键
        Key<?>[][] gridKeys = createKeysForXPad(xPadKey);

        switch (level0Char) {
            case "a": {
                // a ai an ao ang
                xPadKey.zone_2_keys[4][1][0] = levelFinalCharKey("ai");
                xPadKey.zone_2_keys[4][1][1] = levelFinalCharKey("ao");
                xPadKey.zone_2_keys[4][1][2] = levelFinalCharKey("an");
                xPadKey.zone_2_keys[5][0][2] = levelFinalCharKey("ang");
                return gridKeys;
            }
            case "e": {
                // e ei en er eng
                xPadKey.zone_2_keys[4][1][0] = levelFinalCharKey("ei");
                xPadKey.zone_2_keys[4][1][1] = levelFinalCharKey("er");
                xPadKey.zone_2_keys[4][1][2] = levelFinalCharKey("en");
                xPadKey.zone_2_keys[5][0][2] = levelFinalCharKey("eng");
                return gridKeys;
            }
            case "o": {
                // o ou
                xPadKey.zone_2_keys[4][1][0] = levelFinalCharKey("ou");
                xPadKey.zone_2_keys[4][1][1] = null;
                xPadKey.zone_2_keys[4][1][2] = null;
                return gridKeys;
            }
            // Note：对 m 的单音节拼音提供连续输入支持
            case "m": {
                // m ma me mi mo mu
                xPadKey.zone_2_keys[5][0][2] = levelFinalCharKey("m");
                break;
            }
            case "h": {
                // hm hng ha he hu
                xPadKey.zone_2_keys[3][1][2] = levelFinalCharKey("hm");
                xPadKey.zone_2_keys[5][0][2] = levelFinalCharKey("hng");
                break;
            }
            // Note：因为可用按键位不足，故而 n 的单音节拼音只能通过释放手指输入，
            // 无法连续输入，或者以 ng 替代，因为二者的候选字是一样的
            case "n": {
                // n ng na ne ni nu nü
                xPadKey.zone_2_keys[3][1][2] = levelFinalCharKey("ng");
                break;
            }
        }

        if (level1Char == null) {
            PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
            if (level0CharsTree == null) {
                return gridKeys;
            }

            for (int i = 0; i < xPadKey.zone_2_keys.length; i++) {
                Key<?>[][] zone_2_key = xPadKey.zone_2_keys[i];

                for (int j = 0; j < zone_2_key.length; j++) {
                    Key<?>[] keys = zone_2_key[j];
                    // Note: 第 1 级后继按键与键盘初始按键位置保持一致
                    for (int k = 0; k < keys.length; k++) {
                        Key<?> key = keys[k];
                        // 右下角的功能和符号按键保持不动
                        if (i + j == 1) {
                            continue;
                        } else if (key.getLevel() != Key.Level.level_final) {
                            keys[k] = null;
                        }

                        String nextChar = key.getText();
                        PinyinCharsTree child = level0CharsTree.getChild(nextChar);
                        if (child == null) {
                            continue;
                        }

                        // 忽略已单独布局的后继字母按键
                        if ((level0Char.equals("h") //
                             && (nextChar.equals("m") || nextChar.equals("n"))) //
                            || (level0Char.equals("n") && nextChar.equals("g"))) {
                            continue;
                        }

                        // 双音节拼音放置在第 4 分区的左右两边的轴线外侧
                        if (child.isPinyin()) {
                            int layer = j == 0 ? i - 1 : i + 1;
                            int row = j == 0 ? j + 1 : j - 1;

                            String finalChar = level0CharsTree.value + child.value;
                            xPadKey.zone_2_keys[layer][row][k] = levelFinalCharKey(finalChar);
                        }

                        // 若第 2 级后继只有一个拼音，则直接放置
                        if (child.countChild() == 1) {
                            nextChar = child.getNextChars().get(0);
                            keys[k] = level2CharKey("", nextChar);
                        } else if (child.countChild() > 1) {
                            keys[k] = level1CharKey(nextChar);
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < xPadKey.zone_2_keys.length; i++) {
                Key<?>[][] zone_2_key = xPadKey.zone_2_keys[i];
                for (int j = 0; j < zone_2_key.length; j++) {
                    // 右下角的功能和符号按键保持不动
                    if (i + j == 1) {
                        continue;
                    }

                    Key<?>[] keys = zone_2_key[j];
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

        return gridKeys;
    }

    /** 候选字按键的分页大小 */
    public int getInputCandidateKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 在键盘上可显示的最佳候选字的数量 */
    public int getBestCandidatesCount() {
        return 17;
    }

    /** 创建输入候选字按键 */
    public Key<?>[][] createInputCandidateKeys(
            PinyinCharsTree charsTree, CharInput input,//
            List<PinyinWord.Spell> spells, List<InputWord> words, //
            int startIndex, PinyinWord.Filter wordFilter
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getInputCandidateKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();
        int index_mid = getGridMiddleColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        if (totalPage > 2 || !wordFilter.isEmpty()) {
            CtrlKey key = ctrlKey(CtrlKey.Type.Filter_PinyinInputCandidate_advance);
            gridKeys[2][0] = key;

            if (!wordFilter.isEmpty()) {
                key.setIconResId(R.drawable.ic_filter_filled);
            }
        }

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.DropInput);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        // 声调过滤按键
        GridCoord[] spellKeyCorrds = getInputCandidateStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < spellKeyCorrds.length && j < spells.size(); i++, j++) {
            GridCoord keyCoord = spellKeyCorrds[i];
            PinyinWord.Spell spell = spells.get(j);

            int row = keyCoord.row;
            int column = keyCoord.column;

            boolean disabled = wordFilter.spells.contains(spell);
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinInputCandidate_by_Spell;

            gridKeys[row][column] = advanceFilterKey(type, spell.value, spell).setDisabled(disabled);
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
            && !charsTree.isPinyinCharsInput(startingToggle)) {
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
            && !charsTree.isPinyinCharsInput(endingToggle)) {
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
            List<PinyinWord.Spell> spells, List<PinyinWord.Radical> radicals, //
            int startIndex, PinyinWord.Filter wordFilter
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
            PinyinWord.Spell spell = spells.get(j);

            int row = keyCoord.row;
            int column = keyCoord.column;

            boolean disabled = wordFilter.spells.contains(spell);
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

                PinyinWord.Radical radical = radicals.get(dataIndex++);
                boolean disabled = wordFilter.radicals.contains(radical);
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
            Input.Option currentOption, boolean hasSpell, boolean hasVariant
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        boolean isOnlyPinyin = currentOption != null
                               && currentOption.wordSpellUsedType == InputWord.SpellUsedType.replacing;
        boolean isWithPinyin = currentOption != null
                               && currentOption.wordSpellUsedType == InputWord.SpellUsedType.following;
        boolean isVariantUsed = currentOption != null && currentOption.wordVariantUsed;
        int index_end = getGridLastColumnIndex();

        gridKeys[1][index_end] = commitOptionKey( //
                                                  CtrlKey.InputListCommitOption.Option.only_pinyin //
        ).setDisabled(!hasSpell || isOnlyPinyin);

        gridKeys[2][index_end] = commitOptionKey( //
                                                  CtrlKey.InputListCommitOption.Option.with_pinyin //
        ).setDisabled(!hasSpell || isWithPinyin);

        gridKeys[4][index_end] = commitOptionKey( //
                                                  isVariantUsed
                                                  ? CtrlKey.InputListCommitOption.Option.switch_trad_to_simple
                                                  : CtrlKey.InputListCommitOption.Option.switch_simple_to_trad //
        ).setDisabled(!hasVariant || isOnlyPinyin);

        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);

        return gridKeys;
    }

    public CharKey level0CharKey(String ch) {
        return alphabetKey(ch);
    }

    public CharKey level1CharKey(String ch) {
        KeyColor color = key_char_special_color;

        return alphabetKey(ch).setLevel(Key.Level.level_1).setColor(color);
    }

    public CharKey levelFinalCharKey(String ch) {
        KeyColor color = key_char_special_color;

        return alphabetKey(ch).setLevel(Key.Level.level_final).setColor(color);
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
            case switch_simple_to_trad:
                label = "简➙繁";
                break;
            case switch_trad_to_simple:
                label = "繁➙简";
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
