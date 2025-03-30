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

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
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
                        level0CharKey("s", "S"),
                        level0CharKey("c", "C"),
                        level0CharKey("q", "Q"),
                        level0CharKey("zh", "Zh", "ZH"),
                        level0CharKey("ch", "Ch", "CH"),
                        } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Math),
                symbolKey("？", "?"),
                level0CharKey("p", "P"),
                level0CharKey("z", "Z"),
                level0CharKey("x", "X"),
                level0CharKey("b", "B"),
                level0CharKey("sh", "Sh", "SH"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Latin),
                // 😄
                emojiKey("\uD83D\uDE04"),
                symbolKey("；", ";"),
                level0CharKey("w", "W"),
                level0CharKey("ü", "v", "V"),
                level0CharKey("u", "U"),
                level0CharKey("d", "D"),
                level0CharKey("y", "Y"),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Emoji),
                symbolKey("：", ":"),
                level0CharKey("f", "F"),
                level0CharKey("o", "O"),
                ctrlKey(CtrlKey.Type.Editor_Cursor_Locator),
                level0CharKey("i", "I"),
                level0CharKey("m", "M"),
                this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Symbol),
                // 😉
                emojiKey("\uD83D\uDE09"),
                symbolKey("。", "."),
                level0CharKey("k", "K"),
                level0CharKey("e", "E"),
                level0CharKey("a", "A"),
                level0CharKey("l", "L"),
                level0CharKey("n", "N"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput, (b) -> b.disabled(!this.config.hasRevokableInputsCommit)),
                symbolKey("，", ","),
                level0CharKey("r", "R"),
                level0CharKey("j", "J"),
                level0CharKey("t", "T"),
                level0CharKey("g", "G"),
                level0CharKey("h", "H"),
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    @Override
    protected void fillLevel1NextCharKeys(
            Key[][] gridKeys, PinyinCharsTree level0CharsTree,//
            String level0Char, String level1Char
    ) {
        super.fillLevel1NextCharKeys(gridKeys, level0CharsTree, level0Char, level1Char);

        switch (level0Char) {
            case "h": {
                gridKeys[3][5] = gridKeys[3][6];
                gridKeys[3][4] = gridKeys[4][7];
                gridKeys[3][6] = gridKeys[4][7] = noopCtrlKey();
                break;
            }
            case "n": {
                gridKeys[3][4] = gridKeys[5][5];
                gridKeys[5][5] = noopCtrlKey();
                break;
            }
        }
    }

    @Override
    protected void fillLevel2NextCharKeys(
            Key[][] gridKeys, //
            String level0Char, String level1Char, String level2Char, //
            Map<Integer, List<String>> level2NextChars
    ) {
        List<String> keyChars = new ArrayList<>();
        level2NextChars.forEach((len, chars) -> {
            keyChars.addAll(chars);
        });

        Map<String, GridCoord> coords = getLevel2KeyCoords(level1Char);
        coords.forEach((ch, coord) -> {
            if (!keyChars.contains(ch)) {
                return;
            }

            boolean disabled = Objects.equals(level2Char, ch);
            Key key = level2CharKey("", ch, (b) -> b.disabled(disabled));
            fillGridKeyByCoord(gridKeys, coord, key);
        });
    }

    /** 根据拼音{@link CharKey.Level#level_1 第一级}按键，获取第二级按键坐标 */
    private Map<String, GridCoord> getLevel2KeyCoords(String level1Char) {
        if (level1Char == null) {
            return new HashMap<>();
        }

        switch (level1Char) {
            case "i": {
                return new HashMap<String, GridCoord>() {{
                    put("iao", coord(1, 6));
                    put("iang", coord(1, 7));
                    put("ia", coord(2, 6));
                    put("ian", coord(2, 7));
                    put("in", coord(3, 6));
                    put("ing", coord(3, 7));
                    put("ie", coord(4, 6));
                    put("iu", coord(4, 7));
                    put("iong", coord(5, 6));
                }};
            }
            case "u": {
                return new HashMap<String, GridCoord>() {{
                    put("un", coord(0, 5));
                    put("uo", coord(0, 6));
                    put("ui", coord(1, 4));
                    put("ue", coord(1, 5));
                    put("uai", coord(1, 6));
                    put("uang", coord(1, 7));
                    put("ua", coord(2, 6));
                    put("uan", coord(2, 7));
                }};
            }
            case "a": {
                return new HashMap<String, GridCoord>() {{
                    put("ao", coord(4, 6));
                    put("ang", coord(5, 4));
                    put("an", coord(5, 5));
                    put("ai", coord(5, 6));
                }};
            }
            case "e": {
                return new HashMap<String, GridCoord>() {{
                    put("ei", coord(4, 3));
                    put("er", coord(5, 2));
                    put("en", coord(5, 3));
                    put("eng", coord(5, 4));
                }};
            }
            case "o": {
                return new HashMap<String, GridCoord>() {{
                    put("ou", coord(3, 2));
                    put("ong", coord(2, 3));
                }};
            }
            case "ü": {
                return new HashMap<String, GridCoord>() {{
                    put("üe", coord(1, 3));
                }};
            }
        }
        return new HashMap<>();
    }
}
