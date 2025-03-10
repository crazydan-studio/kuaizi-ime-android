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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputQuickView;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputClipViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputCompletionViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputQuickPlaceholderViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputQuickViewData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.quick.InputQuickViewHolder;

/**
 * {@link InputQuickView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-10
 */
public class InputQuickViewAdapter extends RecyclerViewAdapter<InputQuickViewData, InputQuickViewHolder> {
    private static final int VIEW_TYPE_PLACEHOLDER = -1;
    private static final int VIEW_TYPE_COMPLETION = 0;
    private static final int VIEW_TYPE_CLIP = 1;

    public InputQuickViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    @Override
    public void onBindViewHolder(@NonNull InputQuickViewHolder holder, int position) {
        InputQuickViewData item = getItem(position);

        holder.bind(item.data);
    }

    @Override
    public int getItemViewType(int position) {
        InputQuickViewData item = getItem(position);

        switch (item.type) {
            case input_completion: {
                return VIEW_TYPE_COMPLETION;
            }
            case input_clip: {
                return VIEW_TYPE_CLIP;
            }
        }
        return VIEW_TYPE_PLACEHOLDER;
    }

    @NonNull
    @Override
    public InputQuickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_COMPLETION: {
                View view = inflateItemView(parent, R.layout.input_completion_view);
                return new InputCompletionViewHolder(view);
            }
            case VIEW_TYPE_CLIP: {
                View view = inflateItemView(parent, R.layout.input_clip_view);
                return new InputClipViewHolder(view);
            }
            default: {
                View view = inflateItemView(parent, R.layout.input_quick_placeholder_view);
                return new InputQuickPlaceholderViewHolder(view);
            }
        }
    }
}
