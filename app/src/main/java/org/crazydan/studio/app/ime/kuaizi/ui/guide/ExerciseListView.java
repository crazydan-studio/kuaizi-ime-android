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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseListViewLayoutManager;

/**
 * {@link Exercise 练习题}列表视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseListView extends RecyclerView {
    public final ExerciseListViewAdapter adapter;

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
        new PagerSnapHelper().attachToRecyclerView(this);
    }

    public void active(int position) {
        smoothScrollToPosition(position);
    }
}
