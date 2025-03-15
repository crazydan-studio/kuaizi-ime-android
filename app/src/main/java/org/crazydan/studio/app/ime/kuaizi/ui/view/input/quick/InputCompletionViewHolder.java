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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;

/**
 * {@link InputCompletion} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputCompletionViewHolder extends InputQuickViewHolder {
    private final ViewGroup inputsView;

    public InputCompletionViewHolder(@NonNull View itemView) {
        super(itemView);

        this.inputsView = itemView.findViewById(R.id.inputs);
    }

    @Override
    public void bind(Object data) {
        InputCompletion.ViewData completion = (InputCompletion.ViewData) data;

        whenViewReady(this.inputsView, (view) -> {
            view.removeAllViews();

            completion.inputs.forEach((input) -> createChildView(view, input));
        });
    }

    private void createChildView(ViewGroup view, InputCompletion.CharInputViewData data) {
        // Note：若设置了 root，则返回值也为该 root，
        // 这里需直接处理 R.layout.char_input_view 视图，故设置为 null
        View childView = LayoutInflater.from(getContext()).inflate(R.layout.input_completion_input_char_view, null);

        // Note：在 layout xml 中设置的布局不会生效，需显式设置
        ViewGroup.MarginLayoutParams layoutParams
                = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                   ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.rightMargin = //
        layoutParams.leftMargin = //
                (int) (ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width) / 2f);

        childView.setLayoutParams(layoutParams);

        InputCompletionCharInputViewHolder childViewHolder = new InputCompletionCharInputViewHolder(childView);
        childViewHolder.bind(data);

        view.addView(childView);
    }
}
