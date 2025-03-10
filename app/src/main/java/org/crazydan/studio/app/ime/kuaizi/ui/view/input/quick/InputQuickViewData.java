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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-10
 */
public class InputQuickViewData {
    public enum Type {
        /** 剪贴板输入 */
        input_clip,
        /** 补全输入 */
        input_completion,
        /** 列表占位，用于添加至列表尾部，从而可使列表滚动超出实际的数据范围，以方便就近选择 */
        input_placeholder,
    }

    public final Type type;
    public final Object data;

    public static List<InputQuickViewData> from(List<?> dataList) {
        if (dataList.isEmpty()) {
            return List.of();
        }

        List<InputQuickViewData> result = dataList.stream().map((data) -> {
            Type type = null;

            if (data instanceof InputCompletion.ViewData) {
                type = Type.input_completion;
            } else if (data instanceof InputClip) {
                type = Type.input_clip;
            }

            return new InputQuickViewData(type, data);
        }).collect(Collectors.toList());

        // 在数据量大于 1 时，始终向尾部添加一条占位数据，以支持将最后一条数据滚动到视图顶部
        if (result.size() > 1) {
            result.add(new InputQuickViewData(Type.input_placeholder, null));
        }

        return result;
    }

    InputQuickViewData(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InputQuickViewData that = (InputQuickViewData) o;
        return this.type == that.type && Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.data);
    }
}
