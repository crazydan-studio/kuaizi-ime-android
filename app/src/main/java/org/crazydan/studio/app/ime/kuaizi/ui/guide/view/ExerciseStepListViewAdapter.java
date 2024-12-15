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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import java.util.List;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseStep;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepListViewAdapter extends RecyclerViewAdapter<ExerciseStepView> {
    private final static int VIEW_TYPE_NORMAL = 0;
    private final static int VIEW_TYPE_FINAL = 1;

    private List<ExerciseStep> data;

    public void bind(List<ExerciseStep> data) {
        this.data = data;

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.data != null ? this.data.size() : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseStepView view, int position) {
        ExerciseStep step = this.data.get(position);

        view.bind(step, position);
    }

    @NonNull
    @Override
    public ExerciseStepView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FINAL) {
            return new ExerciseStepFinalView(inflateItemView(parent, R.layout.guide_exercise_step_final_view));
        }
        return new ExerciseStepView(inflateItemView(parent, R.layout.guide_exercise_step_normal_view));
    }

    @Override
    public int getItemViewType(int position) {
        ExerciseStep step = this.data.get(position);

        if (step instanceof ExerciseStep.Final) {
            return VIEW_TYPE_FINAL;
        }
        return VIEW_TYPE_NORMAL;
    }

    public boolean isFinalStep(int position) {
        if (position < getItemCount()) {
            return this.data.get(position) instanceof ExerciseStep.Final;
        }
        return false;
    }
}
