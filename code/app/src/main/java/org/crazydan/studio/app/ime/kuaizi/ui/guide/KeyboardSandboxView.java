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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardView;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * 键盘的沙盒视图，用于获取与键盘按键相同的图形
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-22
 */
public class KeyboardSandboxView extends FrameLayout implements KeyImageRender {
    private final Map<String, Key> keys = new LinkedHashMap<>();
    private final Map<String, Drawable> imageCache = new HashMap<>();

    private KeyboardView keyboardView;

    public KeyboardSandboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** 更新视图 */
    public void update(int themeResId, HexagonOrientation gridItemOrientation) {
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        ThemeUtils.inflate(this, R.layout.sandbox_keyboard_view, themeResId, true);
        this.keyboardView = findViewById(R.id.keyboard);

        this.imageCache.clear();

        this.keyboardView.setGridItemOrientation(gridItemOrientation);
        // Note: 在所要绘制的按键超过矩阵可含按键数量时（8x6），
        // 可适当增加行列数，只要确保与实际按键的相对大小一致且图像不模糊即可
        this.keyboardView.update(new Key[][] { this.keys.values().toArray(new Key[0]) }, 8, 6, false);
    }

    @Override
    public String withKey(Key key) {
        if (key == null) {
            return "";
        }

        String code = "key-hash:" + key.hashCode();
        if (!this.keys.containsKey(code)) {
            this.keys.put(code, key);
        }
        return code;
    }

    @Override
    public Drawable renderKey(String code, int width, int height) {
        String key = String.format(Locale.getDefault(), "%s:%d:%d", code, width, height);

        return this.imageCache.computeIfAbsent(key, (k) -> {
            View view = this.keyboardView.getItemViewByKey(this.keys.get(code));
            return ViewUtils.toDrawable(view, width, height);
        });
    }
}
