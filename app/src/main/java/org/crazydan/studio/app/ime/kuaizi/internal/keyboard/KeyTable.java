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
    private static final Map<List<String>, Integer[]> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, Integer[]> ctrl_key_styles = new HashMap<>();
    /** 环绕型字符按键布局的按键背景和前景色：从内到外分为不同层级 */
    private static final int[][] key_char_around_level_bg_colors = new int[][] {
            new int[] { R.attr.key_char_level_2_bg_color, R.attr.key_char_level_2_fg_color },
            new int[] { R.attr.key_char_level_4_bg_color, R.attr.key_char_level_4_fg_color },
            new int[] { R.attr.key_char_level_3_bg_color, R.attr.key_char_level_3_fg_color },
            new int[] { R.attr.key_char_level_1_bg_color, R.attr.key_char_level_1_fg_color },
            };

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
                    new int[] { 2, 2 }, new int[] { 2, 3 }, new int[] { 2, 1 },
                    },
            //
            new int[][] {
                    new int[] { 1, 2 }, new int[] { 1, 1 },
                    },
            //
            new int[][] {
                    new int[] { 3, 2 }, new int[] { 3, 1 },
                    },
            //
            new int[][] {
                    new int[] { 1, 3 }, new int[] { 3, 3 }, new int[] { 2, 4 },
                    },
            };

    static {
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü", "v"), new Integer[] {
                R.attr.key_char_level_0_fg_color, R.attr.key_char_level_0_bg_color
        });
        char_key_color_palette.put(Arrays.asList("ch", "sh", "zh"), new Integer[] {
                R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color
        });
        char_key_color_palette.put(Arrays.asList("h", "w", "z", "x", "y"), new Integer[] {
                R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color
        });
        char_key_color_palette.put(Arrays.asList("f", "g", "d", "b", "c"), new Integer[] {
                R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color
        });
        char_key_color_palette.put(Arrays.asList("p", "q", "s", "t", "r"), new Integer[] {
                R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color
        });
        char_key_color_palette.put(Arrays.asList("k", "j", "m", "l", "n"), new Integer[] {
                R.attr.key_char_level_5_fg_color, R.attr.key_char_level_5_bg_color
        });

        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            new Integer[] { R.drawable.ic_left_hand_backspace, R.attr.key_ctrl_backspace_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Space, new Integer[] { R.drawable.ic_space, R.attr.key_ctrl_space_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            new Integer[] { R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.CommitInputList,
                            new Integer[] { R.drawable.ic_right_hand_like, R.attr.key_ctrl_commit_bg_color });

        ctrl_key_styles.put(CtrlKey.Type.SwitchIME,
                            new Integer[] { R.drawable.ic_keyboard, R.attr.key_ctrl_switcher_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.SwitchHandMode, new Integer[] {
                R.drawable.ic_switch_to_left_hand, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToMathKeyboard, new Integer[] {
                R.drawable.ic_calculator, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToSymbolKeyboard, new Integer[] {
                R.drawable.ic_symbol, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToEmotionKeyboard, new Integer[] {
                R.drawable.ic_emotion, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToLatinKeyboard, new Integer[] {
                R.drawable.ic_switch_zi_to_a, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToPinyinKeyboard, new Integer[] {
                R.drawable.ic_switch_a_to_zi, R.attr.key_ctrl_switcher_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToNumberKeyboard, new Integer[] {
                R.drawable.ic_alphabet_number, R.attr.key_ctrl_switcher_bg_color
        });

        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor,
                            new Integer[] { R.drawable.ic_right_hand_pointer, R.attr.key_ctrl_locator_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor_Locator,
                            new Integer[] { R.drawable.ic_map_location_pin, R.attr.key_ctrl_locator_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.LocateInputCursor_Selector,
                            new Integer[] { R.drawable.ic_right_hand_selection, R.attr.key_ctrl_locator_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Exit,
                            new Integer[] { R.drawable.ic_right_hand_exit, R.attr.key_ctrl_exit_bg_color });

        ctrl_key_styles.put(CtrlKey.Type.DropInput,
                            new Integer[] { R.drawable.ic_trash_can, R.attr.key_ctrl_backspace_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.ConfirmInput,
                            new Integer[] { R.drawable.ic_confirm, R.attr.key_ctrl_confirm_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.RevokeInput,
                            new Integer[] { R.drawable.ic_revoke_input, R.attr.key_ctrl_switcher_bg_color });

        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_ng, new Integer[] {
                -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_zcs_h, new Integer[] {
                -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_nl, new Integer[] {
                -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color
        });

        ctrl_key_styles.put(CtrlKey.Type.NoOp,
                            new Integer[] { -1, R.attr.key_ctrl_noop_bg_color, R.attr.key_ctrl_noop_fg_color });
        ctrl_key_styles.put(CtrlKey.Type.Undo,
                            new Integer[] { -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color });
        ctrl_key_styles.put(CtrlKey.Type.Redo,
                            new Integer[] { -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color });
        ctrl_key_styles.put(CtrlKey.Type.Cut, new Integer[] { -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color });
        ctrl_key_styles.put(CtrlKey.Type.Paste,
                            new Integer[] { -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color });
        ctrl_key_styles.put(CtrlKey.Type.Copy,
                            new Integer[] { -1, R.attr.key_bg_color, R.attr.key_highlight_fg_color });

        ctrl_key_styles.put(CtrlKey.Type.Math_Equal,
                            new Integer[] { -1, R.attr.key_fg_color, R.attr.key_highlight_fg_color });
    }

    /** 创建{@link PinyinKeyboard 拼音键盘}按键 */
    public static Key<?>[][] createPinyinKeys(Config config) {
        // TODO 根据系统支持情况和配置等信息，调整部分按键的显示或隐藏

        // 右手模式的纵向屏幕 7 x 6 的按键表
        return new Key[][] {
                new Key[] {
                        //ctrlKey(CtrlKey.Type.SwitchIME),
                        ctrlKey(CtrlKey.Type.SwitchHandMode).setDisabled(true),
                        alphabetKey("zh").withReplacements("Zh", "ZH"),
                        alphabetKey("ch").withReplacements("Ch", "CH"),
                        alphabetKey("sh").withReplacements("Sh", "SH"),
                        alphabetKey("o").withReplacements("O"),
                        alphabetKey("e").withReplacements("E"),
                        alphabetKey("a").withReplacements("A"),
                        } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToMathKeyboard),
                alphabetKey("f").withReplacements("F"),
                alphabetKey("g").withReplacements("G"),
                alphabetKey("b").withReplacements("B"),
                alphabetKey("c").withReplacements("C"),
                alphabetKey("d").withReplacements("D"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToLatinKeyboard),
                alphabetKey("k").withReplacements("K"),
                alphabetKey("j").withReplacements("J"),
                alphabetKey("n").withReplacements("N"),
                alphabetKey("m").withReplacements("M"),
                alphabetKey("l").withReplacements("L"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                symbolKey("！").withReplacements("!"),
                alphabetKey("p").withReplacements("P"),
                alphabetKey("r").withReplacements("R"),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("h").withReplacements("H"),
                alphabetKey("y").withReplacements("Y"),
                config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard),
                alphabetKey("q").withReplacements("Q"),
                alphabetKey("s").withReplacements("S"),
                alphabetKey("t").withReplacements("T"),
                alphabetKey("w").withReplacements("W"),
                alphabetKey("x").withReplacements("X"),
                alphabetKey("z").withReplacements("Z"),
                } //
                , new Key[] {
                symbolKey("？").withReplacements("?"),
                alphabetKey("u").withReplacements("U"),
                alphabetKey("i").withReplacements("I"),
                alphabetKey("ü").withReplacements("v", "V"),
                symbolKey("。").withReplacements("."),
                symbolKey("，").withReplacements(","),
                ctrlKey(CtrlKey.Type.RevokeInput).setDisabled(true),
                },
                };
    }

    /** 创建{@link LatinKeyboard 拉丁文键盘}按键 */
    public static Key<?>[][] createLatinKeys(Config config) {
        return new Key[][] {
                new Key[] {
                        //ctrlKey(CtrlKey.Type.SwitchIME),
                        ctrlKey(CtrlKey.Type.SwitchHandMode).setDisabled(true),
                        alphabetKey("a").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        alphabetKey("b").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        alphabetKey("c").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        alphabetKey("d").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        alphabetKey("e").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        alphabetKey("f").setBgColorAttrId(R.attr.key_char_level_0_bg_color),
                        } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("g").setBgColorAttrId(R.attr.key_char_level_1_bg_color),
                alphabetKey("h").setBgColorAttrId(R.attr.key_char_level_1_bg_color),
                alphabetKey("i").setBgColorAttrId(R.attr.key_char_level_1_bg_color),
                alphabetKey("j").setBgColorAttrId(R.attr.key_char_level_1_bg_color),
                alphabetKey("k").setBgColorAttrId(R.attr.key_char_level_1_bg_color),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToPinyinKeyboard),
                alphabetKey("l").setBgColorAttrId(R.attr.key_char_level_2_bg_color),
                alphabetKey("m").setBgColorAttrId(R.attr.key_char_level_2_bg_color),
                alphabetKey("n").setBgColorAttrId(R.attr.key_char_level_2_bg_color),
                alphabetKey("o").setBgColorAttrId(R.attr.key_char_level_2_bg_color),
                alphabetKey("p").setBgColorAttrId(R.attr.key_char_level_2_bg_color),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("q").setBgColorAttrId(R.attr.key_char_level_3_bg_color),
                alphabetKey("r").setBgColorAttrId(R.attr.key_char_level_3_bg_color),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("s").setBgColorAttrId(R.attr.key_char_level_3_bg_color),
                alphabetKey("t").setBgColorAttrId(R.attr.key_char_level_3_bg_color),
                enterCtrlKey(config),
                } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("u").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                alphabetKey("v").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                alphabetKey("w").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                alphabetKey("x").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                alphabetKey("y").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                alphabetKey("z").setBgColorAttrId(R.attr.key_char_level_4_bg_color),
                } //
                , new Key[] {
                symbolKey(":"),
                symbolKey("?"),
                symbolKey("#"),
                symbolKey("@"),
                symbolKey(","),
                symbolKey("."),
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard).setDisabled(true),
                },
                };
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
                        keys[i][j] = alphabetKey(nextChar).setFgColorAttrId(key.getFgColorAttrId())
                                                          .setBgColorAttrId(key.getBgColorAttrId());
                        break;
                    } else if (nextChar.equals(key.getText())) {
                        keys[i][j] = key;
                        break;
                    }
                }
            }
        }

        // 在指定可用位置创建第 2 级字母按键
        Iterator<String> it = level2NextChars.iterator();
        for (int level = 0; level < pinyin_level_2_key_level_coords.length; level++) {
            int[][] keyCoords = pinyin_level_2_key_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                if (!it.hasNext()) {
                    break;
                }

                String text = it.next();
                int x = keyCoord[0];
                int y = keyCoord[1];
                int bgAttrId = key_char_around_level_bg_colors[level][0];
                int fgAttrId = key_char_around_level_bg_colors[level][1];

                if (text == null) {
                    keys[x][y] = noopCtrlKey();
                } else {
                    keys[x][y] = alphabetKey(text).setLabel(startChar + text)
                                                  .setFgColorAttrId(fgAttrId)
                                                  .setBgColorAttrId(bgAttrId);
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
        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[3][3] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[3][6] = ctrlKey(CtrlKey.Type.CommitInputList);
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.DropInput);

        if (!candidates.isEmpty()) {
            gridKeys[0][0] = noopCtrlKey((startIndex / pageSize + 1) //
                                         + "/" //
                                         + ((int) Math.ceil(candidates.size() / (pageSize * 1.0))));
        }

        CharInput startingToggle = input.copy();
        if (input.is_Pinyin_SCZ_Starting()) {
            String s = input.getChars().get(0).substring(0, 1);

            gridKeys[0][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_zcs_h, s + "," + s + "h");
            startingToggle.toggle_Pinyin_SCZ_Starting();
        } else if (input.is_Pinyin_NL_Starting()) {
            // Note: 第二个右侧添加占位空格，以让字母能够对齐切换箭头
            gridKeys[0][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_nl, "n,l  ");
            startingToggle.toggle_Pinyin_NL_Starting();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!startingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(startingToggle)) {
            gridKeys[0][6] = noopCtrlKey();
        }

        CharInput endingToggle = input.copy();
        if (input.is_Pinyin_NG_Ending()) {
            String s = input.getChars().get(input.getChars().size() - 1);
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);

            gridKeys[1][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_ng, tail + "," + tail + "g");
            endingToggle.toggle_Pinyin_NG_Ending();
        }
        // 若拼音变换无效，则不提供切换按钮
        if (!endingToggle.getChars().equals(input.getChars()) //
            && !PinyinDictDB.getInstance().hasValidPinyin(endingToggle)) {
            gridKeys[1][6] = noopCtrlKey();
        }

        int wordIndex = startIndex;
        for (int level = 0; level < input_word_key_around_level_coords.length; level++) {
            int[][] keyCoords = input_word_key_around_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (wordIndex < candidates.size()) {
                    InputWord word = candidates.get(wordIndex);

                    if (word != null) {
                        int bgAttrId = key_char_around_level_bg_colors[level][0];
                        int fgAttrId = key_char_around_level_bg_colors[level][1];

                        InputWordKey key = InputWordKey.create(word)
                                                       .setFgColorAttrId(fgAttrId)
                                                       .setBgColorAttrId(bgAttrId);

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
        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[3][1] = ctrlKey(CtrlKey.Type.LocateInputCursor_Locator);
        gridKeys[3][4] = ctrlKey(CtrlKey.Type.LocateInputCursor_Selector);

        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][6] = enterCtrlKey(config);
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.Exit);

        gridKeys[4][3] = ctrlKey(CtrlKey.Type.Cut, "剪切");
        gridKeys[5][1] = ctrlKey(CtrlKey.Type.Redo, "重做");
        gridKeys[5][2] = ctrlKey(CtrlKey.Type.Undo, "撤销");
        gridKeys[5][3] = ctrlKey(CtrlKey.Type.Paste, "粘贴");
        gridKeys[5][4] = ctrlKey(CtrlKey.Type.Copy, "复制");

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
    public static Key<?>[][] createSymbolKeys(Config config, int startIndex, Symbol[] symbols) {
        int pageSize = getSymbolKeysPageSize();

        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[0][6] = ctrlKey(CtrlKey.Type.SwitchToEmotionKeyboard).setDisabled(true);
        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[2][6] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[3][3] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][6] = config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.Exit).setIconResId(R.drawable.ic_right_hand_exit);

        gridKeys[0][0] = noopCtrlKey((startIndex / pageSize + 1) //
                                     + "/" //
                                     + ((int) Math.ceil(symbols.length / (pageSize * 1.0))));

        int symbolIndex = startIndex;
        for (int level = 0; level < symbol_key_around_level_coords.length; level++) {
            int[][] keyCoords = symbol_key_around_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (symbolIndex < symbols.length) {
                    Symbol symbol = symbols[symbolIndex];

                    int bgAttrId = key_char_around_level_bg_colors[level][0];
                    int fgAttrId = key_char_around_level_bg_colors[level][1];
                    String text = symbol.getText();
                    CharKey key = symbol.isDoubled() ? doubleSymbolKey(text) : symbolKey(text);

                    if (symbol.isDoubled()) {
                        String label = text.charAt(0) + " " + text.charAt(1);
                        key.setLabel(label);
                    }

                    key.setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);
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
        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[0][0] = ctrlKey(CtrlKey.Type.SwitchHandMode).setDisabled(true);
        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[2][6] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[3][6] = config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[3][3] = ctrlKey(CtrlKey.Type.Math_Equal, "=");
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.Exit).setIconResId(R.drawable.ic_right_hand_exit);

        int keyIndex = 0;
        for (int level = 0; level < math_key_around_level_coords.length; level++) {
            int[][] keyCoords = math_key_around_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (keyIndex < keys.length) {
                    Key<?> key = keys[keyIndex];

                    int bgAttrId = key_char_around_level_bg_colors[level][0];
                    int fgAttrId = key_char_around_level_bg_colors[level][1];
                    key.setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);

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
        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[2][6] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[3][3] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][6] = enterCtrlKey(config);

        int keyIndex = 0;
        for (int level = 0; level < number_key_around_level_coords.length; level++) {
            int[][] keyCoords = number_key_around_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (keyIndex < keys.length) {
                    Key<?> key = keys[keyIndex];

                    int bgAttrId = key_char_around_level_bg_colors[level][0];
                    int fgAttrId = key_char_around_level_bg_colors[level][1];
                    key.setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);

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
               ? ctrlKey(CtrlKey.Type.Enter).setIconResId(R.drawable.ic_right_hand_ok)
                                            .setBgColorAttrId(R.attr.key_ctrl_ok_bg_color)
               : ctrlKey(CtrlKey.Type.Enter);
    }

    public static CtrlKey ctrlKey(CtrlKey.Type type) {
        return ctrlKey(type, null);
    }

    public static CtrlKey ctrlKey(CtrlKey.Type type, String label) {
        return ctrlKey(CtrlKey.create(type).setLabel(label));
    }

    public static CtrlKey ctrlKey(CtrlKey key) {
        int iconResId = 0;
        int bgAttrId = 0;
        int fgAttrId = 0;

        Integer[] configs = ctrl_key_styles.get(key.getType());
        if (configs != null) {
            iconResId = configs[0];
            bgAttrId = configs[1];
            fgAttrId = configs.length > 2 ? configs[2] : 0;
        }

        return key.setIconResId(iconResId).setBgColorAttrId(bgAttrId).setFgColorAttrId(fgAttrId);
    }

    public static CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    public static CtrlKey noopCtrlKey(String label) {
        return ctrlKey(CtrlKey.noop().setLabel(label));
    }

    public static CharKey alphabetKey(String text) {
        return charKey(CharKey.Type.Alphabet, text);
    }

    public static CharKey numberKey(String text) {
        return charKey(CharKey.Type.Number, text);
    }

    public static CharKey symbolKey(String text) {
        return charKey(CharKey.Type.Symbol, text).setFgColorAttrId(R.attr.key_char_symbol_fg_color)
                                                 .setBgColorAttrId(R.attr.key_char_symbol_bg_color);
    }

    public static CharKey doubleSymbolKey(String text) {
        return charKey(CharKey.Type.DoubleSymbol, text);
    }

    private static CharKey charKey(CharKey.Type type, String text) {
        return charKey(CharKey.create(type, text).setLabel(text));
    }

    private static CharKey charKey(CharKey key) {
        int fgAttrId = 0;
        int bgAttrId = 0;

        for (Map.Entry<List<String>, Integer[]> entry : char_key_color_palette.entrySet()) {
            Integer[] configs = entry.getValue();
            if (entry.getKey().contains(key.getText().toLowerCase())) {
                fgAttrId = configs[0];
                bgAttrId = configs[1];
                break;
            }
        }

        return key.setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);
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
