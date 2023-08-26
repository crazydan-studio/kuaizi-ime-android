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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;

/**
 * {@link Keyboard 键盘}的按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class KeyTable {
    /** 字母按键调色板 */
    private static final Map<List<String>, KeyColor> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, KeyStyle> ctrl_key_styles = new HashMap<>();
    /** 环绕型布局的字符按键的配色：从内到外分为不同层级 */
    private static final KeyColor[] key_char_around_level_colors = new KeyColor[] {
            KeyColor.create(R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color),
            KeyColor.create(R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color),
            KeyColor.create(R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color),
            KeyColor.create(R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color),
            };
    /** 拉丁文键盘字符按键的配色 */
    private static final KeyColor[] latin_key_char_alphabet_level_colors = new KeyColor[] {
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

    /** 从中心按键由内到外的候选字环形布局坐标 */
    private static final int[][][] input_word_key_around_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    new int[] { 2, 3 },
                    },
            // level 2
            new int[][] {
                    new int[] { 1, 3 },
                    new int[] { 1, 4 },
                    new int[] { 2, 5 },
                    new int[] { 3, 5 },
                    new int[] { 4, 5 },
                    new int[] { 5, 4 },
                    new int[] { 5, 3 },
                    new int[] { 5, 2 },
                    new int[] { 4, 2 },
                    new int[] { 3, 1 },
                    new int[] { 2, 2 },
                    new int[] { 1, 2 },
                    },
            // level 3
            new int[][] {
                    new int[] { 0, 4 },
                    new int[] { 0, 5 },
                    new int[] { 1, 5 },
                    new int[] { 2, 6 },
                    new int[] { 3, 6 },
                    new int[] { 4, 6 },
                    new int[] { 5, 5 },
                    new int[] { 5, 1 },
                    new int[] { 4, 1 },
                    new int[] { 3, 0 },
                    new int[] { 2, 1 },
                    new int[] { 1, 1 },
                    new int[] { 0, 2 },
                    new int[] { 0, 3 },
                    },
            };

    /** 表情符号的分组按键坐标 */
    private static final int[][] emoji_group_key_coords = new int[][] {
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
    private static final int[][][] emoji_key_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 1, 1 },
                    new int[] { 1, 2 },
                    new int[] { 1, 3 },
                    new int[] { 1, 4 },
                    new int[] { 1, 5 },
                    new int[] { 1, 6 },
                    },
            // level 2
            new int[][] {
                    new int[] { 2, 1 },
                    new int[] { 2, 2 },
                    new int[] { 2, 3 },
                    new int[] { 2, 4 },
                    new int[] { 2, 5 },
                    new int[] { 2, 6 },
                    new int[] { 2, 7 },
                    },
            // level 3
            new int[][] {
                    new int[] { 3, 0 },
                    new int[] { 3, 1 },
                    new int[] { 3, 2 },
                    new int[] { 3, 4 },
                    new int[] { 3, 5 },
                    new int[] { 3, 6 },
                    },
            // level 4
            new int[][] {
                    new int[] { 4, 1 },
                    new int[] { 4, 2 },
                    new int[] { 4, 3 },
                    new int[] { 4, 4 },
                    new int[] { 4, 5 },
                    new int[] { 4, 6 },
                    },
            // level 5
            new int[][] {
                    new int[] { 5, 1 },
                    new int[] { 5, 2 },
                    new int[] { 5, 3 },
                    new int[] { 5, 4 },
                    new int[] { 5, 5 },
                    new int[] { 5, 6 },
                    },
            };

    /** 从中心按键由内到外的标点符号环形布局坐标 */
    private static final int[][][] symbol_key_around_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    new int[] { 2, 3 },
                    },
            // level 2
            new int[][] {
                    new int[] { 1, 3 },
                    new int[] { 1, 4 },
                    new int[] { 2, 5 },
                    new int[] { 3, 5 },
                    new int[] { 4, 5 },
                    new int[] { 5, 4 },
                    new int[] { 5, 3 },
                    new int[] { 5, 2 },
                    new int[] { 4, 2 },
                    new int[] { 3, 1 },
                    new int[] { 2, 2 },
                    new int[] { 1, 2 },
                    },
            // level 3
            new int[][] {
                    new int[] { 0, 4 },
                    new int[] { 0, 5 },
                    new int[] { 1, 5 },
                    new int[] { 2, 6 },
                    new int[] { 3, 6 },
                    new int[] { 4, 6 },
                    new int[] { 5, 5 },
                    new int[] { 5, 1 },
                    new int[] { 4, 1 },
                    new int[] { 3, 0 },
                    new int[] { 2, 1 },
                    new int[] { 1, 1 },
                    new int[] { 0, 2 },
                    new int[] { 0, 3 },
                    },
            };

    /** 从中心按键由内到外的数学按键环形布局坐标 */
    private static final int[][][] math_key_around_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    new int[] { 2, 3 },
                    new int[] { 1, 3 },
                    },
            // level 2
            new int[][] {
                    new int[] { 0, 4 },
                    new int[] { 1, 4 },
                    new int[] { 2, 5 },
                    new int[] { 3, 5 },
                    new int[] { 4, 5 },
                    new int[] { 4, 2 },
                    new int[] { 3, 1 },
                    new int[] { 2, 2 },
                    new int[] { 1, 2 },
                    new int[] { 0, 3 },
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

    /** 拼音{@link Key.Level#level_2 第二级}按键坐标 */
    private static final int[][][] pinyin_level_2_key_level_coords = new int[][][] {
            //
            new int[][] {
                    new int[] { 2, 3 }, new int[] { 2, 2 }, new int[] { 2, 4 },
                    },
            //
            new int[][] {
                    new int[] { 1, 3 }, new int[] { 1, 2 },
                    },
            //
            new int[][] {
                    new int[] { 3, 3 }, new int[] { 3, 2 },
                    },
            //
            new int[][] {
                    new int[] { 1, 4 }, new int[] { 3, 4 }, new int[] { 2, 5 },
                    },
            };

    /** 拼音{@link Key.Level#level_2 第二级}按键坐标（左手模式） */
    private static final int[][][] pinyin_level_2_key_level_coords_left_hand = new int[][][] {
            //
            new int[][] {
                    new int[] { 2, 5 }, new int[] { 2, 4 }, new int[] { 2, 6 },
                    },
            //
            new int[][] {
                    new int[] { 1, 4 }, new int[] { 1, 5 },
                    },
            //
            new int[][] {
                    new int[] { 3, 4 }, new int[] { 3, 5 },
                    },
            //
            new int[][] {
                    new int[] { 1, 3 }, new int[] { 3, 3 }, new int[] { 2, 3 },
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
        ctrl_key_styles.put(CtrlKey.Type.ToggleSymbol_Locale_Zh_and_En,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));

        ctrl_key_styles.put(CtrlKey.Type.ToggleSymbol_Emoji,
                            KeyStyle.withIcon(R.drawable.ic_emoji, R.attr.key_ctrl_switcher_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.ToggleEmoji_Group,
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

        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_ng,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_zcs_h,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_nl,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.FilterInputCandidate_stroke_heng,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.FilterInputCandidate_stroke_shu,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.FilterInputCandidate_stroke_pie,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.FilterInputCandidate_stroke_na,
                            KeyStyle.withColor(R.attr.key_highlight_fg_color, R.attr.key_bg_color));
        ctrl_key_styles.put(CtrlKey.Type.FilterInputCandidate_stroke_zhe,
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

    /** 创建{@link PinyinKeyboard 拼音键盘}按键 */
    public static Key<?>[][] createPinyinKeys(Config config) {
        Key<?>[][] keys = new Key[][] {
                new Key[] {
                        //ctrlKey(config, CtrlKey.Type.SwitchIME),
                        ctrlKey(config, CtrlKey.Type.SwitchHandMode),
                        symbolKey("；").withReplacements(";"),
                        alphabetKey("zh").withReplacements("Zh", "ZH"),
                        alphabetKey("ch").withReplacements("Ch", "CH"),
                        alphabetKey("sh").withReplacements("Sh", "SH"),
                        alphabetKey("o").withReplacements("O"),
                        alphabetKey("e").withReplacements("E"),
                        alphabetKey("a").withReplacements("A"),
                        } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToMathKeyboard),
                symbolKey("：").withReplacements(":"),
                alphabetKey("r").withReplacements("R"),
                alphabetKey("g").withReplacements("G"),
                alphabetKey("f").withReplacements("F"),
                alphabetKey("d").withReplacements("D"),
                alphabetKey("c").withReplacements("C"),
                alphabetKey("b").withReplacements("B"),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToLatinKeyboard),
                symbolKey("！").withReplacements("!"),
                alphabetKey("s").withReplacements("S"),
                alphabetKey("m").withReplacements("M"),
                alphabetKey("l").withReplacements("L"),
                alphabetKey("k").withReplacements("K"),
                alphabetKey("j").withReplacements("J"),
                alphabetKey("h").withReplacements("H"),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToEmojiKeyboard),
                symbolKey("？").withReplacements("?"),
                alphabetKey("t").withReplacements("T"),
                alphabetKey("n").withReplacements("N"),
                ctrlKey(config, CtrlKey.Type.LocateInputCursor),
                alphabetKey("q").withReplacements("Q"),
                alphabetKey("p").withReplacements("P"),
                config.hasInputs ? ctrlKey(config, CtrlKey.Type.CommitInputList) : enterCtrlKey(config),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToSymbolKeyboard),
                symbolKey("。").withReplacements("."),
                alphabetKey("ü").withReplacements("v", "V"),
                alphabetKey("z").withReplacements("Z"),
                alphabetKey("y").withReplacements("Y"),
                alphabetKey("x").withReplacements("X"),
                alphabetKey("w").withReplacements("W"),
                ctrlKey(config, CtrlKey.Type.Space),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.RevokeInput).setDisabled(true),
                symbolKey("，").withReplacements(","),
                alphabetKey("u").withReplacements("U"),
                alphabetKey("i").withReplacements("I"),
                emojiKey("\uD83D\uDE02"),
                emojiKey("\uD83D\uDE04"),
                emojiKey("\uD83D\uDE09"),
                ctrlKey(config, CtrlKey.Type.Backspace),
                },
                };

        return changeLayoutForHandMode(config, keys);
    }

    /** 创建{@link LatinKeyboard 拉丁文键盘}按键 */
    public static Key<?>[][] createLatinKeys(Config config) {
        Key<?>[][] keys = new Key[][] {
                new Key[] {
                        //ctrlKey(config, CtrlKey.Type.SwitchIME),
                        ctrlKey(config, CtrlKey.Type.SwitchHandMode),
                        alphabetKey("a"),
                        alphabetKey("b"),
                        alphabetKey("c"),
                        alphabetKey("d"),
                        alphabetKey("e"),
                        alphabetKey("f"),
                        noopCtrlKey(),
                        } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("g"),
                alphabetKey("h"),
                alphabetKey("i"),
                alphabetKey("j"),
                alphabetKey("k"),
                ctrlKey(config, CtrlKey.Type.Backspace),
                noopCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToPinyinKeyboard),
                alphabetKey("l"),
                alphabetKey("m"),
                alphabetKey("n"),
                alphabetKey("o"),
                alphabetKey("p"),
                ctrlKey(config, CtrlKey.Type.Space),
                noopCtrlKey(),
                } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("q"),
                alphabetKey("r"),
                ctrlKey(config, CtrlKey.Type.LocateInputCursor),
                alphabetKey("s"),
                alphabetKey("t"),
                config.hasInputs ? ctrlKey(config, CtrlKey.Type.CommitInputList) : enterCtrlKey(config),
                noopCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(config, CtrlKey.Type.SwitchToSymbolKeyboard),
                alphabetKey("u"),
                alphabetKey("v"),
                alphabetKey("w"),
                alphabetKey("x"),
                alphabetKey("y"),
                alphabetKey("z"),
                noopCtrlKey(),
                } //
                , new Key[] {
                symbolKey(":"),
                symbolKey("!"),
                symbolKey("?"),
                symbolKey("#"),
                symbolKey("@"),
                symbolKey(","),
                symbolKey("."),
                noopCtrlKey(),
                },
                };

        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];

                if (key instanceof CharKey && ((CharKey) key).getType() == CharKey.Type.Alphabet) {
                    KeyColor color = latin_key_char_alphabet_level_colors[i];
                    key.setColor(color);
                }
            }
        }

        return changeLayoutForHandMode(config, keys);
    }

    private static Key<?>[][] emptyGridKeys() {
        Key<?>[][] gridKeys = new Key[6][8];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        return gridKeys;
    }

    /** 创建拼音后继字母第 1/2 级按键 */
    public static Key<?>[][] createPinyinNextCharKeys(
            Config config, String startChar, Collection<String> level1NextChars, Collection<String> level2NextChars
    ) {
        Key<?>[][] keys = createPinyinKeys(config);

        // Note: 第 1 级后继按键与键盘初始按键位置保持一致
        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];

                keys[i][j] = noopCtrlKey();
                if (!(key instanceof CharKey)) {
                    continue;
                }

                for (String nextChar : level1NextChars) {
                    if (nextChar.length() > key.getText().length() //
                        // Note: hng 中的第 1 级按键 ng 使用 n 所在键位
                        && nextChar.startsWith(key.getText())) {
                        keys[i][j] = alphabetKey(nextChar).setColor(key.getColor());
                        break;
                    } else if (nextChar.equals(key.getText())) {
                        keys[i][j] = key;
                        break;
                    }
                }
            }
        }

        boolean isLeft = config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;
        // 在指定可用位置创建第 2 级字母按键
        Iterator<String> it = level2NextChars.iterator();
        int[][][] levelKeyCoords = isLeft ? pinyin_level_2_key_level_coords_left_hand : pinyin_level_2_key_level_coords;

        for (int level = 0; level < levelKeyCoords.length; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                if (!it.hasNext()) {
                    break;
                }

                String text = it.next();
                int x = keyCoord[0];
                int y = keyCoord[1];
                KeyColor color = key_char_around_level_colors[level];

                if (text == null) {
                    keys[x][y] = noopCtrlKey();
                } else {
                    keys[x][y] = alphabetKey(text).setLabel(startChar + text).setColor(color);
                }
            }
        }

        return keys;
    }

    /** 候选字按键的分页大小 */
    public static int getInputCandidateKeysPageSize() {
        int size = 0;
        for (int[][] level : input_word_key_around_level_coords) {
            size += level.length;
        }
        return size;
    }

    /** 创建输入候选字按键 */
    public static Key<?>[][] createInputCandidateWordKeys(
            Config config, CharInput input, List<InputWord> candidates, int startIndex, int pageSize
    ) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int dataSize = candidates.size();
        int currentPage = startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));
        int remainDataSize = dataSize - startIndex;

        int index_0 = changeIndexForHandMode(config, gridKeys, 0);
        int index_1 = changeIndexForHandMode(config, gridKeys, 1);
        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[3][index_3] = ctrlKey(config, CtrlKey.Type.ConfirmInput);
        gridKeys[5][index_6] = ctrlKey(config, CtrlKey.Type.DropInput);

        // 从第二页开始支持按笔画过滤
        if (currentPage > 1 && totalPage > 1 && remainDataSize > 8) {
            gridKeys[0][index_1] = ctrlKey(config, CtrlKey.Type.FilterInputCandidate_stroke_heng).setLabel("一");
            gridKeys[1][index_0] = ctrlKey(config, CtrlKey.Type.FilterInputCandidate_stroke_shu).setLabel("丨");
            gridKeys[2][index_0] = ctrlKey(config, CtrlKey.Type.FilterInputCandidate_stroke_pie).setLabel("丿");
            gridKeys[4][index_0] = ctrlKey(config, CtrlKey.Type.FilterInputCandidate_stroke_na).setLabel("㇏");
            gridKeys[5][index_0] = ctrlKey(config,
                                           CtrlKey.Type.FilterInputCandidate_stroke_zhe).setLabel("\uD840\uDCCB");
        }

        if (currentPage == 1) {
            gridKeys[3][index_6] = ctrlKey(config, CtrlKey.Type.CommitInputList);
        }

        gridKeys[0][index_0] = noopCtrlKey(currentPage + "/" + totalPage);

        CharInput startingToggle = input.copy();
        if (input.is_Pinyin_SCZ_Starting()) {
            String s = input.getChars().get(0).substring(0, 1);

            gridKeys[0][index_6] = ctrlKey(config, CtrlKey.Type.ToggleInputSpell_zcs_h).setLabel(s + "," + s + "h");
            startingToggle.toggle_Pinyin_SCZ_Starting();
        } else if (input.is_Pinyin_NL_Starting()) {
            // Note: 第二个右侧添加占位空格，以让字母能够对齐切换箭头
            gridKeys[0][index_6] = ctrlKey(config, CtrlKey.Type.ToggleInputSpell_nl).setLabel("n,l  ");
            startingToggle.toggle_Pinyin_NL_Starting();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!startingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(startingToggle)) {
            gridKeys[0][index_6] = noopCtrlKey();
        }

        CharInput endingToggle = input.copy();
        if (input.is_Pinyin_NG_Ending()) {
            String s = input.getChars().get(input.getChars().size() - 1);
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);

            gridKeys[1][index_6] = ctrlKey(config, CtrlKey.Type.ToggleInputSpell_ng).setLabel(tail + "," + tail + "g");
            endingToggle.toggle_Pinyin_NG_Ending();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!endingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(endingToggle)) {
            gridKeys[1][index_6] = noopCtrlKey();
        }

        int wordIndex = startIndex;
        int[][][] levelKeyCoords = changeLayoutForHandMode(config, input_word_key_around_level_coords);

        for (int level = 0; level < levelKeyCoords.length; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (wordIndex < candidates.size()) {
                    InputWord word = candidates.get(wordIndex);

                    if (word != null) {
                        KeyColor color = key_char_around_level_colors[level];

                        InputWordKey key = InputWordKey.create(word).setColor(color);

                        // 禁用已被选中的候选字按键
                        if (word.equals(input.getWord())) {
                            key.setDisabled(true);
                        }

                        gridKeys[x][y] = key;
                    }
                } else {
                    break;
                }

                wordIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 创建定位按键 */
    public static Key<?>[][] createLocatorKeys(Config config) {
        Key<?>[][] gridKeys = emptyGridKeys();

        boolean isLeft = config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left;
        int index_1 = changeIndexForHandMode(config, gridKeys, 1);
        int index_2 = changeIndexForHandMode(config, gridKeys, 2);
        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_4 = changeIndexForHandMode(config, gridKeys, 4);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[3][index_1] = ctrlKey(config, CtrlKey.Type.LocateInputCursor_Locator);
        gridKeys[3][index_4] = ctrlKey(config, CtrlKey.Type.LocateInputCursor_Selector);

        gridKeys[1][index_6] = ctrlKey(config, CtrlKey.Type.Backspace);
        gridKeys[3][index_6] = enterCtrlKey(config);
        gridKeys[5][index_6] = ctrlKey(config, CtrlKey.Type.Exit);

        gridKeys[4][isLeft ? 4 : 3] = ctrlKey(config, CtrlKey.Type.Cut).setLabel("剪切");
        gridKeys[5][index_1] = ctrlKey(config, CtrlKey.Type.Redo).setLabel("重做");
        gridKeys[5][index_2] = ctrlKey(config, CtrlKey.Type.Undo).setLabel("撤销");
        gridKeys[5][index_3] = ctrlKey(config, CtrlKey.Type.Paste).setLabel("粘贴");
        gridKeys[5][index_4] = ctrlKey(config, CtrlKey.Type.Copy).setLabel("复制");

        return gridKeys;
    }

    /** 表情符号按键的分页大小 */
    public static int getEmojiKeysPageSize() {
        int size = 0;
        for (int[][] level : input_word_key_around_level_coords) {
            size += level.length;
        }
        return size;
    }

    /** 创建表情符号按键 */
    public static Key<?>[][] createEmojiKeys(
            Config config, CharInput input, List<String> groups, List<InputWord> words, String selectedGroup,
            int startIndex, int pageSize
    ) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int dataSize = words.size();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));
        InputWord selectedWord = input != null ? input.getWord() : null;

        int index_0 = changeIndexForHandMode(config, gridKeys, 0);
        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        for (int i = 0, j = 0; i < emoji_group_key_coords.length && j < groups.size(); i++, j++) {
            int[] keyCoord = emoji_group_key_coords[i];
            String group = groups.get(j);
            boolean selected = group.equals(selectedGroup);

            int x = keyCoord[0];
            int y = keyCoord[1];
            gridKeys[x][y] = ctrlKey(config, CtrlKey.Type.ToggleEmoji_Group).setLabel(group).setDisabled(selected);
        }

        gridKeys[3][index_3] = ctrlKey(config, CtrlKey.Type.ConfirmInput);
//        gridKeys[5][index_6] = ctrlKey(config, CtrlKey.Type.DropInput);

//        if (currentPage == 1) {
//            gridKeys[3][index_6] = ctrlKey(config, CtrlKey.Type.CommitInputList);
//        }

        int wordIndex = startIndex;
        int[][][] levelKeyCoords = changeLayoutForHandMode(config, emoji_key_level_coords);

        for (int level = 0; level < levelKeyCoords.length; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (wordIndex < words.size()) {
                    InputWord word = words.get(wordIndex);

                    if (word != null) {
                        KeyColor color = latin_key_char_alphabet_level_colors[level];

                        InputWordKey key = InputWordKey.create(word).setColor(color);

                        // 禁用已被选中的按键
                        if (word.equals(selectedWord)) {
                            key.setDisabled(true);
                        }

                        gridKeys[x][y] = key;
                    }
                } else {
                    break;
                }

                wordIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 标点符号按键的分页大小 */
    public static int getSymbolKeysPageSize() {
        int size = 0;
        for (int[][] level : symbol_key_around_level_coords) {
            size += level.length;
        }
        return size;
    }

    /** 创建标点符号按键 */
    public static Key<?>[][] createSymbolKeys(Config config, Symbol[] symbols, int startIndex, int pageSize) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int dataSize = symbols.length;
        if (dataSize == 0) {
            return gridKeys;
        }

        int currentPage = startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_0 = changeIndexForHandMode(config, gridKeys, 0);
        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[1][index_6] = ctrlKey(config, CtrlKey.Type.ToggleSymbol_Locale_Zh_and_En).setLabel("中/英");
        gridKeys[3][index_3] = ctrlKey(config, CtrlKey.Type.LocateInputCursor);
        gridKeys[5][index_6] = ctrlKey(config, CtrlKey.Type.Exit);

        gridKeys[0][index_0] = noopCtrlKey(currentPage + "/" + totalPage);

        int symbolIndex = startIndex;
        int[][][] levelKeyCoords = changeLayoutForHandMode(config, symbol_key_around_level_coords);

        for (int level = 0; level < levelKeyCoords.length; level++) {
            int[][] keyCoords = levelKeyCoords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (symbolIndex < symbols.length) {
                    Symbol symbol = symbols[symbolIndex];

                    KeyColor color = key_char_around_level_colors[level];
                    String text = symbol.getText();
                    CharKey key = symbol.isDoubled() ? doubleSymbolKey(text) : symbolKey(text);

                    if (symbol.isDoubled()) {
                        String label = text.charAt(0) + " " + text.charAt(1);
                        key.setLabel(label);
                    }

                    key.setColor(color);
                    symbol.getReplacements().forEach(key::withReplacements);

                    gridKeys[x][y] = key;
                } else {
                    break;
                }

                symbolIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 创建{@link MathKeyboard 数学键盘}按键 */
    public static Key<?>[][] createMathKeys(Config config, Key<?>[] keys) {
        Key<?>[][] gridKeys = emptyGridKeys();

        int index_0 = changeIndexForHandMode(config, gridKeys, 0);
        int index_3 = changeIndexForHandMode(config, gridKeys, 3);
        int index_6 = changeIndexForHandMode(config, gridKeys, 6);

        gridKeys[0][index_0] = ctrlKey(config, CtrlKey.Type.SwitchHandMode);
        gridKeys[1][index_6] = ctrlKey(config, CtrlKey.Type.Backspace);
        gridKeys[2][index_6] = ctrlKey(config, CtrlKey.Type.Space);
        gridKeys[3][index_6] = config.hasInputs ? ctrlKey(config, CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[3][index_3] = ctrlKey(config, CtrlKey.Type.Math_Equal).setLabel("=");

        if (config.keyboardConfig.getSwitchFromType() != null) {
            gridKeys[5][index_6] = ctrlKey(config, CtrlKey.Type.Exit);
        }

        int keyIndex = 0;
        int[][][] levelKeyCoords = changeLayoutForHandMode(config, math_key_around_level_coords);

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
        int[][][] levelKeyCoords = changeLayoutForHandMode(config, number_key_around_level_coords);

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

    public static CtrlKey enterCtrlKey(Config config) {
        return config.keyboardConfig.isSingleLineInput()
               ? ctrlKey(config,
                         CtrlKey.create(CtrlKey.Type.Enter),
                         key_ctrl_ok_style)
               : ctrlKey(config, CtrlKey.Type.Enter);
    }

    public static CtrlKey ctrlKey(Config config, CtrlKey.Type type) {
        return ctrlKey(config, CtrlKey.create(type));
    }

    public static CtrlKey ctrlKey(Config config, CtrlKey key) {
        KeyStyle style = ctrl_key_styles.get(key.getType());

        return ctrlKey(config, key, style);
    }

    public static CtrlKey ctrlKey(Config config, CtrlKey key, KeyStyle style) {
        int icon = 0;
        KeyColor color = KeyColor.none();

        if (style != null) {
            icon = style.icon.right;
            if (config != null && config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left) {
                icon = style.icon.left;
            }

            color = style.color;
        }

        return key.setIconResId(icon).setColor(color);
    }

    public static CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    public static CtrlKey noopCtrlKey(String label) {
        return ctrlKey(null, CtrlKey.noop().setLabel(label));
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

    public static CharKey doubleSymbolKey(String text) {
        return charKey(CharKey.Type.DoubleSymbol, text);
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
    private static <T> T[][] changeLayoutForHandMode(Config config, T[][] rightHandLayout) {
        if (config.keyboardConfig.getHandMode() == Keyboard.HandMode.Left) {
            T[][] newLayout = Arrays.copyOf(rightHandLayout, rightHandLayout.length);

            for (int i = 0; i < newLayout.length; i++) {
                T[] newRow = newLayout[i] = Arrays.copyOf(rightHandLayout[i], rightHandLayout[i].length);

                int mid = (newRow.length - 1) / 2;
                for (int j = newRow.length - 1, k = 0; j > mid; j--, k++) {
                    T tmp = newRow[j];
                    newRow[j] = newRow[k];
                    newRow[k] = tmp;
                }
            }
            return newLayout;
        }
        return rightHandLayout;
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
    }
}
