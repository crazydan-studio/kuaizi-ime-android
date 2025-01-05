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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;

/**
 * {@link Keyboard 键盘}的按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public abstract class KeyTable {
    /** {@link CharKey} 按键的配色 */
    protected static final Key.Color key_char_color = Key.Color.create(R.attr.key_fg_color, R.attr.key_bg_color);
    /** 特殊的 {@link CharKey} 按键的配色 */
    protected static final Key.Color key_char_special_color = Key.Color.create(R.attr.key_highlight_fg_color,
                                                                               R.attr.key_bg_color);
    /** {@link CharKey.Type#Symbol} 按键的配色 */
    protected static final Key.Color key_char_symbol_color = Key.Color.create(R.attr.key_char_symbol_fg_color,
                                                                              R.attr.key_char_symbol_bg_color);
    /** {@link CharKey.Type#Emoji} 按键的配色 */
    protected static final Key.Color key_char_emoji_color = key_char_symbol_color;

    private static final Key.Color key_char_level_0_color = Key.Color.create(R.attr.key_char_level_0_fg_color,
                                                                             R.attr.key_char_level_0_bg_color);
    private static final Key.Color key_char_level_1_color = Key.Color.create(R.attr.key_char_level_1_fg_color,
                                                                             R.attr.key_char_level_1_bg_color);
    private static final Key.Color key_char_level_2_color = Key.Color.create(R.attr.key_char_level_2_fg_color,
                                                                             R.attr.key_char_level_2_bg_color);
    private static final Key.Color key_char_level_3_color = Key.Color.create(R.attr.key_char_level_3_fg_color,
                                                                             R.attr.key_char_level_3_bg_color);
    private static final Key.Color key_char_level_4_color = Key.Color.create(R.attr.key_char_level_4_fg_color,
                                                                             R.attr.key_char_level_4_bg_color);
    private static final Key.Color key_char_level_5_color = Key.Color.create(R.attr.key_char_level_5_fg_color,
                                                                             R.attr.key_char_level_5_bg_color);
    /** OK 按键的样式 */
    private static final Key.Style key_ctrl_ok_style = Key.Style.withIcon(R.drawable.ic_right_hand_ok,
                                                                          R.drawable.ic_left_hand_ok,
                                                                          R.attr.key_ctrl_ok_bg_color);
    /** 文本类控制按键的样式 */
    private static final Key.Style key_ctrl_label_style = Key.Style.withColor(R.attr.key_ctrl_label_color,
                                                                              R.attr.key_bg_color);
    private static final Key.Style key_ctrl_noop_style = Key.Style.withColor(R.attr.key_ctrl_noop_fg_color,
                                                                             R.attr.key_ctrl_noop_bg_color);

    /** {@link InputWordKey} 按键的配色 */
    protected static final Key.Color[] key_input_word_level_colors = new Key.Color[] {
            key_char_level_0_color,
            key_char_level_1_color,
            key_char_level_2_color,
            key_char_level_3_color,
            key_char_level_4_color,
            };

    /** 字母按键调色板 */
    private static final Map<List<String>, Key.Color> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, Key.Style> ctrl_key_styles = new HashMap<>();

    static {
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü", "v"), key_char_special_color);
        char_key_color_palette.put(Arrays.asList("ch", "sh", "zh"), key_char_special_color);
        char_key_color_palette.put(Arrays.asList("w", "z", "x", "y"), key_char_color);
        char_key_color_palette.put(Arrays.asList("f", "g", "d", "b", "c"), key_char_color);
        char_key_color_palette.put(Arrays.asList("p", "q", "n", "s", "t", "r"), key_char_color);
        char_key_color_palette.put(Arrays.asList("h", "k", "j", "m", "l"), key_char_color);
        char_key_color_palette.put(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                                   key_char_special_color);

        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            Key.Style.withIcon(R.drawable.ic_backspace, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Space,
                            Key.Style.withIcon(R.drawable.ic_space, R.attr.key_ctrl_space_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            Key.Style.withIcon(R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Commit_InputList,
                            Key.Style.withIcon(R.drawable.ic_commit, R.attr.key_ctrl_commit_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Commit_InputList_Option, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.Switch_IME,
                            Key.Style.withIcon(R.drawable.ic_keyboard, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Switch_HandMode,
                            Key.Style.withIcon(R.drawable.ic_switch_to_left_hand,
                                               R.drawable.ic_switch_to_right_hand,
                                               R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Toggle_Symbol_Group, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Emoji_Group, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.Editor_Cursor_Locator,
                            Key.Style.withIcon(R.drawable.ic_input_cursor, R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Editor_Range_Selector,
                            Key.Style.withIcon(R.drawable.ic_right_hand_selection,
                                               R.drawable.ic_left_hand_selection,
                                               R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Exit,
                            Key.Style.withIcon(R.drawable.ic_right_hand_exit,
                                               R.drawable.ic_left_hand_exit,
                                               R.attr.key_ctrl_exit_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.DropInput,
                            Key.Style.withIcon(R.drawable.ic_trash_can, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.ConfirmInput,
                            Key.Style.withIcon(R.drawable.ic_right_hand_ok,
                                               R.drawable.ic_left_hand_ok,
                                               R.attr.key_ctrl_confirm_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.RevokeInput,
                            Key.Style.withIcon(R.drawable.ic_revoke_input, R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Toggle_Pinyin_Spell, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinCandidate_by_Spell, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinCandidate_advance,
                            Key.Style.withIcon(R.drawable.ic_filter_empty, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Confirm_PinyinCandidate_Filter,
                            Key.Style.withIcon(R.drawable.ic_right_hand_ok,
                                               R.drawable.ic_left_hand_ok,
                                               R.attr.key_ctrl_confirm_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.NoOp, key_ctrl_noop_style);
        ctrl_key_styles.put(CtrlKey.Type.Edit_Editor, key_ctrl_label_style);
    }

    protected final KeyTableConfig config;

    protected KeyTable(KeyTableConfig config) {
        this.config = config;
    }

    // ======================= Start: 网格 =======================

    /** 统计网格的大小 */
    protected static <T> int countGridSize(T[][] grid) {
        int size = 0;

        for (T[] row : grid) {
            size += row.length;
        }
        return size;
    }

    protected abstract Key[][] initGrid();

    protected Key[][] createEmptyGrid() {
        Key[][] gridKeys = initGrid();
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        return gridKeys;
    }

    protected int getGridFirstColumnIndex() {
        return 0;
    }

    protected int getGridLastColumnIndex() {
        return initGrid()[0].length - 1;
    }

    protected int getGridMiddleColumnIndex() {
        int length = initGrid()[0].length;

        if (length % 2 == 0) {
            return length / 2;
        }
        return length / 2 + 1;
    }

    protected void fillGridKeyByCoord(Key[][] gridKeys, GridCoord coord, Key key) {
        int row = coord.row;
        int column = coord.column;

        gridKeys[row][column] = key;
    }

    /** 根据网格坐标向网格填充按键 */
    protected void fillGridKeysByCoord(Key[][] gridKeys, GridCoord[][] keyCoords, Key[] keys) {
        int dataIndex = 0;
        for (GridCoord[] coords : keyCoords) {
            for (GridCoord coord : coords) {
                if (dataIndex >= keys.length) {
                    break;
                }

                Key key = keys[dataIndex++];
                fillGridKeyByCoord(gridKeys, coord, key);
            }
        }
    }

    /** 根据网格坐标向网格填充等级按键 */
    protected <T> void fillGridLevelKeysByCoord(
            Key[][] gridKeys, GridCoord[][] levelKeyCoords, //
            List<T> dataList, int startIndex, //
            BiFunction<T, Integer, Key> keyCreator
    ) {
        int dataSize = dataList.size();
        int dataIndex = startIndex;

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            GridCoord[] keyCoords = levelKeyCoords[level];
            for (GridCoord keyCoord : keyCoords) {
                if (dataIndex >= dataSize) {
                    break;
                }

                T data = dataList.get(dataIndex++);
                if (data != null) {
                    Key key = keyCreator.apply(data, level);
                    fillGridKeyByCoord(gridKeys, keyCoord, key);
                }
            }
        }
    }

    // ======================= End: 网格 =======================

    // ======================= Start: 坐标 =======================

    /** 创建{@link GridCoord 网格坐标} */
    public GridCoord coord(int row, int column) {
        return coord(row, column, 0);
    }

    /** 创建{@link GridCoord 网格坐标} */
    public GridCoord coord(int row, int column, int layer) {
        return new GridCoord(row, column, layer);
    }

    protected GridCoord[][] getLevelKeyCoords() {
        return getLevelKeyCoords(false);
    }

    protected GridCoord[][] getLevelKeyCoords(boolean containsMiddle) {
        return new GridCoord[][] {
                // level 1
                new GridCoord[] {
                        coord(1, 6), coord(1, 5),
                        //
                        coord(1, 4), coord(1, 3),
                        //
                        coord(1, 2), coord(1, 1),
                        },
                // level 2
                new GridCoord[] {
                        coord(2, 6), coord(2, 5),
                        //
                        coord(2, 4), coord(2, 3),
                        //
                        coord(2, 2), coord(2, 1),
                        },
                // level 3
                containsMiddle ? new GridCoord[] {
                        coord(3, 6), coord(3, 5),
                        //
                        coord(3, 4), coord(3, 3),
                        //
                        coord(3, 2), coord(3, 1),
                        } : new GridCoord[] {
                        coord(3, 6), coord(3, 5),
                        //
                        coord(3, 3), coord(3, 2),
                        //
                        coord(3, 1),
                        },
                // level 4
                new GridCoord[] {
                        coord(4, 6), coord(4, 5),
                        //
                        coord(4, 4), coord(4, 3),
                        //
                        coord(4, 2), coord(4, 1),
                        },
                // level 5
                new GridCoord[] {
                        coord(5, 6), coord(5, 5),
                        //
                        coord(5, 4), coord(5, 3),
                        //
                        coord(5, 2), coord(5, 1),
                        },
                };
    }

    // ======================= End: 坐标 =======================

    // ======================= Start: 按键 =======================

    public CtrlKey enterCtrlKey() {
        Key.Style style = this.config.keyboard.useSingleLineInputMode ? key_ctrl_ok_style : null;

        return ctrlKey(CtrlKey.Type.Enter, style, CtrlKey.Builder.noop);
    }

    /** 创建 键盘切换 控制按键 */
    public CtrlKey switcherCtrlKey(Keyboard.Type type) {
        return switcherCtrlKey(type, CtrlKey.Builder.noop);
    }

    /** 创建 键盘切换 控制按键 */
    public CtrlKey switcherCtrlKey(Keyboard.Type type, int icon) {
        return switcherCtrlKey(type, icon, CtrlKey.Builder.noop);
    }

    public CtrlKey switcherCtrlKey(Keyboard.Type type, int icon, Consumer<CtrlKey.Builder> c) {
        return switcherCtrlKey(type, (b) -> {
            b.icon(icon);
            c.accept(b);
        });
    }

    private CtrlKey switcherCtrlKey(Keyboard.Type type, Consumer<CtrlKey.Builder> c) {
        CtrlKey.Option<Keyboard.Type> option = new CtrlKey.Option<>(type);

        Key.Style style = null;
        switch (type) {
            case Math:
                style = Key.Style.withIcon(R.drawable.ic_calculator, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Latin:
                style = Key.Style.withIcon(R.drawable.ic_switch_to_latin, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Pinyin:
                style = Key.Style.withIcon(R.drawable.ic_switch_to_pinyin, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Emoji:
                style = Key.Style.withIcon(R.drawable.ic_emoji, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Symbol:
                style = Key.Style.withIcon(R.drawable.ic_symbol, R.attr.key_ctrl_switcher_bg_color);
                break;
        }

        return ctrlKey(CtrlKey.Type.Switch_Keyboard, style, (b) -> {
            b.option(option);
            c.accept(b);
        });
    }

    /** 占位按键，且不触发事件 */
    public CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    /** 占位按键，且不触发事件 */
    public CtrlKey noopCtrlKey(String label) {
        return ctrlKey(CtrlKey.Type.NoOp, (b) -> b.label(label));
    }

    public CtrlKey ctrlKey(CtrlKey.Type type) {
        return ctrlKey(type, CtrlKey.Builder.noop);
    }

    public CtrlKey ctrlKey(CtrlKey.Type type, Consumer<CtrlKey.Builder> c) {
        return ctrlKey(type, null, c);
    }

    public CharKey alphabetKey(String value, String... replacements) {
        return alphabetKey(value, (b) -> b.replacements(replacements));
    }

    public CharKey alphabetKey(String value, Consumer<CharKey.Builder> c) {
        return charKey(CharKey.Type.Alphabet, value, c);
    }

    public CharKey symbolKey(String value, String... replacements) {
        return symbolKey(value, (b) -> b.replacements(replacements));
    }

    public CharKey symbolKey(String value, Consumer<CharKey.Builder> c) {
        return charKey(CharKey.Type.Symbol, value, c);
    }

    public CharKey numberKey(String value) {
        return numberKey(value, CharKey.Builder.noop);
    }

    public CharKey numberKey(String value, Consumer<CharKey.Builder> c) {
        return charKey(CharKey.Type.Number, value, c);
    }

    public CharKey emojiKey(String value) {
        return charKey(CharKey.Type.Emoji, value, CharKey.Builder.noop);
    }

    private static CharKey charKey(CharKey.Type type, String value, Consumer<CharKey.Builder> c) {
        Key.Color color = null;
        switch (type) {
            case Emoji: {
                color = key_char_emoji_color;
                break;
            }
            case Symbol: {
                color = key_char_symbol_color;
                break;
            }
            default: {
                for (Map.Entry<List<String>, Key.Color> entry : char_key_color_palette.entrySet()) {
                    Key.Color keyColor = entry.getValue();

                    if (entry.getKey().contains(value)) {
                        color = keyColor;
                        break;
                    }
                }
            }
        }

        Key.Color finalColor = color;
        return CharKey.build((b) -> {
            b.type(type).value(value).label(value).color(finalColor);
            c.accept(b);
        });
    }

    private CtrlKey ctrlKey(CtrlKey.Type type, Key.Style style, Consumer<CtrlKey.Builder> c) {
        Integer icon = null;
        Key.Color color = null;

        if (style == null) {
            style = ctrl_key_styles.get(type);
        }

        if (style != null) {
            color = style.color;
            switch (this.config.keyboard.handMode) {
                case left: {
                    icon = style.icon.left;
                    break;
                }
                case right: {
                    icon = style.icon.right;
                    break;
                }
            }
        }

        Integer finalIcon = icon;
        Key.Color finalColor = color;
        return CtrlKey.build((b) -> {
            b.type(type).icon(finalIcon).color(finalColor);
            c.accept(b);
        });
    }

    public InputWordKey inputWordKey(InputWord word, int level) {
        return inputWordKey(word, level, InputWordKey.Builder.noop);
    }

    protected InputWordKey inputWordKey(InputWord word, int level, Consumer<InputWordKey.Builder> c) {
        Key.Color color = key_input_word_level_colors[level];

        return InputWordKey.build((b) -> {
            b.word(word).color(color);
            c.accept(b);
        });
    }

    protected SymbolKey symbolKey(Symbol symbol, int level) {
        Key.Color color = key_input_word_level_colors[level];

        return SymbolKey.build((b) -> b.symbol(symbol).color(color));
    }

    // ======================= Start: 按键 =======================

    // ======================= Start: X Pad 按键 =======================

    protected Key[][] createKeysForXPad() {
        return createKeysForXPad(createXPadKey());
    }

    protected Key[][] createKeysForXPad(XPadKey xPadKey) {
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.Switch_HandMode),
                        //
                        null, null, null, null, null, null,
                        //
                        null,
                        } //
                , new Key[] {
                null,
                //
                null, null, null, null, null, null,
                //
                null,
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Emoji),
                //
                null, null, null, null, null, null,
                //
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Symbol),
                //
                null, null, null, xPadKey, null, null,
                //
                this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput, (b) -> b.disabled(!this.config.keyboard.hasRevokableInputsCommit)),
                //
                null, null, null, null, null, null,
                //
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    protected XPadKey createXPadKey() {
        return null;
    }

    /**
     * @param zone_2_keys
     *         按键布局规则：<ul>
     *         <li>拇指遮挡部分不放置或少放置输入按键，最好仅放置不常用的空格或回删等功能按键；</li>
     *         <li>常用输入按键尽可能放置在辐射轴线的第一的位置，且按顺时针输入为最佳方向；</li>
     *         <li>数字类按键最好全部向顺时针方向输入，以便于顺畅地连续输入。其余符号按键向逆时针方向输入；</li>
     *         <li>按键均往辐射轴线的外侧排列，以留下更多的视觉空间，从而降低手指遮挡概率；</li>
     *         </ul>
     */
    protected XPadKey xPadKey(Keyboard.Type activeKeyboard, Key[][][] zone_2_keys) {
        Key[] zone_1_keys = new Key[] {
                switcherCtrlKey(Keyboard.Type.Latin,
                                R.drawable.ic_latin,
                                (b) -> b.disabled(activeKeyboard == Keyboard.Type.Latin)),
                switcherCtrlKey(Keyboard.Type.Pinyin,
                                R.drawable.ic_pinyin,
                                (b) -> b.disabled(activeKeyboard == Keyboard.Type.Pinyin)),
                switcherCtrlKey(Keyboard.Type.Number,
                                R.drawable.ic_number,
                                (b) -> b.disabled(activeKeyboard == Keyboard.Type.Number)),
                null,
                null,
                switcherCtrlKey(Keyboard.Type.Math,
                                R.drawable.ic_math,
                                (b) -> b.disabled(activeKeyboard == Keyboard.Type.Math)),
                };

        return XPadKey.build((b) -> b.zone_0_key(ctrlKey(CtrlKey.Type.Editor_Cursor_Locator))
                                     .zone_1_keys(zone_1_keys)
                                     .zone_2_keys(zone_2_keys));
    }

    // ======================= End: X Pad 按键 =======================

    public static class GridCoord {
        public final int row;
        public final int column;
        public final int layer;

        public GridCoord(int row, int column, int layer) {
            this.row = row;
            this.column = column;
            this.layer = layer;
        }
    }
}
