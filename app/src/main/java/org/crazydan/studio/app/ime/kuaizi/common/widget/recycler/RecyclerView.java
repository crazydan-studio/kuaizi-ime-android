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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-29
 */
public abstract class RecyclerView<A extends RecyclerViewAdapter<?, ?>>
        extends androidx.recyclerview.widget.RecyclerView {

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        RecyclerViewAdapter<?, ?> adapter = createAdapter();
        setAdapter(adapter);
    }

    /** 创建{@link RecyclerViewAdapter 适配器} */
    abstract protected A createAdapter();

    @NonNull
    @Override
    public A getAdapter() {
        return (A) super.getAdapter();
    }

    /** 获取与指定视图绑定的{@link RecyclerViewAdapter 适配器}数据项 */
    public <I> I getAdapterItem(View view) {
        if (view == null) {
            return null;
        }

        RecyclerViewHolder holder = (RecyclerViewHolder) getChildViewHolder(view);

        return getAdapterItem(holder);
    }

    /** 获取与指定 {@link RecyclerViewHolder} 绑定的{@link RecyclerViewAdapter 适配器}数据项 */
    public <I> I getAdapterItem(ViewHolder holder) {
        A adapter = getAdapter();

        return (I) adapter.getItem(holder);
    }
}
