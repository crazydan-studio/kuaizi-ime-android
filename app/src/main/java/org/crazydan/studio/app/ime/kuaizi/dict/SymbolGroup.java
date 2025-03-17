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

package org.crazydan.studio.app.ime.kuaizi.dict;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-28
 */
public enum SymbolGroup {
    /** 汉语标点符号: https://zh.wikipedia.org/wiki/%E6%A0%87%E7%82%B9%E7%AC%A6%E5%8F%B7 */
    han("中文", new Symbol[] {
            single("，"), single("。"), single("？"), single("！"), single("："), single("；"),
            //
            single("、"), single("…"), single("～"), single("—"), single("·"),
            //
            single("｜"), single("＿"), single("￣"), single("ˉ"),
            //
            single("﹏"), single("﹍"), single("﹎"), single("¨"), single("˜"), single("﹋"),
            //
            single("‘"), single("’"), pair("‘", "’"), single("“"), single("”"), pair("“", "”"),
            //
            single("（"), single("）"), pair("（", "）"), single("「"), single("」"), pair("「", "」"),
            //
            single("《"), single("》"), pair("《", "》"), single("【"), single("】"), pair("【", "】"),
            //
            single("〈"), single("〉"), pair("〈", "〉"), single("［"), single("］"), pair("［", "］"),
            //
            single("〔"), single("〕"), pair("〔", "〕"), single("﹛"), single("﹜"), pair("﹛", "﹜"),
            //
            single("『"), single("』"), pair("『", "』"), single("〖"), single("〗"), pair("〖", "〗"),
            //
            single("‹"), single("›"), pair("‹", "›"), single("〝"), single("〞"), pair("〝", "〞"),
            //
            single("︴"), single("〃"),
            //
            single("﹁"), single("﹂"), single("﹃"), single("﹄"), single("︵"), single("︶"),
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
            single("&"), single("*"), single("-"), single("_"), single("+"), single("="),
            //
            single("/"), single("\\"), single("|"), single("~"), single("`"),
            //
            single("'"), pair("'", "'"), single("\""), pair("\"", "\""),
            //
            single("("), single(")"), pair("(", ")"), single("["), single("]"), pair("[", "]"),
            //
            single("{"), single("}"), pair("{", "}"), single("<"), single(">"), pair("<", ">"),
            }),

    /**
     * 数学符号：https://coolsymbol.com/pi-symbol-infinity-symbol-sum-sigma-symbol-square-root-symbol-integral-symbol-math-signs.html
     */
    math("数学", new Symbol[] {
            //
            single("+"), single("-"), single("="), single("^"), single("%"),
            //
            single("℅"), single("‰"), single("‱"), single("π"), single("∞"), single("Σ"),
            //
            single("√"), single("∛"), single("∜"), single("∫"), single("∬"),
            //
            single("∭"), single("∮"), single("∯"), single("∰"), single("∱"),
            //
            single("∲"), single("∳"), single("∀"), single("∁"), single("∂"),
            //
            single("∃"), single("∄"), single("∅"), single("∆"), single("∇"),
            //
            single("∈"), single("∉"), single("∊"), single("∋"), single("∌"),
            //
            single("∍"), single("∎"), single("∏"), single("∐"), single("∑"),
            //
            single("−"), single("∓"), single("∔"), single("∕"), single("∖"),
            //
            single("∗"), single("∘"), single("∙"), single("∝"), single("∟"),
            //
            single("∠"), single("∡"), single("∢"), single("∣"), single("∤"),
            //
            single("∥"), single("∦"), single("∧"), single("∨"), single("∩"),
            //
            single("∪"), single("∴"), single("∵"), single("∶"), single("∷"),
            //
            single("∸"), single("∹"), single("∺"), single("∻"), single("∼"),
            //
            single("∽"), single("∾"), single("∿"), single("≀"), single("≁"),
            //
            single("≂"), single("≃"), single("≄"), single("≅"), single("≆"),
            //
            single("≇"), single("≈"), single("≉"), single("≊"), single("≋"),
            //
            single("≌"), single("≍"), single("≎"), single("≏"), single("≐"),
            //
            single("≑"), single("≒"), single("≓"), single("≔"), single("≕"),
            //
            single("≖"), single("≗"), single("≘"), single("≙"), single("≚"),
            //
            single("≛"), single("≜"), single("≝"), single("≞"), single("≟"),
            //
            single("≠"), single("≡"), single("≢"), single("≣"), single("≤"),
            //
            single("≥"), single("≦"), single("≧"), single("≨"), single("≩"),
            //
            single("≪"), single("≫"), single("≬"), single("≭"), single("≮"),
            //
            single("≯"), single("≰"), single("≱"), single("≲"), single("≳"),
            //
            single("≴"), single("≵"), single("≶"), single("≷"), single("≸"),
            //
            single("≹"), single("≺"), single("≻"), single("≼"), single("≽"),
            //
            single("≾"), single("≿"), single("⊀"), single("⊁"), single("⊂"),
            //
            single("⊃"), single("⊄"), single("⊅"), single("⊆"), single("⊇"),
            //
            single("⊈"), single("⊉"), single("⊊"), single("⊋"), single("⊌"),
            //
            single("⊍"), single("⊎"), single("⊏"), single("⊐"), single("⊑"),
            //
            single("⊒"), single("⊓"), single("⊔"), single("⊕"), single("⊖"),
            //
            single("⊗"), single("⊘"), single("⊙"), single("⊚"), single("⊛"),
            //
            single("⊜"), single("⊝"), single("⊞"), single("⊟"), single("⊠"),
            //
            single("⊡"), single("⊢"), single("⊣"), single("⊤"), single("⊥"),
            //
            single("⊦"), single("⊧"), single("⊨"), single("⊩"), single("⊪"),
            //
            single("⊫"), single("⊬"), single("⊭"), single("⊮"), single("⊯"),
            //
            single("⊰"), single("⊱"), single("⊲"), single("⊳"), single("⊴"),
            //
            single("⊵"), single("⊶"), single("⊷"), single("⊸"), single("⊹"),
            //
            single("⊺"), single("⊻"), single("⊼"), single("⊽"), single("⊾"),
            //
            single("⊿"), single("⋀"), single("⋁"), single("⋂"), single("⋃"),
            //
            single("⋄"), single("⋅"), single("⋆"), single("⋇"), single("⋈"),
            //
            single("⋉"), single("⋊"), single("⋋"), single("⋌"), single("⋍"),
            //
            single("⋎"), single("⋏"), single("⋐"), single("⋑"), single("⋒"),
            //
            single("⋓"), single("⋔"), single("⋕"), single("⋖"), single("⋗"),
            //
            single("⋘"), single("⋙"), single("⋚"), single("⋛"), single("⋜"),
            //
            single("⋝"), single("⋞"), single("⋟"), single("⋠"), single("⋡"),
            //
            single("⋢"), single("⋣"), single("⋤"), single("⋥"), single("⋦"),
            //
            single("⋧"), single("⋨"), single("⋩"), single("⋪"), single("⋫"),
            //
            single("⋬"), single("⋭"), single("⋮"), single("⋯"), single("⋰"),
            //
            single("⋱"), single("⁺"), single("⁻"), single("⁼"), single("⁽"),
            //
            single("⁾"), single("ⁿ"), single("₊"), single("₋"), single("₌"),
            //
            single("₍"), single("₎"), single("✖"), single("﹢"), single("﹣"),
            //
            single("＋"), single("－"), single("／"), single("＝"), single("÷"),
            //
            single("±"), single("×"), single("✓"), single("☒"), single("✗"),
            //
            single("✘"), single("✕"), single("☓"), single("х"), single("╳"),
            //
            single("ㄨ"), single("メ"), single("＜"), single("＞"),
            //
            single("/"), single("‖"), single("⌒"), single("㏑"), single("㏒"),
            //
            single("℃"), single("℉"), single("㏄"), single("㎎"), single("㎏"),
            //
            single("㎜"), single("㎝"), single("㎞"), single("㏎"), single("㎡"),
            //
            single("㏕"), single("⅟"), single("½"), single("⅓"), single("⅕"), single("⅙"),
            //
            single("⅛"), single("⅔"), single("⅖"), single("⅚"), single("⅜"),
            //
            single("¾"), single("⅗"), single("⅝"), single("⅞"), single("⅘"),
            //
            single("¼"), single("⅐"), single("⅑"), single("⅒"), single("↉"),
            }),
    /**
     * 箭头符号：https://coolsymbol.com/arrow-symbols-arrow-signs.html
     */
    arrow("箭头", new Symbol[] {
            //
            single("↕"), single("↖"), single("↗"), single("↘"), single("↙"),
            //
            single("↚"), single("↛"), single("↜"), single("↝"), single("↞"),
            //
            single("↟"), single("↠"), single("↡"), single("↢"), single("↣"),
            //
            single("↤"), single("↥"), single("↦"), single("↧"), single("↨"),
            //
            single("↩"), single("↪"), single("↫"), single("↬"), single("↭"),
            //
            single("↮"), single("↯"), single("↰"), single("↱"), single("↲"),
            //
            single("↳"), single("↴"), single("↶"), single("↷"), single("↸"),
            //
            single("↹"), single("↺"), single("↻"), single("↼"), single("↽"),
            //
            single("↾"), single("↿"), single("⇀"), single("⇁"), single("⇂"),
            //
            single("⇃"), single("⇄"), single("⇅"), single("⇆"), single("⇇"),
            //
            single("⇈"), single("⇉"), single("⇊"), single("⇋"), single("⇌"),
            //
            single("⇍"), single("⇎"), single("⇏"), single("⇕"), single("⇖"),
            //
            single("⇗"), single("⇘"), single("⇙"), single("⇚"), single("⇛"),
            //
            single("⇜"), single("⇝"), single("⇞"), single("⇟"), single("⇠"),
            //
            single("⇡"), single("⇢"), single("⇣"), single("⇤"), single("⇥"),
            //
            single("⇦"), single("⇧"), single("⇨"), single("⇩"), single("⇪"),
            //
            single("⌅"), single("⌆"), single("⌤"), single("⏎"), single("▶"),
            //
            single("☇"), single("☈"), single("☊"), single("☋"), single("☌"),
            //
            single("☍"), single("➔"), single("➘"), single("➙"), single("➚"),
            //
            single("➛"), single("➜"), single("➝"), single("➞"), single("➟"),
            //
            single("➠"), single("➡"), single("➢"), single("➣"), single("➤"),
            //
            single("➥"), single("➦"), single("➧"), single("➨"), single("➩"),
            //
            single("➪"), single("➫"), single("➬"), single("➭"), single("➮"),
            //
            single("➯"), single("➱"), single("➲"), single("➳"), single("➴"),
            //
            single("➵"), single("➶"), single("➷"), single("➸"), single("➹"),
            //
            single("➺"), single("➻"), single("➼"), single("➽"), single("➾"),
            //
            single("⤴"), single("⤵"), single("↵"), single("↓"), single("↔"),
            //
            single("←"), single("→"), single("↑"), single("⌦"), single("⌫"),
            //
            single("⌧"), single("⇰"), single("⇫"), single("⇬"), single("⇭"),
            //
            single("⇳"), single("⇮"), single("⇯"), single("⇱"), single("⇲"),
            //
            single("⇴"), single("⇵"), single("⇷"), single("⇸"), single("⇹"),
            //
            single("⇺"), single("⇑"), single("⇓"), single("⇽"), single("⇾"),
            //
            single("⇿"), single("⬳"), single("⟿"), single("⤉"), single("⤈"),
            //
            single("⇻"), single("⇼"), single("⬴"), single("⤀"), single("⬵"),
            //
            single("⤁"), single("⬹"), single("⤔"), single("⬺"), single("⤕"),
            //
            single("⬶"), single("⤅"), single("⬻"), single("⤖"), single("⬷"),
            //
            single("⤐"), single("⬼"), single("⤗"), single("⬽"), single("⤘"),
            //
            single("⤝"), single("⤞"), single("⤟"), single("⤠"), single("⤡"),
            //
            single("⤢"), single("⤣"), single("⤤"), single("⤥"), single("⤦"),
            //
            single("⤪"), single("⤨"), single("⤧"), single("⤩"), single("⤭"),
            //
            single("⤮"), single("⤯"), single("⤰"), single("⤱"), single("⤲"),
            //
            single("⤫"), single("⤬"), single("⬐"), single("⬎"), single("⬑"),
            //
            single("⬏"), single("⤶"), single("⤷"), single("⥂"), single("⥃"),
            //
            single("⥄"), single("⭀"), single("⥱"), single("⥶"), single("⥸"),
            //
            single("⭂"), single("⭈"), single("⭊"), single("⥵"), single("⭁"),
            //
            single("⭇"), single("⭉"), single("⥲"), single("⭋"), single("⭌"),
            //
            single("⥳"), single("⥴"), single("⥆"), single("⥅"), single("⥹"),
            //
            single("⥻"), single("⬰"), single("⥈"), single("⬾"), single("⥇"),
            //
            single("⬲"), single("⟴"), single("⥷"), single("⭃"), single("⥺"),
            //
            single("⭄"), single("⥉"), single("⥰"), single("⬿"), single("⤳"),
            //
            single("⥊"), single("⥋"), single("⥌"), single("⥍"), single("⥎"),
            //
            single("⥏"), single("⥐"), single("⥑"), single("⥒"), single("⥓"),
            //
            single("⥔"), single("⥕"), single("⥖"), single("⥗"), single("⥘"),
            //
            single("⥙"), single("⥚"), single("⥛"), single("⥜"), single("⥝"),
            //
            single("⥞"), single("⥟"), single("⥠"), single("⥡"), single("⥢"),
            //
            single("⥤"), single("⥣"), single("⥥"), single("⥦"), single("⥨"),
            //
            single("⥧"), single("⥩"), single("⥮"), single("⥯"), single("⥪"),
            //
            single("⥬"), single("⥫"), single("⥭"), single("⤌"), single("⤍"),
            //
            single("⤎"), single("⤏"), single("⬸"), single("⤑"), single("⬱"),
            //
            single("⟸"), single("⟹"), single("⟺"), single("⤂"), single("⤃"),
            //
            single("⤄"), single("⤆"), single("⤇"), single("⤊"), single("⤋"),
            //
            single("⭅"), single("⭆"), single("⟰"), single("⟱"), single("⇐"),
            //
            single("⇒"), single("⇔"), single("⇶"), single("⟵"), single("⟶"),
            //
            single("⟷"), single("⬄"), single("⬀"), single("⬁"), single("⬂"),
            //
            single("⬃"), single("⬅"), single("⬆"), single("⬇"), single("⬈"),
            //
            single("⬉"), single("⬊"), single("⬋"), single("⬌"), single("⬍"),
            //
            single("⟻"), single("⟼"), single("⤒"), single("⤓"), single("⤙"),
            //
            single("⤚"), single("⤛"), single("⤜"), single("⥼"), single("⥽"),
            //
            single("⥾"), single("⥿"), single("⤼"), single("⤽"), single("⤾"),
            //
            single("⤿"), single("⤸"), single("⤺"), single("⤹"), single("⤻"),
            //
            single("⥀"), single("⥁"), single("⟲"), single("⟳"),
            }),
    /**
     * 序号符号：
     * - https://tool.lmeee.com/fuhao/shuzi
     * - https://coolsymbol.com/number-symbols.html
     */
    index("序号", new Symbol[] {
            //
            single("⓪"), single("①"), single("②"), single("③"), single("④"),
            //
            single("⑤"), single("⑥"), single("⑦"), single("⑧"), single("⑨"),
            //
            single("⑩"), single("⑪"), single("⑫"), single("⑬"), single("⑭"),
            //
            single("⑮"), single("⑯"), single("⑰"), single("⑱"), single("⑲"),
            //
            single("⑳"), single("㉑"), single("㉒"), single("㉓"), single("㉔"),
            //
            single("㉕"), single("㉖"), single("㉗"), single("㉘"), single("㉙"),
            //
            single("㉚"), single("㉛"), single("㉜"), single("㉝"), single("㉞"),
            //
            single("㉟"), single("㊱"), single("㊲"), single("㊳"), single("㊴"),
            //
            single("㊵"), single("㊶"), single("㊷"), single("㊸"), single("㊹"),
            //
            single("㊺"), single("㊻"), single("㊼"), single("㊽"), single("㊾"),
            //
            single("㊿"), single("⓵"), single("⓶"), single("⓷"), single("⓸"),
            //
            single("⓹"), single("⓺"), single("⓻"), single("⓼"), single("⓽"),
            //
            single("⓾"), single("➀"), single("➁"), single("➂"), single("➃"),
            //
            single("➄"), single("➅"), single("➆"), single("➇"), single("➈"),
            //
            single("➉"), single("⑴"), single("⑵"), single("⑶"), single("⑷"),
            //
            single("⑸"), single("⑹"), single("⑺"), single("⑻"), single("⑼"),
            //
            single("⑽"), single("⑾"), single("⑿"), single("⒀"), single("⒁"),
            //
            single("⒂"), single("⒃"), single("⒄"), single("⒅"), single("⒆"),
            //
            single("⒇"), single("➊"), single("➋"), single("➌"), single("➍"),
            //
            single("➎"), single("➏"), single("➐"), single("➑"), single("➒"),
            //
            single("➓"), single("⓫"), single("⓬"), single("⓭"), single("⓮"),
            //
            single("⓯"), single("⓰"), single("⓱"), single("⓲"), single("⓳"),
            //
            single("⓴"), single("⓿"), single("❶"), single("❷"), single("❸"),
            //
            single("❹"), single("❺"), single("❻"), single("❼"), single("❽"),
            //
            single("❾"), single("❿"), single("⁰"), single("０"), single("１"),
            //
            single("２"), single("３"), single("４"), single("５"), single("６"),
            //
            single("７"), single("８"), single("９"), single("º"), single("¹"),
            //
            single("²"), single("³"), single("⁴"), single("⁵"), single("⁶"),
            //
            single("⁷"), single("⁸"), single("⁹"), single("₀"), single("₁"),
            //
            single("₂"), single("₃"), single("₄"), single("₅"), single("₆"),
            //
            single("₇"), single("₈"), single("₉"), single("⒈"), single("⒉"),
            //
            single("⒊"), single("⒋"), single("⒌"), single("⒍"), single("⒎"),
            //
            single("⒏"), single("⒐"), single("⒑"), single("⒒"), single("⒓"),
            //
            single("⒔"), single("⒕"), single("⒖"), single("⒗"), single("⒘"),
            //
            single("⒙"), single("⒚"), single("⒛"), single("㊀"), single("㊁"),
            //
            single("㊂"), single("㊃"), single("㊄"), single("㊅"), single("㊆"),
            //
            single("㊇"), single("㊈"), single("㊉"), single("㈠"), single("㈡"),
            //
            single("㈢"), single("㈣"), single("㈤"), single("㈥"), single("㈦"),
            //
            single("㈧"), single("㈨"), single("㈩"), single("Ⅰ"), single("Ⅱ"),
            //
            single("Ⅲ"), single("Ⅳ"), single("Ⅴ"), single("Ⅵ"), single("Ⅶ"),
            //
            single("Ⅷ"), single("Ⅸ"), single("Ⅹ"), single("Ⅺ"), single("Ⅻ"),
            //
            single("Ⅼ"), single("Ⅽ"), single("Ⅾ"), single("Ⅿ"), single("ⅰ"),
            //
            single("ⅱ"), single("ⅲ"), single("ⅳ"), single("ⅴ"), single("ⅵ"),
            //
            single("ⅶ"), single("ⅷ"), single("ⅸ"), single("ⅹ"), single("ⅺ"),
            //
            single("ⅻ"), single("ⅼ"), single("ⅽ"), single("ⅾ"), single("ⅿ"),
            //
            single("ↀ"), single("ↁ"), single("ↂ"), single("ⓐ"), single("ⓑ"),
            //
            single("ⓒ"), single("ⓓ"), single("ⓔ"), single("ⓕ"), single("ⓖ"),
            //
            single("ⓗ"), single("ⓘ"), single("ⓙ"), single("ⓚ"), single("ⓛ"),
            //
            single("ⓜ"), single("ⓝ"), single("ⓞ"), single("ⓟ"), single("ⓠ"),
            //
            single("ⓡ"), single("ⓢ"), single("ⓣ"), single("ⓤ"), single("ⓥ"),
            //
            single("ⓦ"), single("ⓧ"), single("ⓨ"), single("ⓩ"), single("Ⓐ"),
            //
            single("Ⓑ"), single("Ⓒ"), single("Ⓓ"), single("Ⓔ"), single("Ⓕ"),
            //
            single("Ⓖ"), single("Ⓗ"), single("Ⓘ"), single("Ⓙ"), single("Ⓚ"),
            //
            single("Ⓛ"), single("Ⓜ"), single("Ⓝ"), single("Ⓞ"), single("Ⓟ"),
            //
            single("Ⓠ"), single("Ⓡ"), single("Ⓢ"), single("Ⓣ"), single("Ⓤ"),
            //
            single("Ⓥ"), single("Ⓦ"), single("Ⓧ"), single("Ⓨ"), single("Ⓩ"),
            //
            single("⒜"), single("⒝"), single("⒞"), single("⒟"), single("⒠"),
            //
            single("⒡"), single("⒢"), single("⒣"), single("⒤"), single("⒥"),
            //
            single("⒦"), single("⒧"), single("⒨"), single("⒩"), single("⒪"),
            //
            single("⒫"), single("⒬"), single("⒭"), single("⒮"), single("⒯"),
            //
            single("⒰"), single("⒱"), single("⒲"), single("⒳"), single("⒴"),
            //
            single("⒵"),
            }),
    /**
     * 几何符号：
     * - https://tool.lmeee.com/fuhao/sanjiao
     * - https://coolsymbol.com/
     */
    geometry("几何", new Symbol[] {
            //
            single("❏"), single("❐"), single("❑"), single("❒"), single("▀"),
            //
            single("▁"), single("▂"), single("▃"), single("▄"), single("▅"),
            //
            single("▆"), single("▇"), single("▉"), single("▊"), single("▋"),
            //
            single("█"), single("▌"), single("▐"), single("▍"), single("▎"),
            //
            single("▏"), single("▕"), single("▛"), single("▜"), single("▝"),
            //
            single("▞"), single("▟"), single("▖"), single("▗"), single("▘"),
            //
            single("▙"), single("▚"), single("░"), single("▒"), single("▓"),
            //
            single("▔"), single("▬"), single("▢"), single("▣"), single("▤"),
            //
            single("▥"), single("▦"), single("▧"), single("▨"), single("▩"),
            //
            single("▪"), single("▫"), single("▭"), single("▮"), single("▯"),
            //
            single("☰"), single("☲"), single("☱"), single("☴"), single("☵"),
            //
            single("☶"), single("☳"), single("☷"), single("▰"), single("▱"),
            //
            single("◧"), single("◨"), single("◩"), single("◪"), single("◫"),
            //
            single("∎"), single("■"), single("□"), single("⊞"), single("⊟"),
            //
            single("⊠"), single("⊡"), single("❘"), single("❙"), single("❚"),
            //
            single("〓"), single("♦"), single("⋄"), single("◊"), single("◈"),
            //
            single("◇"), single("◆"), single("⎔"), single("۞"), single("✚"),
            //
            single("✜"), single("◰"), single("◱"), single("◲"), single("◳"),
            //
            single("◻"), single("◼"), single("◽"), single("◾"), single("⧈"),
            //
            single("⎚"), single("☖"), single("¤"), single("⍍"), single("⍔"),
            //
            single("⍙"), single("⋈"), single("☗"), single("◄"), single("▲"),
            //
            single("▼"), single("►"), single("◀"), single("◣"), single("◥"),
            //
            single("◤"), single("◢"), single("▶"), single("◂"), single("▴"),
            //
            single("▾"), single("▸"), single("‣"), single("ㅿ"), single("◁"),
            //
            single("△"), single("▽"), single("▷"), single("∆"), single("∇"),
            //
            single("⊳"), single("⊲"), single("⊴"), single("⊵"), single("◅"),
            //
            single("▻"), single("▵"), single("▿"), single("◃"), single("▹"),
            //
            single("◭"), single("◮"), single("⫷"), single("⫸"), single("⋖"),
            //
            single("⋗"), single("⋪"), single("⋫"), single("⋬"), single("⋭"),
            //
            single("⍫"), single("∡"), single("⌳"), single("⌲"), single("⍢"),
            //
            single("⊿"), single("◬"), single("≜"), single("⑅"), single("◉"),
            //
            single("○"), single("◌"), single("°"), single("º"), single("o"),
            //
            single("O"), single("·"), single("•"), single("☉"), single("Θ"),
            //
            single("☼"), single("◍"), single("◎"), single("●"), single("◐"),
            //
            single("◑"), single("◒"), single("◓"), single("◔"), single("◕"),
            //
            single("◖"), single("◗"), single("❂"), single("☢"), single("⊗"),
            //
            single("⊙"), single("◘"), single("◙"), single("◚"), single("◛"),
            //
            single("◜"), single("◝"), single("◞"), single("◟"), single("◠"),
            //
            single("◡"), single("◯"), single("〇"), single("〶"), single("⚫"),
            //
            single("⬤"), single("◦"), single("∅"), single("∘"), single("⊕"),
            //
            single("⊖"), single("⊘"), single("⊚"), single("⊛"), single("⊜"),
            //
            single("⊝"), single("❍"), single("⦿"), single("│"), single("┃"),
            //
            single("╽"), single("╿"), single("╏"), single("║"), single("╎"),
            //
            single("┇"), single("︱"), single("┊"), single("︳"), single("┋"),
            //
            single("┆"), single("╵"), single("〡"), single("〢"), single("╹"),
            //
            single("╻"), single("╷"), single("〣"), single("☰"), single("☱"),
            //
            single("☲"), single("☳"), single("☴"), single("☵"), single("☶"),
            //
            single("☷"), single("≡"), single("✕"), single("═"), single("━"),
            //
            single("─"), single("╍"), single("┅"), single("┉"), single("┄"),
            //
            single("┈"), single("╌"), single("╴"), single("╶"), single("╸"),
            //
            single("╺"), single("╼"), single("╾"), single("﹉"), single("﹍"),
            //
            single("﹊"), single("﹎"), single("︲"), single("⑆"), single("⑇"),
            //
            single("⑈"), single("⑉"), single("⑊"), single("⑄"), single("⑀"),
            //
            single("︴"), single("﹏"), single("﹌"), single("﹋"), single("╳"),
            //
            single("╲"), single("╱"), single("︶"), single("︵"), single("〵"),
            //
            single("〴"), single("〳"), single("〆"), single("`"), single("ᐟ"),
            //
            single("‐"), single("⁃"), single("⎯"), single("〄"), single("﹄"),
            //
            single("﹃"), single("﹂"), single("﹁"), single("┕"), single("┓"),
            //
            single("└"), single("┐"), single("┖"), single("┒"), single("┗"),
            //
            single("┑"), single("┍"), single("┙"), single("┏"), single("┛"),
            //
            single("┎"), single("┚"), single("┌"), single("┘"), single("「"),
            //
            single("」"), single("『"), single("』"), single("˩"), single("˥"),
            //
            single("├"), single("┝"), single("┞"), single("┟"), single("┠"),
            //
            single("┡"), single("┢"), single("┣"), single("┤"), single("┥"),
            //
            single("┦"), single("┧"), single("┨"), single("┩"), single("┪"),
            //
            single("┫"), single("┬"), single("┭"), single("┮"), single("┯"),
            //
            single("┰"), single("┱"), single("┲"), single("┳"), single("┴"),
            //
            single("┵"), single("┶"), single("┷"), single("┸"), single("┹"),
            //
            single("┺"), single("┻"), single("┼"), single("┽"), single("┾"),
            //
            single("┿"), single("╀"), single("╁"), single("╂"), single("╃"),
            //
            single("╄"), single("╅"), single("╆"), single("╇"), single("╈"),
            //
            single("╉"), single("╊"), single("╋"), single("╒"), single("╕"),
            //
            single("╓"), single("╖"), single("╔"), single("╗"), single("╘"),
            //
            single("╛"), single("╙"), single("╜"), single("╚"), single("╝"),
            //
            single("╞"), single("╡"), single("╟"), single("╢"), single("╠"),
            //
            single("╣"), single("╥"), single("╨"), single("╧"), single("╤"),
            //
            single("╦"), single("╩"), single("╪"), single("╫"), single("╬"),
            //
            single("〒"), single("⊢"), single("⊣"), single("⊤"), single("⊥"),
            //
            single("╭"), single("╮"), single("╯"), single("╰"), single("⊦"),
            //
            single("⊧"), single("⊨"), single("⊩"), single("⊪"), single("⊫"),
            //
            single("⊬"), single("⊭"), single("⊮"), single("⊯"), single("⊺"),
            //
            single("〦"), single("〧"), single("〨"), single("˦"), single("˧"),
            //
            single("˨"), single("⑁"), single("⑂"), single("⑃"), single("∟"),
            }),
    /** https://coolsymbol.com/ */
    misc("杂项", new Symbol[] {
            //
            single("★"), single("☆"), single("✡"), single("✦"), single("✧"),
            //
            single("✩"), single("✪"), single("✫"), single("✬"), single("✭"),
            //
            single("✮"), single("✯"), single("✰"), single("⁂"), single("⁎"),
            //
            single("⁑"), single("✢"), single("✣"), single("✤"), single("✥"),
            //
            single("✱"), single("✲"), single("✳"), single("✴"), single("✵"),
            //
            single("✶"), single("✷"), single("✸"), single("✹"), single("✺"),
            //
            single("✻"), single("✼"), single("✽"), single("✾"), single("✿"),
            //
            single("❀"), single("❁"), single("❂"), single("❃"), single("❇"),
            //
            single("❈"), single("❉"), single("❊"), single("❋"), single("❄"),
            //
            single("❆"), single("❅"), single("⋆"), single("≛"), single("ᕯ"),
            //
            single("✲"), single("࿏"), single("꙰"), single("۞"), single("⭒"),
            //
            single("⍟"), single("⭐"), single("🌠"), single("🌟"), single("💫"),
            //
            single("✨"), single("🌃"), single("🔯"), single("©"), single("®"),
            //
            single("™"), single("℠"), single("℡"), single("℗"), single("‱"),
            //
            single("№"), single("℀"), single("℁"), single("℅"), single("℆"),
            //
            single("⅍"), single("☊"), single("☎"), single("☏"), single("⌨"),
            //
            single("✁"), single("✂"), single("✃"), single("✄"), single("✆"),
            //
            single("✇"), single("✈"), single("✉"), single("✎"), single("✏"),
            //
            single("✐"), single("✑"), single("✒"), single("‰"), single("§"),
            //
            single("¶"), single("✌️"), single("☝️"), single("☞"), single("☛"),
            //
            single("☟"), single("☜"), single("☚"), single("✍️"), single("¢"),
            //
            single("$"), single("€"), single("£"), single("¥"), single("₮"),
            //
            single("৲"), single("৳"), single("௹"), single("฿"), single("៛"),
            //
            single("₠"), single("₡"), single("₢"), single("₣"), single("₤"),
            //
            single("₥"), single("₦"), single("₧"), single("₨"), single("₩"),
            //
            single("₪"), single("₫"), single("₭"), single("₯"), single("₰"),
            //
            single("₱"), single("₲"), single("₳"), single("₴"), single("₵"),
            //
            single("￥"), single("﷼"), single("¤"), single("ƒ"), single("♔"),
            //
            single("♕"), single("♖"), single("♗"), single("♘"), single("♙"),
            //
            single("♚"), single("♛"), single("♜"), single("♝"), single("♞"),
            //
            single("♟"), single("♤"), single("♠"), single("♧"), single("♣"),
            //
            single("♡"), single("♥"), single("♢"), single("♦"), single("♩"),
            //
            single("♪"), single("♫"), single("♬"), single("♭"), single("♮"),
            //
            single("♯"), single("°"), single("ø"), single("؂"), single("≠"),
            //
            single("≭"), single("°"), single("℃"), single("℉"), single("ϟ"),
            //
            single("☀"), single("☁"), single("☂"), single("☃"), single("☉"),
            //
            single("☼"), single("☽"), single("☾"), single("♁"), single("♨"),
            //
            single("❄"), single("❅"), single("❆"), single("☇"), single("☈"),
            //
            single("☄"),
            }),
    /**
     * 其他符号：https://tool.lmeee.com/fuhao/teshu
     */
    other("其他", new Symbol[] {
            //
            single("☯"), single("☭"), single("☻"), single("♂"), single("♀"),
            //
            single("の"), single("あ"), single("ぃ"), single("￡"), single("Ю"),
            //
            single("⊹"), single("⊱"), single("⋛"), single("⋚"), single("⊰"),
            //
            single("۩"), single("‿"), single("｡"), single("❥"), single("❦"),
            //
            single("❧"), single("ღ"), single("☋"), single("☌"), single("☍"),
            //
            single("✟"), single("ஐ"), single("㊣"), single("큐"), single("«"),
            //
            single("»"), single("☨"), single("✞"), single("✛"), single("✙"),
            //
            single("✠"), single("†"), single("‡"), single("웃"), single("유"),
            //
            single("¡"), single("¿"), single("☿"), single("☤"), single("⌘"),
            //
            single("※"), single("Σ"), single("卐"), single("❖"), single("½"),
            //
            single("⋌"), single("ン"), single("ッ"), single("ツ"), single("シ"),
            //
            single("ヅ"), single("Ü"), single("ϡ"), single("⚘"), single("☐"),
            //
            single("☥"), single("☩"), single("☧"), single("☬"), single("♆"),
            //
            single("༄"), single("இ"), single("ண"), single("Ψ"), single("Ұ"),
            //
            single("￠"), single("лв"), single("円"), single("र"), single("Kč"),
            //
            single("zł"),
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
