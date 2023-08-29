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

package org.crazydan.studio.app.ime.kuaizi.internal.data;

import org.crazydan.studio.app.ime.kuaizi.internal.Symbol;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public enum SymbolGroup {
    /** 汉语标点符号: https://zh.wikipedia.org/wiki/%E6%A0%87%E7%82%B9%E7%AC%A6%E5%8F%B7 */
    han("中文", new Symbol[] {
            single("，"), single("。"), single("？"), single("！"), single("："), single("；"),
            //
            single("·"), single("、"), single("×"), single("÷"),
            //
            single("‘"), single("’"), pair("‘", "’"), single("“"), single("”"), pair("“", "”"),
            //
            single("「"), single("」"), pair("「", "」"), single("『"), single("』"), pair("『", "』"),
            //
            single("（"), single("）"), pair("（", "）"), single("〔"), single("〕"), pair("〔", "〕"),
            //
            single("〈"), single("〉"), pair("〈", "〉"), single("《"), single("》"), pair("《", "》"),
            //
            single("［"), single("］"), pair("［", "］"), single("【"), single("】"), pair("【", "】"),
            //
            single("－"), single("——"), single("～"), single("＿＿"), single("﹏﹏"), single("……"),
            //
            single("﹁"), single("﹂"), single("﹃"), single("﹄"),
            }),

    /** 拉丁文标点符号: https://zh.wikipedia.org/wiki/%E6%A0%87%E7%82%B9%E7%AC%A6%E5%8F%B7 */
    latin("英文", new Symbol[] {
            single(","), single("."), single("?"), single("!"), single(":"), single(";"),
            //
            single("@"), single("#"), single("$"), single("%"), single("^"),
            //
            single("&"), single("*"), single("-"), single("+"), single("="),
            //
            single("/"), single("\\"), single("|"), single("~"), single("`"),
            //
            single("'"), pair("'", "'"), single("\""), pair("\"", "\""),
            //
            single("("), single(")"), pair("(", ")"), single("["), single("]"), pair("[", "]"),
            //
            single("{"), single("}"), pair("{", "}"), single("<"), single(">"), pair("<", ">"),
            //
            single("–"), single("—"),
            });

    public final String name;
    public final Symbol[] symbols;

    SymbolGroup(String name, Symbol[] symbols) {
        this.name = name;
        this.symbols = symbols;
    }

    private static Symbol single(String text) {
        return Symbol.single(text);
    }

    private static Symbol pair(String left, String right) {
        return Symbol.pair(left, right);
    }
}
