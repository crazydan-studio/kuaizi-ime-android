/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.widget.recycler;

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
        if (oldItems.size() > newItems.size()) {
            for (int i = newItems.size(); i < oldItems.size(); i++) {
                notifyItemRemoved(i);
            }
        } else if (oldItems.size() < newItems.size()) {
            for (int i = oldItems.size(); i < newItems.size(); i++) {
                notifyItemInserted(i);
            }
        }

        int size = Math.min(oldItems.size(), newItems.size());
        for (int i = 0; i < size; i++) {
            I oldItem = oldItems.get(i);
            I newItem = newItems.get(i);

            if (!Objects.equals(oldItem, newItem)) {
                notifyItemChanged(i);
            }
        }
    }
}
