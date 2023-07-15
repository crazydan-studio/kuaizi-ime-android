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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.pinyin;

import java.util.Arrays;
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
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.HandMode;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;

/**
 * {@link PinyinKeyboard 汉语拼音键盘}的按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class KeyTable {
    /** 字母按键调色板 */
    private static final Map<List<String>, Integer[]> char_key_color_palette = new HashMap<>();
    /** 控制按键样式：图标+背景色 */
    private static final Map<CtrlKey.Type, Integer[]> ctrl_key_styles = new HashMap<>();
    /** 输入候选字按键背景色 */
    private static final int[] input_word_key_level_bg_colors = new int[] {
            R.attr.input_word_key_level_0_bg_color,
            R.attr.input_word_key_level_1_bg_color,
            R.attr.input_word_key_level_2_bg_color,
            R.attr.input_word_key_level_3_bg_color,
            };

    /** 以 候选字确认按键 为中心的从内到外的候选字环形布局坐标 */
    private static final int[][] grid_key_coords = new int[][] {
            // level 0
            new int[] { 3, 3 },
            // level 1: 1~6
            new int[] { 2, 3 },
            new int[] { 2, 4 },
            new int[] { 3, 4 },
            new int[] { 4, 4 },
            new int[] { 4, 3 },
            new int[] { 3, 2 },
            // level 2: 7~18
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
            // level 3: 19~36
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
            };

    static {
        char_key_color_palette.put(Arrays.asList("i", "a", "e", "o", "u", "ü"),
                                   new Integer[] { R.attr.key_char_final_fg_color, R.attr.key_char_final_bg_color });
        char_key_color_palette.put(Arrays.asList("f", "m", "p", "d", "b", "t", "y"), new Integer[] {
                R.attr.key_char_initial_fg_color, R.attr.key_char_initial_bg_color
        });
        char_key_color_palette.put(Arrays.asList("j", "q", "x"), new Integer[] {
                R.attr.key_char_initial_jqx_fg_color, R.attr.key_char_initial_jqx_bg_color
        });
        char_key_color_palette.put(Arrays.asList("s", "c", "z"), new Integer[] {
                R.attr.key_char_initial_scz_fg_color, R.attr.key_char_initial_scz_bg_color
        });
        char_key_color_palette.put(Arrays.asList("g", "w", "k"), new Integer[] {
                R.attr.key_char_initial_hgwk_fg_color, R.attr.key_char_initial_hgwk_bg_color
        });
        char_key_color_palette.put(Arrays.asList("r", "h", "n", "l"), new Integer[] {
                R.attr.key_char_initial_nl_fg_color, R.attr.key_char_initial_nl_bg_color
        });
        char_key_color_palette.put(Arrays.asList("：", "！", "？", "、", "，", "。"), new Integer[] {
                R.attr.key_char_punctuation_fg_color, R.attr.key_char_punctuation_bg_color
        });

        ctrl_key_styles.put(CtrlKey.Type.SwitchIME,
                            new Integer[] { R.drawable.ic_keyboard, R.attr.key_ctrl_switch_ime_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Backspace,
                            new Integer[] { R.drawable.ic_backspace_left, R.attr.key_ctrl_backspace_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.CommitInput,
                            new Integer[] { R.drawable.ic_right_like, R.attr.key_ctrl_confirm_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.DropInput,
                            new Integer[] { R.drawable.ic_trash_can, R.attr.key_ctrl_backspace_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_ng, new Integer[] {
                -1, R.attr.key_ctrl_toggle_input_spell_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_zcs_h, new Integer[] {
                -1, R.attr.key_ctrl_toggle_input_spell_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.ToggleInputSpell_nl, new Integer[] {
                -1, R.attr.key_ctrl_toggle_input_spell_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchHandMode, new Integer[] {
                R.drawable.ic_switch_to_left_hand, R.attr.key_ctrl_switch_hand_mode_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.Space, new Integer[] { R.drawable.ic_space, R.attr.key_ctrl_space_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.Enter,
                            new Integer[] { R.drawable.ic_enter_left, R.attr.key_ctrl_enter_bg_color });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToAlphanumericKeyboard, new Integer[] {
                R.drawable.ic_alphabet_number, R.attr.key_ctrl_switch_to_alphanumeric_keyboard_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.SwitchToPunctuationKeyboard, new Integer[] {
                R.drawable.ic_punctuation, R.attr.key_ctrl_switch_to_punctuation_keyboard_bg_color
        });
        ctrl_key_styles.put(CtrlKey.Type.Locator,
                            new Integer[] { R.drawable.ic_left_hand_move, R.attr.key_ctrl_locator_bg_color });
    }

    /** 创建基础按键 */
    public static Key<?>[][] createKeys(Keyboard.KeyFactory.Option option, Configure config) {
        // TODO 根据系统支持情况和配置等信息，调整部分按键的显示或隐藏

        // 右手模式的纵向屏幕 7 x 6 的按键表
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.SwitchIME).setDisabled(true),
                        punctuationKey("："),
                        alphabetKey("i"),
                        alphabetKey("a"),
                        alphabetKey("e"),
                        alphabetKey("o"),
                        alphabetKey("u"),
                        } //
                , new Key[] {
                punctuationKey("！"),
                alphabetKey("ü"),
                alphabetKey("j"),
                alphabetKey("q"),
                alphabetKey("s"),
                alphabetKey("z"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchHandMode).setDisabled(true),
                punctuationKey("？"),
                alphabetKey("l"),
                alphabetKey("x"),
                alphabetKey("g"),
                alphabetKey("c"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                punctuationKey("、"),
                alphabetKey("n"),
                alphabetKey("h"),
                ctrlKey(CtrlKey.Type.Locator),
                alphabetKey("w"),
                alphabetKey("k"),
                config.hasInputs ? ctrlKey(CtrlKey.Type.CommitInput) : ctrlKey(CtrlKey.Type.Enter),
                } //
                , new Key[] {
                noopCtrlKey(),
                punctuationKey("，"),
                alphabetKey("r"),
                alphabetKey("f"),
                alphabetKey("m"),
                alphabetKey("p"),
                ctrlKey(CtrlKey.Type.SwitchToAlphanumericKeyboard).setDisabled(true),
                } //
                , new Key[] {
                noopCtrlKey(),
                punctuationKey("。"),
                alphabetKey("d"),
                alphabetKey("b"),
                alphabetKey("t"),
                alphabetKey("y"),
                ctrlKey(CtrlKey.Type.SwitchToPunctuationKeyboard).setDisabled(true),
                },
                };
    }

    /**
     * 创建后继字母按键
     * <p/>
     * 若 <code>nextChars</code> 为 <code>null</code>，则创建{@link #createKeys 基础按键}，
     * 若其为空，则返回空白按键
     */
    public static Key<?>[][] createNextCharKeys(
            Keyboard.KeyFactory.Option option, Configure config, List<String> nextChars
    ) {
        Key<?>[][] keys = createKeys(option, config);

        if (nextChars == null) {
            return keys;
        } else if (nextChars.isEmpty()) {
            return new Key[6][7];
        }

        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];
                if (!(key instanceof CharKey) //
                    || !nextChars.contains(((CharKey) key).getText())) {
                    keys[i][j] = null;
                }
            }
        }
        return keys;
    }

    /** 候选字按键的分页大小 */
    public static int getInputCandidateKeysPageSize() {
        return grid_key_coords.length;
    }

    /** 创建输入候选字按键 */
    public static Key<?>[][] createInputCandidateKeys(
            Keyboard.KeyFactory.Option option, Configure config, CharInput input, int startIndex
    ) {
        Key<?>[][] defaultKeys = createKeys(option, config);

        List<InputWord> inputWords = input.getCandidates();
        int pageSize = getInputCandidateKeysPageSize();

        Key<?>[][] gridKeys = new Key[6][7];
        for (int i = 0; i < gridKeys.length; i++) {
            for (int j = 0; j < gridKeys[i].length; j++) {
                Key<?> defaultKey = defaultKeys[i][j];

                if (defaultKey instanceof CharKey) {
                    CharKey charKey = (CharKey) defaultKey;
                    InputWordKey wordKey = InputWordKey.word(null).setBgColorAttrId(R.attr.key_ctrl_noop_bg_color);

                    wordKey.setCharKey(charKey);
                    charKey.setFgColorAttrId(wordKey.getBgColorAttrId());

                    gridKeys[i][j] = wordKey;
                } else {
                    gridKeys[i][j] = noopCtrlKey();
                }
            }
        }

        gridKeys[1][6] = ctrlKey(CtrlKey.Type.DropInput);
        if (!inputWords.isEmpty()) {
            gridKeys[0][0] = noopCtrlKey((startIndex / pageSize + 1) //
                                         + "/" //
                                         + ((int) Math.ceil(inputWords.size() / (pageSize * 1.0))));
        }

        if (input.isPinyinTongue()) {
            String s = input.getChars().get(0);
            gridKeys[4][0] = ctrlKey(CtrlKey.Type.ToggleInputSpell_zcs_h, s + "," + s + "h");
        } else if (input.isPinyinNL()) {
            gridKeys[4][0] = ctrlKey(CtrlKey.Type.ToggleInputSpell_nl, "n,l  ");
        }
        if (input.isPinyinRhyme()) {
            String s = String.join("", input.getChars());
            String tail = s.endsWith("g") ? s.substring(s.length() - 3, s.length() - 1) : s.substring(s.length() - 2);
            gridKeys[5][0] = ctrlKey(CtrlKey.Type.ToggleInputSpell_ng, tail + "," + tail + "g");
        }

        for (int i = 0; i < pageSize; i++) {
            int[] gridKeyCoord = grid_key_coords[i];
            int x = gridKeyCoord[0];
            int y = gridKeyCoord[1];
            Key<?> defaultKey = gridKeys[x][y];

            int wordIndex = i + startIndex;
            if (wordIndex < inputWords.size()) {
                InputWord word = inputWords.get(wordIndex);
                int level = i == 0 ? 0 : i <= 6 ? 1 : i <= 18 ? 2 : 3;

                int bgAttrId = input_word_key_level_bg_colors[level];
                InputWordKey wordKey = InputWordKey.word(word)
                                                   .setFgColorAttrId(R.attr.input_word_key_fg_color)
                                                   .setBgColorAttrId(bgAttrId);
                if (defaultKey instanceof InputWordKey) {
                    CharKey charKey = ((InputWordKey) defaultKey).getCharKey();
                    wordKey.setCharKey(charKey);
                    charKey.setFgColorAttrId(wordKey.getBgColorAttrId());
                }

                gridKeys[x][y] = wordKey;
            }
        }

        return gridKeys;
    }

    public static CtrlKey ctrlKey(CtrlKey.Type type) {
        return ctrlKey(type, null);
    }

    public static CtrlKey ctrlKey(CtrlKey.Type type, String text) {
        int iconResId = 0;
        int bgAttrId = 0;
        for (Map.Entry<CtrlKey.Type, Integer[]> entry : ctrl_key_styles.entrySet()) {
            if (entry.getKey() == type) {
                iconResId = entry.getValue()[0];
                bgAttrId = entry.getValue()[1];
                break;
            }
        }

        CtrlKey key = text != null ? CtrlKey.create(type, text) : CtrlKey.create(type, iconResId);
        return key.setBgColorAttrId(bgAttrId);
    }

    public static CtrlKey noopCtrlKey() {
        return noopCtrlKey(null);
    }

    public static CtrlKey noopCtrlKey(String text) {
        return CtrlKey.noop(text).setBgColorAttrId(R.attr.key_ctrl_noop_bg_color);
    }

    public static CharKey alphabetKey(String text) {
        return charKey(CharKey.Type.Alphabet, text);
    }

    public static CharKey punctuationKey(String text) {
        return charKey(CharKey.Type.Punctuation, text);
    }

    private static CharKey charKey(CharKey.Type type, String text) {
        int fgAttrId = 0;
        int bgAttrId = 0;
        for (Map.Entry<List<String>, Integer[]> entry : char_key_color_palette.entrySet()) {
            if (entry.getKey().contains(text)) {
                fgAttrId = entry.getValue()[0];
                bgAttrId = entry.getValue()[1];
                break;
            }
        }

        return CharKey.create(type, text).setFgColorAttrId(fgAttrId).setBgColorAttrId(bgAttrId);
    }

    public static class Configure {
        private final HandMode handMode;
        private final boolean hasInputs;

        public Configure(HandMode handMode, boolean hasInputs) {
            this.handMode = handMode;
            this.hasInputs = hasInputs;
        }
    }
}
