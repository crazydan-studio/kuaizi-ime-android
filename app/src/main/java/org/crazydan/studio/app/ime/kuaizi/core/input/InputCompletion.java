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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.Input;

import static org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData.getInputTextAndSpell;

/**
 * {@link InputCompletions 输入补全}的内容
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputCompletion {
    /** 补全的输入列表 */
    public final List<CharInput> inputs = new ArrayList<>();

    @Override
    public String toString() {
        return this.inputs.stream().map(Objects::toString).collect(Collectors.joining(" "));
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ViewData that = (ViewData) o;
            return Objects.equals(this.inputs, that.inputs);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.inputs);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CharInputViewData that = (CharInputViewData) o;
            return Objects.equals(this.text, that.text) && Objects.equals(this.spell, that.spell);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.text, this.spell);
        }
    }
}
