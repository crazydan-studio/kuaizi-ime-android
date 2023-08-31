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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.LatinKeyboard;

/**
 * {@link LatinKeyboard 拉丁文键盘}按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-30
 */
public class LatinKeyTable extends KeyTable {

    public static LatinKeyTable create(Config config) {
        return new LatinKeyTable(config);
    }

    protected LatinKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link LatinKeyboard 拉丁文键盘}按键 */
    public Key<?>[][] createKeys() {
        Key<?>[][] keys = new Key[][] {
                new Key[] {
                        //ctrlKey(CtrlKey.Type.SwitchIME),
                        ctrlKey(CtrlKey.Type.SwitchHandMode),
                        numberKey("8"),
                        numberKey("7"),
                        numberKey("6"),
                        numberKey("5"),
                        numberKey("4"),
                        numberKey("3"),
                        numberKey("2"),
                        } //
                , new Key[] {
                numberKey("9"),
                alphabetKey("f"),
                alphabetKey("e"),
                alphabetKey("d"),
                alphabetKey("c"),
                alphabetKey("b"),
                alphabetKey("a"),
                numberKey("1"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToPinyinKeyboard),
                alphabetKey("l"),
                alphabetKey("k"),
                alphabetKey("j"),
                alphabetKey("i"),
                alphabetKey("h"),
                alphabetKey("g"),
                numberKey("0"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToEmojiKeyboard),
                alphabetKey("q"),
                alphabetKey("p"),
                alphabetKey("o"),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("n"),
                alphabetKey("m"),
                this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard),
                alphabetKey("w"),
                alphabetKey("v"),
                alphabetKey("u"),
                alphabetKey("t"),
                alphabetKey("s"),
                alphabetKey("r"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                symbolKey("#"),
                symbolKey("@"),
                symbolKey(","),
                symbolKey("."),
                alphabetKey("z"),
                alphabetKey("y"),
                alphabetKey("x"),
                ctrlKey(CtrlKey.Type.Backspace),
                },
                };

        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                Key<?> key = keys[i][j];

                if (key.isLatin() && !key.isNumber()) {
                    KeyColor color = latin_key_char_alphabet_level_colors[i - 1];
                    key.setColor(color);
                }
            }
        }

        return relayoutForHandMode(this.config, keys);
    }
}
