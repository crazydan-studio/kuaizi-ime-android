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
import android.widget.TextView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * 确认弹窗
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-14
 */
public class PopupConfirm {
    private final ViewGroup parent;
    private final View anchor;

    private CharSequence message;

    private Button negativeBtn;
    private Button positiveBtn;

    private Runnable cancelable;

    public static PopupConfirm with(ViewGroup parent) {
        return new PopupConfirm(parent);
    }

    private PopupConfirm(ViewGroup parent) {
        this.parent = parent;
        this.anchor = parent.findViewById(R.id.popup_confirm_anchor);
    }

    public PopupConfirm setMessage(int resId, Object... args) {
        String message = getString(resId, args);
        return setMessage(message);
    }

    public PopupConfirm setMessage(CharSequence message) {
        this.message = message;
        return this;
    }

    public PopupConfirm setNegativeButton(int resId, View.OnClickListener listener) {
        this.negativeBtn = new Button(getString(resId), listener);
        return this;
    }

    public PopupConfirm setPositiveButton(int resId, View.OnClickListener listener) {
        this.positiveBtn = new Button(getString(resId), listener);
        return this;
    }

    public PopupConfirm show() {
        // 已显示，则不再处理
        if (this.cancelable != null) {
            return this;
        }

        View contentView = createContentView();
        ViewGroup.LayoutParams contentViewLayout = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                              ViewGroup.LayoutParams.MATCH_PARENT);
        // 锚点视图仅用于定位弹窗所属的父窗口，且该父窗口应该采用 FrameLayout，以便于弹窗始终在最上层显示
        ViewGroup parent = (ViewGroup) this.anchor.getParent();
        parent.addView(contentView, contentViewLayout);

        // 动画显示与退出
        Context context = this.parent.getContext();
        int[] attrs = new int[] { android.R.attr.windowEnterAnimation, android.R.attr.windowExitAnimation };
        int[] animResIds = ThemeUtils.getStyledAttrs(context, R.style.Theme_Kuaizi_PopupWindow_Animation, attrs);
        int enterAnimResId = animResIds[0];
        int exitAnimResId = animResIds[1];

        Animation enterAnimation = AnimationUtils.loadAnimation(context, enterAnimResId);
        Animation exitAnimation = AnimationUtils.loadAnimation(context, exitAnimResId);

        contentView.startAnimation(enterAnimation);

        this.cancelable = () -> {
            exitAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    PopupConfirm.this.cancelable = null;

                    contentView.setVisibility(View.GONE);
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

    private View createContentView() {
        View contentView = View.inflate(this.parent.getContext(), R.layout.popup_confirm_view, null);
        // 阻止事件向上层视图传播，从而保证窗口的模态特性
        contentView.setOnTouchListener((v, event) -> true);

        TextView messageView = contentView.findViewById(R.id.message);
        messageView.setText(this.message);

        TextView negativeBtnView = contentView.findViewById(R.id.btn_negative);
        initButtonView(negativeBtnView, this.negativeBtn);

        TextView positiveBtnView = contentView.findViewById(R.id.btn_positive);
        initButtonView(positiveBtnView, this.positiveBtn);

        return contentView;
    }

    private String getString(int resId, Object... args) {
        return this.parent.getContext().getString(resId, args);
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
