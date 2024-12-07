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

package org.crazydan.studio.app.ime.kuaizi.pane.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;

/**
 * {@link Input 输入}补全
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionInput extends BaseInput<GapInput> {
    /** 补全开始的起始位置 */
    public final int startIndex;
    public final List<CharInput> inputs = new ArrayList<>();

    public CompletionInput(int startIndex) {
        this.startIndex = startIndex;
    }

    public void add(CharInput input) {
        this.inputs.add(input);
    }

    @NonNull
    @Override
    public String toString() {
        return this.startIndex + ": " + this.inputs.stream().map(Objects::toString).collect(Collectors.joining(""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        // Note：为避免补全位置发生变动而导致 CompletionView 视图发生抖动，
        // 故而，不检查补全位置是否相等
        CompletionInput that = (CompletionInput) o;
        return this.inputs.equals(that.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.inputs);
    }
}
