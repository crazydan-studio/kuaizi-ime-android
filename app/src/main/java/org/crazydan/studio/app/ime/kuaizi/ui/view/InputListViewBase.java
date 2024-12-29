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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.List;
import java.util.Optional;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.CharInputViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputViewHolder;

import static org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgType.SingleTap_Input;

/**
 * {@link InputListView} 的基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListViewBase extends RecyclerView<InputListViewAdapter>
        implements ViewGestureDetector.Listener, InputMsgListener {
    private final InputListViewLayoutManager layoutManager;

    private UserInputMsgListener listener;

    public InputListViewBase(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.layoutManager = new InputListViewLayoutManager(context);
        setLayoutManager(this.layoutManager);

        // Note：取消动画以确保输入能够直接显隐，不做淡化
        setItemAnimator(null);

        RecyclerViewGestureDetector gesture = new RecyclerViewGestureDetector(this);
        gesture.addListener(this);
    }

    @Override
    protected InputListViewAdapter createAdapter() {
        return new InputListViewAdapter();
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    /** 向上传递 {@link UserInputMsg} 消息 */
    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        InputViewHolder holder = findVisibleInputViewUnder(data.x, data.y);

        if (type != ViewGestureDetector.GestureType.SingleTap) {
            return;
        }

        UserInputMsgData.Where where = UserInputMsgData.Where.inner;
        Input<?> input = Optional.ofNullable((InputViewData) getAdapterItem(holder))
                                 .map((item) -> item.input)
                                 .orElse(null);

        if (input == null) {
            if (data.x < getPaddingStart()) {
                where = UserInputMsgData.Where.head;
            } else {
                where = UserInputMsgData.Where.tail;
            }
        }

        UserInputMsg msg = new UserInputMsg(SingleTap_Input, new UserInputMsgData(input, where));
        fire_UserInputMsg(msg);
    }

    /** 响应来自上层派发的 {@link InputMsg} 消息 */
    @Override
    public void onMsg(InputMsg msg) {
        // Note: 作为与算术输入视图的公共逻辑，仅针对 Input_Choose_Done 的处理，其余消息各自单独处理
        switch (msg.type) {
            case Input_Choose_Done: {
                // 若首次选中算术表达式，则需保持滚动条不动。
                // 因为在算术表达式长度超出可见区域时，
                // 滚动条移动会造成算术表达式视图直接接收 ACTION_CANCEL
                // 事件而丢失 ACTION_UP 事件，从而不能触发单击消息并进而选中算术表达式中的目标输入。
                // 注：事件是从父 InputList 传递到子 InputList 的，
                // 所以，无法优先处理子 InputList 的事件
                boolean needToLockScrolling = msg.data().input.isMathExpr();

                update(msg.inputFactory, true, needToLockScrolling);
                break;
            }
        }
    }

    /** 发送 {@link UserInputMsg} 消息 */
    protected void fire_UserInputMsg(UserInputMsg msg) {
        if (this.listener != null) {
            this.listener.onMsg(msg);
            return;
        }

        // Note: 此类视图存在嵌套的情况，故而，需将消息交给最上层的视图转发
        ViewParent parent = this;
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof InputListViewBase) {
                ((InputListViewBase) parent).fire_UserInputMsg(msg);
                break;
            }
        }
    }

    // =============================== End: 消息处理 ===================================

    // =============================== Start: 更新视图 ===================================

    public void update(InputFactory inputFactory) {
        update(inputFactory, true, false);
    }

    public void update(InputFactory inputFactory, boolean canBeSelected, boolean needToLockScrolling) {
        List<InputViewData> dataList = inputFactory.getInputs();
        getAdapter().updateItems(dataList, canBeSelected);

        if (!needToLockScrolling) {
            scrollToSelectedInput(dataList);
        }
    }

    // =============================== End: 更新视图 ===================================

    /** 滚动到选中输入的位置，确保其处于可见区域 */
    protected void scrollToSelectedInput(List<InputViewData> dataList) {
        // Note: 因为配对符号均会被选中，故而，只有含有 pending input 的才是正在输入的 input
        InputViewData selectedData = dataList.stream().filter(d -> d.pending != null).findFirst().orElse(null);
        if (selectedData == null) {
            return;
        }

        int position = selectedData.position;
        View view = getSelectedInputView(selectedData.input, position);

        int offset = 0;
        if (view != null) {
            Point parentLocation = ViewUtils.getLocationOnScreen(this);
            int parentWidth = getMeasuredWidth();
            int parentPadding = getPaddingLeft() + getPaddingRight();
            int parentLeft = parentLocation.x;
            int parentRight = parentLeft + parentWidth;

            Point viewLocation = ViewUtils.getLocationOnScreen(view);
            int viewWidth = view.getMeasuredWidth();
            int viewLeft = viewLocation.x;
            int viewRight = viewLeft + viewWidth;
            int viewMaxRight = viewRight + parentPadding;

            // 项目宽度超出可见区域，则滚动位置需移至其尾部
            if (viewWidth + parentPadding > parentWidth) {
                offset = viewMaxRight - parentRight;

                // 已经移动完成，则不做处理。用于处理多次相邻调用的情况
                if (offset == 0) {
                    return;
                }
            }
            // 若项目已在可见区域内，且其右侧加上空白后未超出可见区域，则不需要滚动
            else if (viewLeft >= parentLeft) {
                offset = viewMaxRight - parentRight;

                if (offset <= 0) {
                    return;
                }
            }
        }

        if (offset == 0) {
            scrollToPosition(position);
        } else {
            scrollBy(offset, 0);
        }
    }

    /** 找到指定坐标下可见的 {@link  InputViewHolder} */
    private InputViewHolder findVisibleInputViewUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        InputViewHolder holder = view != null ? (InputViewHolder) getChildViewHolder(view) : null;

        // 若点击位置更靠近输入之间的 Gap 位置，则返回该 Gap
        if (holder instanceof CharInputViewHolder) {
            int gap = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width);
            int position = getChildAdapterPosition(view);
            float left = view.getLeft();
            float right = view.getRight();

            // 取当前输入左边的 Gap
            // Note：不能通过 getChildAt(position) 方式获取 ViewHolder 对应位置的视图，
            // 因为子视图的位置不一定与 ViewHolder 的视图位置等同
            if (x < left - gap) {
                holder = (InputViewHolder) findViewHolderForAdapterPosition(position - 1);
            }
            // 取当前输入右边的 Gap
            else if (x > right - gap) {
                holder = (InputViewHolder) findViewHolderForAdapterPosition(position + 1);
            }
        }

        return holder;
    }

    /** 获取选中输入的视图，若选中输入为算术输入，则获取其内部所选中的输入视图 */
    private View getSelectedInputView(Input<?> selectedInput, int selectedIndex) {
        View view = this.layoutManager.findViewByPosition(selectedIndex);

        if (view == null || !selectedInput.isMathExpr()) {
            return view;
        }

        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);

            if (child instanceof InputListViewReadonly) {
                InputListViewReadonly ro = (InputListViewReadonly) child;
                int position = ((MathExprInput) selectedInput).getInputList().getSelectedIndex();

                return ro.getLayoutManager().findViewByPosition(position);
            }
        }
        return null;
    }
}
