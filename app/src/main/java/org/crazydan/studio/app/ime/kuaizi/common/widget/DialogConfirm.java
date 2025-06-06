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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * 确认对话框
 * <p/>
 * 内嵌在 {@link FrameLayout} 类型的布局容器 {@link #parent} 内
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-14
 */
public class DialogConfirm {
    private final ViewGroup parent;
    private final View anchor;

    private CharSequence message;

    private Button negativeBtn;
    private Button positiveBtn;

    private Runnable cancelable;

    public static DialogConfirm with(ViewGroup parent) {
        return new DialogConfirm(parent);
    }

    private DialogConfirm(ViewGroup parent) {
        this.parent = parent;
        this.anchor = parent.findViewById(R.id.popup_confirm_anchor);
    }

    public DialogConfirm setMessage(int resId, Object... args) {
        String message = getString(resId, args);
        return setMessage(message);
    }

    public DialogConfirm setMessage(CharSequence message) {
        this.message = message;
        return this;
    }

    public DialogConfirm setNegativeButton(int resId, View.OnClickListener listener) {
        this.negativeBtn = new Button(getString(resId), listener);
        return this;
    }

    public DialogConfirm setPositiveButton(int resId, View.OnClickListener listener) {
        this.positiveBtn = new Button(getString(resId), listener);
        return this;
    }

    public DialogConfirm show() {
        // 已显示，则不再处理
        if (this.cancelable != null) {
            return this;
        }

        View contentView = createContentView();
        ViewGroup.LayoutParams contentViewLayout = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                              ViewGroup.LayoutParams.MATCH_PARENT);
        // 锚点视图仅用于定位弹窗所属的父窗口，且该父窗口应该采用 FrameLayout，以便于弹窗始终在最上层显示
        ViewGroup parent = this.anchor != null ? (ViewGroup) this.anchor.getParent() : this.parent;
        parent.addView(contentView, contentViewLayout);

        // 显示与退出动画
        Context context = getContext();
        int enterAnimResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_fade_in);
        int exitAnimResId = ThemeUtils.getResourceByAttrId(context, R.attr.anim_fade_out);

        Animation enterAnimation = AnimationUtils.loadAnimation(context, enterAnimResId);
        Animation exitAnimation = AnimationUtils.loadAnimation(context, exitAnimResId);

        contentView.startAnimation(enterAnimation);

        this.cancelable = () -> {
            exitAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    DialogConfirm.this.cancelable = null;

                    ViewUtils.hide(contentView);
                    parent.removeView(contentView);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            contentView.startAnimation(exitAnimation);
        };

        return this;
    }

    public void dismiss() {
        if (this.cancelable != null) {
            this.cancelable.run();
        }
    }

    private Context getContext() {
        return this.parent.getContext();
    }

    private View createContentView() {
        Context context = getContext();

        View rootView = View.inflate(context, R.layout.dialog_confirm_view, null);
        // 阻止事件向上层视图传播，从而保证窗口的模态特性
        rootView.setOnTouchListener((v, event) -> true);

        TextView messageView = rootView.findViewById(R.id.message);
        messageView.setText(this.message);

        TextView negativeBtnView = rootView.findViewById(R.id.btn_negative);
        initButtonView(negativeBtnView, this.negativeBtn);

        TextView positiveBtnView = rootView.findViewById(R.id.btn_positive);
        initButtonView(positiveBtnView, this.positiveBtn);

        View contentView = rootView.findViewById(R.id.content);
        ViewUtils.addShadow(contentView, R.attr.dialog_shadow_style);

        return rootView;
    }

    private String getString(int resId, Object... args) {
        return getContext().getString(resId, args);
    }

    private void initButtonView(TextView view, Button btn) {
        if (btn == null) {
            view.setOnClickListener(null);
            ViewUtils.hide(view);
            return;
        }

        view.setText(btn.text);
        view.setOnClickListener((v) -> {
            if (btn.listener != null) {
                btn.listener.onClick(v);
            }
            dismiss();
        });
    }

    private static class Button {
        private final String text;
        private final View.OnClickListener listener;

        private Button(String text, View.OnClickListener listener) {
            this.text = text;
            this.listener = listener;
        }
    }
}
