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
public abstract class RecyclerViewAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    protected static View inflateItemView(ViewGroup parent, int itemViewResId) {
        return inflateItemView(parent.getContext(), parent, itemViewResId);
    }

    protected static View inflateItemView(Context context, ViewGroup parent, int itemViewResId) {
        return LayoutInflater.from(context).inflate(itemViewResId, parent, false);
    }

    protected <I> void updateItems(List<I> oldItems, List<I> newItems) {
        updateItems(oldItems, newItems, (o, n) -> Objects.equals(o, n) ? 0 : -1);
    }

    protected <I> void updateItems(List<I> oldItems, List<I> newItems, Comparator<I> comparator) {
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
}
