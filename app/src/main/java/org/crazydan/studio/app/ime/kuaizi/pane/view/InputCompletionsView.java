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

package org.crazydan.studio.app.ime.kuaizi.pane.view;

import java.util.List;
import java.util.function.Supplier;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputListMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.view.completion.CompletionView;
import org.crazydan.studio.app.ime.kuaizi.pane.view.completion.CompletionViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.view.completion.CompletionViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;

/**
 * 输入补全列表视图
 * <p/>
 * 注：在需要显示前调用 {@link #update} 更新列表数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class InputCompletionsView extends RecyclerView implements ViewGestureDetector.Listener {
    private final CompletionViewAdapter adapter;

    private Supplier<InputList> inputListGetter;

    public InputCompletionsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        CompletionViewLayoutManager layoutManager = new CompletionViewLayoutManager(context);
        setLayoutManager(layoutManager);

        this.adapter = new CompletionViewAdapter(layoutManager);
        setAdapter(this.adapter);

        RecyclerViewGestureDetector gesture = new RecyclerViewGestureDetector();
        gesture.bind(this) //
               .addListener(this);
    }

    public void setInputList(Supplier<InputList> inputListGetter) {
        this.inputListGetter = inputListGetter;
    }

    public InputList getInputList() {
        return this.inputListGetter.get();
    }

    public void update() {
        InputList inputList = getInputList();
        List<CompletionInput> completions = inputList.getCompletions();

        this.adapter.updateDataList(completions);
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        CompletionView completionView = findCompletionViewUnder(data.x, data.y);
        if (completionView == null) {
            return;
        }

        if (type == ViewGestureDetector.GestureType.SingleTap) {
            onSingleTap(completionView, data);
        }
    }

    private void onSingleTap(CompletionView completionView, ViewGestureDetector.GestureData data) {
        CompletionInput completion = completionView.getData();

        InputListMsg msg = InputListMsg.Input_Completion_Choose_Doing;
        InputListMsgData msgData = new InputListMsgData(completion);
        getInputList().fireUserInputMsg(msg, msgData);
    }

    private CompletionView findCompletionViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        if (view == null) {
            return null;
        }

        CompletionView viewHolder = (CompletionView) getChildViewHolder(view);
        this.adapter.updateBindViewHolder(viewHolder);

        return viewHolder;
    }
}
