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

package org.crazydan.studio.app.ime.kuaizi.core.view;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.core.view.completion.CompletionView;
import org.crazydan.studio.app.ime.kuaizi.core.view.completion.CompletionViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.view.completion.CompletionViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.core.view.completion.CompletionViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;

/**
 * 输入补全列表视图
 * <p/>
 * 注：在需要显示前调用 {@link #updateInputList} 更新列表数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class InputCompletionsView extends RecyclerView {
    private final CompletionViewAdapter adapter;

    private InputList inputList;

    public InputCompletionsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        CompletionViewLayoutManager layoutManager = new CompletionViewLayoutManager(context);
        setLayoutManager(layoutManager);

        this.adapter = new CompletionViewAdapter(layoutManager);
        setAdapter(this.adapter);

        RecyclerViewGestureDetector gesture = new RecyclerViewGestureDetector();
        gesture.bind(this) //
               .addListener(new CompletionViewGestureListener(this));
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public void updateInputList(InputList inputList) {
        this.inputList = inputList;

        List<CompletionInput> completions = inputList.getCompletions();
        this.adapter.updateDataList(completions);
    }

    public CompletionView findCompletionViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        if (view == null) {
            return null;
        }

        CompletionView viewHolder = (CompletionView) getChildViewHolder(view);
        this.adapter.updateBindViewHolder(viewHolder);

        return viewHolder;
    }
}
