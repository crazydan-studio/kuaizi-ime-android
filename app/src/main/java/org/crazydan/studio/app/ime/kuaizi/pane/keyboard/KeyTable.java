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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.XPadKey;

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
    /** {@link CharKey.Type#Symbol 标点符号}按键的配色 */
    protected static final Key.Color key_char_symbol_color = Key.Color.create(R.attr.key_char_symbol_fg_color,
                                                                              R.attr.key_char_symbol_bg_color);
    /** {@link CharKey.Type#Emoji 表情符号}按键的配色 */
    protected static final Key.Color key_char_emoji_color = key_char_symbol_color;
    /** 字母按键调色板 */
    private static final Map<List<String>, Key.Color> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, KeyStyle> ctrl_key_styles = new HashMap<>();
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
    /** {@link InputWordKey 候选字}按键的配色 */
    protected static final Key.Color[] key_input_word_level_colors = new Key.Color[] {
            key_char_level_0_color,
            key_char_level_1_color,
            key_char_level_2_color,
            key_char_level_3_color,
            key_char_level_4_color,
            };
    private static final Key.Color key_char_level_5_color = Key.Color.create(R.attr.key_char_level_5_fg_color,
                                                                             R.attr.key_char_level_5_bg_color);
    /** OK 按键的样式 */
    private static final KeyStyle key_ctrl_ok_style = KeyStyle.withIcon(R.drawable.ic_right_hand_ok,
                                                                        R.drawable.ic_left_hand_ok,
                                                                        R.attr.key_ctrl_ok_bg_color);
    /** 文本类控制按键的样式 */
    private static final KeyStyle key_ctrl_label_style = KeyStyle.withColor(R.attr.key_ctrl_label_color,
                                                                            R.attr.key_bg_color);
    private static final KeyStyle key_ctrl_noop_style = KeyStyle.withColor(R.attr.key_ctrl_noop_fg_color,
                                                                           R.attr.key_ctrl_noop_bg_color);

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
                            KeyStyle.withIcon(R.drawable.ic_backspace, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Space, KeyStyle.withIcon(R.drawable.ic_space, R.attr.key_ctrl_space_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            KeyStyle.withIcon(R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Pinyin_End,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_end,
                                              R.drawable.ic_left_hand_end,
                                              R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Commit_InputList,
                            KeyStyle.withIcon(R.drawable.ic_commit, R.attr.key_ctrl_commit_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Commit_InputList_Option, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.Switch_IME,
                            KeyStyle.withIcon(R.drawable.ic_keyboard, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Switch_HandMode,
                            KeyStyle.withIcon(R.drawable.ic_switch_to_left_hand,
                                              R.drawable.ic_switch_to_right_hand,
                                              R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Toggle_Symbol_Group, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Emoji_Group, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.Editor_Cursor_Locator,
                            KeyStyle.withIcon(R.drawable.ic_input_cursor, R.attr.key_ctrl_locator_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Editor_Range_Selector,
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
                            KeyStyle.withIcon(R.drawable.ic_right_hand_ok,
                                              R.drawable.ic_left_hand_ok,
                                              R.attr.key_ctrl_confirm_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.RevokeInput,
                            KeyStyle.withIcon(R.drawable.ic_revoke_input, R.attr.key_ctrl_switcher_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.Toggle_Pinyin_spell, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinCandidate_by_Spell, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinCandidate_advance,
                            KeyStyle.withIcon(R.drawable.ic_filter_empty, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Confirm_PinyinCandidate_Filter,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_ok,
                                              R.drawable.ic_left_hand_ok,
                                              R.attr.key_ctrl_confirm_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.NoOp, key_ctrl_noop_style);
        ctrl_key_styles.put(CtrlKey.Type.Edit_Editor, key_ctrl_label_style);
    }

    protected final Config config;

    protected KeyTable(Config config) {
        this.config = config;
    }

    protected static <T> int countGridSize(T[][] grid) {
        int size = 0;

        for (T[] row : grid) {
            size += row.length;
        }
        return size;
    }

    private static CharKey charKey(CharKey.Type type, String text) {
        return charKey(CharKey.create(type, text).setLabel(text));
    }

    private static CharKey charKey(CharKey key) {
        Key.Color color = null;

        for (Map.Entry<List<String>, Key.Color> entry : char_key_color_palette.entrySet()) {
            Key.Color keyColor = entry.getValue();
            if (entry.getKey().contains(key.getText().toLowerCase())) {
                color = keyColor;
                break;
            }
        }

        return key.setColor(color);
    }

    /** 创建{@link GridCoord 网格坐标} */
    public GridCoord coord(int row, int column) {
        return coord(row, column, 0);
    }

    /** 创建{@link GridCoord 网格坐标} */
    public GridCoord coord(int row, int column, int layer) {
        return new GridCoord(row, column, layer);
    }

    protected abstract Key<?>[][] initGrid();

    protected Key<?>[][] createEmptyGrid() {
        Key<?>[][] gridKeys = initGrid();
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

    public CtrlKey enterCtrlKey() {
        return this.config.isSingleLineInput() //
               ? ctrlKey(CtrlKey.create(CtrlKey.Type.Enter), key_ctrl_ok_style) //
               : ctrlKey(CtrlKey.Type.Enter);
    }

    /** 创建 键盘切换 控制按键 */
    public CtrlKey switcherCtrlKey(Keyboard.Type type) {
        CtrlKey.Option<?> option = new CtrlKey.KeyboardSwitchOption(type);

        KeyStyle style = null;
        switch (type) {
            case Math:
                style = KeyStyle.withIcon(R.drawable.ic_calculator, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Latin:
                style = KeyStyle.withIcon(R.drawable.ic_switch_to_latin, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Pinyin:
                style = KeyStyle.withIcon(R.drawable.ic_switch_to_pinyin, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Emoji:
                style = KeyStyle.withIcon(R.drawable.ic_emoji, R.attr.key_ctrl_switcher_bg_color);
                break;
            case Symbol:
                style = KeyStyle.withIcon(R.drawable.ic_symbol, R.attr.key_ctrl_switcher_bg_color);
                break;
        }

        return ctrlKey(CtrlKey.Type.Switch_Keyboard, style).setOption(option);
    }

    public CtrlKey ctrlKey(CtrlKey.Type type) {
        return ctrlKey(CtrlKey.create(type));
    }

    public CtrlKey ctrlKey(CtrlKey.Type type, KeyStyle style) {
        return ctrlKey(ctrlKey(type), style);
    }

    public CtrlKey ctrlKey(CtrlKey key) {
        KeyStyle style = ctrl_key_styles.get(key.getType());

        return ctrlKey(key, style);
    }

    public CtrlKey ctrlKey(CtrlKey key, KeyStyle style) {
        Integer icon = null;
        Key.Color color = Key.Color.none();

        if (style != null) {
            icon = style.icon.right;
            if (this.config != null //
                && this.config.isLeftHandMode()) {
                icon = style.icon.left;
            }

            color = style.color;
        }

        switch (key.getType()) {
            case NoOp:
            case Editor_Cursor_Locator:
            case Editor_Range_Selector:
                break;
        }

        return key.setIconResId(icon).setColor(color);
    }

    /** 占位按键，且不触发事件 */
    public CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    /** 占位按键，且不触发事件 */
    public CtrlKey noopCtrlKey(String label) {
        return ctrlKey(CtrlKey.noop().setLabel(label));
    }

    public CharKey alphabetKey(String text) {
        return charKey(CharKey.Type.Alphabet, text);
    }

    public CharKey numberKey(String text) {
        return charKey(CharKey.Type.Number, text);
    }

    public CharKey emojiKey(String text) {
        return charKey(CharKey.Type.Emoji, text).setColor(key_char_emoji_color);
    }

    public CharKey symbolKey(String text) {
        return charKey(CharKey.Type.Symbol, text).setColor(key_char_symbol_color);
    }

    protected Key<?>[][] createKeysForXPad() {
        return createKeysForXPad(createXPadKey());
    }

    protected Key<?>[][] createKeysForXPad(XPadKey xPadKey) {
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
                this.config.hasInputs() ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.RevokeInput).setDisabled(!this.config.hasRevokingInputs()),
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
    protected XPadKey xPadKey(Keyboard.Type activeKeyboard, Key<?>[][][] zone_2_keys) {
        Key<?>[] zone_1_keys = new Key[] {
                switcherCtrlKey(Keyboard.Type.Latin).setIconResId(R.drawable.ic_latin),
                switcherCtrlKey(Keyboard.Type.Pinyin).setIconResId(R.drawable.ic_pinyin),
                switcherCtrlKey(Keyboard.Type.Number).setIconResId(R.drawable.ic_number),
                null,
                null,
                switcherCtrlKey(Keyboard.Type.Math).setIconResId(R.drawable.ic_math),
                };
        for (Key<?> key : zone_1_keys) {
            if (key != null //
                && ((CtrlKey.KeyboardSwitchOption) ((CtrlKey) key).getOption()).value() == activeKeyboard) {
                key.setDisabled(true);
            }
        }

        return new XPadKey(ctrlKey(CtrlKey.Type.Editor_Cursor_Locator), //
                           zone_1_keys, zone_2_keys);
    }

    private static class KeyStyle {
        private final KeyIcon icon;
        private final Key.Color color;

        public KeyStyle(Key.Color color) {
            this(new KeyIcon(null, null), color);
        }

        public KeyStyle(KeyIcon icon, Key.Color color) {
            this.icon = icon;
            this.color = color;
        }

        public static KeyStyle withColor(int fg, int bg) {
            return withColor(Key.Color.create(fg, bg));
        }

        public static KeyStyle withColor(Key.Color color) {
            return new KeyStyle(color);
        }

        public static KeyStyle withIcon(int right, int left, int bg) {
            return new KeyStyle(new KeyIcon(right, left), Key.Color.create(null, bg));
        }

        public static KeyStyle withIcon(int resId, int bg) {
            return new KeyStyle(new KeyIcon(resId), Key.Color.create(null, bg));
        }
    }

    private static class KeyIcon {
        /** 右手模式的图标资源 id */
        public final Integer right;
        /** 左手模式的图标资源 id */
        public final Integer left;

        public KeyIcon(Integer resId) {
            this.right = resId;
            this.left = resId;
        }

        public KeyIcon(Integer right, Integer left) {
            this.right = right;
            this.left = left;
        }
    }

    public static class GridCoord {
        public final int row;
        public final int column;
        public final int layer;

        public GridCoord(int row, int column) {
            this(row, column, 0);
        }

        public GridCoord(int row, int column, int layer) {
            this.row = row;
            this.column = column;
            this.layer = layer;
        }
    }

    public static class Config {
        private final Configuration keyboardConf;

        private final boolean hasInputs;
        /** 是否有待撤回输入 */
        private final boolean hasRevokingInputs;
        /** 是否已选中字符输入 */
        private final boolean charInputSelected;

        public Config(Configuration keyboardConf) {
            this(keyboardConf, false, false, false);
        }

        public Config(
                Configuration keyboardConf, boolean hasInputs, boolean hasRevokingInputs, boolean charInputSelected
        ) {
            this.keyboardConf = keyboardConf;

            this.hasInputs = hasInputs;
            this.hasRevokingInputs = hasRevokingInputs;
            this.charInputSelected = charInputSelected;
        }

        public boolean hasInputs() {
            return this.hasInputs;
        }

        public boolean hasRevokingInputs() {
            return this.hasRevokingInputs;
        }

        public boolean isCharInputSelected() {
            return this.charInputSelected;
        }

        // ================================================================
        public boolean isLeftHandMode() {
            return this.keyboardConf.isLeftHandMode();
        }

        public boolean isSingleLineInput() {
            return this.keyboardConf.bool(Conf.single_line_input);
        }

        public boolean isXInputPadEnabled() {
            return this.keyboardConf.isXInputPadEnabled();
        }

        public boolean isLatinUsePinyinKeysInXInputPadEnabled() {
            return this.keyboardConf.isLatinUsePinyinKeysInXInputPadEnabled();
        }
    }
}
