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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.EditorEditMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputClipMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.user.UserInputClipMsgData;

import static org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgType.SingleTap_Btn_Save_As_Favorite;

/**
 * 输入候选视图
 * <p/>
 * 负责按键提示、快捷输入列表的显示（浮动窗口形式）
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-19
 */
public class CandidatesView extends BaseThemedView {
    private PopupWindow popupWindow;

    private Map<PopupType, Popup> popups;

    public CandidatesView(@NonNull Context context, @Nullable AttributeSet attrs) {
        // Note: 在当前视图内不嵌入其他视图，相关视图均在浮动窗口中显示
        super(context, attrs, 0, true);

        this.popupWindow = createPopupWindow();
    }

    public void destroy() {
        closePopupWindow();

        this.popupWindow = null;
        this.popups = null;
    }

    public void close() {
        showInputQuickList(null);
        showTooltip(null, false);

        closePopupWindow();
    }

    // =============================== Start: 视图更新 ===================================

    @Override
    protected void doLayout() {
        closePopupWindow();

        Context context = getContext();
        // 构造候选视图，并继承上下文中的主题配置
        View contentView = inflate(context, R.layout.ime_candidates_view, null);
        this.popupWindow.setContentView(contentView);

        InputQuickListView quickListView = contentView.findViewById(R.id.quick_list);
        quickListView.setListener(this);

        View tooltipView = contentView.findViewById(R.id.tooltip);
        View snackbarView = contentView.findViewById(R.id.snackbar);

        this.popups = new HashMap<PopupType, Popup>() {{
            put(PopupType.quick_list,
                new Popup((View) quickListView.getParent(), R.attr.anim_fade_in, R.attr.anim_fade_out));
            put(PopupType.tooltip,
                new Popup((View) tooltipView.getParent(), R.attr.anim_fade_in, R.attr.anim_fade_out));

            put(PopupType.snackbar, new Popup(snackbarView, R.attr.anim_slide_in, R.attr.anim_slide_out));
        }};
    }

    @Override
    protected void updateLayoutDirection() {
        updateLayoutDirection(popup(PopupType.quick_list).view, true);

        // Note: Snackbar 按普通的方式调整布局方向
        updateLayoutDirection(popup(PopupType.snackbar).view, false);
    }

    private Popup popup(PopupType type) {
        Popup popup = this.popups.get(type);
        assert popup != null;
        return popup;
    }

    // =============================== End: 视图更新 ===================================

    // =============================== Start: 消息处理 ===================================

    @Override
    protected void handleMsg(InputMsg msg) {
        // Note: 快捷输入没有确定的隐藏时机，故而，需针对每个消息做一次处理，在数据为 null 时隐藏，有数据时显示
        showInputQuickList(msg.inputQuickList);

        switch (msg.type) {
            case InputChars_Input_Popup_Show_Doing: {
                InputCharsInputPopupShowMsgData data = msg.data();
                showInputKeyTip(data.text, data.hideDelayed);
                break;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyTip(null, false);
                break;
            }
            case InputFavorite_Save_Done: {
                showTooltip(EditorAction.favorite.tipResId);
                break;
            }
            case InputFavorite_Paste_Done: {
                showTooltip(EditorAction.paste.tipResId);
                break;
            }
            case Editor_Edit_Doing: {
                EditorEditMsgData data = msg.data();
                on_Editor_Edit_Doing_Msg(data.action);
                break;
            }
            case InputClip_CanBe_Favorite: {
                on_InputClip_CanBe_Favorite_Msg(msg.data());
                break;
            }
            default: {
                this.log.warn("Ignore message %s", () -> new Object[] { msg.type });
            }
        }
    }

    private void on_InputClip_CanBe_Favorite_Msg(InputClipMsgData data) {
        Popup popup = popup(PopupType.snackbar);
        View view = popup.view;

        TextView textView = view.findViewById(R.id.snackbar_text);
        textView.setText(data.source.confirmResId);

        TextView actionBtn = view.findViewById(R.id.snackbar_action);
        actionBtn.setText(R.string.btn_save_as_favorite);
        actionBtn.setOnClickListener((v) -> saveClipToFavorite(data.clip));

        popup.show(50000000);
    }

    private void on_Editor_Edit_Doing_Msg(EditorAction action) {
        // 对编辑内容的操作做气泡提示，以告知用户处理结果，避免静默处理造成的困惑
        // Note: 复制和粘贴可能内容为空，不会提示可收藏，因此，仍需提示
        switch (action) {
            case backspace: // 已作气泡提示
            case favorite: { // 有专门的收藏确认提示
                break;
            }
            default: {
                showTooltip(action.tipResId);
                break;
            }
        }
    }

    private void saveClipToFavorite(InputClip clip) {
        UserInputClipMsgData data = new UserInputClipMsgData(clip);
        UserInputMsg msg = UserInputMsg.build((b) -> b.type(SingleTap_Btn_Save_As_Favorite).data(data));

        onMsg(msg);
    }

    // =============================== End: 消息处理 ===================================

    // ==================== Start: 气泡提示 ==================

    private void showInputQuickList(List<?> dataList) {
        Popup popup = popup(PopupType.quick_list);
        InputQuickListView view = popup.view.findViewById(R.id.quick_list);

        if (CollectionUtils.isEmpty(dataList)) {
            // Note: 不置空列表，以确保退场动画效果能完整呈现
            popup.close();
        } else {
            view.update(dataList);
            popup.show();
        }
    }

    private void showInputKeyTip(String key, boolean hideDelayed) {
        if (this.config.bool(ConfigKey.disable_input_key_popup_tips)) {
            showTooltip(null, false);
            return;
        }

        showTooltip(key, hideDelayed);
    }

    private void showTooltip(int tipResId) {
        String tips = tipResId != 0 ? getContext().getString(tipResId) : null;

        showTooltip(tips, true);
    }

    private void showTooltip(String tip, boolean hideDelayed) {
        Popup popup = popup(PopupType.tooltip);
        if (CharUtils.isBlank(tip)) {
            //popup.close();
            return;
        }

        TextView textView = popup.view.findViewById(R.id.tooltip);
        textView.setText(tip);

        popup.show(hideDelayed ? 8000000 : 0);
    }

    private PopupWindow createPopupWindow() {
        PopupWindow window = new PopupWindow(WindowManager.LayoutParams.MATCH_PARENT,
                                             WindowManager.LayoutParams.WRAP_CONTENT);

        //window.setTouchable(false); // 内容视图不可点击，整个窗口都是直接穿透的
        window.setClippingEnabled(false);
        window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Note: 动画由内容视图处理，窗口自身直接隐藏或显示
        window.setAnimationStyle(0);

        return window;
    }

    private void closePopupWindow() {
        PopupWindow window = this.popupWindow;
        View contentView = window.getContentView();

        // Note: 先隐藏内容视图，在延迟关闭窗口，以避免其出现跳闪
        ViewUtils.hide(contentView);
        post(window::dismiss);
    }

    private void showPopupWindow() {
        PopupWindow window = this.popupWindow;
        View contentView = window.getContentView();
        ViewUtils.show(contentView);

        if (window.isShowing()) {
            return;
        }

        // 放置于被布局的目标之上
        View target = this;

        // Note: 为避免窗口定位出现频繁变动，需固定内容视图的高度
        int contentViewHeight = (int) ScreenUtils.pxFromDimension(target.getContext(), R.dimen.popup_candidates_height);
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) target.getRootView().getLayoutParams();
        boolean isInIME = params != null && params.type == WindowManager.LayoutParams.TYPE_INPUT_METHOD;

        post(() -> {
            int[] loc = new int[2];
            target.getLocationOnScreen(loc);

            int x = 0;
            int y = (isInIME ? 0 : loc[1]) - contentViewHeight;

            // 设置初始显示位置：其仅在未显示时有效
            // 在嵌入应用的模式下，窗口偏移相对于整个屏幕，
            // 而若是输入法形态，则窗口偏移相对于输入法窗口
            window.showAtLocation(target, Gravity.TOP, x, y);
        });
    }

    private void onPopupShow() {
        showPopupWindow();
    }

    private void onPopupClose() {
        for (Popup popup : this.popups.values()) {
            if (!popup.isClosed()) {
                return;
            }
        }

        closePopupWindow();
    }

    // ==================== End: 气泡提示 ==================

    private enum PopupType {
        quick_list,
        tooltip,
        snackbar,
    }

    private enum PopupState {
        showing,
        shown,
        closing,
        closed,
    }

    private class Popup {
        public final View view;
        private final Runnable closeCallback;

        private final Animation enterAnim;
        private final Animation exitAnim;

        private PopupState state;

        Popup(View view, int enterAnimAttrId, int exitAnimAttrId) {
            this.view = view;
            this.state = PopupState.closed;
            ViewUtils.hide(view);

            this.closeCallback = this::close;

            Context context = view.getContext();
            int animResId = ThemeUtils.getResourceByAttrId(context, enterAnimAttrId);
            this.enterAnim = AnimationUtils.loadAnimation(context, animResId);
            this.enterAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    updateState(PopupState.showing);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateState(PopupState.shown);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            animResId = ThemeUtils.getResourceByAttrId(context, exitAnimAttrId);
            this.exitAnim = AnimationUtils.loadAnimation(context, animResId);
            this.exitAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    updateState(PopupState.closing);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    ViewUtils.hide(view);
                    updateState(PopupState.closed);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }

        public boolean isClosed() {
            return this.state == PopupState.closed;
        }

        public void show() {
            show(0);
        }

        public void show(long closeDelayMillis) {
            switch (this.state) {
                case shown:
                case showing: {
                    callDelayClose(closeDelayMillis);
                    return;
                }
                case closing: {
                    this.exitAnim.cancel();
                    break;
                }
            }

            updateState(PopupState.showing);
            this.view.startAnimation(this.enterAnim);
            // Note: 只有已显示的视图才能应用动画
            ViewUtils.show(this.view);

            callDelayClose(closeDelayMillis);
        }

        public void close() {
            this.view.removeCallbacks(this.closeCallback);

            switch (this.state) {
                case closed:
                case closing: {
                    return;
                }
                case showing: {
                    this.enterAnim.cancel();
                    break;
                }
            }

            updateState(PopupState.closing);
            this.view.startAnimation(this.exitAnim);
            // Note: 只有已显示的视图才能应用动画
            ViewUtils.show(this.view);
        }

        private void updateState(PopupState state) {
            if (this.state == state) {
                return;
            }

            this.state = state;
            switch (this.state) {
                case showing: {
                    onPopupShow();
                    break;
                }
                case closed: {
                    onPopupClose();
                    break;
                }
            }
        }

        private void callDelayClose(long delayInMillis) {
            this.view.removeCallbacks(this.closeCallback);

            if (delayInMillis <= 0) {
                return;
            }
            this.view.postDelayed(this.closeCallback, delayInMillis);
        }
    }
}
