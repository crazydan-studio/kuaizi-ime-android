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

package org.crazydan.studio.app.ime.kuaizi.common.utils;

import java.util.function.Consumer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.crazydan.studio.app.ime.kuaizi.common.widget.Shadow;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-07
 */
public class ThemeUtils {

    /** 通过属性 id 获取资源 id */
    public static int getResourceByAttrId(Context context, int attrId) {
        return getValue(context, attrId).resourceId;
    }

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

    /** 获取指定 R.style 中的属性值，返回结果的属性值顺序与参数 attrs 中的属性顺序一致 */
    public static int[] getStyledAttrs(Context context, int styleResId, int[] attrs) {
        int[] results = new int[attrs.length];

        try (
                TypedArray typedArray = context.obtainStyledAttributes(styleResId, attrs)
        ) {
            for (int i = 0; i < attrs.length; i++) {
                results[i] = typedArray.getResourceId(i, 0);
            }
        }
        return results;
    }

    public static void applyStyledAttrs(
            Context context, AttributeSet attrSet, int[] attrs, Consumer<TypedArray> apply
    ) {
        try (TypedArray typedArray = context.obtainStyledAttributes(attrSet, attrs)) {
            apply.accept(typedArray);
        }
    }

    /** 获取主题中设置的 attrId 的值 */
    public static TypedValue getValue(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrId, typedValue, true);

        return typedValue;
    }

    /** 更新 {@link Activity} 的主题，并重启 {@link Activity} */
    public static void changeTheme(Activity activity, int themeResId) {
        Intent intent = activity.getIntent();

        activity.setTheme(themeResId);

        activity.finish();
        activity.startActivity(intent);
    }
}
