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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinWord {
    private static final Map<String, String> replacements = new HashMap<>();

    static {
        String[][] pairs = new String[][] {
                new String[] { "a", "ā", "á", "ǎ", "à" },
                new String[] { "o", "ō", "ó", "ǒ", "ò" },
                new String[] { "e", "ē", "é", "ě", "è" },
                new String[] { "i", "ī", "í", "ǐ", "ì" },
                new String[] { "u", "ū", "ú", "ǔ", "ù" },
                new String[] { "ü", "ǖ", "ǘ", "ǚ", "ǜ" },
                };
        for (String[] pair : pairs) {
            for (int i = 1; i < pair.length; i++) {
                replacements.put(pair[i], pair[0]);
            }
        }
    }

    private String word;
    private String pinyin;
    private String explanation;

    private String[] pinyinChars;

    public String getWord() {
        return this.word;
    }

    public String getPinyin() {
        if (this.pinyin.contains("ɡ")) {
            // 将中文 ɡ 替换为英文字符 g
            this.pinyin = this.pinyin.replaceAll("ɡ", "g");
        }

        // 纠正: http://www.zd9999.com/zi/index.htm
        switch (this.pinyin) {
            case "èr发fā":
                this.pinyin = "èr";
                break;
            case "bei0":
                this.pinyin = "bei";
                break;
            case "diu1":
                this.pinyin = "diu";
                break;
            case "lou0":
                this.pinyin = "lou";
                break;
        }
        return this.pinyin;
    }

    public boolean isInvalid() {
        return this.explanation.contains("韩文吏读字")
               || this.explanation.contains("日本和字")
               || this.explanation.contains("韩用汉字")
               || this.explanation.contains("韩国汉字")
               || this.pinyin.contains("eo")
               || this.pinyin.equals("dem")
               || this.pinyin.equals("uu")
               || this.pinyin.equals("zo")
               || this.pinyin.equals("yen")
               || this.pinyin.equals("ra")
               || this.pinyin.equals("ɡeu")
               || this.pinyin.equals("ɡo")
               || this.pinyin.contains("lǜ亇ma")
               || this.pinyin.contains("gōngfēn")
               || this.pinyin.contains("gǒngli")
               || this.pinyin.contains("bàike")
               || this.pinyin.contains("haoke");
    }

    public String[] getPinyinChars() {
        if (this.pinyinChars == null) {
            this.pinyinChars = new String[getPinyin().length()];

            for (int i = 0; i < getPinyin().length(); i++) {
                String ch = getPinyin().charAt(i) + "";
                this.pinyinChars[i] = replacements.getOrDefault(ch, ch);
            }
        }
        return this.pinyinChars;
    }
}
