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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.Objects;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
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

        update(keys, columns, rows, null, isLeftHandMode);
    }

    /** 更新视图 */
    protected void update(
            Key[][] keys, int columns, int rows, Integer themeResId, boolean isLeftHandMode
    ) {
        XPadKey xPadKey = getXPadKeyFrom(keys);
        boolean xPadEnabled = xPadKey != null;
        HexagonOrientation orientation = xPadEnabled ? HexagonOrientation.FLAT_TOP : this.gridItemOrientation;

        KeyboardViewLayoutManager layoutManager = (KeyboardViewLayoutManager) getLayoutManager();
        layoutManager.setReversed(isLeftHandMode);
        layoutManager.enableXPad(xPadEnabled);
        layoutManager.setGridItemOrientation(orientation);
        layoutManager.configGrid(columns, rows, this.gridItemMinRadius, this.gridItemSpacing, this.gridMaxPaddingRight);

        if (xPadEnabled) {
            // Note：为避免重建 view 造成 X 面板视图刷新，采用重绑定方式做视图内部的更新
            XPadKeyViewHolder holder = getXPadKeyViewHolder(xPadKey);
            if (holder != null) {
                holder.bind(xPadKey);
            }
        }

        getAdapter().updateItems(keys, themeResId, orientation);
    }

    public float getBottomSpacing() {
        KeyboardViewLayoutManager layoutManager = (KeyboardViewLayoutManager) getLayoutManager();
        return layoutManager.getGridPaddingBottom();
    }

    public XPadKeyViewHolder getXPadKeyViewHolder() {
        XPadKey xPadKey = getAdapter().getXPadKey();
        return getXPadKeyViewHolder(xPadKey);
    }

    private XPadKeyViewHolder getXPadKeyViewHolder(XPadKey xPadKey) {
        return ((XPadKeyViewHolder) getVisibleKeyViewHolder(getItemViewByKey(xPadKey)));
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
