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

/**
 * {@link ExerciseListView} 数据适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListViewAdapter extends RecyclerViewAdapter<Exercise.ViewData, ExerciseViewHolder> {
    private final static int VIEW_TYPE_FREE_MODE = 0;
    private final static int VIEW_TYPE_NORMAL_MODE = 1;
    private final static int VIEW_TYPE_INTRODUCE_MODE = 2;

    public ExerciseListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise.ViewData item = getItem(position);

        holder.bind(item, position);
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_FREE_MODE: {
                View view = inflateItemView(parent, R.layout.guide_exercise_mode_free_view);
                return new ExerciseFreeViewHolder(view);
            }
            case VIEW_TYPE_INTRODUCE_MODE: {
                View view = inflateItemView(parent, R.layout.guide_exercise_mode_introduce_view);
                return new ExerciseIntroduceViewHolder(view);
            }
        }

        View view = inflateItemView(parent, R.layout.guide_exercise_mode_normal_view);
        return new ExerciseViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Exercise.ViewData item = getItem(position);

        switch (item.mode) {
            case free:
                return VIEW_TYPE_FREE_MODE;
            case introduce:
                return VIEW_TYPE_INTRODUCE_MODE;
            default:
                return VIEW_TYPE_NORMAL_MODE;
        }
    }
}
