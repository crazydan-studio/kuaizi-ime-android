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

import java.util.Objects;

import android.content.Context;
import android.content.SharedPreferences;
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
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.core.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.LatinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.MathKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.NumberKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.PinyinKeyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputAudioPlayDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCharsInputPopupShowingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.InputCommonMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardHandModeSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.msg.input.KeyboardSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.core.view.InputCompletionsView;
import org.crazydan.studio.app.ime.kuaizi.core.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.core.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.core.view.key.XPadKeyView;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.widget.AudioPlayer;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class ImeInputView extends FrameLayout implements InputMsgListener, UserInputMsgListener {
    /** 记录系统的持久化配置 */
    private final Configuration sysConf;
    /** 记录应用临时变更的配置 */
    private final Configuration appConf;

    private final AudioPlayer audioPlayer;

    private InputMsgListener listener;

    private Keyboard keyboard;
    private KeyboardView keyboardView;
    private final InputList inputList;
    private InputListView inputListView;

    private PopupWindow inputKeyPopupWindow;
    private PopupWindow inputCompletionsPopupWindow;
    private InputCompletionsView inputCompletionsView;

    private View settingsBtnView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;
    private boolean disableSettingsBtn;

    public ImeInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        PinyinDict.getInstance().init(getContext());

        this.appConf = new Configuration();
        this.sysConf = new Configuration(this::onConfigurationChanged);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.sysConf.bind(preferences);

        this.inputList = new InputList();
        this.inputList.setConfig(this::getConfig);
        this.inputList.setListener(this);

        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.load(getContext(),
                              R.raw.tick_single,
                              R.raw.tick_double,
                              R.raw.page_flip,
                              R.raw.tick_clock,
                              R.raw.tick_ping);

        relayoutViews();
    }

    public void setListener(InputMsgListener listener) {
        this.listener = listener;
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public Keyboard getKeyboard() {
        return this.keyboard;
    }

    public Keyboard.Type getKeyboardType() {
        return this.keyboard != null ? this.keyboard.getType() : null;
    }

    public Configuration getConfig() {
        Configuration config = this.sysConf.copy();
        config.merge(this.appConf);

        Keyboard.Orientation orientation;
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            orientation = Keyboard.Orientation.landscape;
        } else {
            orientation = Keyboard.Orientation.portrait;
        }
        config.set(Conf.orientation, orientation);

        return config;
    }

    public XPadKeyView getXPadKeyView() {
        return this.keyboardView.getXPadKeyView();
    }

    // =================================================
    public void setSubtype(Keyboard.Subtype subtype) {
        this.appConf.set(Conf.subtype, subtype);
    }

    public void setSingleLineInput(boolean enabled) {
        this.appConf.set(Conf.single_line_input, enabled);
    }

    /** 若传参 null，则表示使用系统持久化配置值 */
    public void disableUserInputData(Boolean disabled) {
        this.appConf.set(Conf.disable_user_input_data, disabled);
    }

    /** 若传参 null，则表示使用系统持久化配置值 */
    public void disableInputKeyPopupTips(Boolean disabled) {
        this.appConf.set(Conf.disable_input_key_popup_tips, disabled);
    }

    /** 若传参 null，则表示使用系统持久化配置值 */
    public void enableXInputPad(Boolean enabled) {
        Boolean old = this.appConf.get(Conf.enable_x_input_pad);
        this.appConf.set(Conf.enable_x_input_pad, enabled);

        if (!Objects.equals(old, enabled)) {
            updateBottomSpacing();
        }
    }

    /** 若传参 null，则表示使用系统持久化配置值 */
    public void enableCandidateVariantFirst(Boolean enabled) {
        this.appConf.set(Conf.enable_candidate_variant_first, enabled);
    }

    public void disableSettingsBtn(boolean disabled) {
        this.disableSettingsBtn = disabled;

        toggleEnableSettingsBtn();
    }
    // ============================================

    /** 启动指定类型的键盘，并清空输入列表 */
    public void startInput(Keyboard.Type type) {
        startInput(type, true);
    }

    /** 启动指定类型的键盘 */
    public void startInput(Keyboard.Type type, boolean resetInputList) {
        //Log.i("SwitchKeyboard", String.format("%s - %s", type, resetInputList));
        updateKeyboard(type);

        // 先更新键盘，再重置输入列表
        if (resetInputList) {
            getInputList().reset(false);
        }
    }

    /** 隐藏输入 */
    public void hideInput() {
        showInputCompletionsPopupWindow(false);
    }

    /** 结束输入 */
    public void finishInput() {
        getInputList().clear();
        getKeyboard().reset();
    }

    private void onConfigurationChanged(Conf conf, Object oldValue, Object newValue) {
        Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }

        // Note: 仅当新旧配置值不相等时才会触发配置更新，故而，仅需检查哪些配置项发生了变更即可
        switch (conf) {
            case theme: {
                relayoutViews();
                break;
            }
            case enable_x_input_pad:
            case adapt_desktop_swipe_up_gesture: {
                updateBottomSpacing();
                break;
            }
            case enable_candidate_variant_first: {
                getInputList().setOption(null);
                break;
            }
        }

        onMsg(keyboard, InputMsg.Keyboard_Config_Update_Done, new InputCommonMsgData());
    }

    /** 响应 {@link UserInputMsg} 消息 */
    @Override
    public void onMsg(InputList inputList, UserInputMsg msg, UserInputMsgData msgData) {
        Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }

        keyboard.onMsg(inputList, msg, msgData);
    }

    /** 响应 {@link InputMsg} 消息 */
    @Override
    public void onMsg(Keyboard keyboard, InputMsg msg, InputMsgData msgData) {
        // Note: 存在在相邻消息中切换键盘的情况，故而，需忽略切换前的键盘消息
        if (getKeyboard() != keyboard) {
            return;
        }

        this.keyboardView.onMsg(keyboard, msg, msgData);
        this.inputListView.onMsg(keyboard, msg, msgData);

        boolean completionsShown = getInputList().hasCompletions();
        showInputCompletionsPopupWindow(completionsShown);

        switch (msg) {
            case InputAudio_Play_Doing: {
                on_InputAudio_Play_Doing_Msg((InputAudioPlayDoingMsgData) msgData);
                return;
            }
            case Keyboard_Switch_Doing: {
                Keyboard.Type target = ((KeyboardSwitchingMsgData) msgData).target;
                updateKeyboard(target);

                // Note: 消息发送者需为更新后的 Keyboard
                onMsg(getKeyboard(), InputMsg.Keyboard_Switch_Done, msgData);
                return;
            }
            case Keyboard_HandMode_Switch_Doing: {
                Keyboard.HandMode mode = ((KeyboardHandModeSwitchingMsgData) msgData).mode;
                this.appConf.set(Conf.hand_mode, mode);

                onMsg(keyboard, InputMsg.Keyboard_HandMode_Switch_Done, msgData);
                return;
            }
            case InputChars_Input_Popup_Show_Doing: {
                showInputKeyPopupWindow(((InputCharsInputPopupShowingMsgData) msgData).text,
                                        ((InputCharsInputPopupShowingMsgData) msgData).hideDelayed);
                return;
            }
            case InputChars_Input_Popup_Hide_Doing: {
                showInputKeyPopupWindow(null, false);
                return;
            }
            default: {
                // 有新输入，则清空 删除撤销数据
                if (!getInputList().isEmpty()) {
                    getInputList().clearDeleteCancels();
                }

                toggleShowInputListCleanBtn();
            }
        }

        // 最后处理外部监听，以确保内部已经处理完成
        if (this.listener != null) {
            this.listener.onMsg(keyboard, msg, msgData);
        }
    }

    /** 根据配置更新键盘：涉及键盘切换等 */
    private void updateKeyboard(Keyboard.Type type) {
        Keyboard oldKeyboard = this.keyboard;
        Keyboard.Type oldType = oldKeyboard != null ? oldKeyboard.getType() : null;

        if (oldType != null && (oldType == type || type == Keyboard.Type.Keep_Current)) {
            oldKeyboard.reset();
            return;
        }

        // ====================================================
        Keyboard.Subtype subtype = null;
        switch (type) {
            // 切换系统子键盘时的情况
            case By_Subtype: {
                subtype = this.appConf.get(Conf.subtype);
                break;
            }
            // 仅首次切换到本输入法时的情况
            case Keep_Current: {
                subtype = SystemUtils.getImeSubtype(getContext());
                break;
            }
        }
        if (subtype != null) {
            if (subtype == Keyboard.Subtype.latin) {
                type = Keyboard.Type.Latin;
            } else {
                type = Keyboard.Type.Pinyin;
            }
        }

        if (oldKeyboard != null) {
            oldKeyboard.destroy();
        }

        Keyboard newKeyboard = createKeyboard(type, oldType);
        newKeyboard.setInputList(this::getInputList);
        newKeyboard.setConfig(this::getConfig);

        this.keyboard = newKeyboard;
        newKeyboard.start();
    }

    private void relayoutViews() {
        resetPopupWindows();
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Configuration config = getConfig();
        Keyboard.ThemeType theme = config.get(Conf.theme);
        int themeResId = Keyboard.Config.getThemeResId(getContext(), theme);

        View rootView = inflateWithTheme(R.layout.ime_input_view_layout, themeResId);

        this.settingsBtnView = rootView.findViewById(R.id.settings);
        toggleEnableSettingsBtn();

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.keyboardView.setConfig(this::getConfig);
        this.keyboardView.setKeyboard(this::getKeyboard);

        this.inputListView = rootView.findViewById(R.id.input_list);
        this.inputListView.setInputList(this::getInputList);

        View inputKeyView = inflateWithTheme(R.layout.input_popup_key_view, themeResId, false);
        this.inputCompletionsView = inflateWithTheme(R.layout.input_completions_view, themeResId, false);
        this.inputCompletionsView.setInputList(this::getInputList);
        preparePopupWindows(this.inputCompletionsView, inputKeyView);

        updateBottomSpacing();
    }

    private void updateBottomSpacing() {
        Configuration config = getConfig();

        // Note: 仅竖屏模式下需要添加底部空白
        addBottomSpacing(this,
                         config.bool(Conf.adapt_desktop_swipe_up_gesture)
                         && !config.isXInputPadEnabled()
                         && Objects.equals(config.get(Conf.orientation), Keyboard.Orientation.portrait));
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

    private Keyboard createKeyboard(Keyboard.Type type, Keyboard.Type oldType) {
        switch (type) {
            case Math:
                return new MathKeyboard(this, oldType);
            case Latin:
                return new LatinKeyboard(this, oldType);
            case Number:
                return new NumberKeyboard(this, oldType);
            default:
                return new PinyinKeyboard(this, oldType);
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

        this.inputCompletionsView.update();
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
        Configuration config = getConfig();
        if (config.bool(Conf.disable_input_key_popup_tips)) {
            return;
        }

        PopupWindow window = this.inputKeyPopupWindow;

        if (key == null || key.isEmpty()) {
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

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onCleanInputList(View v) {
        on_InputAudio_Play_Doing_Msg(new InputAudioPlayDoingMsgData(null,
                                                                    InputAudioPlayDoingMsgData.AudioType.SingleTick));

        getInputList().reset(true);
    }

    private void onCancelCleanInputList(View v) {
        on_InputAudio_Play_Doing_Msg(new InputAudioPlayDoingMsgData(null,
                                                                    InputAudioPlayDoingMsgData.AudioType.SingleTick));

        getInputList().cancelDelete();
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

    private void on_InputAudio_Play_Doing_Msg(InputAudioPlayDoingMsgData data) {
        Configuration config = getConfig();
        if (data.audioType == InputAudioPlayDoingMsgData.AudioType.PageFlip) {
            if (config.bool(Conf.disable_input_candidates_paging_audio)) {
                return;
            }
        } else if (config.bool(Conf.disable_key_clicked_audio)) {
            return;
        }

        switch (data.audioType) {
            case SingleTick:
                this.audioPlayer.play(R.raw.tick_single);
                break;
            case DoubleTick:
                this.audioPlayer.play(R.raw.tick_double);
                break;
            case ClockTick:
                this.audioPlayer.play(R.raw.tick_clock);
                break;
            case PingTick:
                this.audioPlayer.play(R.raw.tick_ping);
                break;
            case PageFlip:
                this.audioPlayer.play(R.raw.page_flip);
                break;
        }
    }
}
