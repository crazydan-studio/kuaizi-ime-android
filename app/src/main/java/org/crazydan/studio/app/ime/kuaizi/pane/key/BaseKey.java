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

package org.crazydan.studio.app.ime.kuaizi.pane.key;

import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public abstract class BaseKey<K extends BaseKey<?>> implements Key<K> {
    private String label;
    private Level level = Level.level_0;
    private Integer iconResId;
    private Integer labelDimensionId;

    private boolean disabled;
    private Color color = Color.none();

    @Override
    public boolean isSpace() {
        return false;
    }

    @Override
    public boolean isLatin() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isSymbol() {
        return false;
    }

    @Override
    public boolean isEmoji() {
        return false;
    }

    @Override
    public boolean isMathOp() {
        return false;
    }

    @Override
    public boolean isDisabled() {
        return this.disabled;
    }

    @Override
    public K setDisabled(boolean disabled) {
        this.disabled = disabled;
        return (K) this;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public K setLabel(String label) {
        this.label = label;
        return (K) this;
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    @Override
    public K setLevel(Level level) {
        this.level = level;
        return (K) this;
    }

    @Override
    public Integer getIconResId() {
        return this.iconResId;
    }

    @Override
    public K setIconResId(Integer iconResId) {
        this.iconResId = iconResId;
        return (K) this;
    }

    @Override
    public Integer getLabelDimensionId() {
        return this.labelDimensionId;
    }

    @Override
    public K setLabelDimensionId(Integer labelDimensionId) {
        this.labelDimensionId = labelDimensionId;
        return (K) this;
    }

    @Override
    public Color getColor() {
        return this.color;
    }

    @Override
    public K setColor(Color color) {
        this.color = color == null ? Color.none() : color;
        return (K) this;
    }

    @NonNull
    @Override
    public String toString() {
        return this.label + "(" + getText() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseKey<?> that = (BaseKey<?>) o;
        return Objects.equals(this.getIconResId(), that.getIconResId())
               && Objects.equals(this.getLabelDimensionId(),
                                 that.getLabelDimensionId())
               && Objects.equals(this.getLabel(), that.getLabel())
               && Objects.equals(this.getText(), that.getText())
               && Objects.equals(this.getLevel(), that.getLevel())
               && this.disabled == that.disabled
               && Objects.equals(this.color.fg, that.color.fg)
               && Objects.equals(this.color.bg, that.color.bg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getIconResId(),
                            this.getLabelDimensionId(),
                            this.getLabel(),
                            this.getText(),
                            this.getLevel(),
                            this.disabled,
                            this.color.fg,
                            this.color.bg);
    }
}
