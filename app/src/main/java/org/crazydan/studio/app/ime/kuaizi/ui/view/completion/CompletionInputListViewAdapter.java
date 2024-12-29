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

package org.crazydan.studio.app.ime.kuaizi.ui.view.completion;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.ui.view.CompletionInputListView;

/**
 * {@link CompletionInputListView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionInputListViewAdapter extends RecyclerViewAdapter<CompletionInput, CompletionInputViewHolder> {
    private CompletionInputListViewLayoutManager layoutManager;

    public CompletionInputListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    public void setLayoutManager(CompletionInputListViewLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    public void updateViewHolder(CompletionInputViewHolder holder) {
        CompletionInput item = getItem(holder);

        int index = this.items.indexOf(item);
        CompletionInput newItem = getItem(index);

        // 更新 变更了补全位置 的数据，以确保在应用补全时能够对应到正确的补全位置
        if (newItem != item) {
            holder.bind(newItem);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull CompletionInputViewHolder holder, int position) {
        CompletionInput item = getItem(position);

        holder.bind(item);

        holder.getScrollView().setOnTouchListener(this::handleScrollViewEvent);
    }

    @NonNull
    @Override
    public CompletionInputViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflateItemView(parent, R.layout.input_completion_view);
        return new CompletionInputViewHolder(view);
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
