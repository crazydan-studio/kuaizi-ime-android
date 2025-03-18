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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.Objects;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.log.Logger;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyViewHolder;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyboardViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.KeyboardViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.XPadKeyViewHolder;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link KeyboardView} 的基类
 * <p/>
 * 仅包含按键布局相关的职能
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-22
 */
public abstract class KeyboardViewBase extends RecyclerView<KeyboardViewAdapter, Key> {
    protected final Logger log = Logger.getLogger(getClass());

    private final float gridMaxPaddingRight;
    private final float gridItemMinRadius;
    private final float gridItemSpacing;

    private HexagonOrientation gridItemOrientation = HexagonOrientation.POINTY_TOP;

    public KeyboardViewBase(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.gridMaxPaddingRight = ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_right_spacing);
        this.gridItemMinRadius = ScreenUtils.pxFromDimension(getContext(), R.dimen.key_view_bg_min_radius);
        this.gridItemSpacing = ScreenUtils.pxFromDimension(getContext(), R.dimen.key_view_spacing);
    }

    @Override
    protected KeyboardViewAdapter createAdapter() {
        return new KeyboardViewAdapter();
    }

    @Override
    protected LayoutManager createLayoutManager(Context context) {
        return new KeyboardViewLayoutManager();
    }

    public void setGridItemOrientation(HexagonOrientation gridItemOrientation) {
        this.gridItemOrientation = gridItemOrientation;
    }

    /** 更新视图 */
    protected void update(Key[][] keys, boolean isLeftHandMode) {
        int columns = keys[0].length;
        int rows = keys.length;

        update(keys, columns, rows, isLeftHandMode);
    }

    /** 更新视图 */
    protected void update(Key[][] keys, int columns, int rows, boolean isLeftHandMode) {
        XPadKey oldXPadKey = getAdapter().getXPadKey();
        // Note: 可能会切换到表情、符号等非 XPad 输入面板上，此时，XPad 是不可用的
        XPadKey newXPadKey = getXPadKeyFrom(keys);
        boolean xPadEnabled = newXPadKey != null;
        HexagonOrientation orientation = xPadEnabled ? HexagonOrientation.FLAT_TOP : this.gridItemOrientation;

        KeyboardViewLayoutManager layoutManager = (KeyboardViewLayoutManager) getLayoutManager();
        layoutManager.setReversed(isLeftHandMode);
        layoutManager.enableXPad(xPadEnabled);
        layoutManager.setGridItemOrientation(orientation);
        layoutManager.configGrid(columns, rows, this.gridItemMinRadius, this.gridItemSpacing, this.gridMaxPaddingRight);

        // Note: XPadKey 将根据其 #compareTo 接口的返回值确定是否重建视图，
        // 而该值始终返回 0，即，保持其视图不变
        getAdapter().updateItems(keys, orientation);

        // Note：若 XPadKey 发生了变化，则手动更新 XPad 视图的内部
        if (xPadEnabled && !Objects.equals(oldXPadKey, newXPadKey)) {
            XPadKeyViewHolder holder = getXPadKeyViewHolder(newXPadKey);
            if (holder != null) {
                holder.bind(newXPadKey);
            }
        }
    }

    public float getBottomSpacing() {
        KeyboardViewLayoutManager layoutManager = (KeyboardViewLayoutManager) getLayoutManager();
        return layoutManager.getGridPaddingBottom();
    }

    /** Note: 若只是 {@link XPadKey} 内部的按键布局变化，则 {@link XPadKeyViewHolder} 将不会被重建 */
    public XPadKeyViewHolder getXPadKeyViewHolder() {
        XPadKey xPadKey = getAdapter().getXPadKey();
        return getXPadKeyViewHolder(xPadKey);
    }

    private XPadKeyViewHolder getXPadKeyViewHolder(XPadKey xPadKey) {
        View view = getItemViewByKey(xPadKey);
        // Note: 视图更新与数据绑定不是同步的，在不同键盘发生切换时，
        // 获取的视图类型与预期的会不一致，所以，需要对其做类型检查
        KeyViewHolder<?> holder = getVisibleKeyViewHolder(view);

        return holder instanceof XPadKeyViewHolder ? (XPadKeyViewHolder) holder : null;
    }

    /** 找到指定坐标下可见的 {@link  KeyViewHolder} */
    public KeyViewHolder<?> findVisibleKeyViewHolderUnderLoose(float x, float y) {
        KeyboardViewLayoutManager layoutManager = (KeyboardViewLayoutManager) getLayoutManager();
        View child = layoutManager.findChildViewUnderLoose(x, y);

        return getVisibleKeyViewHolder(child);
    }

    private KeyViewHolder<?> getVisibleKeyViewHolder(View view) {
        KeyViewHolder<?> holder = view != null ? (KeyViewHolder<?>) getChildViewHolder(view) : null;

        return holder != null && !holder.isHidden() ? holder : null;
    }

    protected View getItemViewByKey(Key key) {
        if (key == null) {
            return null;
        }

        LayoutManager layoutManager = getLayoutManager();

        int total = layoutManager.getChildCount();
        for (int i = 0; i < total; i++) {
            View view = layoutManager.getChildAt(i);
            KeyViewHolder<?> holder = getVisibleKeyViewHolder(view);

            Key viewKey = getAdapterItem(holder);
            if (Objects.equals(viewKey, key)) {
                return view;
            }
        }
        return null;
    }

    protected XPadKey getXPadKeyFrom(Key[][] keys) {
        for (Key[] key : keys) {
            for (Key k : key) {
                if (k instanceof XPadKey) {
                    return (XPadKey) k;
                }
            }
        }
        return null;
    }
}
