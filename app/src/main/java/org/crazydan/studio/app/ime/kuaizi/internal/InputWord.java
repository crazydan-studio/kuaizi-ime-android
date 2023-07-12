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

package org.crazydan.studio.app.ime.kuaizi.internal;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;

/**
 * {@link Input 输入}候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputWord {
    private String value;
    private String notation;

    private boolean traditional;
    private int level;
    private float weight;
    /** 笔画数 */
    private int strokes;

    public static InputWord from(PinyinCharTree.Word cw) {
        InputWord iw = new InputWord(cw.getValue(), cw.getNotation());
        iw.setTraditional(cw.isTraditional());
        iw.setLevel(cw.getLevel());
        iw.setWeight(cw.getWeight());
        iw.setStrokes(cw.getStrokes());

        return iw;
    }

    public InputWord(String value, String notation) {
        this.value = value;
        this.notation = notation;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNotation() {
        return this.notation;
    }

    public void setNotation(String notation) {
        this.notation = notation;
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

    public int getStrokes() {
        return this.strokes;
    }

    public void setStrokes(int strokes) {
        this.strokes = strokes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InputWord that = (InputWord) o;
        return this.value.equals(that.value) && this.notation.equals(that.notation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.notation);
    }
}
