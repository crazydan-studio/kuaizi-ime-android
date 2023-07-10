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
                new String[] { "e", "ē", "é", "ě", "è", "ê" },
                new String[] { "i", "ī", "í", "ǐ", "ì" },
                new String[] { "u", "ū", "ú", "ǔ", "ù" },
                new String[] { "ü", "ǖ", "ǘ", "ǚ", "ǜ" },
                new String[] { "n", "ń", "ň", "ǹ" },
                new String[] { "m", "m̄", "m̀" },
                new String[] { "e", "ê̄", "ê̌" },
                };
        for (String[] pair : pairs) {
            for (int i = 1; i < pair.length; i++) {
                replacements.put(pair[i], pair[0]);
            }
        }
    }

    private final String word;
    private final String pinyin;

    private boolean traditional;
    private int level;
    private float weight;
    private String[] pinyinChars;

    public PinyinWord(String word, String pinyin) {
        this.word = word;
        this.pinyin = pinyin;
    }

    public String getWord() {
        return this.word;
    }

    public String getPinyin() {
        return this.pinyin;
    }

    public boolean isValid() {
        return true;
    }

    public String[] getPinyinChars() {
        if (this.pinyinChars == null) {
            if ("m̀".equals(getPinyin()) || "m̄".equals(getPinyin()) //
                || "ê̄".equals(getPinyin()) || "ê̌".equals(getPinyin())) {
                this.pinyinChars = new String[] { replacements.getOrDefault(getPinyin(), getPinyin()) };
            } else {
                this.pinyinChars = new String[getPinyin().length()];

                for (int i = 0; i < getPinyin().length(); i++) {
                    String ch = getPinyin().charAt(i) + "";
                    this.pinyinChars[i] = replacements.getOrDefault(ch, ch);
                }
            }
        }
        return this.pinyinChars;
    }

    public boolean isTraditional() {
        return this.traditional;
    }

    public void setTraditional(boolean traditional) {
        this.traditional = traditional;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getWeight() {
        return this.weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
