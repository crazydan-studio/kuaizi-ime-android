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
        I newAdapterItem = rv.getAdapterItemUnder(e.getX(), e.getY());

        I oldAdapterItem = this.prevAdapterItem;
        this.prevAdapterItem = newAdapterItem;

        return (oldAdapterItem != null //
                && !rv.isSameAdapterItem(oldAdapterItem, newAdapterItem)) //
               || oldAdapterItem != newAdapterItem;
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
