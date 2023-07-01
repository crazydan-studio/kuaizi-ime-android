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

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;

/**
 * 汉语拼音{@link Keyboard 键盘}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class PinyinKeyboard extends BaseKeyboard {
    /** 纵向 7 x 6 的按键矩阵 */
    private static final Key[][] portrait_right_hand_keys = new Key[][] {
            new Key[] {
                    CtrlKey.switchIME(R.drawable.ic_keyboard),
                    CharKey.punctuation("："),
                    CharKey.alphabet("i"),
                    CharKey.alphabet("a"),
                    CharKey.alphabet("e"),
                    CharKey.alphabet("o"),
                    CharKey.alphabet("u"),
                    } //
            , new Key[] {
            CharKey.punctuation("！"),
            CharKey.punctuation("？"),
            CharKey.alphabet("j"),
            CharKey.alphabet("q"),
            CharKey.alphabet("s"),
            CharKey.alphabet("z"),
            CtrlKey.backspace(R.drawable.ic_backspace_left),
            } //
            , new Key[] {
            CtrlKey.switchHandMode(R.drawable.ic_switch_to_left_hand),
            CharKey.alphabet("ü"),
            CharKey.alphabet("h"),
            CharKey.alphabet("g"),
            CharKey.alphabet("x"),
            CharKey.alphabet("c"),
            CtrlKey.space(R.drawable.ic_space),
            } //
            , new Key[] {
            CharKey.punctuation("、"),
            CharKey.alphabet("n"),
            CharKey.alphabet("l"),
            CtrlKey.locator(R.drawable.ic_left_hand_move),
            CharKey.alphabet("w"),
            CharKey.alphabet("k"),
            CtrlKey.enter(R.drawable.ic_enter_left),
            } //
            , new Key[] {
            null,
            CharKey.punctuation("，"),
            CharKey.alphabet("r"),
            CharKey.alphabet("f"),
            CharKey.alphabet("m"),
            CharKey.alphabet("p"),
            CtrlKey.switchToAlphanumericKeyboard(R.drawable.ic_alphabet_number),
            } //
            , new Key[] {
            null,
            CharKey.punctuation("。"),
            CharKey.alphabet("d"),
            CharKey.alphabet("b"),
            CharKey.alphabet("t"),
            CharKey.alphabet("y"),
            CtrlKey.switchToPunctuationKeyboard(R.drawable.ic_punctuation),
            },
            };
}
