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
        // 按键布局参考: https://nbviewer.org/url/norvig.com/ipython/Gesture%20Typing.ipynb#Question-7:-Is-there-a-Keyboard-that-Maximizes-User-Satisfaction?
        // data:image/png;base64,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QvQBwBCiDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9Ds5c+aMIpGIIpGIBg0apPz8fEUiEZWWlqq9vd31vITjx4+roKAg8daQsVhMBQUFOnbsmONlyTIzMxOP3/z58/Wf//zH9aSUbr31Vv3+979Puu3xxx9XZWWlo0VdeZ6nsWPH6uWXX07ctn79ek2YMMHhqq5ycnIS/11bW6tPfepTOn78uMNFqW3atCnxvd7xkZmZqW3btrme1vu8q4iff5yqqipv5cqVvt1fZ37sXLFihTdnzhzP8zxvzpw53k9+8pMe3+elerozJyfH8zzPa29v9yZNmuT97ne/82NWFz3d+dRTT3kVFRVJt40cOdLbuXNnj+73Uj3defDgQa+4uNg7d+6cF4/HvaKiIu/IkSM+rfuvnuzseM63b9/uFRYW9so+z/P3e93zPO9Xv/qV98UvftHX+/Q8/3f6gffI7caDDz6onJwcLViwwJf768yPnefPn9ctt9yiiooKPf3003rjjTeUmZnp08IP9HRnOBxWPB6XJK1atUonT57UypUr/ZqX0NOdzc3NKi4u1smTJ5WVlaXGxkaVl5fr7bff9nGlP8/7D37wAw0YMECtra269tprtWTJEp/W/VdPdobDYW3ZskUVFRXaunWrhgwZ4vO6D/j5vX748GHddtttikajys/P9+U+O6Tje+RmuR6A/5+srCytWLFCEyZM0CuvvOJ78P30r3/9S1u3btWMGTNcT0kpLy9PI0aMUG1trSZNmqSamhpNmTLF9ayUli1bpkgkouzsbO3bt8/1nC7OnTunu+66S3/+8597Lfh+am9v17333qtVq1b5Hvx0xZl+gG3dulU33HCDDhw44HpKSm1tbYpEIsrPz1dmZqamT5/uelK3pk2bppqaGknS888/r2nTpjlelFr//v01depUTZ8+XX369HE9p4u+ffuqrKxMa9eudT3lf/LAAw9o2LBhmjx5suspVwzRD6g33nhD27dvVzQa1WOPPaZ33nnH9aQu+vXrp7q6Op08eVJNTU3avHmz60ndmjRpknbs2KG6ujq99957ikQirid1KyMjQ6FQyPWMlDIyMvTCCy9o7969Wr58ues5H+pPf/qTNm3apNWrV7ueckUR/QDyPE/f/va39cQTT2jw4MFatGiRFi5c6HpWt6655hqtWbNGixcvTrvzzQ45OTkaN26cKioqdO+997qeE2jZ2dnasmWLnnvuOT3zzDOu56QUi8VUUVGhX//61xowYIDrOVcU0f8Q6Xo1tWbNGn384x/XbbfdJkmqrKxUQ0ODdu7c6XhZss6PXyQSUWFhoV544QWHiz7ctGnTdODAgbQ92uksXb82O3bl5ubq5Zdf1iOPPJKWP+H98pe/1OnTp/Wtb30r6dc2169f73par+O3dxxgp7/Y6a8g7AzCRik9d3KlDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDrrr3yE3X1xm/FDv9xU5/BWFnEDbm5ua6ntDFVRX9dHvdagBINxzvAIAhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDiD4AGEL0AcAQog8AhhB9ADCE6AOAIUQfAAwh+gBgCNEHAEOIPgAYQvQBwBCiDwCGEH0AMIToA4AhRB8ADCH6AGAI0QcAQ4g+ABhC9AHAEKIPAIYQfQAwhOgDgCFEHwAMIfoAYAjRBwBDiD4AGEL0AcAQog8AhhB9ADCE6AOAIUQfAAwh+gBgCNEHAEOIPgAYQvQBwBCiDwCG/B/CI60K6hV8rQAAAABJRU5ErkJggg==
        Key<?>[][] keys = new Key[][] {
                new Key[] {
                        ctrlKey(CtrlKey.Type.SwitchHandMode),
                        numberKey("5"),
                        numberKey("0"),
                        alphabetKey("q").withReplacements("Q"),
                        alphabetKey("d").withReplacements("D"),
                        alphabetKey("e").withReplacements("E"),
                        alphabetKey("b").withReplacements("B"),
                        alphabetKey("a").withReplacements("A"),
                        } //
                , new Key[] {
                numberKey("6"),
                numberKey("1"),
                alphabetKey("i").withReplacements("I"),
                alphabetKey("j").withReplacements("J"),
                alphabetKey("m").withReplacements("M"),
                alphabetKey("u").withReplacements("U"),
                alphabetKey("g").withReplacements("G"),
                alphabetKey("f").withReplacements("F"),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToPinyinKeyboard),
                numberKey("7"),
                numberKey("2"),
                alphabetKey("o").withReplacements("O"),
                alphabetKey("n").withReplacements("N"),
                alphabetKey("h").withReplacements("H"),
                alphabetKey("l").withReplacements("L"),
                ctrlKey(CtrlKey.Type.Backspace),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToEmojiKeyboard),
                numberKey("8"),
                numberKey("3"),
                alphabetKey("c").withReplacements("C"),
                ctrlKey(CtrlKey.Type.LocateInputCursor),
                alphabetKey("w").withReplacements("W"),
                alphabetKey("p").withReplacements("P"),
                this.config.hasInputs() ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey(),
                } //
                , new Key[] {
                ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard),
                numberKey("9"),
                numberKey("4"),
                alphabetKey("r").withReplacements("R"),
                alphabetKey("x").withReplacements("X"),
                alphabetKey("t").withReplacements("T"),
                alphabetKey("s").withReplacements("S"),
                ctrlKey(CtrlKey.Type.Space),
                } //
                , new Key[] {
                symbolKey("#"),
                symbolKey("@"),
                symbolKey(","),
                symbolKey("."),
                alphabetKey("z").withReplacements("Z"),
                alphabetKey("k").withReplacements("K"),
                alphabetKey("y").withReplacements("Y"),
                alphabetKey("v").withReplacements("V"),
                },
                };

        for (Key<?>[] value : keys) {
            for (Key<?> key : value) {
                if (key.isLatin() && !key.isNumber()) {
                    KeyColor color = key_char_color;

                    key.setColor(color);
                }
            }
        }
        return keys;
    }
}
