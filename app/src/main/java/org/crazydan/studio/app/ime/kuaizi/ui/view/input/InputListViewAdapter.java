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

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputListView;

/**
 * {@link InputListView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class InputListViewAdapter extends RecyclerViewAdapter<InputViewData, InputViewHolder> {
    private static final int VIEW_TYPE_CHAR_INPUT = 0;
    private static final int VIEW_TYPE_GAP_INPUT = 1;
    private static final int VIEW_TYPE_SPACE_INPUT = 2;
    private static final int VIEW_TYPE_MATH_EXPR_INPUT = 3;

    public InputListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    @Override
    public void onBindViewHolder(@NonNull InputViewHolder holder, int position) {
        InputViewData item = getItem(position);
        holder.bind(item);
    }

    @Override
    public int getItemViewType(int position) {
        InputViewData item = getItem(position);

        switch (item.type) {
            case Gap: {
                return VIEW_TYPE_GAP_INPUT;
            }
            case Space: {
                return VIEW_TYPE_SPACE_INPUT;
            }
            case MathExpr: {
                return VIEW_TYPE_MATH_EXPR_INPUT;
            }
        }
        return VIEW_TYPE_CHAR_INPUT;
    }

    @NonNull
    @Override
    public InputViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_GAP_INPUT: {
                View view = inflateItemView(parent, R.layout.input_gap_view);
                return new GapInputViewHolder(view);
            }
            case VIEW_TYPE_SPACE_INPUT: {
                View view = inflateItemView(parent, R.layout.input_space_view);
                return new SpaceInputViewHolder(view);
            }
            case VIEW_TYPE_MATH_EXPR_INPUT: {
                View view = inflateItemView(parent, R.layout.input_math_expr_view);
                return new MathExprInputViewHolder(view);
            }
            default: {
                View view = inflateItemView(parent, R.layout.input_char_view);
                return new CharInputViewHolder(view);
            }
        }
    }
}
