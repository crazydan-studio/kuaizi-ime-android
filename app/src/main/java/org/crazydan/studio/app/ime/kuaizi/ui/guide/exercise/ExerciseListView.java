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

import java.util.List;
import java.util.stream.Collectors;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerPageView;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
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
public class ExerciseListView extends RecyclerPageView<ExerciseListViewAdapter>
        implements InputMsgListener, ExerciseMsgListener {
    private ExerciseViewMsgListener listener;

    public ExerciseListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, new ExerciseListViewLayoutManager(context));

        addPageActiveListener(this::onPageActive);
    }

    @Override
    protected ExerciseListViewAdapter createAdapter() {
        return new ExerciseListViewAdapter();
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
