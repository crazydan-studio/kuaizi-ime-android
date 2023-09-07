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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;

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

    /** 应用阴影 */
    public static void applyShadow(Paint paint, String shadow) {
        if (shadow == null) {
            return;
        }

        String[] splits = shadow.trim().split("\\s+");
        int dx = ScreenUtils.dpToPx(Integer.parseInt(splits[0]));
        int dy = ScreenUtils.dpToPx(Integer.parseInt(splits[1]));
        int radius = ScreenUtils.dpToPx(Integer.parseInt(splits[2]));
        int color = Color.parseColor(splits[3]);

        paint.setShadowLayer(radius, dx, dy, color);
    }

    private static TypedValue getValue(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrId, typedValue, true);

        return typedValue;
    }
}
