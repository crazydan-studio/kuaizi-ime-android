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

import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-02-12
 */
public class Toast {
    public static final int LENGTH_SHORT = Snackbar.LENGTH_SHORT;
    public static final int LENGTH_LONG = Snackbar.LENGTH_LONG;
    public static final int LENGTH_INDEFINITE = Snackbar.LENGTH_INDEFINITE;

    private final View root;
    private final View anchor;

    private CharSequence text;
    private int duration;

    private CharSequence actionText;
    private View.OnClickListener actionListener;

    public static Toast with(View root) {
        return new Toast(root);
    }

    private Toast(View root) {
        this.root = root;
        this.anchor = root.findViewById(R.id.toast_anchor);
    }

    public Toast setText(int resId) {
        return setText(this.root.getContext().getText(resId));
    }

    public Toast setText(CharSequence text) {
        this.text = text;
        return this;
    }

    public Toast setHtml(String html, Object... args) {
        Spanned text = ViewUtils.parseHtml(html, args);
        return setText(text);
    }

    public Toast setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public Toast setAction(int resId, View.OnClickListener listener) {
        return setAction(this.root.getContext().getText(resId), listener);
    }

    public Toast setAction(CharSequence text, View.OnClickListener listener) {
        this.actionText = text;
        this.actionListener = listener;
        return this;
    }

    public void show() {
        // Note: Snackbar 将自动管理多个实例，确保将前一个隐藏后再显示下一个
        Snackbar snackbar = Snackbar.make(this.root, this.text, this.duration)
                                    .setAnchorView(this.anchor)
                                    .setAction(this.actionText, this.actionListener);
        View snackbarView = snackbar.getView();

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        // TODO Snackbar 自适应内容高度无效，暂时不清楚原因
        ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        snackbarView.setLayoutParams(params);

        snackbar.show();
    }
}
