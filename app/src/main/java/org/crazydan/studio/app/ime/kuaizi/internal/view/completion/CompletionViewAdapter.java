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

package org.crazydan.studio.app.ime.kuaizi.internal.view.completion;

import java.util.List;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewAdapter;

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

    public void updateDataList(List<CompletionInput> completions) {
        this.completions = completions;

        notifyDataSetChanged();
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
        boolean canScrollCompletion = ((HorizontalScrollView) view).canScrollHorizontally(-1)
                                      || ((HorizontalScrollView) view).canScrollHorizontally(1);

        // https://stackoverflow.com/questions/29426858/scrollview-inside-a-recyclerview-android/68506793#answer-68506793
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                this.manager.enableScroll(!canScrollCompletion);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                this.manager.enableScroll(true);
                return true;
            }
        }
        return false;
    }
}
