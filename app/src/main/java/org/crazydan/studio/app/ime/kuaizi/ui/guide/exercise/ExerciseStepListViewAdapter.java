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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.KeyImageRender;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepListViewAdapter extends RecyclerViewAdapter<ExerciseStep.ViewData, ExerciseStepViewHolder> {
    private final static int VIEW_TYPE_NORMAL = 0;
    private final static int VIEW_TYPE_LAST = 1;

    private KeyImageRender keyImageRender;

    public ExerciseStepListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    public void setKeyImageRender(KeyImageRender keyImageRender) {
        this.keyImageRender = keyImageRender;
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseStepViewHolder holder, int position) {
        ExerciseStep.ViewData item = getItem(position);

        holder.bind(this.keyImageRender, item, position);
    }

    @NonNull
    @Override
    public ExerciseStepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LAST) {
            View view = inflateItemView(parent, R.layout.guide_exercise_step_last_view);
            return new ExerciseStepLastViewHolder(view);
        }

        View view = inflateItemView(parent, R.layout.guide_exercise_step_normal_view);
        return new ExerciseStepViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        ExerciseStep.ViewData item = getItem(position);

        if (item.last) {
            return VIEW_TYPE_LAST;
        }
        return VIEW_TYPE_NORMAL;
    }

    /** 指定位置是否为最后一步 */
    public boolean isLastStep(int position) {
        if (position < getItemCount()) {
            return getItem(position).last;
        }
        return false;
    }
}
