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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.ui.view.KeyboardViewBase;

/**
 * 键盘的沙盒视图，用于获取与键盘按键相同的图形
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-22
 */
public class KeyboardSandboxView extends KeyboardViewBase implements KeyImageRender {
    private final Map<String, Key<?>> keys = new LinkedHashMap<>();

    private final Map<String, Drawable> imageCache = new HashMap<>();

    public KeyboardSandboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** 在涉及添加视图的操作完成后，统一进行全部视图的更新 */
    public <T> T withMutation(int themeResId, Supplier<T> mutation) {
        T result = mutation.get();

        this.imageCache.clear();
        update(new Key[][] { this.keys.values().toArray(new Key[0]) }, 8, 6, themeResId, false);

        return result;
    }

    @Override
    public String withKey(Key<?> key) {
        if (key == null) {
            return "";
        }

        String code = "key-hash:" + key.hashCode();
        if (!this.keys.containsKey(code)) {
            // TODO 已添加的 key 数量超过 8x6 时，如何处理？
            this.keys.put(code, key);
        }
        return code;
    }

    @Override
    public Drawable renderKey(String code, int width, int height) {
        String key = String.format(Locale.getDefault(), "%s:%d:%d", code, width, height);

        return this.imageCache.computeIfAbsent(key, (k) -> {
            View view = getItemViewByKey(this.keys.get(code));
            return ViewUtils.toDrawable(view, width, height);
        });
    }
}
