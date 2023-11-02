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

package org.crazydan.studio.app.ime.kuaizi.utils;

import java.util.function.Consumer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.crazydan.studio.app.ime.kuaizi.widget.Shadow;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-07
 */
public class ThemeUtils {

    /** 通过属性 id 获取颜色值 */
    public static int getColorByAttrId(Context context, int attrId) {
        // https://stackoverflow.com/questions/17277618/get-color-value-programmatically-when-its-a-reference-theme#answer-17277714
        return getValue(context, attrId).data;
    }

    /** 通过属性 id 获取字符串值 */
    public static String getStringByAttrId(Context context, int attrId) {
        return getValue(context, attrId).string.toString();
    }

    /** 构造资源视图并应用指定的主题样式 */
    public static <T extends View> T inflate(ViewGroup root, int resId, int themeResId, boolean attachToRoot) {
        // https://stackoverflow.com/questions/65433795/unable-to-update-the-day-and-night-modes-in-android-with-window-manager-screens#answer-67340930
        Context ctx = new ContextThemeWrapper(root.getContext(), themeResId);

        return (T) LayoutInflater.from(ctx).inflate(resId, root, attachToRoot);
    }

    /** 应用阴影 */
    public static void applyShadow(Paint paint, String shadow) {
        Shadow s = Shadow.parse(shadow);
        if (s == null) {
            return;
        }

        paint.setShadowLayer(s.radius, s.dx, s.dy, s.color);
    }

    /** 应用边框 */
    public static boolean applyBorder(Paint paint, String border) {
        if (border == null || border.trim().isEmpty()) {
            return false;
        }

        String[] splits = border.trim().split("\\s+");
        float width = ScreenUtils.dpToPx(Float.parseFloat(splits[0]));
        int color = Color.parseColor(splits[1]);

        if (width > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(color);
            paint.setStrokeWidth(width);

            return true;
        }
        return false;
    }

    public static void applyStyledAttributes(
            Context context, AttributeSet attrSet, int[] attrs, Consumer<TypedArray> apply
    ) {
        TypedArray typedArray = null;
        try {
            typedArray = context.obtainStyledAttributes(attrSet, attrs);

            apply.accept(typedArray);
        } finally {
            if (typedArray != null) {
                typedArray.recycle();
            }
        }
    }

    private static TypedValue getValue(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrId, typedValue, true);

        return typedValue;
    }
}
