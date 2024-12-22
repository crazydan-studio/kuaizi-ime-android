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

package org.crazydan.studio.app.ime.kuaizi.common.widget.recycler;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;

/**
 * 支持翻页的 {@link RecyclerView} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-21
 */
public abstract class RecyclerPageView extends RecyclerView {
    protected final Logger log = Logger.getLogger(getClass());

    private final PagerSnapHelper pager;
    private final List<PageActiveListener> pageActiveListeners = new ArrayList<>();

    public RecyclerPageView(
            @NonNull Context context, @Nullable AttributeSet attrs, //
            @NonNull LinearLayoutManager layoutManager
    ) {
        super(context, attrs);

        setLayoutManager(layoutManager);

        // 以翻页形式切换项目至视图中心
        // - 用 RecyclerView 打造一个轮播图: https://juejin.cn/post/6844903512447385613
        // - 使用 RecyclerView 实现 Gallery 画廊效果，并控制 Item 停留位置: https://cloud.tencent.com/developer/article/1041258
        // - 用 RecyclerView 打造一个轮播图（进阶版）: https://juejin.cn/post/6844903513189777421
        // - RecyclerView 实现 Gallery 画廊效果: https://www.cnblogs.com/xwgblog/p/7580812.html
        this.pager = new PagerSnapHelper();
        this.pager.attachToRecyclerView(this);

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_IDLE) {
                    onRecyclerViewScrolled();
                }
            }
        });
    }

    public void addPageActiveListener(PageActiveListener listener) {
        this.pageActiveListeners.add(listener);
    }

    /** 激活指定位置的视图 */
    public void activatePage(int position) {
        smoothScrollToPosition(position);
    }

    /** 激活当前位置之后的视图 */
    public void activateNextPage() {
        int position = getActivePagePosition();
        int total = getAdapter().getItemCount();

        if (position < total - 1) {
            activatePage(position + 1);
        }
    }

    /** 获取激活页的位置 */
    public int getActivePagePosition() {
        return ((LinearLayoutManager) getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    public <T extends ViewHolder> T getActivePageViewHolder() {
        // https://stackoverflow.com/questions/43305295/how-to-get-the-center-item-after-recyclerview-snapped-it-to-center#answer-43305341
        View view = this.pager.findSnapView(getLayoutManager());
        assert view != null;

        return (T) getChildViewHolder(view);
    }

    private void onRecyclerViewScrolled() {
        int position = getActivePagePosition();
        if (position < 0) {
            return;
        }

        this.log.debug("Trigger active page %d", position);
        this.pageActiveListeners.forEach((listener) -> listener.onPageActive(position));
    }

    public interface PageActiveListener {
        void onPageActive(int position);
    }
}
