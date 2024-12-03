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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;

/**
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

    public void attachTo(RecyclerView view) {
        createDots(view);
        activeDot(view);

        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                activeDot(view);
            }
        });
    }

    private void createDots(RecyclerView view) {
        RecyclerView.Adapter<?> adapter = view.getAdapter();
        if (adapter == null) {
            return;
        }

        createDots(view, adapter.getItemCount());
        adapter.registerAdapterDataObserver(new DefaultAdapterDataObserver() {
            @Override
            public void onChanged() {
                createDots(view, adapter.getItemCount());
                activeDot(view);
            }
        });
    }

    private void clearDots() {
        removeAllViews();
    }

    private void createDots(RecyclerView recycler, int count) {
        int size = this.dotSize;
        if (size <= 0 || count <= 0) {
            clearDots();
            return;
        }

        int childCount = getChildCount();
        if (childCount == count) {
            return;
        }

        // 删除多余的
        if (childCount > count) {
            for (int i = count; i < childCount; i++) {
                removeViewAt(i);
            }
        }
        // 补充新增的
        else {
            for (int i = childCount; i < count; i++) {
                View view = createDot(recycler, i, i == this.activeDot);
                addView(view);
            }
        }
    }

    private void activeDot(RecyclerView view) {
        RecyclerView.LayoutManager manager = view.getLayoutManager();

        // https://stackoverflow.com/questions/24989218/get-visible-items-in-recyclerview#answer-64054637
        if (manager instanceof LinearLayoutManager) {
            int position = ((LinearLayoutManager) manager).findFirstCompletelyVisibleItemPosition();

            activeDot(position);
        }
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

    private View createDot(RecyclerView recycler, int position, boolean isActive) {
        int size = this.dotSize;
        int spacing = this.dotSpacing;

        // https://github.com/shichaohui/PageRecyelerViewDemo/blob/master/src/main/java/com/example/sch/myapplication/PageIndicatorView.java#L62
        LayoutParams params = new LayoutParams(size, size);
        params.setMargins(spacing, 0, spacing, 0);

        View view = new View(getContext());
        view.setBackgroundResource(isActive ? R.drawable.bg_dot_full : R.drawable.bg_dot_hole);
        view.setLayoutParams(params);

        view.setOnClickListener((v) -> {
            activeDot(position);
            recycler.smoothScrollToPosition(position);
        });

        return view;
    }

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
