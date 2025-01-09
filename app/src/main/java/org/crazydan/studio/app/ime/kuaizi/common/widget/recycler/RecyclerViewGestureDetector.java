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

package org.crazydan.studio.app.ime.kuaizi.common.widget.recycler;

import android.view.MotionEvent;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;

/**
 * {@link RecyclerView} 的手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class RecyclerViewGestureDetector<I> extends ViewGestureDetector {
    private I prevAdapterItem;

    public RecyclerViewGestureDetector(RecyclerView<?, I> rv) {
        rv.addOnItemTouchListener(new ItemTouchListener());
    }

    private boolean hasChangedViewData(RecyclerView<?, I> rv, MotionEvent e) {
        // 当某个数据的视图更新后，其 view 实例可能会重建，
        // 使得在双击、长按 tick 等事件周期内 发生了视图更新 的数据不能接收这类事件，
        // 因为监测状态被重置了，所以，只能根据数据自身是否变化做监测重置判断
        I newAdapterItem = rv.findAdapterItemUnder(e.getX(), e.getY());

        I oldAdapterItem = this.prevAdapterItem;
        this.prevAdapterItem = newAdapterItem;

        this.log.debug("@@ Gesture Items: %s, %s", oldAdapterItem, newAdapterItem);

        if (oldAdapterItem != null && newAdapterItem != null) {
            return !rv.isSameAdapterItem(oldAdapterItem, newAdapterItem);
        }
        return oldAdapterItem != newAdapterItem;
    }

    private class ItemTouchListener implements androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }

        @Override
        public boolean onInterceptTouchEvent(
                @NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e
        ) {
            // Note: onTouchEvent 默认只在该函数返回 true 时才执行，
            // 故，在该函数始终返回 false 时，只能在该函数中执行手势检测处理
            onTouchEvent(rv, e);

            // 始终返回 false 以避免禁用 RecyclerView 的滚动功能
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull androidx.recyclerview.widget.RecyclerView rv, @NonNull MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                // 若触发事件的视图数据发生了变化，则需要重置事件监测
                if (hasChangedViewData((RecyclerView) rv, e)) {
                    reset();
                }
            }
            //this.log.debug("%s: fire event %s", rv.getClass().getSimpleName(), getActionName(e));

            RecyclerViewGestureDetector.this.onTouchEvent(e);
        }
    }
}
