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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Point;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.Symbol;
import org.crazydan.studio.app.ime.kuaizi.internal.data.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.SymbolKey;

/**
 * {@link Keyboard 键盘}的按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public abstract class KeyTable {
    /** 字母按键调色板 */
    private static final Map<List<String>, KeyColor> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, KeyStyle> ctrl_key_styles = new HashMap<>();
    /** 环绕型布局的字符按键的配色：从内到外分为不同层级 */
    protected static final KeyColor[] key_char_around_level_colors = new KeyColor[] {
            KeyColor.create(R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color),
            KeyColor.create(R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color),
            KeyColor.create(R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color),
            KeyColor.create(R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color),
            };
    /** 拉丁文键盘字符按键的配色 */
    protected static final KeyColor[] latin_key_char_alphabet_level_colors = new KeyColor[] {
            KeyColor.create(R.attr.key_char_level_0_fg_color, R.attr.key_char_level_0_bg_color),
            KeyColor.create(R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color),
            KeyColor.create(R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color),
            KeyColor.create(R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color),
            KeyColor.create(R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color),
            };
    /** {@link CharKey.Type#Symbol 标点符号}按键的配色 */
    private static final KeyColor key_char_symbol_color = KeyColor.create(R.attr.key_char_symbol_fg_color,
                                                                          R.attr.key_char_symbol_bg_color);
    /** {@link CharKey.Type#Emoji 表情符号}按键的配色 */
    private static final KeyColor key_char_emoji_color = KeyColor.create(R.attr.key_fg_color, R.attr.key_bg_color);
    /** OK 按键的样式 */
    private static final KeyStyle key_ctrl_ok_style = KeyStyle.withIcon(R.drawable.ic_right_hand_ok,
                                                                        R.drawable.ic_left_hand_ok,
                                                                        R.attr.key_ctrl_ok_bg_color);

    /** 表情符号的分组按键坐标 */
    private static final int[][] symbol_emoji_group_key_coords = new int[][] {
            new int[] { 1, 7 },
            new int[] { 0, 7 },
            new int[] { 0, 6 },
            new int[] { 0, 5 },
            new int[] { 0, 4 },
            new int[] { 0, 3 },
            new int[] { 0, 2 },
            new int[] { 0, 1 },
            new int[] { 1, 0 },
            new int[] { 2, 0 },
            };

    /** 表情符号的分组按键坐标 */
    private static final int[][] symbol_emoji_group_key_coords_left_hand = new int[][] {
            new int[] { 1, 0 },
            new int[] { 0, 1 },
            new int[] { 0, 2 },
            new int[] { 0, 3 },
            new int[] { 0, 4 },
            new int[] { 0, 5 },
            new int[] { 0, 6 },
            new int[] { 0, 7 },
            new int[] { 1, 7 },
            new int[] { 2, 7 },
            };
    private static final int[][][] symbol_emoji_key_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 1, 6 },
                    new int[] { 1, 5 },
                    new int[] { 1, 4 },
                    new int[] { 1, 3 },
                    new int[] { 1, 2 },
                    new int[] { 1, 1 },
                    },
            // level 2
            new int[][] {
                    new int[] { 2, 6 },
                    new int[] { 2, 5 },
                    new int[] { 2, 4 },
                    new int[] { 2, 3 },
                    new int[] { 2, 2 },
                    new int[] { 2, 1 },
                    },
            // level 3
            new int[][] {
                    new int[] { 3, 6 }, new int[] { 3, 5 },
                    //new int[] { 3, 4 },
                    new int[] { 3, 3 }, new int[] { 3, 2 }, new int[] { 3, 1 },
                    },
            // level 4
            new int[][] {
                    new int[] { 4, 6 },
                    new int[] { 4, 5 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 4, 2 },
                    new int[] { 4, 1 },
                    },
            // level 5
            new int[][] {
                    new int[] { 5, 6 },
                    new int[] { 5, 5 },
                    new int[] { 5, 4 },
                    new int[] { 5, 3 },
                    new int[] { 5, 2 },
                    new int[] { 5, 1 },
                    },
            };

    /** 从中心按键由内到外的数字按键环形布局坐标 */
    private static final int[][][] number_key_around_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 2, 3 },
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    new int[] { 5, 4 },
                    new int[] { 5, 5 },
                    new int[] { 5, 2 },
                    new int[] { 5, 1 },
                    },
            // level 2
            new int[][] {
                    new int[] { 5, 3 },
                    new int[] { 4, 5 },
                    new int[] { 3, 5 },
                    new int[] { 2, 5 },
                    new int[] { 1, 4 },
                    new int[] { 1, 3 },
                    new int[] { 1, 2 },
                    new int[] { 2, 2 },
                    new int[] { 3, 1 },
                    new int[] { 4, 2 },
                    },
            };

    static {
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü", "v"),
                                   KeyColor.create(R.attr.key_char_level_0_fg_color, R.attr.key_char_level_0_bg_color));
        char_key_color_palette.put(Arrays.asList("ch", "sh", "zh"),
                                   KeyColor.create(R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color));
        char_key_color_palette.put(Arrays.asList("w", "z", "x", "y"),
                                   KeyColor.create(R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color));
        char_key_color_palette.put(Arrays.asList("f", "g", "d", "b", "c"),
                                   KeyColor.create(R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color));
        char_key_color_palette.put(Arrays.asList("p", "q", "n", "s", "t", "r"),
                                   KeyColor.create(R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color));
        char_key_color_palette.put(Arrays.asList("h", "k", "j", "m", "l"),
                                   KeyColor.create(R.attr.key_char_level_5_fg_color, R.attr.key_char_level_5_bg_color));
        char_key_color_palette.put(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                                   KeyColor.create(R.attr.key_char_level_5_fg_color, R.attr.key_char_level_5_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            KeyStyle.withIcon(R.drawable.ic_backspace, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Space, KeyStyle.withIcon(R.drawable.ic_space, R.attr.key_ctrl_space_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            KeyStyle.withIcon(R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.CommitInputList,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_like,
                                              R.drawable.ic_left_hand_like,
                                              R.attr.key_ctrl_commit_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.SwitchIME,
                            KeyStyle.withIcon(R.drawable.ic_keyboard, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchHandMode,
                            KeyStyle.withIcon(R.drawable.ic_switch_to_left_hand,
                                              R.drawable.ic_switch_to_right_hand,
                                              R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchToMathKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_calculator, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchToLatinKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_switch_zi_to_a, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchToPinyinKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_switch_a_to_zi, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchToNumberKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_alphabet_number, R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.SwitchToSymbolKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_symbol, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.SwitchToEmojiKeyboard,
                            KeyStyle.withIcon(R.drawable.ic_emoji, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Symbol_Group,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Emoji_Group,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_pointer,
                                              R.drawable.ic_left_hand_pointer,
                                              R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor_Locator,
                            KeyStyle.withIcon(R.drawable.ic_map_location_pin, R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor_Selector,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_selection,
                                              R.drawable.ic_left_hand_selection,
                                              R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Exit,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_exit,
                                              R.drawable.ic_left_hand_exit,
                                              R.attr.key_ctrl_exit_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.DropInput,
                            KeyStyle.withIcon(R.drawable.ic_trash_can, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.ConfirmInput,
                            KeyStyle.withIcon(R.drawable.ic_confirm, R.attr.key_ctrl_confirm_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.RevokeInput,
                            KeyStyle.withIcon(R.drawable.ic_revoke_input, R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Toggle_PinyinInput_spell,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinInputCandidate_stroke,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.NoOp,
                            KeyStyle.withColor(R.attr.key_ctrl_noop_fg_color, R.attr.key_ctrl_noop_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Undo, KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Redo, KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Cut, KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Paste, KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Copy, KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Math_Equal,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_fg_color));
    }

    protected final Config config;

    protected KeyTable(Config config) {
        this.config = config;
    }

    /** 获取坐标点：其将根据左右手模式翻转坐标 */
    public Point coord(int x, int y) {
        return point(x, getIndexForHandMode(y));
    }

    /** 创建{@link Point 点}：其点位置不做左右手模式翻转 */
    public Point point(int x, int y) {
        return new Point(x, y);
    }

    protected abstract Key<?>[][] initGrid();

    protected Key<?>[][] createEmptyGrid() {
        Key<?>[][] gridKeys = initGrid();
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        return gridKeys;
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

    protected int getGridFirstColumnIndexForHandMode() {
        return getIndexForHandMode(0);
    }

    protected int getGridLastColumnIndexForHandMode() {
        return getIndexForHandMode(getGridLastColumnIndex());
    }

    protected int getGridMiddleColumnIndexForHandMode() {
        return getIndexForHandMode(getGridMiddleColumnIndex());
    }

    /** 若配置启用左手模式，则翻转右手模式按键位置为左手模式按键位置 */
    protected int getIndexForHandMode(int index) {
        if (this.config.isLeftHandMode()) {
            int lastColumnIndex = getGridLastColumnIndex();

            return lastColumnIndex - index;
        }
        return index;
    }

    /** 表情符号按键的分页大小 */
    public static int getEmojiKeysPageSize() {
        int size = 0;
        for (int[][] level : symbol_emoji_key_level_coords) {
            size += level.length;
        }
        return size;
    }

    /** 创建表情符号按键 */
    public static Key<?>[][] createEmojiKeys(
            Config config, List<String> groups, List<InputWord> words, String selectedGroup, int startIndex,
            int pageSize
    ) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int dataSize = words.size();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));
        boolean isLeft = config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;

        int index_7 = changeIndexForHandMode(config, gridKeys, 7);

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[2][index_7] = ctrlKey(config, CtrlKey.Type.Exit);
        gridKeys[3][4] = ctrlKey(config, CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_7] = config.hasInputs ? ctrlKey(config, CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[4][index_7] = ctrlKey(config, CtrlKey.Type.Space);
        gridKeys[5][index_7] = ctrlKey(config, CtrlKey.Type.Backspace);

        int[][] groupKeyCoords = isLeft ? symbol_emoji_group_key_coords_left_hand : symbol_emoji_group_key_coords;
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < groups.size(); i++, j++) {
            int[] keyCoord = groupKeyCoords[i];
            String group = groups.get(j);
            boolean selected = group.equals(selectedGroup);

            int x = keyCoord[0];
            int y = keyCoord[1];
            CtrlKey.Option<?> option = new CtrlKey.TextOption(group);
            gridKeys[x][y] = ctrlKey(config, CtrlKey.Type.Toggle_Emoji_Group).setOption(option)
                                                                             .setLabel(group)
                                                                             .setDisabled(selected);
        }

        int dataIndex = startIndex;
        int[][][] levelKeyCoords = relayoutForHandMode(config, symbol_emoji_key_level_coords);

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (dataIndex < dataSize) {
                    InputWord word = words.get(dataIndex);

                    if (word != null) {
                        KeyColor color = key_char_emoji_color;

                        InputWordKey key = InputWordKey.create(word).setColor(color);
                        gridKeys[x][y] = key;
                    }
                } else {
                    break;
                }

                dataIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 标点符号按键的分页大小 */
    public static int getSymbolKeysPageSize() {
        int size = 0;
        for (int[][] level : symbol_emoji_key_level_coords) {
            size += level.length;
        }
        return size;
    }

    /** 创建标点符号按键 */
    public static Key<?>[][] createSymbolKeys(Config config, SymbolGroup symbolGroup, int startIndex, int pageSize) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int dataSize = symbolGroup.symbols.length;
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));
        boolean isLeft = config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;

        int index_7 = changeIndexForHandMode(config, gridKeys, 7);

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[2][index_7] = ctrlKey(config, CtrlKey.Type.Exit);
        gridKeys[3][4] = ctrlKey(config, CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_7] = config.hasInputs ? ctrlKey(config, CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[4][index_7] = ctrlKey(config, CtrlKey.Type.Space);
        gridKeys[5][index_7] = ctrlKey(config, CtrlKey.Type.Backspace);

        int[][] groupKeyCoords = isLeft ? symbol_emoji_group_key_coords_left_hand : symbol_emoji_group_key_coords;
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < SymbolGroup.values().length; i++, j++) {
            int[] keyCoord = groupKeyCoords[i];
            String group = SymbolGroup.values()[j].name;
            boolean selected = group.equals(symbolGroup.name);

            int x = keyCoord[0];
            int y = keyCoord[1];
            CtrlKey.Option<?> option = new CtrlKey.SymbolGroupOption(SymbolGroup.values()[j]);
            gridKeys[x][y] = ctrlKey(config, CtrlKey.Type.Toggle_Symbol_Group).setOption(option)
                                                                              .setLabel(group)
                                                                              .setDisabled(selected);
        }

        int dataIndex = startIndex;
        int[][][] levelKeyCoords = relayoutForHandMode(config, symbol_emoji_key_level_coords);

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (dataIndex < dataSize) {
                    Symbol data = symbolGroup.symbols[dataIndex];

                    if (data != null) {
                        KeyColor color = latin_key_char_alphabet_level_colors[level];

                        SymbolKey key = SymbolKey.create(data).setLabel(data.text).setColor(color);
                        gridKeys[x][y] = key;
                    }
                } else {
                    break;
                }

                dataIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 创建{@link NumberKeyboard 数字键盘}按键 */
    public static Key<?>[][] createNumberKeys(Config config, Key<?>[] keys) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[1][index_6] = ctrlKey(config, CtrlKey.Type.Backspace);
        gridKeys[2][index_6] = ctrlKey(config, CtrlKey.Type.Space);
        gridKeys[3][index_3] = ctrlKey(config, CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_6] = enterCtrlKey(config);

        int keyIndex = 0;
        int[][][] levelKeyCoords = relayoutForHandMode(config, number_key_around_level_coords);

        for (int level = 0; level < levelKeyCoords.length; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (keyIndex < keys.length) {
                    Key<?> key = keys[keyIndex];

                    KeyColor color = key_char_around_level_colors[level];
                    key.setColor(color);

                    gridKeys[x][y] = key;
                } else {
                    break;
                }

                keyIndex += 1;
            }
        }

        return gridKeys;
    }

    public CtrlKey enterCtrlKey() {
        return this.config.isSingleLineInput() //
               ? ctrlKey(CtrlKey.create(CtrlKey.Type.Enter), key_ctrl_ok_style) //
               : ctrlKey(CtrlKey.Type.Enter);
    }

    public CtrlKey ctrlKey(CtrlKey.Type type) {
        return ctrlKey(CtrlKey.create(type));
    }

    public CtrlKey ctrlKey(CtrlKey key) {
        KeyStyle style = ctrl_key_styles.get(key.getType());

        return ctrlKey(key, style);
    }

    public CtrlKey ctrlKey(CtrlKey key, KeyStyle style) {
        int icon = 0;
        KeyColor color = KeyColor.none();

        if (style != null) {
            icon = style.icon.right;
            if (this.config != null //
                && this.config.isLeftHandMode()) {
                icon = style.icon.left;
            }

            color = style.color;
        }

        return key.setIconResId(icon).setColor(color);
    }

    public CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    public CtrlKey noopCtrlKey(String label) {
        return ctrlKey(CtrlKey.noop().setLabel(label));
    }

    public static CharKey alphabetKey(String text) {
        return charKey(CharKey.Type.Alphabet, text);
    }

    public static CharKey numberKey(String text) {
        return charKey(CharKey.Type.Number, text);
    }

    public static CharKey emojiKey(String text) {
        return charKey(CharKey.Type.Emoji, text).setColor(key_char_emoji_color);
    }

    public static CharKey symbolKey(String text) {
        return charKey(CharKey.Type.Symbol, text).setColor(key_char_symbol_color);
    }

    private static CharKey charKey(CharKey.Type type, String text) {
        return charKey(CharKey.create(type, text).setLabel(text));
    }

    private static CharKey charKey(CharKey key) {
        KeyColor color = null;

        for (Map.Entry<List<String>, KeyColor> entry : char_key_color_palette.entrySet()) {
            KeyColor keyColor = entry.getValue();
            if (entry.getKey().contains(key.getText().toLowerCase())) {
                color = keyColor;
                break;
            }
        }

        return key.setColor(color);
    }

    /** 若配置启用左手模式，则翻转右手模式按键布局为左手模式 */
    protected static <T> T[] relayoutForHandMode(Config config, T[] rightHandLayout) {
        if (!config.isLeftHandMode()) {
            return rightHandLayout;
        }

        T[] newLayout = Arrays.copyOf(rightHandLayout, rightHandLayout.length);

        int mid = (newLayout.length - 1) / 2;
        for (int j = newLayout.length - 1, k = 0; j > mid; j--, k++) {
            T tmp = newLayout[j];
            newLayout[j] = newLayout[k];
            newLayout[k] = tmp;
        }

        return newLayout;
    }

    /** 若配置启用左手模式，则翻转右手模式按键布局为左手模式 */
    protected static <T> T[][] relayoutForHandMode(Config config, T[][] rightHandLayout) {
        if (!config.isLeftHandMode()) {
            return rightHandLayout;
        }

        T[][] newLayout = Arrays.copyOf(rightHandLayout, rightHandLayout.length);
        for (int i = 0; i < newLayout.length; i++) {
            T[] newRow = newLayout[i];
            newLayout[i] = relayoutForHandMode(config, newRow);
        }

        return newLayout;
    }

    /** 若配置启用左手模式，则翻转右手模式按键位置为左手模式按键位置 */
    private static <T> int changeIndexForHandMode(Config config, T[][] gridKeys, int index) {
        if (config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left) {
            return gridKeys[0].length - 1 - index;
        }
        return index;
    }

    private static class KeyStyle {
        private final KeyIcon icon;
        private final KeyColor color;

        public KeyStyle(KeyColor color) {
            this(new KeyIcon(-1, -1), color);
        }

        public KeyStyle(KeyIcon icon, KeyColor color) {
            this.icon = icon;
            this.color = color;
        }

        public static KeyStyle withColor(int fg, int bg) {
            return new KeyStyle(KeyColor.create(fg, bg));
        }

        public static KeyStyle withIcon(int right, int left, int bg) {
            return new KeyStyle(new KeyIcon(right, left), KeyColor.create(-1, bg));
        }

        public static KeyStyle withIcon(int resId, int bg) {
            return new KeyStyle(new KeyIcon(resId), KeyColor.create(-1, bg));
        }
    }

    private static class KeyIcon {
        /** 右手模式的图标资源 id */
        public final int right;
        /** 左手模式的图标资源 id */
        public final int left;

        public KeyIcon(int resId) {
            this.right = resId;
            this.left = resId;
        }

        public KeyIcon(int right, int left) {
            this.right = right;
            this.left = left;
        }
    }

    public static class Config {
        private final Keyboard.Config keyboardConfig;
        private final boolean hasInputs;

        public Config(Keyboard.Config keyboardConfig, boolean hasInputs) {
            this.keyboardConfig = keyboardConfig;
            this.hasInputs = hasInputs;
        }

        public boolean hasInputs() {
            return this.hasInputs;
        }

        public boolean isLeftHandMode() {
            return this.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;
        }

        public boolean isSingleLineInput() {
            return this.keyboardConfig.isSingleLineInput();
        }
    }
}
