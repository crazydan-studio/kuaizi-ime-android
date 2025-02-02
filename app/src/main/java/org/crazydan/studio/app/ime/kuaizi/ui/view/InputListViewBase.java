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
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.core.InputFactory;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.core.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputListViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputViewHolder;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Input;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData.POSITION_END_IN_INPUT_LIST;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData.POSITION_LEFT_IN_GAP_INPUT_PENDING;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData.POSITION_RIGHT_IN_GAP_INPUT_PENDING;
import static org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputSingleTapMsgData.POSITION_START_IN_INPUT_LIST;

/**
 * {@link InputListView} 的基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class InputListViewBase extends RecyclerView<InputListViewAdapter, InputViewData>
        implements ViewGestureDetector.Listener, InputMsgListener {
    protected final Logger log = Logger.getLogger(getClass());

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

        int position = findInputPositionUnder(data.at.x, data.at.y);

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
                // 注：事件是从上层 InputList 传递到嵌套 InputList 的，
                // 所以，无法优先处理嵌套 InputList 的事件
                boolean needToLockScrolling = msg.data().input instanceof MathExprInput;

                this.log.debug("Update view for message %s with locking scrolling: %s",
                               () -> new Object[] { msg.type, needToLockScrolling });

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
        if (inputFactory == null) {
            return;
        }

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
        InputViewData selectedData = dataList.stream().filter(d -> d.hasPending).findFirst().get();
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

    /** 找到指定坐标下的输入位置 */
    private int findInputPositionUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        InputViewHolder holder = view != null ? (InputViewHolder) getChildViewHolder(view) : null;

        // Note: 当前的 Gap 空格通过 Gap 视图的内边距实现，其他输入的宽度均为其实际显示宽度，
        // Gap 与其他视图之间不存在透明区域，故而，在指定位置下得到的输入视图即为实际的目标视图

        // 当 Gap 上正在输入时，其输入采用的是 CharInputViewHolder，
        // 并在其根视图的左右加上 margin 作为左右两侧的 Gap 间隔，但此时其左右并没有 Gap，
        // 也就没有相应的视图，此时点击其左右两侧的间隔区域，将找不到预期的 Gap 视图，
        // 因此，需要扩大查找范围，以确保能够找到 Gap 的输入视图
        if (holder == null) {
            float gap = ScreenUtils.pxFromDimension(getContext(), R.dimen.gap_input_width);
            InputViewData leftInput = findInputUnderByRange(x, y, -2 * gap);
            InputViewData rightInput = findInputUnderByRange(x, y, 2 * gap);

            // Note: 同一时刻只有一个输入为待输入状态
            if (leftInput != null && leftInput.hasPending) {
                // 当前位置在 Gap 输入视图的右侧
                return POSITION_RIGHT_IN_GAP_INPUT_PENDING;
            } else if (rightInput != null && rightInput.hasPending) {
                // 当前位置在 Gap 输入视图的左侧
                return POSITION_LEFT_IN_GAP_INPUT_PENDING;
            }
        }

        if (holder == null) {
            if (x < getPaddingStart()) {
                return POSITION_START_IN_INPUT_LIST;
            } else {
                return POSITION_END_IN_INPUT_LIST;
            }
        } else {
            return holder.getAdapterPosition();
        }
    }

    /** 获取选中输入的视图，若选中输入为算术输入，则获取其内部所选中的输入视图 */
    private View getSelectedInputView(InputViewData selectedData) {
        LayoutManager layoutManager = getLayoutManager();
        View view = layoutManager.findViewByPosition(selectedData.position);

        if (view == null || selectedData.type != InputViewData.Type.MathExpr) {
            return view;
        }

        // 查找算术输入的内部视图：嵌套唯一的 InputListView
        for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View child = ((ViewGroup) view).getChildAt(i);

            if (child instanceof InputListViewReadonly) {
                InputListViewReadonly ro = (InputListViewReadonly) child;
                InputViewData selected = selectedData.inputs.stream().filter(d -> d.hasPending).findFirst().get();

                return ro.getLayoutManager().findViewByPosition(selected.position);
            }
        }
        return null;
    }

    /** 从某一点开始，在指定的位置范围内查找输入 */
    private InputViewData findInputUnderByRange(float x, float y, float range) {
        // 按 1dp 的间隔逼近查找
        float delta = ScreenUtils.dpToPx(1);
        if (range < 0) {
            delta = -delta;
        }

        float dx = delta;
        while (range < 0 ? dx > range : dx < range) {
            InputViewData input = findAdapterItemUnder(x + dx, y);

            if (input != null) {
                return input;
            }
            dx += delta;
        }
        return null;
    }
}
