/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.List;

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
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.CharInputViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputViewHolder;

import static org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData.Type.MathExpr;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Input;

/**
 * {@link InputListView} 的基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListViewBase extends RecyclerView<InputListViewAdapter, InputViewData>
        implements ViewGestureDetector.Listener, InputMsgListener {
    private int positionInParent = -1;

    private UserInputMsgListener listener;

    public InputListViewBase(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // Note：取消动画以确保输入能够直接显隐，不做淡化
        setItemAnimator(null);

        RecyclerViewGestureDetector<InputViewData> gesture = new RecyclerViewGestureDetector<>(this);
        gesture.addListener(this);
    }

    @Override
    protected InputListViewAdapter createAdapter() {
        return new InputListViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new InputListViewLayoutManager(context);
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserInputMsgListener listener) {
        this.listener = listener;
    }

    /** 向上传递 {@link UserInputMsg} 消息 */
    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        if (type != ViewGestureDetector.GestureType.SingleTap) {
            return;
        }

        int position;
        InputViewHolder holder = findVisibleInputViewUnder(data.x, data.y);
        if (holder == null) {
            if (data.x < getPaddingStart()) {
                position = 0;
            } else {
                position = -1;
            }
        } else {
            position = holder.getAdapterPosition();
        }

        UserInputSingleTapMsgData msgData = new UserInputSingleTapMsgData(this.positionInParent, position);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Input).data(msgData));
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

                update(msg.inputFactory, needToLockScrolling);
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

        // Note: 被嵌套的视图不绑定监听器，需通过其上层视图转发
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

    /** 设置当前视图对应的输入在上层输入列表中的位置 */
    public void setPositionInParent(int positionInParent) {
        this.positionInParent = positionInParent;
    }

    public void update(InputFactory inputFactory) {
        update(inputFactory, false);
    }

    public void update(InputFactory inputFactory, boolean needToLockScrolling) {
        List<InputViewData> dataList = inputFactory.getInputs();
        update(dataList, needToLockScrolling);
    }

    public void update(List<InputViewData> dataList, boolean needToLockScrolling) {
        getAdapter().updateItems(dataList);

        if (!needToLockScrolling) {
            scrollToSelectedInput(dataList);
        }
    }

    // =============================== End: 更新视图 ===================================

    /** 滚动到选中输入的位置，确保其处于可见区域 */
    protected void scrollToSelectedInput(List<InputViewData> dataList) {
        InputViewData selectedData = dataList.stream().filter(d -> d.pending).findFirst().get();
        View view = getSelectedInputView(selectedData);

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

        int position = selectedData.position;
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
    private View getSelectedInputView(InputViewData selectedData) {
        LayoutManager layoutManager = getLayoutManager();
        View view = layoutManager.findViewByPosition(selectedData.position);

        if (view == null || selectedData.type != MathExpr) {
            return view;
        }

        // 查找算术输入的内部视图：嵌套唯一的 InputListView
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);

            if (child instanceof InputListViewReadonly) {
                InputListViewReadonly ro = (InputListViewReadonly) child;
                InputViewData selected = selectedData.inputs.stream().filter(d -> d.pending).findFirst().get();

                return ro.getLayoutManager().findViewByPosition(selected.position);
            }
        }
        return null;
    }
}
