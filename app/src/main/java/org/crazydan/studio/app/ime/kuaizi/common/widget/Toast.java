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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-12
 */
public class Toast {
    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private final Context context;

    private CharSequence text;
    private int duration;
    private int gravity = Gravity.BOTTOM | Gravity.CENTER;
    private int xOffset;
    private int yOffset;

    public static Toast with(Context context) {
        return new Toast(context);
    }

    private Toast(Context context) {
        this.context = context;
    }

    public Toast setText(CharSequence text) {
        this.text = text;
        return this;
    }

    public Toast setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public Toast setGravity(int gravity, int xOffset, int yOffset) {
        this.gravity = gravity;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        return this;
    }

    public void show() {
        android.widget.Toast toast;

        // Note: Android API 30 以上版本的 Toast#makeText 不支持定制消息的位置，需采用自定义视图规避该问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            View view = LayoutInflater.from(this.context).inflate(R.layout.app_toast_view, null);
            ((TextView) view.findViewById(R.id.message)).setText(this.text);

            toast = new android.widget.Toast(this.context);
            toast.setView(view);
        } else {
            toast = android.widget.Toast.makeText(this.context, this.text, this.duration);
        }

        toast.setDuration(this.duration);
        toast.setGravity(this.gravity, this.xOffset, this.yOffset);

        toast.show();
    }
}
