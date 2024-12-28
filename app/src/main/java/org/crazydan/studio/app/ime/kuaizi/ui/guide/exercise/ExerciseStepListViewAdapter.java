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
