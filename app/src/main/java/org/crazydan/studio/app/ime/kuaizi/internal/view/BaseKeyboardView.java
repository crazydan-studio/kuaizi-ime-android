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

package org.crazydan.studio.app.ime.kuaizi.internal.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.KeyViewLayoutManager;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.XPadKeyView;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link KeyboardView} 的基类
 * <p/>
 * 仅包含按键布局相关的职能
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-22
 */
public abstract class BaseKeyboardView extends RecyclerView {
    private final HexagonOrientation keyViewOrientation = HexagonOrientation.POINTY_TOP;
    private final float gridMaxPaddingRight;
    private final float gridItemMinRadius;
    private final float gridItemSpacing;

    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;

    public BaseKeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.gridMaxPaddingRight = ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_right_spacing);
        this.gridItemMinRadius = ScreenUtils.pxFromDimension(getContext(), R.dimen.key_view_bg_min_radius);
        this.gridItemSpacing = ScreenUtils.pxFromDimension(getContext(), R.dimen.key_view_spacing);

        this.adapter = new KeyViewAdapter(this.keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(this.keyViewOrientation);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
    }

    protected void updateKeys(Key<?>[][] keys, boolean isLeftHandMode) {
        int columns = keys[0].length;
        int rows = keys.length;

        updateKeys(keys, columns, rows, null, isLeftHandMode);
    }

    protected void updateKeys(Key<?>[][] keys, int columns, int rows, Integer themeResId, boolean isLeftHandMode) {
        XPadKey xPadKey = getXPadKey(keys);
        boolean xPadEnabled = xPadKey != null;
        HexagonOrientation orientation = xPadEnabled ? HexagonOrientation.FLAT_TOP : this.keyViewOrientation;

        this.layoutManager.setReversed(isLeftHandMode);
        this.layoutManager.enableXPad(xPadEnabled);
        this.layoutManager.setGridItemOrientation(orientation);
        this.layoutManager.configGrid(columns,
                                      rows,
                                      this.gridItemMinRadius,
                                      this.gridItemSpacing,
                                      this.gridMaxPaddingRight);

        if (xPadEnabled) {
            // Note：为避免重建 view 造成 X 面板视图刷新，采用重绑定方式做视图内部的更新
            XPadKeyView xPadKeyView = ((XPadKeyView) getVisibleKeyView(getItemViewByKey(xPadKey)));
            if (xPadKeyView != null) {
                xPadKeyView.bind(xPadKey);
            }
        }

        this.adapter.updateKeys(keys, themeResId, orientation);
    }

    public double getBottomSpacing() {
        return this.layoutManager.getGridPaddingBottom();
    }

    /** 找到指定坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnderLoose(float x, float y) {
        View child = this.layoutManager.findChildViewUnderLoose(x, y);

        return getVisibleKeyView(child);
    }

    private KeyView<?, ?> getVisibleKeyView(View view) {
        KeyView<?, ?> keyView = view != null ? (KeyView<?, ?>) getChildViewHolder(view) : null;

        return keyView != null && !keyView.isHidden() ? keyView : null;
    }

    protected View getItemViewByKey(Key<?> key) {
        if (key == null) {
            return null;
        }

        int total = this.layoutManager.getChildCount();
        for (int i = 0; i < total; i++) {
            View view = this.layoutManager.getChildAt(i);
            KeyView<?, ?> keyView = getVisibleKeyView(view);

            if (keyView != null && keyView.getData().equals(key)) {
                return view;
            }
        }
        return null;
    }

    private XPadKey getXPadKey(Key<?>[][] keys) {
        for (Key<?>[] key : keys) {
            for (Key<?> k : key) {
                if (k instanceof XPadKey) {
                    return (XPadKey) k;
                }
            }
        }
        return null;
    }
}
