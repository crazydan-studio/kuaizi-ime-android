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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewLayoutManager;

/**
 * 输入列表视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListView extends RecyclerView implements InputMsgListener {
    private final InputViewAdapter adapter;
    private final InputViewLayoutManager layoutManager;
    private final RecyclerViewGestureDetector gesture;

    private InputList inputList;

    public InputListView(Context context) {
        this(context, null);
    }

    public InputListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new InputViewAdapter();
        this.layoutManager = new InputViewLayoutManager(context);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);

        this.gesture = new RecyclerViewGestureDetector();
        this.gesture.bind(this) //
                    .addListener(new InputViewGestureListener(this));
    }

    public void setInputList(InputList inputList) {
        this.inputList = inputList;
        this.adapter.setInputList(inputList);
    }

    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        this.inputList.onUserInputMsg(msg, data);
    }

    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputtingChars:
            case InputtingCharsDone:
            case ChoosingInputCandidate:
            case InputCommitting:
                this.adapter.updateItems();
                smoothScrollToPosition(this.adapter.getSelectedInputPosition());
                break;
        }
    }

    /** 找到指定坐标下可见的{@link  InputView 输入视图} */
    public InputView<?> findVisibleKeyViewUnder(float x, float y) {
        View child = findChildViewUnder(x, y);

        return child != null ? (InputView<?>) getChildViewHolder(child) : null;
    }
}
