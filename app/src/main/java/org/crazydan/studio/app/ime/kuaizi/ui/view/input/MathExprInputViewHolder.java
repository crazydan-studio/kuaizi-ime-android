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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import java.util.List;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputListViewReadonly;

/**
 * {@link MathExprInput} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-15
 */
public class MathExprInputViewHolder extends InputViewHolder {
    private final InputListViewReadonly inputboardView;
    private final View markerView;

    public MathExprInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.inputboardView = itemView.findViewById(R.id.input_list);
        this.markerView = itemView.findViewById(R.id.marker);
    }

    @Override
    public void bind(InputViewData data) {
        super.bind(data);

        List<InputViewData> inputs = data.inputs;
        whenViewReady(this.inputboardView, (view) -> {
            view.setPositionInParent(data.position);
            view.update(inputs, false);
        });

        whenViewReady(this.markerView, (view) -> {
            setBackgroundColorByAttrId(view,
                                       data.selected
                                       ? R.attr.input_char_math_expr_border_highlight_color
                                       : R.attr.input_char_math_expr_border_color);
            ViewUtils.visible(view, !inputs.isEmpty());
        });
    }
}
