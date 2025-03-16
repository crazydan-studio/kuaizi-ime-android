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
    private final ItemListener listener;
    private final List<Integer> selected;

    public InputFavoriteListViewAdapter(ItemListener listener) {
        super(ItemUpdatePolicy.differ);

        this.selected = new ArrayList<>();
        this.listener = listener;
    }

    public void updateItems(List<InputFavorite> newItems, boolean reset) {
        if (reset) {
            this.selected.clear();
        }

        super.updateItems(newItems);
    }

    /** @return 已选中项的 {@link InputFavorite#id} */
    public List<Integer> getSelectedItems() {
        return new ArrayList<>(this.selected);
    }

    @Override
    public void onBindViewHolder(@NonNull InputFavoriteViewHolder holder, int position) {
        InputFavorite item = getItem(position);
        boolean selected = this.selected.contains(item.id);

        holder.bind(item, selected, (checked) -> {
            if (checked) {
                this.selected.add(item.id);
            } else {
                this.selected.remove(item.id);
            }

            this.listener.onItemCheck(position, checked);
        }, () -> {
            this.listener.onItemPaste(position);
        });
    }

    @NonNull
    @Override
    public InputFavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflateItemView(parent, R.layout.input_favorite_view);
        return new InputFavoriteViewHolder(view);
    }

    public interface ItemListener {

        void onItemCheck(int position, boolean checked);

        void onItemPaste(int position);
    }
}
