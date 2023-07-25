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

import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinWord;

/**
 * {@link Input 输入}候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-08
 */
public class InputWord {
    private final String value;
    private final String notation;
    /** 是否繁体 */
    private final boolean traditional;

    public InputWord(String value, String notation, boolean traditional) {
        this.value = value;
        this.notation = notation;
        this.traditional = traditional;
    }

    public static InputWord from(PinyinWord word) {
        return new InputWord(word.getValue(), word.getPinyin(), word.isTraditional());
    }

    public String getValue() {
        return this.value;
    }

    public String getNotation() {
        return this.notation;
    }

    public boolean isTraditional() {
        return this.traditional;
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
        return this.value.equals(that.value) && this.notation.equals(that.notation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.notation);
    }
}
