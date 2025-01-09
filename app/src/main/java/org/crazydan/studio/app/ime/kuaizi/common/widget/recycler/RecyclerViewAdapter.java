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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class RecyclerViewAdapter<I, H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<H> {
    private final ItemUpdatePolicy itemUpdatePolicy;

    protected List<I> items = new ArrayList<>();

    protected RecyclerViewAdapter(ItemUpdatePolicy itemUpdatePolicy) {
        this.itemUpdatePolicy = itemUpdatePolicy;
    }

    // =================== Start: 数据绑定 ==================

    /**
     * 更新数据项列表
     *
     * @return 更新前的数据项列表
     */
    public List<I> updateItems(List<I> newItems) {
        List<I> oldItems = this.items;
        this.items = newItems;

        switch (this.itemUpdatePolicy) {
            case full: {
                updateItemsByFull(oldItems, newItems);
                break;
            }
            case differ: {
                updateItemsByDiffer(oldItems, newItems);
                break;
            }
            case manual: {
                // 以重载接口方式实现
                break;
            }
        }
        return oldItems;
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    /** 根据 {@link RecyclerView.ViewHolder} 获取与其绑定的数据项 */
    public I getItem(RecyclerView.ViewHolder holder) {
        int position = holder != null ? holder.getAdapterPosition() : -1;

        return getItem(position);
    }

    /** 获取指定位置的数据项 */
    public I getItem(int position) {
        return position < 0 ? null : this.items.get(position);
    }

    /** 根据新旧列表更新差异数据项 */
    protected void updateItemsByDiffer(List<I> oldItems, List<I> newItems) {
        updateItemsByComparator(oldItems, newItems, (o, n) -> Objects.equals(o, n) ? 0 : -1);
    }

    /** 根据新旧列表更新全部数据项 */
    protected void updateItemsByFull(List<I> oldItems, List<I> newItems) {
        updateItemsByComparator(oldItems, newItems, (o, n) -> -1);
    }

    /** 根据新旧列表更新数据项：指定差异判定函数 */
    private void updateItemsByComparator(List<I> oldItems, List<I> newItems, Comparator<I> comparator) {
        int oldItemsSize = oldItems != null ? oldItems.size() : 0;
        int newItemsSize = newItems != null ? newItems.size() : 0;

        if (oldItemsSize > newItemsSize) {
            for (int i = newItemsSize; i < oldItemsSize; i++) {
                notifyItemRemoved(i);
            }
        } else if (oldItemsSize < newItemsSize) {
            for (int i = oldItemsSize; i < newItemsSize; i++) {
                notifyItemInserted(i);
            }
        }

        int size = Math.min(oldItemsSize, newItemsSize);
        for (int i = 0; i < size; i++) {
            I oldItem = oldItems.get(i);
            I newItem = newItems.get(i);

            if (comparator.compare(oldItem, newItem) != 0) {
                notifyItemChanged(i);
            }
        }
    }

    // =================== End: 数据绑定 ==================

    // =================== Start: 视图相关 ==================

    protected static View inflateItemView(ViewGroup parent, int itemViewResId) {
        return inflateItemView(parent.getContext(), parent, itemViewResId);
    }

    protected static View inflateItemView(Context context, ViewGroup parent, int itemViewResId) {
        return LayoutInflater.from(context).inflate(itemViewResId, parent, false);
    }

    // =================== End: 视图相关 ==================

    /** 数据项更新策略 */
    protected enum ItemUpdatePolicy {
        /** 全量更新 */
        full,
        /** 差异更新 */
        differ,
        /** 自行确定 */
        manual,
    }
}
