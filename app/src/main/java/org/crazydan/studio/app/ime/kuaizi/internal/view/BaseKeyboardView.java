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
    private final int keySpacing = 3;
    private final KeyViewAdapter adapter;
    private final KeyViewLayoutManager layoutManager;

    public BaseKeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        HexagonOrientation keyViewOrientation = HexagonOrientation.POINTY_TOP;

        this.adapter = new KeyViewAdapter(keyViewOrientation);
        this.layoutManager = new KeyViewLayoutManager(keyViewOrientation);

        setAdapter(this.adapter);
        setLayoutManager(this.layoutManager);
    }

    protected void updateKeys(Key<?>[][] keys, boolean isLeftHandMode) {
        int columns = keys[0].length;
        int rows = keys.length;

        updateKeys(keys, columns, rows, null, isLeftHandMode);
    }

    protected void updateKeys(Key<?>[][] keys, int columns, int rows, Integer themeResId, boolean isLeftHandMode) {
        int gridMaxPaddingRight = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_right_spacing);
        this.layoutManager.configGrid(columns, rows, this.keySpacing, gridMaxPaddingRight);
        this.layoutManager.setReverse(isLeftHandMode);

        boolean xPadEnabled = hasXPadKey(keys);
        this.layoutManager.enableXPad(xPadEnabled);

        this.adapter.updateKeys(keys, themeResId);
    }

    public double getBottomSpacing() {
        return this.layoutManager.getGridPaddingBottom();
    }

    /** 找到指定坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnderLoose(float x, float y) {
        View child = this.layoutManager.findChildViewUnderLoose(x, y);

        return getVisibleKeyView(child);
    }

    /** 找到指定坐标下可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewUnder(float x, float y) {
        View child = this.layoutManager.findChildViewUnder(x, y);

        return getVisibleKeyView(child);
    }

    /** 找到指定坐标附近可见的{@link  KeyView 按键视图} */
    public KeyView<?, ?> findVisibleKeyViewNear(float x, float y) {
        View child = this.layoutManager.findChildViewNear(x, y, this.keySpacing * 2);

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

    private boolean hasXPadKey(Key<?>[][] keys) {
        for (Key<?>[] key : keys) {
            for (Key<?> k : key) {
                if (k instanceof XPadKey) {
                    return true;
                }
            }
        }
        return false;
    }
}
