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

package org.crazydan.studio.app.ime.kuaizi.core.input.word;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;

/**
 * 拼音字
 * <p/>
 * Note: 若是要支持其他含读音的 {@link InputWord}，则可提取基类 <code>SpellableWord</code>
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

    protected PinyinWord(Builder builder) {
        super(builder);

        this.spell = builder.spell;
        this.radical = builder.radical;
        this.variant = builder.variant;
        this.glyphId = builder.glyphId;
        this.traditional = builder.traditional;
    }

    @Override
    public String toString() {
        return this.value + '(' + this.spell.value + ')';
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
        /** 值 */
        public final String value;
        /** 笔画数 */
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

    /** 拼音字的过滤器 */
    public static class Filter {
        /** 读音 */
        public final List<Spell> spells;
        /** 部首 */
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

            CtrlKey.Option<PinyinWord.Spell> option = key.option();
            PinyinWord.Spell value = option.value;
            if (!key.disabled) {
                this.spells.add(value);
            }
        }

        public void addRadicalByKey(CtrlKey key) {
            CtrlKey.Option<PinyinWord.Radical> option = key.option();
            PinyinWord.Radical value = option.value;

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
            return isEmpty(false);
        }

        public boolean isEmpty(boolean excludeSpells) {
            return (excludeSpells || this.spells.isEmpty()) //
                   && this.radicals.isEmpty();
        }

        public boolean matched(InputWord word) {
            if (!(word instanceof PinyinWord)) {
                return false;
            }

            return (this.spells.isEmpty() //
                    || this.spells.contains(((PinyinWord) word).spell)) //
                   && (this.radicals.isEmpty() //
                       || this.radicals.contains(((PinyinWord) word).radical));
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

    /** {@link PinyinWord} 的构建器 */
    public static class Builder extends InputWord.Builder<Builder, PinyinWord> {
        public static final Consumer<Builder> noop = (b) -> {};

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

        /** @see PinyinWord.Spell#value */
        public Builder spell(String spell) {
            this.spell = new Spell(spell);
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
