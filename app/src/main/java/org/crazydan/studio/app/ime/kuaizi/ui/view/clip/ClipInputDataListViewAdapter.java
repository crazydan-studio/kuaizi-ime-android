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

package org.crazydan.studio.app.ime.kuaizi.ui.view.clip;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.input.clip.ClipInputData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ClipInputDataListView;

/**
 * {@link ClipInputDataListView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-08
 */
public class ClipInputDataListViewAdapter extends RecyclerViewAdapter<ClipInputData, ClipInputDataViewHolder> {

    public ClipInputDataListViewAdapter() {
        super(ItemUpdatePolicy.differ);
    }

    @Override
    public void onBindViewHolder(@NonNull ClipInputDataViewHolder holder, int position) {
        ClipInputData item = getItem(position);

        holder.bind(item);
    }

    @NonNull
    @Override
    public ClipInputDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflateItemView(parent, R.layout.clip_input_data_view);
        return new ClipInputDataViewHolder(view);
    }
}
