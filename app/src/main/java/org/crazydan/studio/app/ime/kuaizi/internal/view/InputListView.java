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
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.CharInputView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.internal.view.input.InputViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;

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
        // Note：取消动画以确保输入能够直接显隐，不做淡化
        setItemAnimator(null);

        this.gesture = new RecyclerViewGestureDetector();
        this.gesture.bind(this) //
                    .addListener(new InputViewGestureListener(this));
    }

    /** 重置视图 */
    public void reset() {
        this.gesture.reset();

        setInputList(null);
    }

    public void setInputList(InputList inputList) {
        this.inputList = inputList;
        this.adapter.updateInputList(this.inputList);
    }

    /** 响应输入列表的点击等消息 */
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        this.inputList.onUserInputMsg(msg, data);
    }

    /** 响应键盘输入消息 */
    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case InputChars_Inputting:
            case InputChars_InputtingEnd:
            case InputCandidate_Choosing:
            case Emoji_Choosing:
            case Symbol_Choosing:
            case InputList_Cleaning:
            case InputList_Revoking:
            case InputList_Committing:
            case InputList_PairSymbol_Committing:
                this.adapter.updateInputList(this.inputList);

                int position = this.inputList.getSelectedIndex();
                scrollToEnd(position);
                break;
        }
    }

    public void scrollToEnd(int position) {
        View item = this.layoutManager.findViewByPosition(position);

        int offset = 0;
        if (item != null) {
            // https://stackoverflow.com/questions/26580723/how-to-scroll-to-the-bottom-of-a-recyclerview-scrolltoposition-doesnt-work#answer-65990642
            int visibleWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
            int itemWidth = item.getMeasuredWidth();

            // 项目宽度超过可见区域宽度时，向可见区域的开始位置方向移动超出的部分，
            // 以确保项目尾部始终在可见区域的结束位置处
            if (itemWidth > visibleWidth) {
                offset = -(itemWidth - visibleWidth);
            }
        }

        // 不需要移动项目时，采用 scrollToPosition 以保持当前滚动位置不变：
        // 在 offset 为 0 时，scrollToPositionWithOffset
        // 会将项目移动到可见区域的开始位置处
        if (offset == 0) {
            this.layoutManager.scrollToPosition(position);
        } else {
            this.layoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    /** 找到指定坐标下可见的{@link  InputView 输入视图} */
    public InputView<?> findVisibleInputViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        InputView<?> inputView = view != null ? (InputView<?>) getChildViewHolder(view) : null;

        // 若点击位置更靠近输入之间的 Gap 位置，则返回该 Gap
        if (inputView instanceof CharInputView) {
            int gap = ScreenUtils.dpToPx(4);
            int position = getChildAdapterPosition(view);
            float left = view.getLeft();
            float right = view.getRight();

            // 取当前输入左边的 Gap
            if (x < left - gap) {
                view = getChildAt(position - 1);
                inputView = view != null ? (InputView<?>) getChildViewHolder(view) : null;
            }
            // 取当前输入右边的 Gap
            else if (x > right - gap) {
                view = getChildAt(position + 1);
                inputView = view != null ? (InputView<?>) getChildViewHolder(view) : null;
            }
        }

        return inputView;
    }

    public Input getLastInput() {
        return this.inputList.getLastInput();
    }

    public Input getFirstInput() {
        return this.inputList.getFirstInput();
    }
}
