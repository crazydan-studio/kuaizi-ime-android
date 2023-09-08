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
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;

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

    private static final KeyColor key_char_level_0_color = KeyColor.create(R.attr.key_char_level_0_fg_color,
                                                                           R.attr.key_char_level_0_bg_color);
    private static final KeyColor key_char_level_1_color = KeyColor.create(R.attr.key_char_level_1_fg_color,
                                                                           R.attr.key_char_level_1_bg_color);
    private static final KeyColor key_char_level_2_color = KeyColor.create(R.attr.key_char_level_2_fg_color,
                                                                           R.attr.key_char_level_2_bg_color);
    private static final KeyColor key_char_level_3_color = KeyColor.create(R.attr.key_char_level_3_fg_color,
                                                                           R.attr.key_char_level_3_bg_color);
    private static final KeyColor key_char_level_4_color = KeyColor.create(R.attr.key_char_level_4_fg_color,
                                                                           R.attr.key_char_level_4_bg_color);
    private static final KeyColor key_char_level_5_color = KeyColor.create(R.attr.key_char_level_5_fg_color,
                                                                           R.attr.key_char_level_5_bg_color);

    /** {@link InputWordKey 候选字}按键的配色 */
    protected static final KeyColor[] key_input_word_level_colors = new KeyColor[] {
            key_char_level_0_color,
            key_char_level_1_color,
            key_char_level_2_color,
            key_char_level_3_color,
            key_char_level_4_color,
            };
    /** {@link CharKey} 按键的配色 */
    protected static final KeyColor key_char_color = KeyColor.create(R.attr.key_fg_color, R.attr.key_bg_color);
    /** {@link CharKey.Type#Symbol 标点符号}按键的配色 */
    protected static final KeyColor key_char_symbol_color = KeyColor.create(R.attr.key_char_symbol_fg_color,
                                                                            R.attr.key_char_symbol_bg_color);
    /** {@link CharKey.Type#Emoji 表情符号}按键的配色 */
    protected static final KeyColor key_char_emoji_color = key_char_symbol_color;
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
        KeyColor specialCharKeyColor = KeyColor.create(R.attr.key_highlight_fg_color, R.attr.key_bg_color);
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü", "v"), specialCharKeyColor);
        char_key_color_palette.put(Arrays.asList("ch", "sh", "zh"), specialCharKeyColor);
        char_key_color_palette.put(Arrays.asList("w", "z", "x", "y"), key_char_color);
        char_key_color_palette.put(Arrays.asList("f", "g", "d", "b", "c"), key_char_color);
        char_key_color_palette.put(Arrays.asList("p", "q", "n", "s", "t", "r"), key_char_color);
        char_key_color_palette.put(Arrays.asList("h", "k", "j", "m", "l"), key_char_color);
        char_key_color_palette.put(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                                   specialCharKeyColor);

        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            KeyStyle.withIcon(R.drawable.ic_backspace, R.attr.key_ctrl_backspace_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Space, KeyStyle.withIcon(R.drawable.ic_space, R.attr.key_ctrl_space_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            KeyStyle.withIcon(R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.CommitInputList,
                            KeyStyle.withIcon(R.drawable.ic_right_hand_commit,
                                              R.drawable.ic_left_hand_commit,
                                              R.attr.key_ctrl_commit_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.Option_CommitInputList, key_ctrl_label_style);

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
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Symbol_Group, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Toggle_Emoji_Group, key_ctrl_label_style);

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

        ctrl_key_styles.put(CtrlKey.Type.Toggle_PinyinInput_spell, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Filter_PinyinInputCandidate_stroke, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.NoOp, key_ctrl_noop_style);
        ctrl_key_styles.put(CtrlKey.Type.Undo, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Redo, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Cut, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Paste, key_ctrl_label_style);
        ctrl_key_styles.put(CtrlKey.Type.Copy, key_ctrl_label_style);

        ctrl_key_styles.put(CtrlKey.Type.Math_Equal, key_ctrl_label_style);
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

    /** 占位按键，且不触发事件 */
    public CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    /** 占位按键，且不触发事件 */
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
        /** 是否已选中字符输入 */
        private final boolean charInputSelected;

        public Config(Keyboard.Config keyboardConfig, boolean hasInputs, boolean charInputSelected) {
            this.keyboardConfig = keyboardConfig;
            this.hasInputs = hasInputs;
            this.charInputSelected = charInputSelected;
        }

        public boolean hasInputs() {
            return this.hasInputs;
        }

        public boolean isCharInputSelected() {
            return this.charInputSelected;
        }

        public boolean isLeftHandMode() {
            return this.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;
        }

        public boolean isSingleLineInput() {
            return this.keyboardConfig.isSingleLineInput();
        }
    }
}
