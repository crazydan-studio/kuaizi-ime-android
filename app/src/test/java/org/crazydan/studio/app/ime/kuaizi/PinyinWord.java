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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinWord {
    private final String word;
    private final Set<String> pinyins = new LinkedHashSet<>();

    private boolean traditional;
    private final Set<String> variants = new LinkedHashSet<>();

    private int level;
    private float weight;
    /** 笔画数 */
    private int stroke;
    /** 笔顺 */
    private String strokeOrder;
    /** 部首 */
    private final Set<String> radicals = new LinkedHashSet<>();
    /** 字型结构 */
    private String struct;

    public PinyinWord(String word) {
        this.word = word;
    }

    public String getWord() {
        return this.word;
    }

    public PinyinWord addPinyin(String... pinyins) {
        this.pinyins.addAll(Arrays.asList(pinyins));
        return this;
    }

    public Set<String> getPinyins() {
        return this.pinyins;
    }

    public boolean isValid() {
        return !this.word.isEmpty() && !this.pinyins.isEmpty();
    }

    public boolean isTraditional() {
        return this.traditional;
    }

    public void setTraditional(boolean traditional) {
        this.traditional = traditional;
    }

    public void addVariant(String... variants) {
        this.variants.addAll(Arrays.asList(variants));
    }

    public Set<String> getVariants() {
        return this.variants;
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

    public int getStroke() {
        return this.stroke;
    }

    public void setStroke(int stroke) {
        this.stroke = stroke;
    }

    public String getStrokeOrder() {
        return this.strokeOrder;
    }

    public void setStrokeOrder(String strokeOrder) {
        this.strokeOrder = strokeOrder;
    }

    public void addRadical(String... radicals) {
        this.radicals.addAll(Arrays.asList(radicals));
    }

    public Set<String> getRadicals() {
        return this.radicals;
    }

    public String getStruct() {
        return this.struct;
    }

    public void setStruct(String struct) {
        this.struct = struct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PinyinWord that = (PinyinWord) o;
        return this.word.equals(that.word) && this.pinyins.equals(that.pinyins);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.word, this.pinyins);
    }
}
