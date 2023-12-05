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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.view.BaseKeyboardView;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * 动态布局的沙盒视图，用于获取布局后的按键视图的图形
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-22
 */
public class DynamicLayoutSandboxView extends BaseKeyboardView {
    private final Map<String, Key<?>> keys = new LinkedHashMap<>();

    private final Map<String, Drawable> imageCache = new HashMap<>();

    public DynamicLayoutSandboxView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** 在涉及添加视图的操作完成后，统一进行全部视图的更新 */
    public <T> T withMutation(int themeResId, Supplier<T> mutation) {
        T result = mutation.get();

        updateWithTheme(themeResId);

        return result;
    }

    /** 根据新的主题重新布局视图，以确保所有视图应用新的主题 */
    public void updateWithNewTheme(int themeResId) {
        // 清空
        updateKeys(new Key[0][0], false);

        updateWithTheme(themeResId);
    }

    public String withKey(Key<?> key) {
        if (key == null) {
            return "";
        }

        String code = "hash:" + key.hashCode();
        if (!this.keys.containsKey(code)) {
            this.keys.put(code, key);
        }

        return code;
    }

    public Drawable getImage(String code, int width, int height) {
        String key = String.format(Locale.getDefault(), "%s:%d:%d", code, width, height);

        return this.imageCache.computeIfAbsent(key, (k) -> {
            View view = getItemViewByKey(this.keys.get(code));
            return ViewUtils.toDrawable(view, width, height);
        });
    }

    public void updateWithTheme(int themeResId) {
        this.imageCache.clear();

        updateKeys(new Key[][] { this.keys.values().toArray(new Key[0]) }, 8, 6, themeResId, false);
    }
}
