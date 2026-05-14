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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinCharsTree;

/**
 * {@link Keyboard.Type#Pinyin} 的按键布局 v2 版
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-30
 */
public class PinyinKeyTableV2 extends PinyinKeyTable {

    protected PinyinKeyTableV2(KeyTableConfig config) {
        super(config);
    }

    public static PinyinKeyTableV2 create(KeyTableConfig config) {
        return new PinyinKeyTableV2(config);
    }

    @Override
    public Key[][] createFullKeyGrid() {
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.Switch_HandMode),
                        // 😂
                        emojiKey("\uD83D\uDE02"),
                        symbolKey("！", "!"),
                        level0CharKey("ü", "v", "V"),
                        level0CharKey("i", "I"),
                        level0CharKey("s", "S"),
                        level0CharKey("c", "C"),
                        level0CharKey("r", "R"),
                        } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Math),
                symbolKey("？", "?"),
                level0CharKey("u", "U"),
                level0CharKey("a", "A"),
                level0CharKey("k", "K"),
                level0CharKey("ch", "Ch", "CH"),
                level0CharKey("z", "Z"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Latin),
                // 😄
                emojiKey("\uD83D\uDE04"),
                symbolKey("；", ";"),
                level0CharKey("o", "O"),
                level0CharKey("e", "E"),
                level0CharKey("g", "G"),
                level0CharKey("zh", "Zh", "ZH"),
                level0CharKey("sh", "Sh", "SH"),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Emoji),
                symbolKey("：", ":"),
                level0CharKey("f", "F"),
                level0CharKey("m", "M"),
                ctrlKey(CtrlKey.Type.Editor_Cursor_Locator),
                level0CharKey("h", "H"),
                level0CharKey("t", "T"),
                this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Symbol),
                // 😉
                emojiKey("\uD83D\uDE09"),
                symbolKey("。", "."),
                level0CharKey("w", "W"),
                level0CharKey("b", "B"),
                level0CharKey("j", "J"),
                level0CharKey("l", "L"),
                level0CharKey("d", "D"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput, (b) -> b.disabled(!this.config.hasRevokableInputsCommit)),
                symbolKey("，", ","),
                level0CharKey("p", "P"),
                level0CharKey("q", "Q"),
                level0CharKey("x", "X"),
                level0CharKey("n", "N"),
                level0CharKey("y", "Y"),
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    @Override
    public Key[][] createNextCharKeys(
            PinyinCharsTree charsTree, //
            String level0Char, String level1Char, String level2Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        PinyinCharsTree level0CharsTree = charsTree.getChild(level0Char);
        if (level0CharsTree == null) {
            return createEmptyGrid();
        }

        Key[][] gridKeys = createEmptyGrid();

        if (level1Char == null) {
            level1Char = "";
        }

        Map<String, GridCoord> level1KeyCoords = new HashMap<>();
        Map<String, GridCoord> level2KeyCoords = new HashMap<>();
        if (CollectionUtils.contains(new String[] {
                "sh", "zh", "ch", "z", "r", "c", "s",
                }, level0Char)) {

            level1KeyCoords.put("i", coord(4, 5));
            level1KeyCoords.put("a", coord(4, 4));
            level1KeyCoords.put("e", coord(3, 4));
            level1KeyCoords.put("u", coord(2, 4));
            level1KeyCoords.put("o", coord(5, 4));

            switch (level1Char) {
                case "a": {
                    level2KeyCoords.put("ai", coord(3,3));
                    level2KeyCoords.put("ao", coord(4,3));
                    level2KeyCoords.put("an", coord(3,2));
                    level2KeyCoords.put("ang", coord(5,2));
                    break;
                }
                case "e": {
                    level2KeyCoords.put("ei", coord(1,4));
                    level2KeyCoords.put("en", coord(2, 5));
                    level2KeyCoords.put("eng", coord(1,5));
                    break;
                }
                case "u": {
                    level2KeyCoords.put("ui", coord(1,2));
                    level2KeyCoords.put("uo", coord(2,3));
                    level2KeyCoords.put("un", coord(1,3));
                    level2KeyCoords.put("ua", coord(0,3));
                    level2KeyCoords.put("uai", coord(0, 4));
                    level2KeyCoords.put("uan", coord(1,4));
                    level2KeyCoords.put("uang", coord(0,5));
                    break;
                }
                case "o": {
                    level2KeyCoords.put("ou", coord(5, 3));
                    level2KeyCoords.put("ong", coord(5, 2));
                    break;
                }
            }
        } else if (CollectionUtils.contains(new String[] {
                "t", "l", "n", "d", "y",
                }, level0Char)) {

            level1KeyCoords.put("e", coord(2,6));
            level1KeyCoords.put("a", coord(3,4));
            level1KeyCoords.put("i", coord(2,5));
            level1KeyCoords.put("o", coord(2,7));
            level1KeyCoords.put("u", coord(4,4));

            if (CollectionUtils.contains(new Object[]{"n","l"},level0Char)) {
                level1KeyCoords.put("ü", coord(5, 4));
            }
            if ("n".equals(level0Char)) {
                level1KeyCoords.put("g", coord(3,6));
            }

            switch (level1Char) {
                case "e": {
                    level2KeyCoords.put("ei", coord(1,5));
                    level2KeyCoords.put("en", coord(0,6));
                    level2KeyCoords.put("eng", coord(1, 6));
                    break;
                }
                case "a": {
                    level2KeyCoords.put("ai", coord(2,3));
                    level2KeyCoords.put("ao", coord(2, 4));
                    level2KeyCoords.put("an", coord(3,3));
                    level2KeyCoords.put("ang", coord(3,2));
                    break;
                }
                case "i": {
                    level2KeyCoords.put("ie", coord(1,4));
                    level2KeyCoords.put("iu", coord(1,2));
                    level2KeyCoords.put("in", coord(0,5));
                    level2KeyCoords.put("ing", coord(1,5));
                    level2KeyCoords.put("ia", coord(2,3));
                    level2KeyCoords.put("iao", coord(2,4));
                    level2KeyCoords.put("ian", coord(0,4));
                    level2KeyCoords.put("iang", coord(1,3));
                    break;
                }
                case "o": {
                    level2KeyCoords.put("ou", coord(1,7));
                    level2KeyCoords.put("ong", coord(1, 6));
                    break;
                }
                case "u": {
                    level2KeyCoords.put("uo", coord(2,3));
                    level2KeyCoords.put("ue", coord(4,3));
                    level2KeyCoords.put("ui", coord(4,3));
                    level2KeyCoords.put("un", coord(3,3));
                    level2KeyCoords.put("uan", coord(3,2));
                    break;
                }
                case "ü": {
                    level2KeyCoords.put("üe", coord(5,3));
                    break;
                }
            }
        } else if (CollectionUtils.contains(new String[] {
                "k", "g", "h",
                }, level0Char)) {

            level1KeyCoords.put("a", coord(2, 4));
            level1KeyCoords.put("o", coord(3, 3));
            level1KeyCoords.put("u", coord(3, 4));
            level1KeyCoords.put("e", coord(4, 5));
            level1KeyCoords.put("ng", coord(4, 6));
            level1KeyCoords.put("m", coord(3, 7));

            switch (level1Char) {
                case "a": {
                    level2KeyCoords.put("ai", coord(1, 3));
                    level2KeyCoords.put("ao", coord(1, 4));
                    level2KeyCoords.put("an", coord(2, 5));
                    level2KeyCoords.put("ang", coord(1, 5));
                    break;
                }
                case "o": {
                    level2KeyCoords.put("ou", coord(4, 4));
                    level2KeyCoords.put("ong", coord(4, 3));
                    break;
                }
                case "u": {
                    level2KeyCoords.put("un", coord(1, 4));
                    level2KeyCoords.put("ui", coord(2, 5));
                    level2KeyCoords.put("uo", coord(1, 5));
                    level2KeyCoords.put("ua", coord(3, 5));
                    level2KeyCoords.put("uai", coord(2, 6));
                    level2KeyCoords.put("uan", coord(3, 6));
                    level2KeyCoords.put("uang", coord(2, 7));
                    break;
                }
                case "e": {
                    level2KeyCoords.put("en", coord(5, 5));
                    level2KeyCoords.put("eng", coord(5, 4));
                    break;
                }
            }
        } else if (CollectionUtils.contains(new String[] {
                "f", "m", "w", "b", "p",
                }, level0Char)) {

            level1KeyCoords.put("i", coord(2, 4));
            level1KeyCoords.put("e", coord(3, 4));
            level1KeyCoords.put("a", coord(3, 5));
            level1KeyCoords.put("u", coord(4, 5));
            level1KeyCoords.put("o", coord(4, 6));

            switch (level1Char) {
                case "i": {
                    level2KeyCoords.put("iu", coord(2, 3));
                    level2KeyCoords.put("ie", coord(1, 3));
                    level2KeyCoords.put("ian", coord(1, 4));
                    level2KeyCoords.put("iao", coord(1, 5));
                    level2KeyCoords.put("in", coord(2, 5));
                    level2KeyCoords.put("ing", coord(2, 6));
                    break;
                }
                case "e": {
                    level2KeyCoords.put("ei", coord(3, 3));
                    level2KeyCoords.put("en", coord(4, 4));
                    level2KeyCoords.put("eng", coord(5, 3));
                    break;
                }
                case "a": {
                    level2KeyCoords.put("ai", coord(2, 5));
                    level2KeyCoords.put("ao", coord(2, 6));
                    level2KeyCoords.put("an", coord(3, 6));
                    level2KeyCoords.put("ang", coord(2, 7));
                    break;
                }
                case "o": {
                    level2KeyCoords.put("ou", coord(5, 5));
                    break;
                }
            }
        } else if (CollectionUtils.contains(new String[] {
                "j", "x", "q",
                }, level0Char)) {

            level1KeyCoords.put("i", coord(3, 4));
            level1KeyCoords.put("u", coord(4, 6));

            switch (level1Char) {
                case "i": {
                    level2KeyCoords.put("iu", coord(3, 3));
                    level2KeyCoords.put("ie", coord(2, 4));
                    level2KeyCoords.put("iong", coord(1, 4));
                    level2KeyCoords.put("in", coord(2, 5));
                    level2KeyCoords.put("ing", coord(1, 5));
                    level2KeyCoords.put("ia", coord(3, 5));
                    level2KeyCoords.put("iao", coord(2, 6));
                    level2KeyCoords.put("ian", coord(3, 6));
                    level2KeyCoords.put("iang", coord(2, 7));
                    break;
                }
                case "u": {
                    level2KeyCoords.put("ue", coord(3, 5));
                    level2KeyCoords.put("un", coord(4, 5));
                    level2KeyCoords.put("uan", coord(5, 5));
                    break;
                }
            }
        } else if (CollectionUtils.contains(new String[] {
                "a", "o", "e",
                }, level0Char)) {

            switch (level0Char) {
                case "e": {
                    level1KeyCoords.put("i", coord(0, 4));
                    level1KeyCoords.put("r", coord(1, 4));
                    level1KeyCoords.put("n", coord(3, 3));
                    level1KeyCoords.put("ng", coord(4, 3));
                    break;
                }
                case "a": {
                    level1KeyCoords.put("i", coord(0, 4));
                    level1KeyCoords.put("o", coord(2, 3));
                    level1KeyCoords.put("n", coord(3, 3));
                    level1KeyCoords.put("ng", coord(4, 3));
                    break;
                }
                case "o": {
                    level1KeyCoords.put("u", coord(1, 2));
                    break;
                }
            }
        }

        String finalLevel1Char = level1Char;
        level1KeyCoords.forEach((ch, coord) -> {
            boolean disabled = Objects.equals(finalLevel1Char, ch);
            Key key = level1CharKey(ch, (b) -> b.disabled(disabled));

            fillGridKeyByCoord(gridKeys, coord, key);
        });

        List<String> level2KeyChars = new ArrayList<>();
        level2NextChars.forEach((len, chars) -> {
            level2KeyChars.addAll(chars);
        });
        level2KeyCoords.forEach((ch, coord) -> {
            if (!level2KeyChars.contains(ch)) {
                return;
            }

            boolean disabled = Objects.equals(level2Char, ch);
            Key key = level2CharKey("", ch, (b) -> b.disabled(disabled));

            fillGridKeyByCoord(gridKeys, coord, key);
        });

        return gridKeys;
    }
}
