/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputboardView;

/**
 * {@link InputboardView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class InputListViewAdapter extends RecyclerViewAdapter<InputViewData, InputViewHolder> {
    private static final int VIEW_TYPE_CHAR_INPUT = 0;
    private static final int VIEW_TYPE_GAP_INPUT = 1;
    private static final int VIEW_TYPE_CHAR_MATH_EXPR_INPUT = 2;

    private boolean canBeSelected;

    public InputListViewAdapter() {
        // Note：在 Gap 添加空格后，涉及对与其相邻的输入视图的更新，
        // 其判断逻辑较复杂且容易遗漏更新，故而，始终对列表视图做全量更新
        super(ItemUpdatePolicy.full);
    }

    public void updateItems(List<InputViewData> newItems, boolean canBeSelected) {
        this.canBeSelected = canBeSelected;

        super.updateItems(newItems);
    }

    @Override
    public void onBindViewHolder(@NonNull InputViewHolder holder, int position) {
        InputViewData item = getItem(position);
        boolean selected = this.canBeSelected && item.selected;

        if (item.input.isMathExpr()) {
            ((MathExprInputViewHolder) holder).bind(item, selected);
        } else if (item.input.isGap()) {
            ((GapInputViewHolder) holder).bind(item, selected);
        } else {
            ((CharInputViewHolder) holder).bind(item, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        InputViewData item = getItem(position);

        if (item.input.isMathExpr()) {
            return VIEW_TYPE_CHAR_MATH_EXPR_INPUT;
        } else if (item.input.isGap()) {
            return VIEW_TYPE_GAP_INPUT;
        }
        return VIEW_TYPE_CHAR_INPUT;
    }

    @NonNull
    @Override
    public InputViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_CHAR_INPUT: {
                View view = inflateItemView(parent, R.layout.input_char_view);
                return new CharInputViewHolder(view);
            }
            case VIEW_TYPE_CHAR_MATH_EXPR_INPUT: {
                View view = inflateItemView(parent, R.layout.input_math_expr_view);
                return new MathExprInputViewHolder(view);
            }
            default: {
                View view = inflateItemView(parent, R.layout.input_gap_view);
                return new GapInputViewHolder(view);
            }
        }
    }
}
