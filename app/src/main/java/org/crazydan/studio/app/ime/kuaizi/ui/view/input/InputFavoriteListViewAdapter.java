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

import java.util.ArrayList;
import java.util.List;

import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputFavoriteListView;

/**
 * {@link InputFavoriteListView} 的 {@link RecyclerView} 适配器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-13
 */
public class InputFavoriteListViewAdapter extends RecyclerViewAdapter<InputFavorite, InputFavoriteViewHolder> {
    private final SparseBooleanArray selected;
    private final Runnable onSelectedListener;

    public InputFavoriteListViewAdapter(Runnable onSelectedListener) {
        super(ItemUpdatePolicy.differ);

        this.selected = new SparseBooleanArray();
        this.onSelectedListener = onSelectedListener;
    }

    @Override
    public List<InputFavorite> updateItems(List<InputFavorite> newItems) {
        this.selected.clear();

        return super.updateItems(newItems);
    }

    /** @return 已选中项的 {@link InputFavorite#id} */
    public List<Integer> getSelectedItems() {
        List<Integer> list = new ArrayList<>(this.selected.size());

        for (int i = 0; i < this.items.size(); i++) {
            InputFavorite item = this.items.get(i);
            boolean selected = this.selected.get(i, false);

            if (selected) {
                list.add(item.id);
            }
        }
        return list;
    }

    @Override
    public void onBindViewHolder(@NonNull InputFavoriteViewHolder holder, int position) {
        InputFavorite item = getItem(position);
        boolean selected = this.selected.get(position, false);

        holder.bind(item, selected, () -> {
            boolean checked = this.selected.get(position, false);
            this.selected.put(position, !checked);

            this.onSelectedListener.run();
        });
    }

    @NonNull
    @Override
    public InputFavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflateItemView(parent, R.layout.input_favorite_view);
        return new InputFavoriteViewHolder(view);
    }
}
