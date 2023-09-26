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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseListViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseView;

/**
 * {@link Exercise 练习题}列表视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListView extends RecyclerView {
    public final ExerciseListViewAdapter adapter;

    private ExerciseActiveListener exerciseActiveListener;

    public ExerciseListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new ExerciseListViewAdapter();
        setAdapter(this.adapter);

        ExerciseListViewLayoutManager layoutManager = new ExerciseListViewLayoutManager(context);
        setLayoutManager(layoutManager);

        // 以翻页形式切换项目至视图中心
        // - 用RecyclerView打造一个轮播图: https://juejin.cn/post/6844903512447385613
        // - 使用 RecyclerView 实现 Gallery 画廊效果，并控制 Item 停留位置: https://cloud.tencent.com/developer/article/1041258
        // - 用RecyclerView打造一个轮播图（进阶版）: https://juejin.cn/post/6844903513189777421
        // - RecyclerView实现Gallery画廊效果: https://www.cnblogs.com/xwgblog/p/7580812.html
        PagerSnapHelper pager = new PagerSnapHelper();
        pager.attachToRecyclerView(this);

        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                ExerciseView exerciseView = getActive(pager);
                ExerciseActiveListener listener = ((ExerciseListView) recyclerView).exerciseActiveListener;

                if (exerciseView == null || listener == null) {
                    return;
                }

                listener.onActive(exerciseView);
            }
        });
    }

    public void active(int position) {
        smoothScrollToPosition(position);
    }

    public void activeNext() {
        int position = ((ExerciseListViewLayoutManager) getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        int total = getAdapter().getItemCount();

        if (position < total - 1) {
            active(position + 1);
        }
    }

    public void setExerciseActiveListener(ExerciseActiveListener exerciseActiveListener) {
        this.exerciseActiveListener = exerciseActiveListener;
    }

    public ExerciseView getActive(PagerSnapHelper pager) {
        // https://stackoverflow.com/questions/43305295/how-to-get-the-center-item-after-recyclerview-snapped-it-to-center#answer-43305341
        View view = pager.findSnapView(getLayoutManager());

        return view != null ? (ExerciseView) getChildViewHolder(view) : null;
    }

    public interface ExerciseActiveListener {
        void onActive(ExerciseView exerciseView);
    }
}
