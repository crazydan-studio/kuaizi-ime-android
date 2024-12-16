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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import java.util.Objects;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.AudioPlayer;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.InputPane;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserInputMsgType;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserMsgListener;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputAudioPlayMsgData;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.input.InputCharsInputPopupShowMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.view.key.XPadKeyView;

/**
 * {@link InputPane 输入面板}的视图
 * <p/>
 * 由 {@link KeyboardView} 和 {@link InputListView} 组成
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class InputPaneView extends FrameLayout implements UserMsgListener, InputMsgListener, ConfigChangeListener {
    private final AudioPlayer audioPlayer;

    private KeyboardView keyboardView;
    private InputListView inputListView;

    private PopupWindow inputKeyPopupWindow;
    private PopupWindow inputCompletionsPopupWindow;
    private InputCompletionsView inputCompletionsView;

    private View settingsBtnView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;
    private boolean disableSettingsBtn;

    private Config config;
    private UserMsgListener listener;

    public InputPaneView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(),
                              R.raw.tick_single,
                              R.raw.tick_double,
                              R.raw.page_flip,
                              R.raw.tick_clock,
                              R.raw.tick_ping);

        relayout();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public XPadKeyView getXPadKeyView() {
        return this.keyboardView.getXPadKeyView();
    }

    public void disableSettingsBtn(boolean disabled) {
        this.disableSettingsBtn = disabled;

        toggleEnableSettingsBtn();
    }

    // =============================== Start: 消息处理 ===================================

    public void setListener(UserMsgListener listener) {
        this.listener = listener;
    }

    /** 响应内部视图的 {@link UserKeyMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserKeyMsg msg) {
        this.listener.onMsg(msg);
    }

    /** 响应内部视图的 {@link UserInputMsg} 消息：从视图向上传递给外部监听者 */
    @Override
    public void onMsg(UserInputMsg msg) {
        this.listener.onMsg(msg);
    }

    // -------------------------------------------

    @Override
    public void onChanged(ConfigKey key, Object oldValue, Object newValue) {
        switch (key) {
            case theme: {
                relayout();
                break;
            }
            case enable_x_input_pad:
            case adapt_desktop_swipe_up_gesture: {
                updateBottomSpacing();
                break;
            }
        }
    }

    // -------------------------------------------

    /** 响应 {@link InputMsg} 消息：向下传递消息给内部视图 */
    @Override
    public void onMsg(InputMsg msg) {
        this.keyboardView.onMsg(msg);
        this.inputListView.onMsg(msg);

        switch (msg.type) {
            case Input_Completion_Clean_Done:
            case Input_Completion_Apply_Done:
            case Input_Completion_Update_Done: {
                boolean completionsShown = inputList.hasCompletions();

                showInputCompletionsPopupWindow(inputList, completionsShown);
                break;
            }
            case InputAudio_Play_Doing: {
                on_InputAudio_Play_Doing_Msg((InputAudioPlayMsgData) msg.data);
                return;
            }
            case InputChars_Input_Popup_Show_Doing: {
                InputCharsInputPopupShowMsgData data = (InputCharsInputPopupShowMsgData) msg.data;

                showInputKeyPopupWindow(data.text, data.hideDelayed);
                return;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyPopupWindow(null, false);
                return;
            }
            default: {
                // TODO 从消息中获取输入状态数据
                toggleShowInputListCleanBtn();
            }
        }
    }

    private void on_InputAudio_Play_Doing_Msg(InputAudioPlayMsgData data) {
        if (data.audioType == InputAudioPlayMsgData.AudioType.PageFlip) {
            if (this.config.bool(ConfigKey.disable_input_candidates_paging_audio)) {
                return;
            }
        } else if (this.config.bool(ConfigKey.disable_key_clicked_audio)) {
            return;
        }

        this.audioPlayer.play(data.audioType.resId);
    }

    // =============================== End: 消息处理 ===================================

    /** 重新布局视图 */
    private void relayout() {
        resetPopupWindows();
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Theme theme = this.config.get(ConfigKey.theme);
        int themeResId = theme.getResId(getContext());

        View rootView = inflateWithTheme(R.layout.input_pane_view_layout, themeResId);

        this.settingsBtnView = rootView.findViewById(R.id.settings);
        toggleEnableSettingsBtn();

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.keyboardView.setListener(this);

        this.inputListView = rootView.findViewById(R.id.input_list);
        this.inputListView.setListener(this);

        View inputKeyView = inflateWithTheme(R.layout.input_popup_key_view, themeResId, false);
        this.inputCompletionsView = inflateWithTheme(R.layout.input_completions_view, themeResId, false);
        this.inputCompletionsView.setListener(this);

        preparePopupWindows(this.inputCompletionsView, inputKeyView);

        updateBottomSpacing();
    }

    private void updateBottomSpacing() {
        // Note: 仅竖屏模式下需要添加底部空白
        addBottomSpacing(this,
                         this.config.bool(ConfigKey.adapt_desktop_swipe_up_gesture)
                         && !this.config.bool(ConfigKey.enable_x_input_pad)
                         && Objects.equals(this.config.get(ConfigKey.orientation), Keyboard.Orientation.portrait));
    }

    private void addBottomSpacing(View rootView, boolean needSpacing) {
        float height = ScreenUtils.pxFromDimension(getContext(), R.dimen.keyboard_bottom_spacing);
        height -= this.keyboardView.getBottomSpacing();

        View bottomSpacingView = rootView.findViewById(R.id.bottom_spacing_view);
        if (needSpacing && height > 0) {
            ViewUtils.show(bottomSpacingView);
            ViewUtils.setHeight(bottomSpacingView, (int) height);
        } else {
            ViewUtils.hide(bottomSpacingView);
        }
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        return inflateWithTheme(resId, themeResId, true);
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId, boolean attachToRoot) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, attachToRoot);
    }

    private void resetPopupWindows() {
        if (this.inputCompletionsPopupWindow != null) {
            this.inputCompletionsPopupWindow.dismiss();
        } else {
            this.inputCompletionsPopupWindow = new PopupWindow();
        }

        if (this.inputKeyPopupWindow != null) {
            this.inputKeyPopupWindow.dismiss();
        } else {
            this.inputKeyPopupWindow = new PopupWindow();
        }
    }

    private void preparePopupWindows(InputCompletionsView completionsView, View keyView) {
        resetPopupWindows();

        initPopupWindow(this.inputCompletionsPopupWindow, completionsView);
        initPopupWindow(this.inputKeyPopupWindow, keyView);
    }

    private void showInputCompletionsPopupWindow(InputList inputList, boolean shown) {
        PopupWindow window = this.inputCompletionsPopupWindow;
        if (!shown) {
            window.dismiss();
            return;
        }

        this.inputCompletionsView.update(inputList);
        if (window.isShowing()) {
            return;
        }

        // https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/+/40056ae7c2757681d88d2e226c4681281bd07129/src/com/android/inputmethod/pinyin/PinyinIME.java#1247
        // https://stackoverflow.com/questions/3514392/android-ime-showing-a-custom-pop-up-dialog-like-swype-keyboard-which-can-ente#answer-32858312
        // Note: 自动补全视图的高度和宽度是固定的，故而，只用取一次
        int width = getMeasuredWidth();
        int height = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.input_completions_view_height);

        showPopupWindow(window, width, height, Gravity.START | Gravity.TOP);
    }

    private void showInputKeyPopupWindow(String key, boolean hideDelayed) {
        if (this.config.bool(ConfigKey.disable_input_key_popup_tips)) {
            return;
        }

        PopupWindow window = this.inputKeyPopupWindow;
        if (CharUtils.isBlank(key)) {
            // Note: 存在因滑动太快而无法隐藏的问题，故而，延迟隐藏
            post(window::dismiss);
            return;
        }

        View contentView = window.getContentView();
        TextView textView = contentView.findViewById(R.id.fg_view);
        textView.setText(key);

        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int width = WindowManager.LayoutParams.WRAP_CONTENT;
        int height = (int) (contentView.getMeasuredHeight() + ScreenUtils.dpToPx(2f));

        showPopupWindow(window, width, height, Gravity.CENTER_HORIZONTAL | Gravity.TOP);

        if (hideDelayed) {
            postDelayed(window::dismiss, 600);
        }
    }

    private void toggleEnableSettingsBtn() {
        if (this.disableSettingsBtn) {
            this.settingsBtnView.setAlpha(0.4f);
            this.settingsBtnView.setOnClickListener(null);
        } else {
            this.settingsBtnView.setAlpha(1.0f);
            this.settingsBtnView.setOnClickListener(this::onShowPreferences);
        }
    }

    private void toggleShowInputListCleanBtn() {
        if (getInputList().isEmpty()) {
            if (getInputList().canCancelDelete()) {
                ViewUtils.hide(this.inputListCleanBtnView);
                ViewUtils.show(this.inputListCleanCancelBtnView);
            } else {
                ViewUtils.show(this.inputListCleanBtnView).setAlpha(0);
                ViewUtils.hide(this.inputListCleanCancelBtnView);
            }

            this.inputListCleanBtnView.setOnClickListener(null);
            this.inputListCleanCancelBtnView.setOnClickListener(this::onCancelCleanInputList);
        } else {
            ViewUtils.show(this.inputListCleanBtnView).setAlpha(1);
            ViewUtils.hide(this.inputListCleanCancelBtnView);

            this.inputListCleanBtnView.setOnClickListener(this::onCleanInputList);
            this.inputListCleanCancelBtnView.setOnClickListener(null);
        }
    }

    private void initPopupWindow(PopupWindow window, View contentView) {
        window.setClippingEnabled(false);
        window.setBackgroundDrawable(null);
        window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        window.setContentView(contentView);

        window.setAnimationStyle(R.style.Theme_Kuaizi_PopupWindow_Animation);
    }

    private void showPopupWindow(PopupWindow window, int width, int height, int gravity) {
        window.setWidth(width);
        window.setHeight(height);

        // 放置于被布局的键盘之上
        View parent = this;
        int[] location = new int[2];
        parent.getLocationInWindow(location);

        int x = location[0];
        int y = location[1] - window.getHeight();

        post(() -> window.showAtLocation(parent, gravity, x, y));
    }

    // ==================== Start: 按键事件处理 ==================

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onCleanInputList(View v) {
        UserInputMsg msg = new UserInputMsg(UserInputMsgType.SingleTap_Btn_Clean_InputList);
        onMsg(msg);
    }

    private void onCancelCleanInputList(View v) {
        UserInputMsg msg = new UserInputMsg(UserInputMsgType.SingleTap_Btn_Cancel_Clean_InputList);
        onMsg(msg);
    }

    // ==================== End: 按键事件处理 ==================
}
