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
import java.util.List;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.core.InputWord;

/**
 * 拼音的{@link InputWord 输入字}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class PinyinWord extends InputWord {
    /** 字形 id */
    private final Integer glyphId;
    /** 部首 */
    private final Radical radical;
    /** 是否繁体 */
    private final boolean traditional;

    public PinyinWord(Integer id, String value, Spell spell) {
        this(id, value, spell, null, null, false);
    }

    public PinyinWord(
            Integer id, String value, Spell spell, //
            Integer glyphId, Radical radical, boolean traditional
    ) {
        super(id, value, spell);

        this.glyphId = glyphId;
        this.radical = radical;
        this.traditional = traditional;
    }

    public static PinyinWord none() {
        return new PinyinWord(null, null, new Spell(null, null, null));
    }

    public static PinyinWord from(InputWord word) {
        if (word instanceof PinyinWord) {
            PinyinWord w = (PinyinWord) word;

            PinyinWord copied = new PinyinWord(w.getId(), w.getValue(), w.getSpell(), //
                                               w.getGlyphId(), w.getRadical(), w.isTraditional());
            copied.setVariant(w.getVariant());
            copied.setWeight(w.getWeight());

            return copied;
        }
        return new PinyinWord(word.getId(), word.getValue(), word.getSpell());
    }

    public Integer getGlyphId() {
        return this.glyphId;
    }

    public Integer getCharsId() {
        return getSpell().charsId;
    }

    public Radical getRadical() {
        return this.radical;
    }

    public boolean isTraditional() {
        return this.traditional;
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
            if (!(word instanceof PinyinWord)) {
                return false;
            }

            return (this.spells.isEmpty() //
                    || this.spells.contains(word.getSpell())) //
                   && (this.radicals.isEmpty() //
                       || this.radicals.contains(((PinyinWord) word).getRadical()));
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
