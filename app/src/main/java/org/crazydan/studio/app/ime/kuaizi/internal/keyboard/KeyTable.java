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
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
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
                    new int[] { 2, 3 },
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    },
            // level 2
            new int[][] {
                    new int[] { 1, 2 },
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
                    },
            // level 3
            new int[][] {
                    new int[] { 0, 2 },
                    new int[] { 0, 3 },
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
                    },
            };

    /** 从中心按键由内到外的标点符号环形布局坐标 */
    private static final int[][][] symbol_key_around_level_coords = new int[][][] {
            // level 1
            new int[][] {
                    new int[] { 2, 3 },
                    new int[] { 2, 4 },
                    new int[] { 3, 4 },
                    new int[] { 4, 4 },
                    new int[] { 4, 3 },
                    new int[] { 3, 2 },
                    },
            // level 2
            new int[][] {
                    new int[] { 1, 2 },
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
                    },
            // level 3
            new int[][] {
                    new int[] { 0, 2 },
                    new int[] { 0, 3 },
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

    static {
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü"), new Integer[] {
                R.attr.key_char_level_0_fg_color, R.attr.key_char_level_0_bg_color
        });
        char_key_color_palette.put(Arrays.asList("p", "q", "g", "h", "d", "b", "c"), new Integer[] {
                R.attr.key_char_level_1_fg_color, R.attr.key_char_level_1_bg_color
        });
        char_key_color_palette.put(Arrays.asList("s", "r", "t", "z", "x", "y"), new Integer[] {
                R.attr.key_char_level_2_fg_color, R.attr.key_char_level_2_bg_color
        });
        char_key_color_palette.put(Arrays.asList("w"), new Integer[] {
                R.attr.key_char_level_3_fg_color, R.attr.key_char_level_3_bg_color
        });
        char_key_color_palette.put(Arrays.asList("l", "m", "n"), new Integer[] {
                R.attr.key_char_level_4_fg_color, R.attr.key_char_level_4_bg_color
        });
        char_key_color_palette.put(Arrays.asList("j", "k", "f"), new Integer[] {
                R.attr.key_char_level_5_fg_color, R.attr.key_char_level_5_bg_color
        });
        char_key_color_palette.put(Arrays.asList("：", "！", "？", "；", "，", "。"), new Integer[] {
                R.attr.key_char_symbol_fg_color, R.attr.key_char_symbol_bg_color
        });

        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            new Integer[] { R.drawable.ic_left_hand_backspace, R.attr.key_ctrl_backspace_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Space, new Integer[] { R.drawable.ic_space, R.attr.key_ctrl_space_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            new Integer[] { R.drawable.ic_new_line, R.attr.key_ctrl_enter_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.CommitInputList,
                            new Integer[] { R.drawable.ic_right_hand_like, R.attr.key_ctrl_confirm_bg_color });

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
                            new Integer[] { R.drawable.ic_right_hand_like, R.attr.key_ctrl_confirm_bg_color });

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

    /** 创建基础按键 */
    public static Key<?>[][] createKeys(Config config) {
        // TODO 根据系统支持情况和配置等信息，调整部分按键的显示或隐藏

        // 右手模式的纵向屏幕 7 x 6 的按键表
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.SwitchIME),
                        alphabetKey("ü").withReplacements("v", "V"),
                        alphabetKey("a").withReplacements("A"),
                        alphabetKey("e").withReplacements("E"),
                        alphabetKey("i").withReplacements("I"),
                        alphabetKey("o").withReplacements("O"),
                        alphabetKey("u").withReplacements("U"),
                        } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchHandMode).setDisabled(true),
                alphabetKey("h").withReplacements("H"),
                alphabetKey("p").withReplacements("P"),
                alphabetKey("w").withReplacements("W"),
                alphabetKey("b").withReplacements("B"),
                alphabetKey("c").withReplacements("C"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToMathKeyboard),
                alphabetKey("g").withReplacements("G"),
                alphabetKey("r").withReplacements("R"),
                alphabetKey("q").withReplacements("Q"),
                alphabetKey("d").withReplacements("D"),
                alphabetKey("z").withReplacements("Z"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("n").withReplacements("N"),
                alphabetKey("s").withReplacements("S"),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("y").withReplacements("Y"),
                alphabetKey("k").withReplacements("K"),
                config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config),
                } //
                , new Key[] {
                noopCtrlKey(),
                alphabetKey("m").withReplacements("M"),
                alphabetKey("l").withReplacements("L"),
                alphabetKey("t").withReplacements("T"),
                alphabetKey("x").withReplacements("X"),
                alphabetKey("f").withReplacements("F"),
                alphabetKey("j").withReplacements("J"),
                } //
                , new Key[] {
                symbolKey("：").withReplacements(":"),
                symbolKey("！").withReplacements("!"),
                symbolKey("？").withReplacements("?"),
                symbolKey("；").withReplacements(";"),
                symbolKey("，").withReplacements(","),
                symbolKey("。").withReplacements("."),
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard),
                },
                };
    }

    /**
     * 创建后继字母按键
     * <p/>
     * 若 <code>nextChars</code> 为 <code>null</code>，则创建{@link #createKeys 基础按键}，
     * 若其为空，则返回空白按键
     */
    public static Key<?>[][] createNextCharKeys(Config config, Collection<String> nextChars) {
        Key<?>[][] keys = createKeys(config);

        if (nextChars == null) {
            return keys;
        } else if (nextChars.isEmpty()) {
            return new Key[6][7];
        }

        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];
                if (!(key instanceof CharKey) //
                    || !nextChars.contains(key.getText())) {
                    keys[i][j] = null;
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
    public static Key<?>[][] createInputCandidateKeys(Config config, CharInput input, int startIndex) {
        List<InputWord> inputWords = input.getCandidates();
        int pageSize = getInputCandidateKeysPageSize();

        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[3][3] = ctrlKey(CtrlKey.Type.ConfirmInput);
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.DropInput);

        if (!inputWords.isEmpty()) {
            gridKeys[0][0] = noopCtrlKey((startIndex / pageSize + 1) //
                                         + "/" //
                                         + ((int) Math.ceil(inputWords.size() / (pageSize * 1.0))));
        }

        if (input.isPinyinStartsWithSCZ()) {
            String s = input.getChars().get(0);
            gridKeys[0][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_zcs_h, s + "," + s + "h");
        } else if (input.isPinyinStartsWithNL()) {
            gridKeys[0][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_nl, "n,l  ");
        }
        if (input.isPinyinEndsWithNG()) {
            String s = String.join("", input.getChars());
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);
            gridKeys[1][6] = ctrlKey(CtrlKey.Type.ToggleInputSpell_ng, tail + "," + tail + "g");
        }

        int wordIndex = startIndex;
        for (int level = 0; level < input_word_key_around_level_coords.length; level++) {
            int[][] keyCoords = input_word_key_around_level_coords[level];

            for (int[] keyCoord : keyCoords) {
                int x = keyCoord[0];
                int y = keyCoord[1];

                if (wordIndex < inputWords.size()) {
                    InputWord word = inputWords.get(wordIndex);

                    int bgAttrId = key_char_around_level_bg_colors[level][0];
                    int fgAttrId = key_char_around_level_bg_colors[level][1];
                    InputWordKey key = InputWordKey.create(word).setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);

                    // 禁用已被选中的候选字按键
                    if (word.equals(input.getWord())) {
                        key.setDisabled(true);
                    }

                    gridKeys[x][y] = key;
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

    /** 创建标点符号按键 */
    public static Key<?>[][] createSymbolKeys(Config config, int startIndex, Symbol[] symbols) {
        int pageSize = getInputCandidateKeysPageSize();

        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[2][6] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[3][3] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][6] = config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[5][6] = ctrlKey(CtrlKey.Type.Exit);

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

    /** 创建数学键盘按键 */
    public static Key<?>[][] createMathKeys(Config config, Key<?>[] keys) {
        Key<?>[][] gridKeys = new Key[6][7];
        Arrays.stream(gridKeys).forEach(row -> Arrays.fill(row, noopCtrlKey()));

        gridKeys[1][6] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[2][6] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[3][6] = config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(config);
        gridKeys[3][3] = ctrlKey(CtrlKey.Type.Math_Equal, "=");
        gridKeys[2][0] = ctrlKey(CtrlKey.Type.Exit).setIconResId(R.drawable.ic_left_hand_exit);

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

    public static CharKey symbolKey(String text) {
        return charKey(CharKey.Type.Symbol, text);
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
            if (entry.getKey().contains(key.getText())) {
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
