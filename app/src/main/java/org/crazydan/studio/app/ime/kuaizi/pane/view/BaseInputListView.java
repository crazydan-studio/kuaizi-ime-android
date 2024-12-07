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

import java.util.function.Supplier;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.view.input.CharInputView;
import org.crazydan.studio.app.ime.kuaizi.pane.view.input.InputView;
import org.crazydan.studio.app.ime.kuaizi.pane.view.input.InputViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.pane.view.input.InputViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewGestureDetector;

/**
 * {@link InputListView} 的基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class BaseInputListView extends RecyclerView implements ViewGestureDetector.Listener {
    private final InputViewAdapter adapter;
    private final InputViewLayoutManager layoutManager;

    private boolean needToLockScrolling;
    private Supplier<InputList> inputListGetter;

    public BaseInputListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new InputViewAdapter();
        this.layoutManager = new InputViewLayoutManager(context);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
        // Note：取消动画以确保输入能够直接显隐，不做淡化
        setItemAnimator(null);

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

    public void update(boolean canBeSelected) {
        InputList inputList = getInputList();
        this.adapter.updateInputList(inputList, canBeSelected);

        if (!this.needToLockScrolling) {
            int position = inputList.getSelectedIndex();
            scrollToSelected(position);
        }
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        InputView<?> inputView = findVisibleInputViewUnder(data.x, data.y);

        if (type == ViewGestureDetector.GestureType.SingleTap) {
            onSingleTap(inputView, data);
        }
    }

    private void onSingleTap(InputView<?> inputView, ViewGestureDetector.GestureData data) {
        Input<?> input = determineInput(inputView, data);

        InputList inputList = getInputList();
        boolean isMathExprSelected = input.isMathExpr() //
                                     && (inputList.isSelected(input) //
                                         || inputList.getPending() == input);
        // 忽略对已选中算术表达式的处理，由其自身的视图做响应
        if (!isMathExprSelected) {
            // 若首次选中算术表达式，则需保持滚动条不动。
            // 因为在算术表达式长度超出可见区域时，
            // 滚动条移动会造成算术表达式视图直接接收 ACTION_CANCEL
            // 事件而丢失 ACTION_UP 事件，从而不能触发单击消息并进而选中算术表达式中的目标输入。
            // 注：事件是从父 InputList 传递到子 InputList 的，
            // 所以，无法优先处理子 InputList的事件
            this.needToLockScrolling = input.isMathExpr();

            inputList.trySelect(input);
        }
        this.needToLockScrolling = false;
    }

    private Input<?> determineInput(InputView<?> inputView, ViewGestureDetector.GestureData data) {
        Input<?> input = inputView != null ? inputView.getData() : null;

        if (input == null) {
            InputList inputList = getInputList();
            if (data.x < getPaddingStart()) {
                input = inputList.getFirstInput();
            } else {
                input = inputList.getLastInput();
            }
        }
        return input;
    }

    protected void scrollToSelected(int position) {
        View item = this.layoutManager.findViewByPosition(position);
        View itemInChildInputList = getSelectedInChildInputList(item);

        // 按子 InputList 中的选中输入进行滚动，以确保选中的算术输入在可见位置
        if (itemInChildInputList != null) {
            item = itemInChildInputList;
        }

        int offset = 0;
        if (item != null) {
            Point parentLocation = ViewUtils.getLocationOnScreen(this);
            int parentWidth = getMeasuredWidth();
            int parentPadding = getPaddingLeft() + getPaddingRight();
            int parentLeft = parentLocation.x;
            int parentRight = parentLeft + parentWidth;

            Point itemLocation = ViewUtils.getLocationOnScreen(item);
            int itemWidth = item.getMeasuredWidth();
            int itemLeft = itemLocation.x;
            int itemRight = itemLeft + itemWidth;
            int itemMaxRight = itemRight + parentPadding;

            // 项目宽度超出可见区域，则滚动位置需移至其尾部
            if (itemWidth + parentPadding > parentWidth) {
                offset = itemMaxRight - parentRight;

                // 已经移动完成，则不做处理。用于处理多次相邻调用的情况
                if (offset == 0) {
                    return;
                }
            }
            // 若项目已在可见区域内，且其右侧加上空白后未超出可见区域，则不需要滚动
            else if (itemLeft >= parentLeft) {
                offset = itemMaxRight - parentRight;

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

    /** 找到指定坐标下可见的{@link  InputView 输入视图} */
    private InputView<?> findVisibleInputViewUnder(float x, float y) {
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

    private View getSelectedInChildInputList(View view) {
        if (view == null) {
            return null;
        }

        for (int j = 0; j < ((ViewGroup) view).getChildCount(); j++) {
            View child = ((ViewGroup) view).getChildAt(j);

            if (child instanceof ReadonlyInputListView) {
                ReadonlyInputListView ro = (ReadonlyInputListView) child;
                int position = ro.getInputList().getSelectedIndex();

                return ro.getLayoutManager().findViewByPosition(position);
            }
        }
        return null;
    }
}
