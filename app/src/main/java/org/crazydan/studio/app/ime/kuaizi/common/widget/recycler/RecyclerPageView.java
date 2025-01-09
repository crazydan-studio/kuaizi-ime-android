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
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;

/**
 * 支持翻页的 {@link RecyclerView} 视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-21
 */
public abstract class RecyclerPageView<A extends RecyclerViewAdapter<I, ?>, I> extends RecyclerView<A, I> {
    protected final Logger log = Logger.getLogger(getClass());

    private final PagerSnapHelper pager;
    private final List<PageActiveListener> pageActiveListeners = new ArrayList<>();

    public RecyclerPageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // 以翻页形式切换项目至视图中心
        // - 用 RecyclerView 打造一个轮播图: https://juejin.cn/post/6844903512447385613
        // - 使用 RecyclerView 实现 Gallery 画廊效果，并控制 Item 停留位置: https://cloud.tencent.com/developer/article/1041258
        // - 用 RecyclerView 打造一个轮播图（进阶版）: https://juejin.cn/post/6844903513189777421
        // - RecyclerView 实现 Gallery 画廊效果: https://www.cnblogs.com/xwgblog/p/7580812.html
        this.pager = new PagerSnapHelper();
        this.pager.attachToRecyclerView(this);

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull androidx.recyclerview.widget.RecyclerView rv, int newState) {
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
        // Note: 若首次激活的为第一个页，则无需滚动，直接触发激活消息即可
        if (position == 0 && getActivePagePosition() < 0) {
            // 在当前视图渲染完成后，再触发消息
            post(() -> firePageActive(position));
        } else {
            smoothScrollToPosition(position);
        }
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

        firePageActive(position);
    }

    private void firePageActive(int position) {
        this.log.debug("Trigger active page %d", position);
        this.pageActiveListeners.forEach((listener) -> listener.onPageActive(position));
    }

    public interface PageActiveListener {
        void onPageActive(int position);
    }
}
