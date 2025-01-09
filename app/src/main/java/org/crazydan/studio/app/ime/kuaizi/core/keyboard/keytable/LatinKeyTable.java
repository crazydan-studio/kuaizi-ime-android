/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable;

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.LatinKeyboard;

/**
 * {@link Keyboard.Type#Latin} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-30
 */
public class LatinKeyTable extends KeyTable {

    protected LatinKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static LatinKeyTable create(KeyTableConfig config) {
        return new LatinKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建 {@link LatinKeyboard} 按键 */
    public Key[][] createKeys() {
        if (this.config.xInputPadEnabled) {
            return createKeysForXPad();
        }

        // 按键布局参考: https://nbviewer.org/url/norvig.com/ipython/Gesture%20Typing.ipynb#Question-7:-Is-there-a-Keyboard-that-Maximizes-User-Satisfaction?
        // data:image/png;base64,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QvQBwBCiDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9Ds5c+aMIpGIIpGIBg0apPz8fEUiEZWWlqq9vd31vITjx4+roKAg8daQsVhMBQUFOnbsmONlyTIzMxOP3/z58/Wf//zH9aSUbr31Vv3+979Puu3xxx9XZWWlo0VdeZ6nsWPH6uWXX07ctn79ek2YMMHhqq5ycnIS/11bW6tPfepTOn78uMNFqW3atCnxvd7xkZmZqW3btrme1vu8q4iff5yqqipv5cqVvt1fZ37sXLFihTdnzhzP8zxvzpw53k9+8pMe3+elerozJyfH8zzPa29v9yZNmuT97ne/82NWFz3d+dRTT3kVFRVJt40cOdLbuXNnj+73Uj3defDgQa+4uNg7d+6cF4/HvaKiIu/IkSM+rfuvnuzseM63b9/uFRYW9so+z/P3e93zPO9Xv/qV98UvftHX+/Q8/3f6gffI7caDDz6onJwcLViwwJf768yPnefPn9ctt9yiiooKPf3003rjjTeUmZnp08IP9HRnOBxWPB6XJK1atUonT57UypUr/ZqX0NOdzc3NKi4u1smTJ5WVlaXGxkaVl5fr7bff9nGlP8/7D37wAw0YMECtra269tprtWTJEp/W/VdPdobDYW3ZskUVFRXaunWrhgwZ4vO6D/j5vX748GHddtttikajys/P9+U+O6Tje+RmuR6A/5+srCytWLFCEyZM0CuvvOJ78P30r3/9S1u3btWMGTNcT0kpLy9PI0aMUG1trSZNmqSamhpNmTLF9ayUli1bpkgkouzsbO3bt8/1nC7OnTunu+66S3/+8597Lfh+am9v17333qtVq1b5Hvx0xZl+gG3dulU33HCDDhw44HpKSm1tbYpEIsrPz1dmZqamT5/uelK3pk2bppqaGknS888/r2nTpjlelFr//v01depUTZ8+XX369HE9p4u+ffuqrKxMa9eudT3lf/LAAw9o2LBhmjx5suspVwzRD6g33nhD27dvVzQa1WOPPaZ33nnH9aQu+vXrp7q6Op08eVJNTU3avHmz60ndmjRpknbs2KG6ujq99957ikQirid1KyMjQ6FQyPWMlDIyMvTCCy9o7969Wr58ues5H+pPf/qTNm3apNWrV7ueckUR/QDyPE/f/va39cQTT2jw4MFatGiRFi5c6HpWt6655hqtWbNGixcvTrvzzQ45OTkaN26cKioqdO+997qeE2jZ2dnasmWLnnvuOT3zzDOu56QUi8VUUVGhX//61xowYIDrOVcU0f8Q6Xo1tWbNGn384x/XbbfdJkmqrKxUQ0ODdu7c6XhZss6PXyQSUWFhoV544QWHiz7ctGnTdODAgbQ92uksXb82O3bl5ubq5Zdf1iOPPJKWP+H98pe/1OnTp/Wtb30r6dc2169f73par+O3dxxgp7/Y6a8g7AzCRik9d3KlDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDrrr3yE3X1xm/FDv9xU5/BWFnEDbm5ua6ntDFVRX9dHvdagBINxzvAIAhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDiD4AGEL0AcAQog8AhhB9ADCE6AOAIUQfAAwh+gBgCNEHAEOIPgAYQvQBwBCiDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDiD4AGEL0AcAQog8AhhB9ADCE6AOAIUQfAAwh+gBgCNEHAEOIPgAYQvQBwBCiDwCG/B/CI60K6hV8rQAAAABJRU5ErkJggg==
        return new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.Switch_HandMode),
                        numberKey("5"),
                        numberKey("0"),
                        alphabetKey("q", "Q"),
                        alphabetKey("d", "D"),
                        alphabetKey("e", "E"),
                        alphabetKey("b", "B"),
                        alphabetKey("a", "A"),
                        } //
                , new Key[] {
                numberKey("6"),
                numberKey("1"),
                alphabetKey("j", "J"),
                alphabetKey("m", "M"),
                alphabetKey("u", "U"),
                alphabetKey("g", "G"),
                alphabetKey("f", "F"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Pinyin),
                numberKey("7"),
                numberKey("2"),
                alphabetKey("o", "O"),
                alphabetKey("n", "N"),
                alphabetKey("h", "H"),
                alphabetKey("l", "L"),
                alphabetKey("i", "I"),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Emoji),
                numberKey("8"),
                numberKey("3"),
                alphabetKey("c", "C"),
                ctrlKey(CtrlKey.Type.Editor_Cursor_Locator),
                alphabetKey("w", "W"),
                alphabetKey("p", "P"),
                this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                switcherCtrlKey(Keyboard.Type.Symbol),
                numberKey("9"),
                numberKey("4"),
                alphabetKey("v", "V"),
                alphabetKey("r", "R"),
                alphabetKey("x", "X"),
                alphabetKey("t", "T"),
                alphabetKey("s", "S"),
                } //
                , new Key[] {
                symbolKey("#"),
                symbolKey("@"),
                symbolKey(","),
                symbolKey("."),
                alphabetKey("z", "Z"),
                alphabetKey("k", "K"),
                alphabetKey("y", "Y"),
                ctrlKey(CtrlKey.Type.Space),
                },
                };
    }

    @Override
    protected XPadKey createXPadKey() {
        if (this.config.latinUsePinyinKeysInXInputPadEnabled) {
            return createPinyinLatinXPadKey();
        }

        return xPadKey(Keyboard.Type.Latin, new Key[][][] {
                // 英语单词中首字母的频率: https://zh.wikipedia.org/zh-cn/%E5%AD%97%E6%AF%8D%E9%A2%91%E7%8E%87#.E8.8B.B1.E8.AF.AD.E5.8D.95.E8.AF.8D.E4.B8.AD.E9.A6.96.E5.AD.97.E6.AF.8D.E7.9A.84.E9.A2.91.E7.8E.87
                // - 排序: echo "" | sort -n -r -k 2
                new Key[][] {
                        new Key[] {
                                symbolKey("@", "&"), //
                                symbolKey("-", "_"), //
                                symbolKey("#", "+"), //
                        }, //
                        new Key[] {
                                symbolKey("."), ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                }, //
                },
                new Key[][] {
                        new Key[] {
                                symbolKey(",", ";"), //
                                symbolKey("?", ":"), //
                                symbolKey("!", "*"),
                                }, //
                        new Key[] {
                                alphabetKey("t", "T"), alphabetKey("f", "F"), alphabetKey("y", "Y"),
                                }, //
                },
                new Key[][] {
                        new Key[] {
                                alphabetKey("m", "M"), alphabetKey("r", "R"), alphabetKey("z", "Z"),
                                }, //
                        new Key[] {
                                alphabetKey("a", "A"), alphabetKey("c", "C"), alphabetKey("u", "U"),
                                }, //
                },
                new Key[][] {
                        new Key[] {
                                alphabetKey("b", "B"), alphabetKey("g", "G"), alphabetKey("x", "X"),
                                },//
                        new Key[] {
                                alphabetKey("s", "S"), alphabetKey("l", "L"), alphabetKey("v", "V"),
                                }, //
                },
                new Key[][] {
                        new Key[] {
                                alphabetKey("o", "O"), alphabetKey("e", "E"), alphabetKey("q", "Q"),
                                }, //
                        new Key[] {
                                alphabetKey("h", "H"), alphabetKey("d", "D"), alphabetKey("j", "J"),
                                }, //
                },
                new Key[][] {
                        new Key[] {
                                alphabetKey("i", "I"), alphabetKey("n", "N"), alphabetKey("k", "K"),
                                }, //
                        new Key[] {
                                alphabetKey("w", "W"), //
                                alphabetKey("p", "P"), //
                                symbolKey("/", "\\"),
                                }, //
                },
                });
    }

    @Override
    public CharKey alphabetKey(String value, Consumer<CharKey.Builder> c) {
        return super.alphabetKey(value, (b) -> {
            b.color(key_char_color);
            c.accept(b);
        });
    }

    private XPadKey createPinyinLatinXPadKey() {
        return xPadKey(Keyboard.Type.Latin, new Key[][][] {
                new Key[][] {
                        new Key[] {
                                alphabetKey("g", "G"), alphabetKey("f", "F"), alphabetKey("p", "P"),
                                }, //
                        new Key[] {
                                symbolKey("."), ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                symbolKey(",", ";"), //
                                symbolKey("?", ":"), //
                                symbolKey("!", "*"),
                                }, //
                        new Key[] {
                                alphabetKey("d", "D"), alphabetKey("b", "B"), alphabetKey("t", "T"),
                                }, //
                }, //
                new Key[][] {
                        new Key[] {
                                alphabetKey("y", "Y"), alphabetKey("h", "H"), alphabetKey("r", "R"),
                                }, //
                        new Key[] {
                                alphabetKey("l", "L"), alphabetKey("m", "M"), alphabetKey("n", "N"),
                                }, //
                }, //
                new Key[][] {
                        new Key[] {
                                alphabetKey("z", "Z"), alphabetKey("s", "S"), alphabetKey("c", "C"),
                                }, //
                        new Key[] {
                                symbolKey("@", "&"), symbolKey("-", "_"), symbolKey("/", "\\"),
                                }, //
                }, //
                new Key[][] {
                        new Key[] {
                                alphabetKey("e", "E"), alphabetKey("a", "A"), alphabetKey("o", "O"),
                                }, //
                        new Key[] {
                                alphabetKey("i", "I"), alphabetKey("u", "U"), alphabetKey("v", "V"),
                                }, //
                }, //
                new Key[][] {
                        new Key[] {
                                alphabetKey("j", "J"), alphabetKey("w", "W"), symbolKey("#", "+"),
                                }, //
                        new Key[] {
                                alphabetKey("x", "X"), alphabetKey("q", "Q"), alphabetKey("k", "K"),
                                }, //
                }, //
        });
    }
}
