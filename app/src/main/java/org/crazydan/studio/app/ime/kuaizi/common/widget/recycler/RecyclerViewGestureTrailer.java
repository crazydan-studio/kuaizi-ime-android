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

import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureDetector;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ViewGestureTrailer;

/**
 * 在 {@link RecyclerView} 之上绘制滑屏轨迹
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-27
 */
public class RecyclerViewGestureTrailer extends ViewGestureTrailer implements ViewGestureDetector.Listener {
    private final RecyclerView recyclerView;

    public RecyclerViewGestureTrailer(RecyclerView recyclerView, boolean disabled) {
        this.recyclerView = recyclerView;

        setDisabled(disabled);
    }

    @Override
    public void onGesture(ViewGestureDetector.GestureType type, ViewGestureDetector.GestureData data) {
        if (!isDisabled()) {
            this.recyclerView.invalidate();
        }

        super.onGesture(type, data);
    }
}
