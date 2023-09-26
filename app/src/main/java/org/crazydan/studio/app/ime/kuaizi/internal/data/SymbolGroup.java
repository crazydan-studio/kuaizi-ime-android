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
            single("·"), single("、"), single("‘"), single("’"), single("－"), single("——"),
            //
            pair("‘", "’"), single("“"), single("”"), pair("“", "”"), single("｜"), single("‖"),
            //
            single("（"), single("）"), pair("（", "）"), single("「"), single("」"), pair("「", "」"),
            //
            single("《"), single("》"), pair("《", "》"), single("【"), single("】"), pair("【", "】"),
            //
            single("〈"), single("〉"), pair("〈", "〉"), single("［"), single("］"), pair("［", "］"),
            //
            single("＿＿"), single("﹏﹏"), single("～"),
            //
            single("……"), single("﹍"), single("﹎"), single("¨"), single("˜"), single("ˉ"),
            //
            single("〔"), single("〕"), pair("〔", "〕"), single("﹛"), single("﹜"), pair("﹛", "﹜"),
            //
            single("『"), single("』"), pair("『", "』"), single("〖"), single("〗"), pair("〖", "〗"),
            //
            single("‹"), single("›"), pair("‹", "›"),
            //
            single("︴"), single("〃"), single("¡"), single("¿"), single("→"),
            //
            single("〝"), single("〞"), pair("〝", "〞"),
            //
            single("﹁"), single("﹂"), single("﹃"), single("﹄"),
            //
            single("―"), single("_"), single("￣"), single("＿"),
            //
            single("﹋"), single("﹏"), single("︵"), single("︶"),
            //
            single("︷"), single("︸"), single("︿"), single("﹀"), single("︹"), single("︺"),
            //
            single("︽"), single("︾"), single("︻"), single("︼"), single("ˆ"), single("ˇ"),
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
            }),

    /**
     * 数学符号：https://tool.lmeee.com/fuhao/shuxue
     * <pre>
     * var data = [];
     * document.querySelectorAll('.copyBtn').forEach(function(el) {
     *   data.push(el.getAttribute('data-clipboard-text'));
     * });
     * console.log(data.join('\n'));
     * </pre>
     */
    math("数学", new Symbol[] {
            single("+"), single("-"), single("×"), single("÷"), single("="), single("≠"),
            //
            single("≈"), single("≥"), single("≮"), single("^"), single("∽"), single("≌"),
            //
            single("≯"), single("＜"), single("＞"), single("≥"), single("≤"), single("‖"), single("∠"),
            //
            single("≡"), single("＋"), single("－"), single("±"), single("/"), single("≤"), single("㏑"),
            //
            single("∨"), single("⊙"), single("㏒"), single("‖"), single("∑"), single("∈"), single("∠"),
            //
            single("⌒"), single("℅"), single("%"), single("‰"), single("∪"),
            //
            single("∶"), single("∴"), single("∵"), single("∷"), single("∝"), single("∞"),
            //
            single("∏"), single("∫"), single("∮"), single("∟"), single("⊿"), single("⊥"), single("∧"), single("∩"),
            //
            single("√"), single("℃"), single("℉"), single("㎎"), single("㎏"), single("㎜"),
            //
            single("㎝"), single("㎞"), single("㎡"), single("㏄"), single("㏎"), single("㏑"),
            //
            single("㏒"), single("㏕"), single("‱"), single("∫"), single("∬"), single("∭"),
            //
            single("∮"), single("∯"), single("∰"), single("∱"), single("∲"), single("∳"),
            //
            single("✓"), single("√"), single("☒"), single("✗"), single("✘"),
            //
            single("ㄨ"), single("✕"), single("╳"), single("メ"), single("ン"), single("х"),
            }),
    /**
     * 箭头符号：https://tool.lmeee.com/fuhao/jiantou
     */
    arrow("箭头", new Symbol[] {
            single("➟"), single("➡"), single("➢"), single("➣"), single("➤"), single("➥"),
            //
            single("➦"), single("➧"), single("➨"), single("➚"), single("➘"), single("➙"),
            //
            single("➛"), single("➜"), single("➝"), single("➞"), single("➸"), single("➲"),
            //
            single("➳"), single("⏎"), single("➴"), single("➵"), single("➶"), single("➷"), single("➸"),
            //
            single("➹"), single("➺"), single("➻"), single("➼"), single("➽"), single("➫"), single("➬"),
            //
            single("➩"), single("➪"), single("➭"), single("➮"), single("➯"), single("➱"),
            //
            single("↖"), single("↑"), single("↗"), single("↥"), single("⇡"), single("↴"), single("←"),
            //
            single("↕"), single("→"), single("↤"), single("↨"), single("↦"), single("⇠"), single("⇢"),
            //
            single("↔"), single("↙"), single("↓"), single("↘"), single("↧"), single("⇣"), single("↵"),
            //
            single("↱"), single("↰"), single("↿"), single("↾"), single("⇀"), single("↼"), single("↝"),
            //
            single("↜"), single("↞"), single("↠"), single("↳"), single("↲"), single("⇃"), single("⇂"),
            //
            single("⇁"), single("↽"), single("↬"), single("↫"), single("⇟"), single("↡"), single("↣"),
            //
            single("↢"), single("⇝"), single("↭"), single("⇜"), single("↛"), single("↮"), single("↚"),
            //
            single("⇞"), single("↟"), single("⇥"), single("⇤"), single("↶"), single("↷"), single("↺"), single("↻"),
            //
            single("↯"), single("⇖"), single("⇑"), single("⇗"), single("⇈"), single("⇧"),
            //
            single("⇄"), single("⇐"), single("⇕"), single("⇒"), single("⇇"), single("⇅"), single("⇉"),
            //
            single("⇦"), single("⇪"), single("⇨"), single("↹"), single("⇙"), single("⇓"),
            //
            single("⇘"), single("⇊"), single("⇩"), single("⇆"), single("⇛"), single("⇔"),
            //
            single("⇚"), single("⇍"), single("⇎"), single("⇏"), single("⇋"), single("⇌"), single("↸"),
            }),
    /**
     * 序号符号：https://tool.lmeee.com/fuhao/shuzi
     */
    index("序号", new Symbol[] {
            single("①"), single("②"), single("③"), single("④"), single("⑤"), single("⑥"),
            //
            single("⑦"), single("⑧"), single("⑨"), single("⑩"), single("⑪"), single("⑫"),
            //
            single("⑬"), single("⑭"), single("⑮"), single("⑯"), single("⑰"), single("⑱"),
            //
            single("⑲"), single("⑳"), single("㉑"), single("㉒"), single("㉓"), single("㉔"),
            //
            single("㉕"), single("㉖"), single("㉗"), single("㉘"), single("㉙"), single("㉚"),
            //
            single("㉛"), single("㉜"), single("㉝"), single("㉞"), single("㉟"), single("㊱"),
            //
            single("㊲"), single("㊳"), single("㊴"), single("㊵"), single("㊶"), single("㊷"),
            //
            single("㊸"), single("㊹"), single("㊺"), single("㊻"), single("㊼"), single("㊽"),
            //
            single("㊾"), single("㊿"), single("⓪"), single("ⓞ"), single("⓵"),
            //
            single("⓶"), single("⓷"),
            //
            single("⓸"), single("⓹"), single("⓺"), single("⓻"), single("⓼"),
            //
            single("⓽"), single("⓾"), single("⑴"), single("⑵"), single("⑶"), single("⑷"),
            //
            single("⑸"), single("⑹"), single("⑺"), single("⑻"), single("⑼"), single("⑽"),
            //
            single("⑾"), single("⑿"), single("⒀"), single("⒁"), single("⒂"), single("⒃"),
            //
            single("⒄"), single("⒅"), single("⒆"), single("⒇"), single("➊"),
            //
            single("➋"), single("➌"), single("➍"),
            //
            single("➎"), single("➏"), single("➐"), single("➑"), single("➒"),
            //
            single("➓"), single("⓫"), single("⓬"), single("⓭"), single("⓮"), single("⓯"),
            //
            single("⓰"), single("⓱"), single("⓲"), single("⓳"), single("⓴"), single("º"),
            //
            single("¹"), single("²"), single("³"), single("⁴"), single("⁵"), single("⁶"),
            //
            single("⁷"), single("⁸"), single("⁹"), single("₀"), single("₁"), single("₂"),
            //
            single("₃"), single("₄"), single("₅"), single("₆"), single("₇"), single("₈"),
            //
            single("₉"), single("⒈"), single("⒉"), single("⒊"), single("⒋"), single("⒌"),
            //
            single("⒍"), single("⒎"), single("⒏"), single("⒐"), single("⒑"),
            //
            single("⒒"), single("⒓"), single("⒔"), single("⒕"), single("⒖"), single("⒗"),
            //
            single("⒘"), single("⒙"), single("⒚"), single("⒛"), single("Ⅰ"), single("Ⅱ"),
            //
            single("Ⅲ"), single("Ⅳ"), single("Ⅴ"), single("Ⅵ"), single("Ⅶ"), single("Ⅷ"),
            //
            single("Ⅸ"), single("Ⅹ"), single("ⅰ"), single("ⅱ"), single("ⅲ"), single("ⅳ"),
            //
            single("ⅴ"), single("ⅵ"), single("ⅶ"), single("ⅷ"), single("ⅸ"),
            //
            single("ⅹ"), single("㊀"), single("㊁"), single("㊂"), single("㊃"),
            //
            single("㊄"), single("㊅"), single("㊆"), single("㊇"), single("㊈"), single("㊉"),
            //
            single("㈠"), single("㈡"), single("㈢"), single("㈣"), single("㈤"),
            //
            single("㈥"), single("㈦"), single("㈧"), single("㈨"), single("㈩"),
            //
            single("ⓐ"), single("ⓑ"), single("ⓒ"), single("ⓓ"), single("ⓔ"),
            //
            single("ⓕ"), single("ⓖ"), single("ⓗ"), single("ⓘ"), single("ⓙ"), single("ⓚ"),
            //
            single("ⓛ"), single("ⓜ"), single("ⓝ"), single("ⓞ"), single("ⓟ"), single("ⓠ"),
            //
            single("ⓡ"), single("ⓢ"), single("ⓣ"), single("ⓤ"), single("ⓥ"), single("ⓦ"),
            //
            single("ⓧ"), single("ⓨ"), single("ⓩ"), single("Ⓐ"), single("Ⓑ"), single("Ⓒ"), single("Ⓓ"),
            //
            single("Ⓔ"), single("Ⓕ"), single("Ⓖ"), single("Ⓗ"), single("Ⓘ"), single("Ⓙ"),
            //
            single("Ⓚ"), single("Ⓛ"), single("Ⓜ"), single("Ⓝ"), single("Ⓞ"), single("Ⓟ"),
            //
            single("Ⓠ"), single("Ⓡ"), single("Ⓢ"), single("Ⓣ"), single("Ⓤ"), single("Ⓥ"),
            //
            single("Ⓦ"), single("Ⓧ"), single("Ⓨ"), single("Ⓩ"), single("⒜"), single("⒝"),
            //
            single("⒞"), single("⒟"), single("⒠"), single("⒡"), single("⒢"), single("⒣"),
            //
            single("⒤"), single("⒥"), single("⒦"), single("⒧"), single("⒨"), single("⒩"),
            //
            single("⒪"), single("⒫"), single("⒬"), single("⒭"), single("⒮"), single("⒯"),
            //
            single("⒰"), single("⒱"), single("⒲"), single("⒳"), single("⒴"), single("⒵"),
            }),
    /**
     * 符号：https://tool.lmeee.com/fuhao/tianqi
     */
    weather("天气", new Symbol[] {
            single("☼"), single("☀"), single("☁"), single("☂"), single("☾"), single("☽"),
            //
            single("☃"), single("⊙"), single("☉"), single("↯"), single("★"), single("✰"),
            //
            single("☆"), single("✩"), single("✪"), single("✫"), single("✬"), single("✭"),
            //
            single("✮"), single("✯"), single("✢"), single("⋆"), single("✢"), single("✣"),
            //
            single("✤"), single("✥"), single("❋"), single("✦"), single("✧"), single("❂"),
            //
            single("✱"), single("✲"), single("✵"), single("✶"), single("✷"),
            //
            single("✸"), single("✹"), single("✺"), single("✻"), single("✼"),
            //
            single("❅"), single("❆"), single("❈"), single("❉"), single("❊"),
            }),
    /**
     * 符号：https://tool.lmeee.com/fuhao/sanjiao
     */
    geometry("几何", new Symbol[] {
            single("▪"), single("▫"), single("■"), single("□"), single("▢"),
            //
            single("⊡"), single("▣"), single("❏"), single("❑"), single("❐"), single("❒"),
            //
            single("◙"), single("◘"), single("▤"), single("▥"), single("▧"), single("▨"), single("▦"),
            //
            single("▩"), single("⊟"), single("◫"), single("⊞"), single("⊠"),
            //
            single("◧"), single("◨"), single("◩"), single("◪"), single("▬"), single("▭"),
            //
            single("▮"), single("▯"), single("░"), single("▒"), single("▓"), single("♦"),
            //
            single("⋄"), single("▱"), single("▰"), single("◆"), single("◇"), single("◈"),
            //
            single("۞"), single("▁"), single("▂"), single("▃"), single("▄"), single("▅"),
            //
            single("▆"), single("▇"), single("█"), single("▏"), single("▎"), single("▍"),
            //
            single("▌"), single("▋"), single("▊"), single("▉"), single("✚"), single("✜"), single("◰"),
            //
            single("◱"), single("◲"), single("◳"), single("◻"), single("◼"), single("◽"), single("◾"),
            //
            single("⧈"), single("▛"), single("▜"), single("▝"), single("▞"), single("▟"),
            //
            single("▖"), single("▗"), single("▘"), single("▙"), single("▚"), single("°"),
            //
            single("º"), single("o"), single("O"), single("·"), single("•"), single("◎"),
            //
            single("○"), single("●"), single("❍"), single("◉"), single("◌"), single("◍"),
            //
            single("⊙"), single("☉"), single("Θ"), single("⊖"), single("⊘"), single("⊕"),
            //
            single("⊗"), single("⊜"), single("◍"), single("◐"), single("◑"), single("◒"),
            //
            single("◓"), single("◔"), single("◕"), single("◖"), single("◗"), single("❂"),
            //
            single("☼"), single("¤"), single("◘"), single("◙"), single("◤"), single("◥"),
            //
            single("◄"), single("►"), single("▶"), single("◀"), single("▲"),
            //
            single("▼"), single("◣"), single("◢"), single("◥"), single("▸"), single("◂"),
            //
            single("▴"), single("▾"), single("△"), single("▽"), single("▷"), single("◁"), single("⊿"),
            //
            single("▻"), single("◅"), single("▵"), single("▿"), single("▹"), single("◃"),
            //
            single("◩"), single("◪"), single("∆"), single("∇"), single("◬"), single("◭"),
            //
            single("◮"), single("‣"), single("ㅿ"), single("⊿"), single("⍍"),
            //
            single("⍔"), single("⍙"), single("≜"), single("⊵"), single("⊴"), single("⋈"),
            //
            single("⑅"), single("⌳"), single("⌲"), single("⋪"), single("⋫"), single("⍢"),
            //
            single("⍫"), single("∡"),
            }),
    /**
     * 符号：https://tool.lmeee.com/fuhao/teshu
     */
    other("其他", new Symbol[] {
            single("☯"), single("☭"), single("♥"), single("♡"), single("☜"), single("☞"),
            //
            single("☎"), single("☏"), single("☻"), single("♧"), single("♂"), single("♀"),
            //
            single("♠"), single("♣"), single("♨"), single("◊"), single("◦"), single("♬"),
            //
            single("♪"), single("♩"), single("♭"), single("の"), single("あ"), single("ぃ"),
            //
            single("￡"), single("Ю"),
            //
            single("〓"), single("§"), single("♤"), single("⊹"), single("⊱"), single("⋛"),
            //
            single("⋌"), single("⋚"), single("⊰"), single("۩"), single("✿"), single("~"), single("."),
            //
            single("‿"), single("｡"), single("┱"), single("┲"), single("❥"), single("❦"),
            //
            single("❧"), single("❃"), single("❁"), single("❀"), single("✄"), single("ღ"), single("☇"),
            //
            single("☈"), single("☊"), single("☋"), single("☌"), single("☍"), single("╬"),
            //
            single("♫"), single("﹌"), single("▀"), single("▐"), single("✎"), single("✟"), single("ஐ"),
            //
            single("㊣"), single("✐"), single("✓"), single("✕"), single("♯"), single("♮"), single("₪"),
            //
            single("큐"), single("«"), single("»"), single("™"), single("☨"), single("✞"),
            //
            single("✛"), single("✙"), single("✠"), single("†"), single("‡"), single("웃"), single("유"),
            //
            single("☒"), single("¿"), single("─"), single("│"), single("☿"), single("☤"), single("♚"),
            //
            single("♛"), single("⌘"), single("※"), single("¡"), single("━"), single("┃"), single("ツ"),
            //
            single("©"), single("®"), single("Σ"), single("卐"), single("┄"), single("┆"),
            //
            single("ϟ"), single("¢"), single("€"), single("£"), single("½"), single("┅"),
            //
            single("┇"), single("┈"), single("┊"), single("❖"), single("┉"), single("┋"), single("ヅ"),
            //
            single("ッ"), single("シ"), single("Ü"), single("ϡ"), single("℠"), single("℗"),
            //
            single("♢"), single("☚"), single("☛"), single("☟"), single("✽"), single("✾"), single("⚘"),
            //
            single("☐"), single("✗"), single("ㄨ"), single("☥"), single("☓"), single("☩"), single("☧"),
            //
            single("☬"), single("♁"), single("♆"), single("༄"), single("இ"), single("ண"),
            //
            single("৳"), single("¶"), single("$"), single("Ψ"), single("￥"), single("₩"),
            //
            single("₫"), single("Ұ"), single("฿"), single("￠"), single("₢"), single("₯"), single("₭"),
            //
            single("₣"), single("₤"), single("₦"), single("₰"), single("₧"), single("₱"),
            //
            single("៛"), single("₨"), single("₮"), single("₴"), single("₳"), single("₵"),
            //
            single("₡"), single("₥"), single("лв"), single("円"), single("र"),
            //
            single("₲"), single("Kč"), single("〒"), single("zł"), single("₠"),
            }),
    ;

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
