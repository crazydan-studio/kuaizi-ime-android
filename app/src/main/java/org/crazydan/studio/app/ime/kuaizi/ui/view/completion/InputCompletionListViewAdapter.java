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

package org.crazydan.studio.app.ime.kuaizi.ui.view.completion;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputCompletionListView;

/**
 * {@link InputCompletionListView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputCompletionListViewAdapter
        extends RecyclerViewAdapter<InputCompletion.ViewData, InputCompletionViewHolder> {
    private InputCompletionListViewLayoutManager layoutManager;

    public InputCompletionListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    public void setLayoutManager(InputCompletionListViewLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onBindViewHolder(@NonNull InputCompletionViewHolder holder, int position) {
        InputCompletion.ViewData item = getItem(position);

        holder.bind(item);

        holder.getScrollView().setOnTouchListener(this::handleScrollViewEvent);
    }

    @NonNull
    @Override
    public InputCompletionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflateItemView(parent, R.layout.input_completion_view);
        return new InputCompletionViewHolder(view);
    }

    private boolean handleScrollViewEvent(View view, MotionEvent event) {
        boolean canScrollCompletion = view.canScrollHorizontally(-1) || view.canScrollHorizontally(1);

        // https://stackoverflow.com/questions/29426858/scrollview-inside-a-recyclerview-android/68506793#answer-68506793
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.layoutManager.enableScroll(!canScrollCompletion);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                this.layoutManager.enableScroll(true);
                break;
            }
        }
        return false;
    }
}
