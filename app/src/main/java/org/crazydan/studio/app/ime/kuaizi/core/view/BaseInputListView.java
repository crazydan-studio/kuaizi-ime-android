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

import java.util.function.Supplier;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.CharInputView;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.InputView;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.InputViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.InputViewGestureListener;
import org.crazydan.studio.app.ime.kuaizi.core.view.input.InputViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewGestureDetector;

/**
 * {@link InputListView} 的基类
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public class BaseInputListView extends RecyclerView {
    private final InputViewAdapter adapter;
    private final InputViewLayoutManager layoutManager;
    private final RecyclerViewGestureDetector gesture;

    private Supplier<InputList> inputListGetter;

    public BaseInputListView(Context context, @Nullable AttributeSet attrs) {
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        this.gesture.reset();
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

        int position = inputList.getSelectedIndex();
        scrollToSelected(position);
    }

    private void scrollToSelected(int position) {
        View item = this.layoutManager.findViewByPosition(position);

        int offset = 0;
        if (item != null) {
            Point parentLocation = ViewUtils.getLocationInWindow(this);
            int parentWidth = getMeasuredWidth();
            int parentPadding = getPaddingLeft() + getPaddingRight();
            int parentLeft = parentLocation.x;
            int parentRight = parentLeft + parentWidth;

            Point itemLocation = ViewUtils.getLocationInWindow(item);
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
}
