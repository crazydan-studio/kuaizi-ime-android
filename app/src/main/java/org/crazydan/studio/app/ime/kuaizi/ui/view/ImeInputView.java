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

package org.crazydan.studio.app.ime.kuaizi.ui.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinDictDB;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.MathKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.Msg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.InputCharsInputPopupShowingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardHandModeSwitchDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputCompletionsView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.key.XPadKeyView;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class ImeInputView extends FrameLayout
        implements SharedPreferences.OnSharedPreferenceChangeListener, InputMsgListener {
    private final InputMsgListener inputMsgListener = (keyboard, msg, msgData) -> {
        // 忽略非绑定键盘的消息
        if (getKeyboard() != keyboard) {
            return;
        }

        this.inputListView.onMsg(keyboard, msg, msgData);
        this.keyboardView.onMsg(keyboard, msg, msgData);

        this.onMsg(keyboard, msg, msgData);
    };

    private final SharedPreferences preferences;
    private final InputList inputList;
    private Keyboard keyboard;

    private KeyboardView keyboardView;
    private InputListView inputListView;
    private PopupWindow inputCompletionsPopupWindow;
    private PopupWindow inputKeyPopupWindow;
    private InputCompletionsView inputCompletionsView;

    private View settingsBtnView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;

    private Keyboard.HandMode keyboardHandMode;
    private Boolean disableUserInputData;
    private Boolean disableInputKeyPopupTips;
    private Boolean disableXInputPad;
    private Boolean disableCandidateVariantFirst;
    private boolean disableSettingsBtn;

    public ImeInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        PinyinDictDB.getInstance().init(getContext());

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.preferences.registerOnSharedPreferenceChangeListener(this);

        this.inputList = new InputList();

        relayoutViews();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Msg.Registry.register(InputMsg.class, this.inputMsgListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public void destroy() {
        Msg.Registry.unregister(this.inputMsgListener);
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public Keyboard getKeyboard() {
        return this.keyboard;
    }

    public XPadKeyView getXPadKeyView() {
        return this.keyboardView.getXPadKeyView();
    }

    public void refresh() {
        updateKeyboardConfig();
    }

    public void disableUserInputData(boolean disabled) {
        this.disableUserInputData = disabled;

        if (this.keyboard != null) {
            this.keyboard.getConfig().setUserInputDataDisabled(disabled);
        }
    }

    public void disableInputKeyPopupTips(boolean disabled) {
        this.disableInputKeyPopupTips = disabled;

        if (this.keyboard != null) {
            this.keyboard.getConfig().setInputKeyPopupTipsDisabled(disabled);
        }
    }

    public void disableXInputPad(Boolean disabled) {
        this.disableXInputPad = disabled;

        if (disabled == null) {
            disabled = !Keyboard.Config.isXInputPadEnabled(this.preferences);
        }

        if (this.keyboard != null) {
            this.keyboard.getConfig().setXInputPadEnabled(!disabled);

            updateBottomSpacing(this.keyboard);
        }
    }

    public void disableCandidateVariantFirst(Boolean disabled) {
        this.disableCandidateVariantFirst = disabled;

        if (disabled == null) {
            disabled = Keyboard.Config.isCandidateVariantFirstEnabled(this.preferences);
        }

        if (this.keyboard != null) {
            this.keyboard.getConfig().setCandidateVariantFirstEnabled(!disabled);
        }
    }

    public void disableSettingsBtn(boolean disabled) {
        this.disableSettingsBtn = disabled;

        if (disabled) {
            this.settingsBtnView.setAlpha(0.4f);
            this.settingsBtnView.setOnClickListener(null);
        } else {
            this.settingsBtnView.setAlpha(1.0f);
            this.settingsBtnView.setOnClickListener(this::onShowPreferences);
        }
    }

    /** 启动指定类型的键盘，并清空输入列表 */
    public void startInput(Keyboard.Type type) {
        startInput(type, true);
    }

    /** 启动指定类型的键盘 */
    public void startInput(Keyboard.Type type, boolean resetInputList) {
        //Log.i("SwitchKeyboard", String.format("%s - %s", type, resetInputList));
        startInput(new Keyboard.Config(type), resetInputList);
    }

    /** 开始输入 */
    public void startInput(Keyboard.Config config, boolean resetInputList) {
        updateKeyboard(config);

        // 先更新键盘，再重置输入列表
        if (resetInputList) {
            getInputList().reset(false);
        }
    }

    /** 结束输入 */
    public void finishInput() {
        getInputList().clear();
        getKeyboard().reset();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateKeyboardConfig();
    }

    @Override
    public void onMsg(Keyboard keyboard, InputMsg msg, InputMsgData msgData) {
        boolean completionsShown = getInputList().hasCompletions();
        showInputCompletionsPopupWindow(completionsShown);

        switch (msg) {
            case Keyboard_Switch_Doing: {
                Keyboard.Type source = ((KeyboardSwitchDoingMsgData) msgData).source;
                Keyboard.Type target = ((KeyboardSwitchDoingMsgData) msgData).target;

                Keyboard.Config config = new Keyboard.Config(target, keyboard.getConfig());
                config.setSwitchFromType(source);

                updateKeyboard(config);
                break;
            }
            case Keyboard_HandMode_Switch_Done: {
                // Note：仅记录切换到的模式以便于切换到其他类型键盘时按该模式绘制按键
                this.keyboardHandMode = ((KeyboardHandModeSwitchDoneMsgData) msgData).mode;
                break;
            }
            case InputChars_Input_Popup_Show_Doing: {
                showInputKeyPopupWindow(((InputCharsInputPopupShowingMsgData) msgData).text,
                                        ((InputCharsInputPopupShowingMsgData) msgData).hideDelayed);
                break;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyPopupWindow(null, false);
                break;
            }
            default: {
                // 有新输入，则清空 删除撤销数据
                if (!getInputList().isEmpty()) {
                    getInputList().clearDeleteCancels();
                }

                toggleShowInputListCleanBtn();
            }
        }
    }

    /** 根据配置更新键盘：涉及键盘切换等 */
    private void updateKeyboard(Keyboard.Config config) {
        Keyboard oldKeyboard = this.keyboard;

        Keyboard newKeyboard = createKeyboard(config.getType());
        if (oldKeyboard == null || !newKeyboard.getClass().equals(oldKeyboard.getClass())) {
            this.keyboard = newKeyboard;
        } else {
            newKeyboard = oldKeyboard;
        }

        Keyboard.Config patchedConfig = patchKeyboardConfig(config);
        newKeyboard.setConfig(patchedConfig);

        if (oldKeyboard != newKeyboard) {
            if (oldKeyboard != null) {
                oldKeyboard.destroy();
            }

            bindKeyboard(newKeyboard);
        } else {
            newKeyboard.reset();
        }

        updateBottomSpacing(newKeyboard);
        updateInputListOption(patchedConfig);

        toggleShowInputListCleanBtn();
    }

    private void updateKeyboardConfig() {
        if (getKeyboard() == null) {
            return;
        }

        Keyboard.Config oldConfig = this.keyboard.getConfig();
        Keyboard.Config newConfig = patchKeyboardConfig(oldConfig);

        this.keyboard.setConfig(newConfig);

        // 主题发生变化，重新绑定视图
        if (needToRelayoutViews(oldConfig, newConfig)) {
            relayoutViews();

            this.keyboard.onThemeUpdated();
        }
        // Note: 仅需更新视图，无需更新监听等
        else if (oldConfig.getHandMode() != newConfig.getHandMode()) {
            if (this.keyboardHandMode != null) {
                this.keyboardHandMode = newConfig.getHandMode();
            }

            this.keyboardView.updateKeyboard(this.keyboard);
        }

        updateInputListOption(newConfig);
    }

    private void reset() {
        resetPopupWindows();
    }

    private void relayoutViews() {
        reset();
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Config config = getKeyboardConfig();
        Keyboard.ThemeType theme = config.getTheme();
        int themeResId = Keyboard.Config.getThemeResId(getContext(), theme);

        View rootView = inflateWithTheme(R.layout.ime_input_view_layout, themeResId);

        this.settingsBtnView = rootView.findViewById(R.id.settings);
        disableSettingsBtn(this.disableSettingsBtn);

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.inputListView = rootView.findViewById(R.id.input_list);

        View inputKeyView = inflateWithTheme(R.layout.input_popup_key_view, themeResId, false);
        this.inputCompletionsView = inflateWithTheme(R.layout.input_completions_view, themeResId, false);
        preparePopupWindows(this.inputCompletionsView, inputKeyView);

        this.inputListView.updateInputList(this.inputList);

        bindKeyboard(this.keyboard);
        updateBottomSpacing(this.keyboard);
    }

    private boolean needToRelayoutViews(Keyboard.Config oldConfig, Keyboard.Config newConfig) {
        return oldConfig.getTheme() != newConfig.getTheme()
               || oldConfig.isDesktopSwipeUpGestureAdapted() != newConfig.isDesktopSwipeUpGestureAdapted()
               || oldConfig.isXInputPadEnabled() != newConfig.isXInputPadEnabled()
               || (newConfig.isXInputPadEnabled() //
                   && (oldConfig.isLatinUsePinyinKeysInXInputPadEnabled()
                       != newConfig.isLatinUsePinyinKeysInXInputPadEnabled()));
    }

    private void updateBottomSpacing(Keyboard keyboard) {
        Keyboard.Config config = keyboard != null ? keyboard.getConfig() : null;

        // Note：仅竖屏模式下需要添加底部空白
        addBottomSpacing(this,
                         config != null
                         && config.isDesktopSwipeUpGestureAdapted()
                         && !config.isXInputPadEnabled()
                         && config.getOrientation() == Keyboard.Orientation.Portrait);
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

    private void bindKeyboard(Keyboard keyboard) {
        if (keyboard == null) {
            return;
        }

        keyboard.setInputList(this.inputList);
        keyboard.start();

        this.keyboardView.updateKeyboard(keyboard);
    }

    private Keyboard.Config patchKeyboardConfig(Keyboard.Config config) {
        Keyboard.Config patchedConfig = new Keyboard.Config(config.getType(), config);

        patchedConfig.syncWith(this.preferences);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            patchedConfig.setOrientation(Keyboard.Orientation.Landscape);
        } else {
            patchedConfig.setOrientation(Keyboard.Orientation.Portrait);
        }

        Keyboard.Subtype subtype = SystemUtils.getImeSubtype(getContext());
        patchedConfig.setSubtype(subtype);

        // 临时修改左右手模式
        if (this.keyboardHandMode != null) {
            patchedConfig.setHandMode(this.keyboardHandMode);
        }
        // 临时禁用对用户输入的记录
        if (this.disableUserInputData != null) {
            patchedConfig.setUserInputDataDisabled(this.disableUserInputData);
        }
        // 临时禁用按键气泡提示：主要用于密码输入场景
        if (this.disableInputKeyPopupTips != null) {
            patchedConfig.setInputKeyPopupTipsDisabled(this.disableInputKeyPopupTips);
        }
        // 临时禁用 X 型输入
        if (this.disableXInputPad != null) {
            patchedConfig.setXInputPadEnabled(!this.disableXInputPad);
        }
        // 临时禁用 繁体候选字优先
        if (this.disableCandidateVariantFirst != null) {
            patchedConfig.setCandidateVariantFirstEnabled(!this.disableCandidateVariantFirst);
        }

        return patchedConfig;
    }

    public Keyboard.Config getKeyboardConfig() {
        if (this.keyboard != null) {
            return this.keyboard.getConfig();
        }

        // 默认以保存的应用配置数据为准
        Keyboard.Config config = new Keyboard.Config(null);
        return patchKeyboardConfig(config);
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        return inflateWithTheme(resId, themeResId, true);
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId, boolean attachToRoot) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, attachToRoot);
    }

    private Keyboard createKeyboard(Keyboard.Type type) {
        switch (type) {
            case Math:
                return new MathKeyboard();
            case Latin:
                return new LatinKeyboard();
            case Number:
                return new NumberKeyboard();
            default:
                return new PinyinKeyboard();
        }
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

    private void showInputCompletionsPopupWindow(boolean shown) {
        PopupWindow window = this.inputCompletionsPopupWindow;

        if (!shown) {
            window.dismiss();
            return;
        }

        this.inputCompletionsView.updateInputList(getInputList());
        if (window.isShowing()) {
            return;
        }

        // https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/+/40056ae7c2757681d88d2e226c4681281bd07129/src/com/android/inputmethod/pinyin/PinyinIME.java#1247
        // https://stackoverflow.com/questions/3514392/android-ime-showing-a-custom-pop-up-dialog-like-swype-keyboard-which-can-ente#answer-32858312
        // Note：自动补全视图的高度和宽度是固定的，故而，只用取一次
        int width = getMeasuredWidth();
        int height = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.input_completions_view_height);

        showPopupWindow(window, width, height, Gravity.START | Gravity.TOP);
    }

    private void showInputKeyPopupWindow(String key, boolean hideDelayed) {
        if (getKeyboardConfig().isInputKeyPopupTipsDisabled()) {
            return;
        }

        PopupWindow window = this.inputKeyPopupWindow;

        if (key == null || key.isEmpty()) {
            // Note：存在因滑动太快而无法隐藏的问题，故而，延迟隐藏
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

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onCleanInputList(View v) {
        getInputList().reset(true);
    }

    private void onCancelCleanInputList(View v) {
        getInputList().cancelDelete();
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

    private void updateInputListOption(Keyboard.Config config) {
        getInputList().setDefaultUseWordVariant(config.isCandidateVariantFirstEnabled());
    }
}
