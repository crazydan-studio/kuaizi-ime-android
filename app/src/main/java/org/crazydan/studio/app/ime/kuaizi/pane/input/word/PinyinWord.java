/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane.input.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;

/**
 * 拼音字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class PinyinWord extends InputWord {
    private final static Builder builder = new Builder();

    /** 读音 */
    public final Spell spell;
    /** 部首 */
    public final Radical radical;
    /** 字的变体 */
    public final String variant;
    /** 字形 id */
    public final Integer glyphId;
    /** 是否繁体 */
    public final boolean traditional;

    /** 构建 {@link PinyinWord} */
    public static PinyinWord build(Consumer<Builder> c) {
        return Builder.build(builder, c);
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

    protected PinyinWord(Builder builder) {
        super(builder);

        this.spell = builder.spell;
        this.radical = builder.radical;
        this.variant = builder.variant;
        this.glyphId = builder.glyphId;
        this.traditional = builder.traditional;
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

    /** 读音使用类型 */
    public enum SpellUsedMode {
        /** 替代 {@link InputWord} */
        replacing,
        /** 跟随 {@link InputWord} */
        following,
    }

    /** 拼音字的部首 */
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

    /** 拼音字的过滤器 */
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

        public void addSpellByKey(CtrlKey key) {
            // Note: 拼音过滤为单选项，且需重置部首过滤
            clear();

            CtrlKey.Option<?> option = key.option;
            PinyinWord.Spell value = (PinyinWord.Spell) option.value();
            if (!key.disabled) {
                this.spells.add(value);
            }
        }

        public void addRadicalByKey(CtrlKey key) {
            CtrlKey.Option<?> option = key.option;
            PinyinWord.Radical value = (PinyinWord.Radical) option.value();

            if (key.disabled) {
                this.radicals.remove(value);
            } else {
                this.radicals.add(value);
            }
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

    /** 拼音字的读音 */
    public static class Spell {
        /** 值 */
        public final String value;
        /** 读音 id */
        public final Integer id;
        /** 字母组合 id */
        public final Integer charsId;

        public Spell(String value) {
            this(value, null, null);
        }

        public Spell(Spell spell) {
            this(spell.value, spell.id, spell.charsId);
        }

        public Spell(String value, Integer id, Integer charsId) {
            this.value = value;
            this.id = id;
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
            return Objects.equals(this.id, that.id) && Objects.equals(this.value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.value);
        }
    }

    /** {@link PinyinWord} 的构建器 */
    public static class Builder extends InputWord.Builder<Builder, PinyinWord> {
        private Spell spell;
        private Radical radical;
        private String variant;
        private Integer glyphId;
        private boolean traditional;

        // ===================== Start: 构建函数 ===================

        @Override
        protected PinyinWord build() {
            return new PinyinWord(this);
        }

        @Override
        protected void reset() {
            super.reset();

            this.spell = null;
            this.radical = null;
            this.variant = null;
            this.glyphId = null;
            this.traditional = false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                                this.spell,
                                this.radical,
                                this.variant,
                                this.glyphId,
                                this.traditional);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see PinyinWord#spell */
        public Builder spell(Spell spell) {
            this.spell = spell;
            return this;
        }

        /** @see PinyinWord#radical */
        public Builder radical(Radical radical) {
            this.radical = radical;
            return this;
        }

        /** @see PinyinWord#variant */
        public Builder variant(String variant) {
            this.variant = variant;
            return this;
        }

        /** @see PinyinWord#glyphId */
        public Builder glyphId(Integer glyphId) {
            this.glyphId = glyphId;
            return this;
        }

        /** @see PinyinWord#traditional */
        public Builder traditional(boolean traditional) {
            this.traditional = traditional;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
