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
import org.crazydan.studio.app.ime.kuaizi.ui.guide.Exercise;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseListView;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewAdapter;

/**
 * {@link ExerciseListView} 数据适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListViewAdapter extends RecyclerViewAdapter<ExerciseView> {
    private final static int VIEW_TYPE_FREE_MODE = 0;
    private final static int VIEW_TYPE_NORMAL_MODE = 1;
    private final static int VIEW_TYPE_INTRODUCE_MODE = 2;

    private List<Exercise> data;

    public void bind(List<Exercise> data) {
        this.data = data;

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseView view, int position) {
        Exercise exercise = this.data.get(position);

        view.bind(exercise, position);
    }

    @NonNull
    @Override
    public ExerciseView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int resId;
        switch (viewType) {
            case VIEW_TYPE_FREE_MODE: {
                resId = R.layout.guide_exercise_mode_free_view;
                break;
            }
            case VIEW_TYPE_INTRODUCE_MODE: {
                resId = R.layout.guide_exercise_mode_introduce_view;
                return new ExerciseIntroduceView(inflateItemView(parent, resId));
            }
            default: {
                resId = R.layout.guide_exercise_mode_normal_view;
                break;
            }
        }
        return new ExerciseView(inflateItemView(parent, resId));
    }

    @Override
    public int getItemViewType(int position) {
        Exercise exercise = this.data.get(position);

        switch (exercise.mode) {
            case free:
                return VIEW_TYPE_FREE_MODE;
            case introduce:
                return VIEW_TYPE_INTRODUCE_MODE;
            default:
                return VIEW_TYPE_NORMAL_MODE;
        }
    }
}
