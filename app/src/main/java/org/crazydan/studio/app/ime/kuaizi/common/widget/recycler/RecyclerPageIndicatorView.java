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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;

/**
 * 轮播指示器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-20
 */
public class RecyclerPageIndicatorView extends LinearLayout {
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

        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            // 该接口在滚动过程中将被实时调用
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int position = view.getVisiblePagePosition();
                // 激活滚动过程中切换的页的指示器
                activeDot(position);
            }
        });

        createDots(view);
    }

    private void createDots(RecyclerPageView view) {
        RecyclerView.Adapter<?> adapter = view.getAdapter();
        assert adapter != null;

        Runnable updateDots = () -> {
            createDots(view, adapter.getItemCount());
            activeDot(view);
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

    private void activeDot(RecyclerPageView view) {
        int position = view.getActivePagePosition();
        activeDot(position);
    }

    private void activeDot(int position) {
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
