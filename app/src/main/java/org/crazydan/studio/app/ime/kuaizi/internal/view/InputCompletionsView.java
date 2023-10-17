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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CompletionInput;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.completion.CompletionView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.completion.CompletionViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.completion.CompletionViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.internal.view.completion.CompletionViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;

/**
 * 输入补全列表视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-11
 */
public class InputCompletionsView extends RecyclerView implements InputMsgListener {
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

    public void reset() {
        setInputList(null);
    }

    public void setInputList(InputList inputList) {
        this.inputList = inputList;
    }

    /** 响应输入列表的点击等消息 */
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        this.inputList.onUserInputMsg(msg, data);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        List<CompletionInput> completions = this.inputList.getCompletions();

        this.adapter.updateDataList(completions);
    }

    public CompletionView findCompletionViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);

        return view != null ? (CompletionView) getChildViewHolder(view) : null;
    }
}
