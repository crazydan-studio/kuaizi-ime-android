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
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
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
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;

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

        updateInputList(null);
    }

    public void updateInputList(InputList inputList) {
        updateInputList(inputList, true);
    }

    public void updateInputList(InputList inputList, boolean canBeSelected) {
        this.inputList = inputList;

        this.adapter.updateInputList(this.inputList, canBeSelected);
    }

    /** 响应输入列表的点击等消息 */
    public void onUserInputMsg(UserInputMsg msg, UserInputMsgData data) {
        this.inputList.onUserInputMsg(msg, data);
    }

    /** 响应键盘输入消息 */
    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case Keyboard_Switch_Doing:
            case Keyboard_State_Change_Done:
            case InputChars_Input_Doing:
            case InputChars_Input_Done:
            case InputCandidate_Choose_Doing:
            case InputCandidate_Choose_Done:
            case Emoji_Choose_Doing:
            case Symbol_Choose_Doing:
            case InputList_Option_Update_Done:
            case InputList_Input_Choose_Done:
            case InputList_Input_Completion_Apply_Done:
            case InputList_Pending_Drop_Done:
            case InputList_Selected_Delete_Done:
            case InputList_Clean_Done:
            case InputList_Cleaned_Cancel_Done:
            case InputList_Commit_Doing:
            case InputList_PairSymbol_Commit_Doing:
            case InputList_Committed_Revoke_Doing:
                updateInputList(this.inputList);

                int position = this.inputList.getSelectedIndex();
                scrollToSelected(position);
                break;
        }
    }

    public void scrollToSelected(int position) {
        View item = this.layoutManager.findViewByPosition(position);

        int offset = 0;
        if (item != null) {
            int parentWidth = getMeasuredWidth();
            int parentPadding = getPaddingLeft() + getPaddingRight();
            Point parentLocation = ViewUtils.getLocationInWindow(this);

            int itemWidth = item.getMeasuredWidth();
            Point itemLocation = ViewUtils.getLocationInWindow(item);

            // 项目宽度超出可见区域，则滚动位置需移至其尾部
            if (itemWidth + parentPadding > parentWidth) {
                offset = (itemLocation.x + itemWidth) - (parentLocation.x + parentWidth)
                         // 尾部留白
                         + parentPadding;

                // 已经移动完成，则不做处理。用于处理多次相邻调用的情况
                if (offset == 0) {
                    return;
                }
            }
            // 若项目已在可见区域内，则不需要滚动
            else if (itemLocation.x >= parentLocation.x //
                     && itemLocation.x + itemWidth <= parentLocation.x + parentWidth) {
                return;
            }
        }

        if (offset == 0) {
            scrollToPosition(position);
        } else {
            scrollBy(offset, 0);
        }
    }

    /** 找到指定坐标下可见的{@link  InputView 输入视图} */
    public InputView<?> findVisibleInputViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        InputView<?> inputView = view != null ? (InputView<?>) getChildViewHolder(view) : null;

        // 若点击位置更靠近输入之间的 Gap 位置，则返回该 Gap
        if (inputView instanceof CharInputView) {
            int gap = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width);
            int position = getChildAdapterPosition(view);
            float left = view.getLeft();
            float right = view.getRight();

            // 取当前输入左边的 Gap
            // Note：不能通过 getChildAt(position) 方式获取 ViewHolder 对应位置的视图，
            // 因为子视图的位置不一定与 ViewHolder 的视图位置等同
            if (x < left - gap) {
                inputView = (InputView<?>) findViewHolderForAdapterPosition(position - 1);
            }
            // 取当前输入右边的 Gap
            else if (x > right - gap) {
                inputView = (InputView<?>) findViewHolderForAdapterPosition(position + 1);
            }
        }

        return inputView;
    }

    public Input<?> getLastInput() {
        return this.inputList.getLastInput();
    }

    public Input<?> getFirstInput() {
        return this.inputList.getFirstInput();
    }
}
