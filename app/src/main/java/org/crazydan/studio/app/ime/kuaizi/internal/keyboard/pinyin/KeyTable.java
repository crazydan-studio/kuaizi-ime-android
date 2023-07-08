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

import java.util.List;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
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
    public static final CtrlKey ctrl_key_choose_word = CtrlKey.chooseWord(R.drawable.ic_choose_word)
                                                              .bgColorAttrId(R.attr.key_ctrl_space_bg_color);

    /** 右手模式的纵向屏幕 7 x 6 的按键表 */
    private static final Key[][] portrait_right_hand = new Key[][] {
            new Key[] {
                    CtrlKey.switchIME(R.drawable.ic_keyboard).bgColorAttrId(R.attr.key_ctrl_switch_ime_bg_color),
                    CharKey.punctuation("：")
                           .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
                    CharKey.alphabet("i")
                           .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
                    CharKey.alphabet("a")
                           .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
                    CharKey.alphabet("e")
                           .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
                    CharKey.alphabet("o")
                           .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
                    CharKey.alphabet("u")
                           .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
                    } //
            , new Key[] {
            CharKey.punctuation("！")
                   .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
            CharKey.punctuation("？")
                   .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
            CharKey.alphabet("j")
                   .fgColorAttrId(R.attr.key_char_initial_jqx_fg_color).bgColorAttrId(R.attr.key_char_initial_jqx_bg_color),
            CharKey.alphabet("q")
                   .fgColorAttrId(R.attr.key_char_initial_jqx_fg_color).bgColorAttrId(R.attr.key_char_initial_jqx_bg_color),
            CharKey.alphabet("s")
                   .fgColorAttrId(R.attr.key_char_initial_scz_fg_color).bgColorAttrId(R.attr.key_char_initial_scz_bg_color),
            CharKey.alphabet("z")
                   .fgColorAttrId(R.attr.key_char_initial_scz_fg_color).bgColorAttrId(R.attr.key_char_initial_scz_bg_color),
            CtrlKey.backspace(R.drawable.ic_backspace_left).bgColorAttrId(R.attr.key_ctrl_backspace_bg_color),
            } //
            , new Key[] {
            CtrlKey.switchHandMode(R.drawable.ic_switch_to_left_hand).bgColorAttrId(R.attr.key_ctrl_switch_hand_mode_bg_color),
            CharKey.alphabet("ü")
                   .fgColorAttrId(R.attr.key_char_final_fg_color).bgColorAttrId(R.attr.key_char_final_bg_color),
            CharKey.alphabet("h")
                   .fgColorAttrId(R.attr.key_char_initial_hgwk_fg_color).bgColorAttrId(R.attr.key_char_initial_hgwk_bg_color),
            CharKey.alphabet("g")
                   .fgColorAttrId(R.attr.key_char_initial_hgwk_fg_color).bgColorAttrId(R.attr.key_char_initial_hgwk_bg_color),
            CharKey.alphabet("x")
                   .fgColorAttrId(R.attr.key_char_initial_jqx_fg_color).bgColorAttrId(R.attr.key_char_initial_jqx_bg_color),
            CharKey.alphabet("c")
                   .fgColorAttrId(R.attr.key_char_initial_scz_fg_color).bgColorAttrId(R.attr.key_char_initial_scz_bg_color),
            CtrlKey.space(R.drawable.ic_space).bgColorAttrId(R.attr.key_ctrl_space_bg_color),
            } //
            , new Key[] {
            CharKey.punctuation("、")
                   .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
            CharKey.alphabet("n")
                   .fgColorAttrId(R.attr.key_char_initial_nl_fg_color).bgColorAttrId(R.attr.key_char_initial_nl_bg_color),
            CharKey.alphabet("l")
                   .fgColorAttrId(R.attr.key_char_initial_nl_fg_color).bgColorAttrId(R.attr.key_char_initial_nl_bg_color),
            CtrlKey.locator(R.drawable.ic_left_hand_move).bgColorAttrId(R.attr.key_ctrl_locator_bg_color),
            CharKey.alphabet("w")
                   .fgColorAttrId(R.attr.key_char_initial_hgwk_fg_color).bgColorAttrId(R.attr.key_char_initial_hgwk_bg_color),
            CharKey.alphabet("k")
                   .fgColorAttrId(R.attr.key_char_initial_hgwk_fg_color).bgColorAttrId(R.attr.key_char_initial_hgwk_bg_color),
            CtrlKey.enter(R.drawable.ic_enter_left).bgColorAttrId(R.attr.key_ctrl_enter_bg_color),
            } //
            , new Key[] {
            CtrlKey.noop().bgColorAttrId(R.attr.key_ctrl_noop_bg_color),
            CharKey.punctuation("，")
                   .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
            CharKey.alphabet("r")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("f")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("m")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("p")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CtrlKey.switchToAlphanumericKeyboard(R.drawable.ic_alphabet_number).bgColorAttrId(R.attr.key_ctrl_switch_to_alphanumeric_keyboard_bg_color),
            } //
            , new Key[] {
            CtrlKey.noop().bgColorAttrId(R.attr.key_ctrl_noop_bg_color),
            CharKey.punctuation("。")
                   .fgColorAttrId(R.attr.key_char_punctuation_fg_color).bgColorAttrId(R.attr.key_char_punctuation_bg_color),
            CharKey.alphabet("d")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("b")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("t")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CharKey.alphabet("y")
                   .fgColorAttrId(R.attr.key_char_initial_fg_color).bgColorAttrId(R.attr.key_char_initial_bg_color),
            CtrlKey.switchToPunctuationKeyboard(R.drawable.ic_punctuation).bgColorAttrId(R.attr.key_ctrl_switch_to_punctuation_keyboard_bg_color),
            },
            };

    public static Key[][] keys(Keyboard.KeyFactory.Option option, HandMode handMode) {
        // TODO 根据系统支持情况和配置等信息，调整部分按键的显示或隐藏
        return portrait_right_hand;
    }

    public static Key[][] inputCandidateKeys(
            Keyboard.KeyFactory.Option option, HandMode handMode, List<InputWord> inputCandidates
    ) {
        // 可放置候选字按键的位置坐标
        int[][] coords = new int[][] {
                new int[] { 0, 6 },
                new int[] { 0, 5 },
                new int[] { 0, 4 },
                new int[] { 0, 3 },
                new int[] { 0, 2 },
                new int[] { 0, 1 },
                new int[] { 1, 0 },
                new int[] { 2, 0 },
                new int[] { 3, 0 },
                new int[] { 4, 1 },
                new int[] { 5, 1 },
                };
        Key[][] keys = new Key[6][7];

        int count = Math.min(coords.length, inputCandidates.size());
        for (int i = 0; i < count; i++) {
            InputWord word = inputCandidates.get(i);
            int[] coord = coords[i];

            keys[coord[0]][coord[1]] = InputWordKey.word(word)
                                                   .fgColorAttrId(R.attr.input_word_key_fg_color)
                                                   .bgColorAttrId(R.attr.input_word_key_bg_color);
        }

        return keys;
    }

    public static Key[][] showKeys(Key[][] keys) {
        traverseKeys(keys, Key::show);
        return keys;
    }

    public static void traverseKeys(Key[][] keys, Consumer<Key> consumer) {
        for (Key[] key : keys) {
            for (Key k : key) {
                consumer.accept(k);
            }
        }
    }
}
