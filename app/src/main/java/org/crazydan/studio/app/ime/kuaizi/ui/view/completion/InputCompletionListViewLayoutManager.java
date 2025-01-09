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

package org.crazydan.studio.app.ime.kuaizi.ui.view.completion;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputCompletionListView;

/**
 * {@link InputCompletionListView} 的{@link RecyclerView}布局器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputCompletionListViewLayoutManager extends LinearLayoutManager {
    private boolean scrollEnabled = true;

    public InputCompletionListViewLayoutManager(Context context) {
        super(context, RecyclerView.HORIZONTAL, false);
    }

    @Override
    public boolean canScrollHorizontally() {
        return this.scrollEnabled;
    }

    public void enableScroll(boolean enabled) {
        this.scrollEnabled = enabled;
    }
}
