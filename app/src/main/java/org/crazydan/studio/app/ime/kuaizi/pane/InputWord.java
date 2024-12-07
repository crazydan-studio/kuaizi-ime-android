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

package org.crazydan.studio.app.ime.kuaizi.pane;

import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;

/**
 * {@link Input 输入}对应的{@link Input#getWord() 字}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputWord {
    /** 对象 id，一般对应持久化的主键值 */
    private final Integer id;
    /** 值 */
    private final String value;
    /** 读音 */
    private final Spell spell;

    /** 字的变体 */
    private String variant;
    /** 字的权重 */
    private int weight;

    public InputWord(Integer id, String value) {
        this(id, value, (String) null);
    }

    public InputWord(Integer id, String value, String spell) {
        this(id, value, new Spell(spell != null ? spell : ""));
    }

    public InputWord(Integer id, String value, Spell spell) {
        this.id = id;
        this.value = value;
        this.spell = new Spell(spell);
    }

    public Integer getId() {
        return this.id;
    }

    public String getValue() {
        return this.value;
    }

    public Spell getSpell() {
        return this.spell;
    }

    public boolean hasSpell() {
        return !CharUtils.isBlank(this.spell.value);
    }

    public String getVariant() {
        return this.variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public boolean hasVariant() {
        return this.variant != null;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @NonNull
    @Override
    public String toString() {
        if (!hasSpell()) {
            return this.value;
        }
        return this.value + '(' + this.spell.value + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        // Note: 不处理与视图更细相关的变更判断，如有必要则在视图对象中处理
        InputWord that = (InputWord) o;
        return this.value.equals(that.value) && Objects.equals(this.spell, that.spell);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.spell);
    }

    /** 读音 */
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
            return Objects.equals(this.id, that.id) && this.value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.value);
        }
    }

    /** 读音使用类型 */
    public enum SpellUsedType {
        /** 替代 {@link InputWord} */
        replacing,
        /** 跟随 {@link InputWord} */
        following,
    }
}
