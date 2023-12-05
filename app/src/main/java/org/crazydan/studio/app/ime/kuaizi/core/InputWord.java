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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * {@link Input 输入}候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputWord {
    /** 唯一 id，对应持久化标识 */
    private final String uid;
    private final String value;
    private final String notation;

    /** 是否已确认 */
    private boolean confirmed;
    /** 候选字来源 */
    private Source source = Source.single;
    /** 字的变体 */
    private String variant;

    public InputWord(String uid, String value) {
        this(uid, value, null);
    }

    public InputWord(String uid, String value, String notation) {
        this.uid = uid;
        this.value = value;
        this.notation = notation;
    }

    protected void copy(InputWord target, InputWord source) {
        target.setSource(source.getSource());
        target.setConfirmed(source.isConfirmed());
        target.setVariant(source.getVariant());
    }

    public InputWord copy() {
        InputWord copied = new InputWord(getUid(), getValue(), getNotation());
        copy(copied, this);

        return copied;
    }

    public String getUid() {
        return this.uid;
    }

    public boolean isConfirmed() {
        return this.confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Source getSource() {
        return this.source != null ? this.source : Source.single;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public boolean isFromPhrase() {
        return getSource() == Source.phrase;
    }

    public String getValue() {
        return this.value;
    }

    public String getNotation() {
        return this.notation;
    }

    public boolean hasNotation() {
        return this.notation != null;
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

    @NonNull
    @Override
    public String toString() {
        if (!hasNotation()) {
            return this.value;
        }
        return this.value + '(' + this.notation + ')';
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
        return this.value.equals(that.value) && Objects.equals(this.notation, that.notation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.notation);
    }

    /** 标注类型 */
    public enum NotationType {
        /** 替代 {@link InputWord} */
        replacing,
        /** 跟随 {@link InputWord} */
        following,
    }

    /** 来源 */
    public enum Source {
        /** 单字 */
        single,
        /** 短语中的字 */
        phrase,
    }
}
