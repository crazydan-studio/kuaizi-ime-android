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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;

/**
 * {@link Keyboard.Type#Pinyin} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-30
 */
public class PinyinKeyTable extends KeyTable {

    protected PinyinKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static PinyinKeyTable create(KeyTableConfig config) {
        return new PinyinKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建 {@link PinyinKeyboard} 按键 */
    public Key[][] createGrid() {
        if (this.config.xInputPadEnabled) {
            return createXPadGrid();
        }

        return createFullKeyGrid();
    }

    @Override
    protected XPadKey createXPadKey() {
        Key[][][] zone_2_keys = createXPadZone2Keys();
        return xPadKey(Keyboard.Type.Pinyin, zone_2_keys);
    }

    /** 创建全按键布局 */
    public Key[][] createFullKeyGrid() {
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.Switch_HandMode),
                        // 😂
                        emojiKey("\uD83D\uDE02"),
                        symbolKey("！", "!"),
                        level0CharKey("ü", "v", "V"),
                        level0CharKey("i", "I"),
                        level0CharKey("u", "U"),
                        level0CharKey("o", "O"),
                        level0CharKey("j", "J"),
                        } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Math),
                symbolKey("？", "?"),
                level0CharKey("d", "D"),
                level0CharKey("m", "M"),
                level0CharKey("x", "X"),
                level0CharKey("q", "Q"),
                level0CharKey("a", "A"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Latin),
                // 😄
                emojiKey("\uD83D\uDE04"),
                symbolKey("；", ";"),
                level0CharKey("b", "B"),
                level0CharKey("l", "L"),
                level0CharKey("y", "Y"),
                level0CharKey("p", "P"),
                level0CharKey("e", "E"),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Emoji),
                symbolKey("：", ":"),
                level0CharKey("s", "S"),
                level0CharKey("t", "T"),
                ctrlKey(CtrlKey.Type.Editor_Cursor_Locator),
                level0CharKey("r", "R"),
                level0CharKey("h", "H"),
                this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Symbol),
                // 😉
                emojiKey("\uD83D\uDE09"),
                symbolKey("。", "."),
                level0CharKey("c", "C"),
                level0CharKey("z", "Z"),
                level0CharKey("f", "F"),
                level0CharKey("n", "N"),
                level0CharKey("k", "K"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput, (b) -> b.disabled(!this.config.hasRevokableInputsCommit)),
                symbolKey("，", ","),
                level0CharKey("sh", "Sh", "SH"),
                level0CharKey("ch", "Ch", "CH"),
                level0CharKey("zh", "Zh", "ZH"),
                level0CharKey("g", "G"),
                level0CharKey("w", "W"),
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    /** 创建拼音后继字母第 1/2 级按键 */
    public Key[][] createNextCharKeys(
            PinyinCharsTree charsTree, //
            String level0Char, String level1Char, String level2Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
        if (level0CharsTree == null) {
            return createEmptyGrid();
        }

        // 在初始键盘上显隐按键
        Key[][] gridKeys = createGrid();

        fillLevel1NextCharKeys(gridKeys, level0CharsTree, level0Char, level1Char);
        fillLevel2NextCharKeys(gridKeys, level0Char, level1Char, level2Char, level2NextChars);

        return gridKeys;
    }

    /** 向 <code>gridKeys</code> 填充拼音{@link CharKey.Level#level_1 第一级}后继按键 */
    protected void fillLevel1NextCharKeys(
            Key[][] gridKeys, PinyinCharsTree level0CharsTree,//
            String level0Char, String level1Char
    ) {
        // Note: 第 1 级后继按键与键盘初始按键位置保持一致
        for (int i = 0; i < gridKeys.length; i++) {
            for (int j = 0; j < gridKeys[i].length; j++) {
                Key key = gridKeys[i][j];

                gridKeys[i][j] = noopCtrlKey();
                if (!(key instanceof CharKey)) {
                    continue;
                }

                String nextChar = key.value;
                PinyinCharsTree child = level0CharsTree.getChild(nextChar);
                if (child == null) {
                    continue;
                }

                if (!child.isPinyin() && child.countChild() == 1) {
                    nextChar = child.getNextChars().get(0);
                }

                boolean disabled = Objects.equals(level1Char, nextChar);
                gridKeys[i][j] = level1CharKey(nextChar, (b) -> b.disabled(disabled));
            }
        }
    }

    /** 向 <code>gridKeys</code> 填充拼音{@link CharKey.Level#level_2 第二级}后继按键 */
    protected void fillLevel2NextCharKeys(
            Key[][] gridKeys, //
            String level0Char, String level1Char, String level2Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
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
                return;
            }
        }

        // 在指定可用位置创建第 2 级字母按键
        level2NextChars.forEach((keyLength, keys) -> {
            GridCoord[] keyCoords = getLevel2KeyCoords(keyLength);
            fillNextCharGridKeys(gridKeys, keyCoords, keys, level0Char, level2Char);
        });
    }

    /** 按指定坐标 <code>keyCoords</code> 向 <code>gridKeys</code> 填充按键 <code>keys</code> */
    protected void fillNextCharGridKeys(
            Key[][] gridKeys, GridCoord[] keyCoords, List<String> keys, String level0Char, String level2Char
    ) {
        int diff = keyCoords.length - keys.size();

        for (int i = 0; i < keys.size(); i++) {
            String text = keys.get(i);
            // 确保按键靠底部进行放置
            GridCoord keyCoord = keyCoords[i + diff];

            boolean disabled = Objects.equals(level2Char, text);
            Key key = level2CharKey(level0Char, text, (b) -> b.disabled(disabled));

            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }
    }

    /** 以韵母作为起始字母，按行创建指定声母（<code>level0Char</code>）的全部拼音组合按键 */
    public Key[][] createFullCharKeys(PinyinCharsTree charsTree, String level0Char) {
        Key[][] gridKeys = createEmptyGrid();

        String[] charOrders = new String[] { "m", "n", "g", "a", "o", "e", "i", "u", "ü" };

        PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
        if (level0CharsTree == null) {
            return createGrid();
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

        GridCoord[] gridCoords = getFullCharKeyCoords();
        for (int i = 0; i < restCharList.size(); i++) {
            String restChar = restCharList.get(i);
            GridCoord keyCoord = gridCoords[i];

            Key key = level2CharKey(level0Char, restChar);
            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }

        return gridKeys;
    }

    /** 创建 X 型输入的拼音后继字母第 1/2 级按键 */
    public Key[][] createXPadNextCharKeys(
            PinyinCharsTree charsTree, //
            String level0Char, String level1Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        // Note: 不能够先构建初始 XPadKey，再在其上直接修改分区按键，
        // 这会导致只读的 XPadKey 的 hash 值与构建时的不一致
        Key[][][] zone_2_keys = createXPadZone2Keys();

        switch (level0Char) {
            // Note: a e o 的后继仅需占用不会被作为拼音起始字母的按键位置，
            // 其余按键均保持不变，以支持连续输入下一个拼音
            case "a": {
                // a ai an ao ang
                zone_2_keys[4][1][0] = levelFinalCharKey("ai");
                zone_2_keys[4][1][1] = levelFinalCharKey("ao");
                zone_2_keys[4][1][2] = levelFinalCharKey("an");
                zone_2_keys[5][0][2] = levelFinalCharKey("ang");

                return createXPadGrid(zone_2_keys);
            }
            case "e": {
                // e ei en er eng
                zone_2_keys[4][1][0] = levelFinalCharKey("ei");
                zone_2_keys[4][1][1] = levelFinalCharKey("er");
                zone_2_keys[4][1][2] = levelFinalCharKey("en");
                zone_2_keys[5][0][2] = levelFinalCharKey("eng");

                return createXPadGrid(zone_2_keys);
            }
            case "o": {
                // o ou
                zone_2_keys[4][1][0] = levelFinalCharKey("ou");
                zone_2_keys[4][1][1] = null;
                zone_2_keys[4][1][2] = null;

                return createXPadGrid(zone_2_keys);
            }
            // Note：对 m 的单音节拼音提供连续输入支持
            case "m": {
                // m ma me mi mo mu
                zone_2_keys[5][0][2] = levelFinalCharKey("m");
                break;
            }
            case "h": {
                // hm hng ha he hu
                zone_2_keys[3][1][2] = levelFinalCharKey("hm");
                zone_2_keys[5][0][2] = levelFinalCharKey("hng");
                break;
            }
            // Note：因为可用按键位不足，故而 n 的单音节拼音只能通过释放手指输入，
            // 无法连续输入，或者以 ng 替代，因为二者的候选字是一样的
            case "n": {
                // n ng na ne ni nu nü
                zone_2_keys[3][1][2] = levelFinalCharKey("ng");
                break;
            }
        }

        if (level1Char == null) {
            PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
            if (level0CharsTree == null) {
                return createXPadGrid(zone_2_keys);
            }

            for (int i = 0; i < zone_2_keys.length; i++) {
                Key[][] zone_2_key = zone_2_keys[i];

                for (int j = 0; j < zone_2_key.length; j++) {
                    Key[] keys = zone_2_key[j];
                    // Note: 第 1 级后继按键与键盘初始按键位置保持一致
                    for (int k = 0; k < keys.length; k++) {
                        Key key = keys[k];
                        // 右下角的功能和符号按键保持不动
                        if (i + j == 1) {
                            continue;
                        } else if (((CharKey) key).level != CharKey.Level.level_final) {
                            keys[k] = null;
                        }

                        String nextChar = key.value;
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
                            zone_2_keys[layer][row][k] = levelFinalCharKey(finalChar);
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
            for (int i = 0; i < zone_2_keys.length; i++) {
                Key[][] zone_2_key = zone_2_keys[i];
                for (int j = 0; j < zone_2_key.length; j++) {
                    // 右下角的功能和符号按键保持不动
                    if (i + j == 1) {
                        continue;
                    }

                    Key[] keys = zone_2_key[j];
                    Arrays.fill(keys, null);
                }
            }

            // 在指定可用位置创建第 2 级字母按键
            level2NextChars.forEach((keyLength, keys) -> {
                GridCoord[] keyCoords = getXPadLevel2KeyCoords(keyLength);
                for (int i = 0; i < keys.size(); i++) {
                    String text = keys.get(i);
                    GridCoord keyCoord = keyCoords[i];
                    int layer = keyCoord.layer;

                    Key key = level2CharKey("", text);
                    fillGridKeyByCoord(zone_2_keys[layer], keyCoord, key);
                }
            });
        }

        return createXPadGrid(zone_2_keys);
    }

    public CharKey level0CharKey(String ch, String... replacements) {
        return alphabetKey(ch, replacements);
    }

    public CharKey level1CharKey(String ch) {
        return level1CharKey(ch, CharKey.Builder.noop);
    }

    public CharKey level1CharKey(String ch, Consumer<CharKey.Builder> c) {
        return alphabetKey(ch, (b) -> {
            b.level(CharKey.Level.level_1).color(key_char_special_color);
            c.accept(b);
        });
    }

    public CharKey levelFinalCharKey(String ch) {
        return alphabetKey(ch, (b) -> b.level(CharKey.Level.level_final).color(key_char_special_color));
    }

    public CharKey level2CharKey(String level0Char, String level2Char) {
        return level2CharKey(level0Char, level2Char, CharKey.Builder.noop);
    }

    public CharKey level2CharKey(String level0Char, String level2Char, Consumer<CharKey.Builder> c) {
        String label = level0Char + level2Char;

        return alphabetKey(level2Char, (b) -> {
            b.level(CharKey.Level.level_2).label(label).color(key_char_color);
            c.accept(b);
        });
    }

    /** 获取拼音{@link CharKey.Level#level_2 第二级}按键坐标 */
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

    /** 获取拼音{@link CharKey.Level#level_2 第二级}按键坐标 */
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

    /** 获取 X 型输入的拼音{@link CharKey.Level#level_2 第二级}按键坐标 */
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

    private Key[][] createXPadGrid(Key[][][] zone_2_keys) {
        XPadKey xPadKey = xPadKey(Keyboard.Type.Pinyin, zone_2_keys);

        return createXPadGrid(xPadKey);
    }

    /** 创建 XPad 第 2 区的按键 */
    private Key[][][] createXPadZone2Keys() {
        // 声母频率: https://www.zhihu.com/question/23111438/answer/559582999
        return new Key[][][] {
                new Key[][] {
                        new Key[] { level0CharKey("g"), level0CharKey("f"), level0CharKey("p"), }, //
                        new Key[] {
                                symbolKey("。"), ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                symbolKey("，", "；"), symbolKey("？", "："), symbolKey("！", "、"),
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
        };
    }
}
