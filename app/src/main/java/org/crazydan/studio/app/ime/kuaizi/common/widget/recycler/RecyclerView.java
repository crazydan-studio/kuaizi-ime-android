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

import java.util.Objects;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-29
 */
public abstract class RecyclerView<A extends RecyclerViewAdapter<I, ?>, I>
        extends androidx.recyclerview.widget.RecyclerView {

    public RecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        RecyclerViewAdapter<?, ?> adapter = createAdapter();
        setAdapter(adapter);

        LayoutManager layoutManager = createLayoutManager(context);
        setLayoutManager(layoutManager);
    }

    /** 创建 {@link RecyclerViewAdapter} */
    abstract protected A createAdapter();

    /** 创建 {@link LayoutManager} */
    abstract protected LayoutManager createLayoutManager(Context context);

    /**
     * 在 {@link RecyclerViewGestureDetector} 中用于检查视图更新前后的
     * {@link RecyclerViewAdapter} 数据项是否相同
     * <p/>
     * 默认通过 {@link Objects#equals} 判断，且两个数据项均不为 null
     * <p/>
     * 仅在启用了 {@link RecyclerViewGestureDetector} 的情况下才需要重载该接口
     */
    protected boolean isSameAdapterItem(I item1, I item2) {
        return Objects.equals(item1, item2);
    }

    @NonNull
    @Override
    public A getAdapter() {return (A) super.getAdapter();}

    @NonNull
    @Override
    public LayoutManager getLayoutManager() {return super.getLayoutManager();}

    /** 获取与指定视图绑定的 {@link RecyclerViewAdapter} 数据项 */
    public I getAdapterItem(View view) {
        if (view == null) {
            return null;
        }

        RecyclerViewHolder holder = (RecyclerViewHolder) getChildViewHolder(view);

        return getAdapterItem(holder);
    }

    /** 获取与指定 {@link RecyclerViewHolder} 绑定的 {@link RecyclerViewAdapter} 数据项 */
    public I getAdapterItem(ViewHolder holder) {
        A adapter = getAdapter();

        return (I) adapter.getItem(holder);
    }

    /** 获取与指定坐标下的视图所绑定的 {@link RecyclerViewAdapter} 数据项 */
    public I getAdapterItemUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        return getAdapterItem(view);
    }

    /** 获取与指定坐标下的视图所绑定的 {@link RecyclerViewAdapter} */
    public <V extends RecyclerViewHolder> V getViewHolderUnder(float x, float y) {
        View view = findChildViewUnder(x, y);
        return view != null ? (V) getChildViewHolder(view) : null;
    }
}
