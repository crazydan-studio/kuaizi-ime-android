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

import java.util.List;
import java.util.stream.Collectors;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerPageView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewLinearLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseViewMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseViewMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseListStartDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseStepStartDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseThemeUpdateDoneMsgData;

/**
 * {@link ExerciseList} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListView extends RecyclerPageView<ExerciseListViewAdapter, Exercise.ViewData>
        implements InputMsgListener, ExerciseMsgListener {
    private ExerciseViewMsgListener listener;

    public ExerciseListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        addPageActiveListener(this::onPageActive);
    }

    @Override
    protected ExerciseListViewAdapter createAdapter() {
        return new ExerciseListViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new RecyclerViewLinearLayoutManager(context);
    }

    /** 更新视图 */
    public void update(List<Exercise> exercises) {
        getAdapter().updateItems(exercises.stream().map(Exercise::createViewData).collect(Collectors.toList()));
    }

    // ================ Start: 消息处理 =================

    public void setListener(ExerciseViewMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMsg(InputMsg msg) {
        ExerciseViewHolder holder = getActivePageViewHolder();
        holder.onMsg(msg);
    }

    @Override
    public void onMsg(ExerciseMsg msg) {
        switch (msg.type) {
            case List_Start_Done: {
                ExerciseListStartDoneMsgData data = (ExerciseListStartDoneMsgData) msg.data;
                update(data.exercises);
                break;
            }
            case Theme_Update_Done: {
                ExerciseThemeUpdateDoneMsgData data = (ExerciseThemeUpdateDoneMsgData) msg.data;

                ExerciseViewHolder holder = getActivePageViewHolder();
                holder.updateSteps(data.exercise.createViewData());
                break;
            }
            case Step_Start_Done: {
                ExerciseStepStartDoneMsgData data = (ExerciseStepStartDoneMsgData) msg.data;

                ExerciseViewHolder holder = getActivePageViewHolder();
                holder.activateStep(data.exercise.createViewData(), data.stepIndex, data.restarted);
                break;
            }
        }
    }

    private void onPageActive(int position) {
        ExerciseViewMsg msg = new ExerciseViewMsg(position);
        this.listener.onMsg(msg);
    }

    // ================ End: 消息处理 =================
}
