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
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.Input;

import static org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData.getInputTextAndSpell;

/**
 * {@link Input} 的输入补全
 * <p/>
 * 用于自动补全正在输入的英文单词或中文句子等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputCompletion {
    /**
     * 开始应用补全的 {@link Input} 所在的位置
     * <p/>
     * 在应用补全时，将从该位置开始，挨个替换输入列表中的输入
     */
    public final int startPosition;
    public final List<CharInput> inputs = new ArrayList<>();

    public InputCompletion(int startPosition) {
        this.startPosition = startPosition;
    }

    public void add(CharInput input) {
        this.inputs.add(input);
    }

    @Override
    public String toString() {
        return this.startPosition + ": " + this.inputs.stream().map(Objects::toString).collect(Collectors.joining(" "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InputCompletion that = (InputCompletion) o;
        return this.startPosition == that.startPosition && Objects.equals(this.inputs, that.inputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startPosition, this.inputs);
    }

    /** 输入补全的视图数据 */
    public static class ViewData {
        public final List<CharInputViewData> inputs;

        ViewData(List<CharInputViewData> inputs) {this.inputs = inputs;}

        public static ViewData create(InputCompletion completion, Input.Option option) {
            List<CharInputViewData> inputs = completion.inputs.stream().map((input) -> {
                String[] textAndSpell = getInputTextAndSpell(input, option);

                return new CharInputViewData(textAndSpell[0], textAndSpell[1]);
            }).collect(Collectors.toList());

            return new ViewData(inputs);
        }
    }

    /** 输入补全的 输入视图数据 */
    public static class CharInputViewData {
        public final String text;
        public final String spell;

        CharInputViewData(String text, String spell) {
            this.text = text;
            this.spell = spell;
        }
    }
}
