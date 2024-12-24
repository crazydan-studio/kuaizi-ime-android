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
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;

/**
 * {@link RecyclerView} 的手势检测器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-13
 */
public class RecyclerViewGestureDetector extends ViewGestureDetector implements RecyclerView.OnItemTouchListener {
    private RecyclerViewData prevViewData;

    /** 绑定到 {@link RecyclerView} 上 */
    public RecyclerViewGestureDetector bind(RecyclerView view) {
        view.addOnItemTouchListener(this);
        return this;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        // Note: onTouchEvent 默认只在该函数返回 true 时才执行，
        // 故，在该函数始终返回 false 时，只能在该函数中执行手势检测处理
        onTouchEvent(rv, e);

        // 始终返回 false 以避免禁用 RecyclerView 的滚动功能
        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            // 若触发事件的视图数据发生了变化，则需要重置事件监测
            if (hasChangedViewData(rv, e)) {
                reset();
            }
        }

        //this.log.debug("%s: fire event %s", rv.getClass().getSimpleName(), getActionName(e));

        onTouchEvent(e);
    }

    private boolean hasChangedViewData(RecyclerView rv, MotionEvent e) {
        RecyclerViewData oldViewData = this.prevViewData;

        // 当某个数据的视图更新后，其 view 实例可能会重建，
        // 使得在双击、长按 tick 等事件周期内发送了视图更新的数据不能接收这类事件，
        // 因为监测状态被重置了，所以，只能根据数据自身是否变化做监测重置判断
        View view = rv.findChildViewUnder(e.getX(), e.getY());
        RecyclerViewData newViewData = view != null
                                       ? ((RecyclerViewHolder<?>) rv.getChildViewHolder(view)).getData()
                                       : null;

        this.prevViewData = newViewData;

        return (oldViewData != null && !oldViewData.isSameWith(newViewData)) //
               || oldViewData != newViewData;
    }
}
