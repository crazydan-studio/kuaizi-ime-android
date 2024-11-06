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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.core.InputWord;

/**
 * 拼音{@link InputWord 字}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class PinyinInputWord extends InputWord {
    /** 字 id */
    private final String wordId;
    /** 拼音 */
    private final Spell spell;
    /** 部首 */
    private final Radical radical;

    /** 是否繁体 */
    private final boolean traditional;
    /** 笔画顺序 */
    private final Map<String, Integer> strokes;

    public PinyinInputWord(
            String uid, String value, String wordId, //
            Spell spell, Radical radical, //
            boolean traditional, String strokeOrder
    ) {
        super(uid, value, spell.value);

        this.wordId = wordId;
        this.spell = spell;
        this.radical = radical;
        this.traditional = traditional;
        this.strokes = new HashMap<>();

        if (strokeOrder != null) {
            for (int i = 0; i < strokeOrder.length(); i++) {
                String stroke = strokeOrder.charAt(i) + "";
                int count = this.strokes.getOrDefault(stroke, 0);
                this.strokes.put(stroke, count + 1);
            }
        }
    }

    public static PinyinInputWord from(InputWord word) {
        return new PinyinInputWord(word.getUid(),
                                   word.getValue(),
                                   word.getUid(),
                                   new Spell(-1, word.getNotation(), "-1"),
                                   null,
                                   false,
                                   null);
    }

    public static String[] getStrokeNames() {
        return new String[] { "一", "丨", "丿", "㇏", "\uD840\uDCCB" /* 𠃋 */ };
    }

    public static String getStrokeCode(String stroke) {
        String code = null;
        switch (stroke) {
            case "一":
                code = "1";
                break;
            case "丨":
                code = "2";
                break;
            case "丿":
                code = "3";
                break;
            case "㇏":
                code = "4";
                break;
            // 𠃋
            case "\uD840\uDCCB":
                code = "5";
                break;
        }
        return code;
    }

    @Override
    public PinyinInputWord copy() {
        PinyinInputWord copied = new PinyinInputWord(getUid(),
                                                     getValue(),
                                                     getWordId(),
                                                     getSpell(),
                                                     getRadical(),
                                                     isTraditional(),
                                                     null);
        copy(copied, this);

        copied.strokes.putAll(this.strokes);

        return copied;
    }

    public String getWordId() {
        return this.wordId;
    }

    public String getCharsId() {
        return getSpell().charsId;
    }

    public Spell getSpell() {
        return this.spell;
    }

    public Radical getRadical() {
        return this.radical;
    }

    public boolean isTraditional() {
        return this.traditional;
    }

    public Map<String, Integer> getStrokes() {
        return this.strokes;
    }

    public static class Spell {
        public final int id;
        public final String value;
        /** 字母组合 id */
        public final String charsId;

        public Spell(int id, String value, String charsId) {
            this.id = id;
            this.value = value;
            this.charsId = charsId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Spell that = (Spell) o;
            return this.id == that.id && this.value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.value);
        }
    }

    public static class Radical {
        public final String value;
        public final int strokeCount;

        public Radical(String value, int strokeCount) {
            this.value = value;
            this.strokeCount = strokeCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Radical that = (Radical) o;
            return this.value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }

    public static class Filter {
        public final List<Spell> spells;
        public final List<Radical> radicals;

        public Filter() {
            this(new ArrayList<>(), new ArrayList<>());
        }

        public Filter(Filter filter) {
            this(new ArrayList<>(filter.spells), new ArrayList<>(filter.radicals));
        }

        public Filter(List<Spell> spells, List<Radical> radicals) {
            this.spells = spells;
            this.radicals = radicals;
        }

        public void clear() {
            this.spells.clear();
            this.radicals.clear();
        }

        public boolean isEmpty() {
            return this.spells.isEmpty() //
                   && this.radicals.isEmpty();
        }

        public boolean matched(InputWord word) {
            if (!(word instanceof PinyinInputWord)) {
                return false;
            }

            return (this.spells.isEmpty() //
                    || this.spells.contains(((PinyinInputWord) word).getSpell())) //
                   && (this.radicals.isEmpty() //
                       || this.radicals.contains(((PinyinInputWord) word).getRadical()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Filter that = (Filter) o;
            return this.spells.equals(that.spells) && this.radicals.equals(that.radicals);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.spells, this.radicals);
        }
    }
}
