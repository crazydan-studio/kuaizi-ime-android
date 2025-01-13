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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;

/**
 * 轮播指示器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-20
 */
public class RecyclerPageIndicatorView extends LinearLayout {
    protected final Logger log = Logger.getLogger(getClass());

    /** 指示器的大小（dp） */
    private int dotSize;
    /** 指示器间距（dp） */
    private int dotSpacing;

    private int activeDot;

    public RecyclerPageIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        ThemeUtils.applyStyledAttributes(context, attrs, R.styleable.RecyclerPageIndicatorView, (typedArray) -> {
            int size = typedArray.getDimensionPixelSize(R.styleable.RecyclerPageIndicatorView_dot_size, 0);
            int spacing = typedArray.getDimensionPixelSize(R.styleable.RecyclerPageIndicatorView_dot_spacing, 0);

            setDotSize(size);
            setDotSpacing(spacing);
        });
    }

    public void setDotSize(int dotSize) {
        this.dotSize = dotSize;
    }

    public void setDotSpacing(int dotSpacing) {
        this.dotSpacing = dotSpacing;
    }

    public void attachTo(RecyclerPageView view) {
        this.activeDot = 0;

        view.addOnScrollListener(new RecyclerOnScrollListener());

        createDots(view);
    }

    private void createDots(RecyclerPageView view) {
        RecyclerView.Adapter<?> adapter = view.getAdapter();
        assert adapter != null;

        Runnable updateDots = () -> {
            createDots(view, adapter.getItemCount());
            activateDot(view);
        };

        adapter.registerAdapterDataObserver(new DefaultAdapterDataObserver() {
            @Override
            public void onChanged() {
                updateDots.run();
            }
        });

        updateDots.run();
    }

    /** 重建锚点 */
    private void createDots(RecyclerPageView recycler, int count) {
        // 清空重建，以避免视图复用造成定位不准的问题
        removeAllViews();

        int size = this.dotSize;
        if (size <= 0 || count <= 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            View view = createDot(recycler, i, i == this.activeDot);
            addView(view);
        }
    }

    private void activateDot(RecyclerPageView view) {
        int position = view.getActivePagePosition();
        activateDot(position);
    }

    private void activateDot(int position) {
        if (this.activeDot == position || position < 0) {
            return;
        }

        View oldActive = getChildAt(this.activeDot);
        if (oldActive != null) {
            oldActive.setBackgroundResource(R.drawable.bg_dot_hole);
        }

        View newActive = getChildAt(position);
        if (newActive == null) {
            return;
        }

        this.activeDot = position;
        newActive.setBackgroundResource(R.drawable.bg_dot_full);
    }

    private View createDot(RecyclerPageView view, int position, boolean active) {
        int size = this.dotSize;
        int spacing = this.dotSpacing;

        // https://github.com/shichaohui/PageRecyelerViewDemo/blob/master/src/main/java/com/example/sch/myapplication/PageIndicatorView.java#L62
        LayoutParams params = new LayoutParams(size, size);
        params.setMargins(spacing, 0, spacing, 0);

        View dotView = new View(getContext());
        dotView.setBackgroundResource(active ? R.drawable.bg_dot_full : R.drawable.bg_dot_hole);
        dotView.setLayoutParams(params);

        dotView.setOnClickListener((v) -> view.activatePage(position));

        return dotView;
    }

    private class RecyclerOnScrollListener extends RecyclerView.OnScrollListener {
        private boolean dragging;

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                this.dragging = true;
            } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                this.dragging = false;
            }
        }

        /** 该接口在滚动过程中将被实时调用 */
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            LinearLayoutManager layout = (LinearLayoutManager) recyclerView.getLayoutManager();

            // Note: 若是拖动，则需要以最终可见页为准，若是滚动，则以正在切换的页为准
            int position = this.dragging
                           ? layout.findFirstCompletelyVisibleItemPosition()
                           : layout.findFirstVisibleItemPosition();

            // 激活滚动过程中切换的页的指示器
            activateDot(position);
        }
    }

    /** 确保监听函数均最终调用 {@link DefaultAdapterDataObserver#onChanged()}，从而减少重载的接口数 */
    private static class DefaultAdapterDataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            onChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            onChanged();
        }
    }
}
