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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.RecyclerViewOnScrolledListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseViewMsg;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.ExerciseViewMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseListStartDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.exercise.msg.data.ExerciseStepStartDoneMsgData;

/**
 * {@link ExerciseList} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListView extends RecyclerView implements InputMsgListener, ExerciseMsgListener {
    private final ExerciseListViewAdapter adapter;
    private final PagerSnapHelper pager;

    private ExerciseViewMsgListener listener;

    public ExerciseListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new ExerciseListViewAdapter();
        setAdapter(this.adapter);

        ExerciseListViewLayoutManager layoutManager = new ExerciseListViewLayoutManager(context);
        setLayoutManager(layoutManager);

        // 以翻页形式切换项目至视图中心
        // - 用 RecyclerView 打造一个轮播图: https://juejin.cn/post/6844903512447385613
        // - 使用 RecyclerView 实现 Gallery 画廊效果，并控制 Item 停留位置: https://cloud.tencent.com/developer/article/1041258
        // - 用 RecyclerView 打造一个轮播图（进阶版）: https://juejin.cn/post/6844903513189777421
        // - RecyclerView 实现 Gallery 画廊效果: https://www.cnblogs.com/xwgblog/p/7580812.html
        this.pager = new PagerSnapHelper();
        this.pager.attachToRecyclerView(this);

        addOnScrollListener(new RecyclerViewOnScrolledListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView) {
                onRecyclerViewScrolled();
            }
        });
    }

    /** 更新视图 */
    public void update(List<Exercise> exercises) {
        this.adapter.updateDataList(exercises);
    }

    // ================ Start: 消息处理 =================

    public void setListener(ExerciseViewMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMsg(InputMsg msg) {
        ExerciseView view = getActiveView();
        view.onMsg(msg);
    }

    @Override
    public void onMsg(ExerciseMsg msg) {
        switch (msg.type) {
            case List_Start_Done: {
                ExerciseListStartDoneMsgData data = (ExerciseListStartDoneMsgData) msg.data;
                update(data.exercises);
                break;
            }
            case Step_Start_Done: {
                ExerciseStepStartDoneMsgData data = (ExerciseStepStartDoneMsgData) msg.data;

                ExerciseView view = getActiveView();
                view.activateStepAt(data.stepIndex);
                break;
            }
        }
    }

    private void onRecyclerViewScrolled() {
        int position = getActivePosition();
        if (position < 0) {
            return;
        }

        ExerciseViewMsg msg = new ExerciseViewMsg(position);
        this.listener.onMsg(msg);
    }

    // ================ End: 消息处理 =================

    /** 延迟激活指定位置的视图 */
    public void delayActivateAt(int position) {
        post(() -> smoothScrollToPosition(position));
    }

    /** 激活指定位置的视图 */
    public void activateAt(int position) {
        smoothScrollToPosition(position);
    }

    /** 激活当前位置之后的视图 */
    public void activateNext() {
        int position = getActivePosition();
        int total = getAdapter().getItemCount();

        if (position < total - 1) {
            activateAt(position + 1);
        }
    }

    private int getActivePosition() {
        return ((ExerciseListViewLayoutManager) getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    private ExerciseView getActiveView() {
        // https://stackoverflow.com/questions/43305295/how-to-get-the-center-item-after-recyclerview-snapped-it-to-center#answer-43305341
        View view = this.pager.findSnapView(getLayoutManager());
        assert view != null;

        return (ExerciseView) getChildViewHolder(view);
    }
}
