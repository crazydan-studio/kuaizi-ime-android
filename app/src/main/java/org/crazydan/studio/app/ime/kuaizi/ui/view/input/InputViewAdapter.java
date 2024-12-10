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

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.input.InputViewData;

/**
 * {@link InputView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class InputViewAdapter extends RecyclerViewAdapter<InputView<?>> {
    private static final int VIEW_TYPE_CHAR_INPUT = 0;
    private static final int VIEW_TYPE_GAP_INPUT = 1;
    private static final int VIEW_TYPE_CHAR_MATH_EXPR_INPUT = 2;

    private List<InputViewData> dataList;
    private boolean canBeSelected;

    /** 更新输入列表 */
    public void updateDataList(List<InputViewData> dataList, boolean canBeSelected) {
        this.dataList = dataList;
        this.canBeSelected = canBeSelected;

        // Note：在 Gap 添加空格后，涉及对与其相邻的输入视图的更新，
        // 其判断逻辑较复杂且容易遗漏更新，故而，直接对列表视图做全量更新
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.dataList == null ? 0 : this.dataList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull InputView<?> view, int position) {
        InputViewData data = this.dataList.get(position);
        boolean selected = this.canBeSelected && data.selected;

        if (data.input.isMathExpr()) {
            ((MathExprInputView) view).bind(data, selected);
        } else if (data.input.isGap()) {
            ((GapInputView) view).bind(data, selected);
        } else {
            ((CharInputView) view).bind(data, selected);
        }
    }

    @Override
    public int getItemViewType(int position) {
        InputViewData data = this.dataList.get(position);

        if (data.input.isMathExpr()) {
            return VIEW_TYPE_CHAR_MATH_EXPR_INPUT;
        } else if (data.input.isGap()) {
            return VIEW_TYPE_GAP_INPUT;
        } else {
            return VIEW_TYPE_CHAR_INPUT;
        }
    }

    @NonNull
    @Override
    public InputView<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CHAR_INPUT) {
            return new CharInputView(inflateItemView(parent, R.layout.input_char_view));
        } else if (viewType == VIEW_TYPE_CHAR_MATH_EXPR_INPUT) {
            return new MathExprInputView(inflateItemView(parent, R.layout.input_math_expr_view));
        } else {
            return new GapInputView(inflateItemView(parent, R.layout.input_gap_view));
        }
    }
}
