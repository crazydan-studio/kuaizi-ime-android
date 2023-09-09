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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;

/**
 * {@link PinyinKeyboard 拼音键盘}按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-30
 */
public class PinyinKeyTable extends KeyTable {

    public static PinyinKeyTable create(Config config) {
        return new PinyinKeyTable(config);
    }

    protected PinyinKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link PinyinKeyboard 拼音键盘}按键 */
    public Key<?>[][] createKeys() {
        return (Key<?>[][]) new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.SwitchHandMode),
                        // 😂
                        emojiKey("\uD83D\uDE02"),
                        symbolKey("！").withReplacements("!"),
                        alphabetKey("ü").withReplacements("v", "V"),
                        alphabetKey("u").withReplacements("U"),
                        alphabetKey("i").withReplacements("I"),
                        alphabetKey("o").withReplacements("O"),
                        alphabetKey("a").withReplacements("A"),
                        } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToMathKeyboard),
                symbolKey("？").withReplacements("?"),
                alphabetKey("d").withReplacements("D"),
                alphabetKey("b").withReplacements("B"),
                alphabetKey("x").withReplacements("X"),
                alphabetKey("q").withReplacements("Q"),
                alphabetKey("j").withReplacements("J"),
                alphabetKey("e").withReplacements("E"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToLatinKeyboard),
                // 😄
                emojiKey("\uD83D\uDE04"),
                symbolKey("；").withReplacements(";"),
                alphabetKey("s").withReplacements("S"),
                alphabetKey("m").withReplacements("M"),
                alphabetKey("y").withReplacements("Y"),
                alphabetKey("p").withReplacements("P"),
                alphabetKey("g").withReplacements("G"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToEmojiKeyboard),
                symbolKey("：").withReplacements(":"),
                alphabetKey("c").withReplacements("C"),
                alphabetKey("t").withReplacements("T"),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("n").withReplacements("N"),
                alphabetKey("k").withReplacements("K"),
                this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard),
                // 😉
                emojiKey("\uD83D\uDE09"),
                symbolKey("。").withReplacements("."),
                alphabetKey("z").withReplacements("Z"),
                alphabetKey("l").withReplacements("L"),
                alphabetKey("r").withReplacements("R"),
                alphabetKey("h").withReplacements("H"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput).setDisabled(true),
                symbolKey("，").withReplacements(","),
                alphabetKey("sh").withReplacements("Sh", "SH"),
                alphabetKey("ch").withReplacements("Ch", "CH"),
                alphabetKey("zh").withReplacements("Zh", "ZH"),
                alphabetKey("f").withReplacements("F"),
                alphabetKey("w").withReplacements("W"),
                ctrlKey(CtrlKey.Type.Backspace),
                },
                };
    }

    /** 创建拼音后继字母第 1/2 级按键 */
    public Key<?>[][] createNextCharKeys(
            String level0Char, String level1Char, String level2Char, Collection<String> level1NextChars,
            Collection<String> level2NextChars
    ) {
        // 在初始键盘上显隐按键
        Key<?>[][] keys = createKeys();

        // Note: 第 1 级后继按键与键盘初始按键位置保持一致
        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];

                keys[i][j] = noopCtrlKey();
                if (!(key instanceof CharKey)) {
                    continue;
                }

                KeyColor color = key_char_special_color;
                for (String nextChar : level1NextChars) {
                    if (nextChar.length() > key.getText().length() //
                        // Note: hng 中的第 1 级按键 ng 使用 n 所在键位
                        && nextChar.startsWith(key.getText())) {
                        key = keys[i][j] = alphabetKey(nextChar).setLevel(Key.Level.level_1).setColor(color);
                        break;
                    } else if (nextChar.equals(key.getText())) {
                        key = keys[i][j] = key.setLevel(Key.Level.level_1).setColor(color);
                        break;
                    }
                }

                boolean disabled = key.getText() != null && key.getText().equals(level1Char);
                key.setDisabled(disabled);
            }
        }

        // 在指定可用位置创建第 2 级字母按键
        Iterator<String> it = level2NextChars.iterator();
        for (GridCoord keyCoord : getLevel2KeyCoords()) {
            if (!it.hasNext()) {
                break;
            }

            String text = it.next();
            String label = level0Char + text;
            int row = keyCoord.row;
            int column = keyCoord.column;
            KeyColor color = key_char_color;

            if (text == null) {
                keys[row][column] = noopCtrlKey();
            } else {
                keys[row][column] = alphabetKey(text).setLevel(Key.Level.level_2).setLabel(label).setColor(color);
            }

            boolean disabled = text != null && text.equals(level2Char);
            keys[row][column].setDisabled(disabled);
        }

        return keys;
    }

    /** 候选字按键的分页大小 */
    public int getInputCandidateKeysPageSize() {
        return countGridSize(getInputCandidateLevelKeyCoords());
    }

    /** 创建输入候选字按键 */
    public Key<?>[][] createInputCandidateKeys(
            CharInput input, List<InputWord> words, Map<String, Integer> strokes, int startIndex
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getInputCandidateKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_end = getGridLastColumnIndex();
        int index_mid = getGridMiddleColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);

        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.CommitInputList);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.DropInput);

        // 部首过滤按键
        String[] filterStrokes = PinyinInputWord.getStrokeNames();
        GridCoord[] filterStrokeKeyCorrds = getInputCandidateStrokeFilterKeyCoords();
        for (int i = 0, j = 0; i < filterStrokeKeyCorrds.length && j < filterStrokes.length; i++, j++) {
            GridCoord keyCoord = filterStrokeKeyCorrds[i];
            String stroke = filterStrokes[j];

            int row = keyCoord.row;
            int column = keyCoord.column;

            String strokeCode = PinyinInputWord.getStrokeCode(stroke);
            Integer strokeCount = strokes.get(strokeCode);

            String label = strokeCount != null ? stroke + "/" + strokeCount : stroke;
            CtrlKey.Type type = CtrlKey.Type.Filter_PinyinInputCandidate_stroke;
            CtrlKey.Option<?> option = new CtrlKey.TextOption(strokeCode);

            gridKeys[row][column] = ctrlKey(type).setOption(option).setLabel(label);
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
        GridCoord[][] levelKeyCoords = getInputCandidateLevelKeyCoords();

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

                KeyColor color = key_input_word_level_colors[level];
                InputWordKey key = InputWordKey.create(word).setColor(color);

                // 禁用已被选中的候选字按键
                if (word.equals(input.getWord())) {
                    key.setDisabled(true);
                }
                gridKeys[row][column] = key;
            }
        }

        return gridKeys;
    }

    /** 创建 输入列表 提交选项 按键 */
    public Key<?>[][] createInputListCommittingOptionKeys() {
        Key<?>[][] gridKeys = createEmptyGrid();

        int index_end = getGridLastColumnIndex();

        CtrlKey.Option<?> option = new CtrlKey.CommitInputListOption(CtrlKey.CommitInputListOption.Option.only_pinyin);
        gridKeys[1][index_end] = ctrlKey(CtrlKey.Type.Option_CommitInputList).setOption(option).setLabel("仅拼音");

        option = new CtrlKey.CommitInputListOption(CtrlKey.CommitInputListOption.Option.with_pinyin);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Option_CommitInputList).setOption(option).setLabel("带拼音");

        gridKeys[3][index_end] = ctrlKey(CtrlKey.Type.CommitInputList);

//        option = new CtrlKey.CommitInputListOption(CtrlKey.CommitInputListOption.Option.switch_simple_trad);
//        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Option_CommitInputList).setOption(option).setLabel("简/繁");

        return gridKeys;
    }

    private GridCoord[][] getInputCandidateLevelKeyCoords() {
        return new GridCoord[][] {
                // level 1
                new GridCoord[] {
                        coord(1, 6), coord(1, 5), coord(1, 4), coord(1, 3), coord(1, 2), coord(1, 1),
                        },
                // level 2
                new GridCoord[] {
                        coord(2, 6), coord(2, 5), coord(2, 4), coord(2, 3), coord(2, 2), coord(2, 1),
                        },
                // level 3
                new GridCoord[] {
                        coord(3, 6), coord(3, 5), coord(3, 3), coord(3, 2), coord(3, 1),
                        },
                // level 4
                new GridCoord[] {
                        coord(4, 6), coord(4, 5), coord(4, 4), coord(4, 3), coord(4, 2), coord(4, 1),
                        },
                // level 5
                new GridCoord[] {
                        coord(5, 6), coord(5, 5), coord(5, 4), coord(5, 3), coord(5, 2), coord(5, 1),
                        },
                };
    }

    /** 获取候选字的笔画过滤按键坐标 */
    private GridCoord[] getInputCandidateStrokeFilterKeyCoords() {
        return new GridCoord[] {
                coord(0, 6), coord(0, 5), coord(0, 4), coord(0, 3), coord(0, 2),
                };
    }

    /** 获取拼音{@link Key.Level#level_2 第二级}按键坐标 */
    private GridCoord[] getLevel2KeyCoords() {
        return new GridCoord[] {
                coord(2, 2),
                coord(3, 2),
                coord(4, 2),
                coord(5, 2),
                coord(5, 3),
                coord(4, 4),
                coord(3, 3),
                coord(4, 5),
                coord(2, 4),
                };
    }
}
