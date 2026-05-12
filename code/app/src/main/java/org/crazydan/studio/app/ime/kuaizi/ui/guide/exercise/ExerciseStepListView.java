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

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLinearLayoutManager;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepListView extends RecyclerView<ExerciseStepListViewAdapter, ExerciseStep.ViewData> {

    public ExerciseStepListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // 禁用动画，以避免练习题在切换过程中出现残影
        setItemAnimator(null);
    }

    @Override
    protected ExerciseStepListViewAdapter createAdapter() {
        return new ExerciseStepListViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new RecyclerViewLinearLayoutManager(context, true);
    }

    /** 更新视图 */
    public void update(Exercise.ViewData exercise) {
        getAdapter().setKeyImageRender(exercise.keyImageRender);

        getAdapter().updateItems(exercise.steps);
    }

    /** 滚动到指定位置 */
    public void scrollTo(int position) {
        // 提前定位到最后一个 step
        if (getAdapter().isLastStep(position + 1)) {
            position += 1;
        }

        smoothScrollToPosition(position);
    }
}
