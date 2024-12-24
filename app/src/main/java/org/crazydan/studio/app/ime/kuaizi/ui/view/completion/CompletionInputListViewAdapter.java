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

import java.util.ArrayList;
import java.util.List;

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
public class CompletionInputListViewAdapter extends RecyclerViewAdapter<CompletionInputViewHolder> {
    private final CompletionInputListViewLayoutManager manager;

    private List<CompletionInput> dataList = new ArrayList<>();

    public CompletionInputListViewAdapter(CompletionInputListViewLayoutManager manager) {
        this.manager = manager;
    }

    public void updateDataList(List<CompletionInput> dataList) {
        List<CompletionInput> oldDataList = this.dataList;
        this.dataList = dataList;

        updateItems(oldDataList, dataList);
    }

    public void updateViewHolder(CompletionInputViewHolder holder) {
        if (this.dataList == null) {
            return;
        }

        CompletionInput data = holder.getData();
        int index = this.dataList.indexOf(data);
        CompletionInput newData = this.dataList.get(index);

        // 更新 变更了补全位置 的数据，以确保在应用补全时能够对应到正确的补全位置
        if (newData != data) {
            holder.bind(newData);
        }
    }

    @Override
    public int getItemCount() {
        return this.dataList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull CompletionInputViewHolder holder, int position) {
        CompletionInput data = this.dataList.get(position);

        holder.bind(data);
        holder.getScrollView().setOnTouchListener(this::handleScrollViewEvent);
    }

    @NonNull
    @Override
    public CompletionInputViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CompletionInputViewHolder(inflateItemView(parent, R.layout.input_completion_view));
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
