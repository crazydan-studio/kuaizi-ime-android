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

package org.crazydan.studio.app.ime.kuaizi.pane.view.completion;

import java.util.List;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;

/**
 * {@link CompletionInput} 的{@link RecyclerView}适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewAdapter extends RecyclerViewAdapter<CompletionView> {
    private final CompletionViewLayoutManager manager;

    private List<CompletionInput> completions;

    public CompletionViewAdapter(CompletionViewLayoutManager manager) {
        this.manager = manager;
    }

    public void updateDataList(List<CompletionInput> newCompletions) {
        List<CompletionInput> oldCompletions = this.completions;
        this.completions = newCompletions;

        updateItems(oldCompletions, newCompletions);
    }

    public void updateBindViewHolder(CompletionView view) {
        if (this.completions == null) {
            return;
        }

        CompletionInput completion = view.getData();
        int index = this.completions.indexOf(completion);
        CompletionInput newCompletion = this.completions.get(index);

        // 更新 变更了补全位置 的数据，以确保在应用补全时能够对应到正确的补全位置
        if (newCompletion != completion) {
            ((RecyclerViewHolder<CompletionInput>) view).bind(newCompletion);
        }
    }

    @Override
    public int getItemCount() {
        return this.completions == null ? 0 : this.completions.size();
    }

    @Override
    public void onBindViewHolder(@NonNull CompletionView view, int position) {
        CompletionInput completion = this.completions.get(position);

        view.bind(completion);

        view.getScrollView().setOnTouchListener(this::handleScrollViewEvent);
    }

    @NonNull
    @Override
    public CompletionView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CompletionView(inflateItemView(parent, R.layout.input_completion_view));
    }

    private boolean handleScrollViewEvent(View view, MotionEvent event) {
        boolean canScrollCompletion = view.canScrollHorizontally(-1) || view.canScrollHorizontally(1);

        // https://stackoverflow.com/questions/29426858/scrollview-inside-a-recyclerview-android/68506793#answer-68506793
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.manager.enableScroll(!canScrollCompletion);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                this.manager.enableScroll(true);
                break;
            }
        }
        return false;
    }
}
