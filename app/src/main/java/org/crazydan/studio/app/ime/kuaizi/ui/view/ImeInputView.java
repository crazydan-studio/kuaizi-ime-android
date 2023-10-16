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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
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
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardHandModeSwitchDoneMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchDoingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputCompletionsView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ThemeUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class ImeInputView extends FrameLayout
        implements InputMsgListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private final SharedPreferences preferences;
    private final Set<InputMsgListener> inputMsgListeners = new HashSet<>();
    private final InputList inputList;
    private KeyboardView keyboardView;
    private InputListView inputListView;
    private PopupWindow inputCompletionsPopup;
    private InputCompletionsView inputCompletionsView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;
    private Keyboard keyboard;
    private Keyboard.HandMode keyboardHandMode;
    private Boolean disableUserInputData;
    private boolean disableSharedPreferencesUpdatedListener;

    public ImeInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        PinyinDictDB.getInstance().init(getContext());

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.preferences.registerOnSharedPreferenceChangeListener(this);

        this.inputList = new InputList();

        relayoutViews();
    }

    public InputList getInputList() {
        return this.inputList;
    }

    public void setDisableUserInputData(boolean disabled) {
        this.disableUserInputData = disabled;

        if (this.keyboard != null) {
            this.keyboard.getConfig().setUserInputDataDisabled(disabled);
        }
    }

    public void setDisableSharedPreferencesUpdatedListener(boolean disabled) {
        this.disableSharedPreferencesUpdatedListener = disabled;
    }

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.add(listener);

        if (this.keyboard != null) {
            this.keyboard.addInputMsgListener(listener);
        }
    }

    /** 按监听类型移除监听 */
    public void removeInputMsgListenerByType(Class<?> cls) {
        Iterator<InputMsgListener> it = this.inputMsgListeners.iterator();
        while (it.hasNext()) {
            InputMsgListener listener = it.next();
            if (listener.getClass() != cls) {
                continue;
            }

            it.remove();
            if (this.keyboard != null) {
                this.keyboard.removeInputMsgListener(listener);
            }
        }
    }

    /** 启动指定类型的键盘，并清空输入列表 */
    public void startInput(Keyboard.Type type) {
        startInput(new Keyboard.Config(type), true);
    }

    /** 开始输入 */
    public void startInput(Keyboard.Config config, boolean resetInputList) {
        if (resetInputList) {
            this.inputList.reset(false);
        }
        updateKeyboard(config);
    }

    /** 结束输入 */
    public void finishInput() {
        this.inputList.cleanCommitRevokes();
        this.inputList.cleanDeleteCancels();

        this.keyboard.reset();
    }

    /** 响应键盘输入消息 */
    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        boolean completionsShown = this.inputList.hasCompletions();
        showInputCompletionsPopup(completionsShown);

        switch (msg) {
            case Keyboard_Switch_Doing: {
                Keyboard.Type source = ((KeyboardSwitchDoingMsgData) data).source;
                Keyboard.Type target = ((KeyboardSwitchDoingMsgData) data).target;

                Keyboard.Config config = new Keyboard.Config(target, this.keyboard.getConfig());
                config.setSwitchFromType(source);

                updateKeyboard(config);
                break;
            }
            case Keyboard_HandMode_Switch_Done: {
                // Note：仅记录切换到的模式以便于切换到其他类型键盘时按该模式绘制按键
                this.keyboardHandMode = ((KeyboardHandModeSwitchDoneMsgData) data).mode;
                break;
            }
            default: {
                // 有新输入，则清空 删除撤销数据
                if (!this.inputList.isEmpty()) {
                    this.inputList.cleanDeleteCancels();
                }

                toggleShowInputListCleanBtn();
            }
        }
    }

    /** 根据配置更新键盘：设计键盘切换等 */
    private void updateKeyboard(Keyboard.Config config) {
        Keyboard oldKeyboard = this.keyboard;

        Keyboard newKeyboard = createKeyboard(config.getType());
        if (this.keyboard == null || !newKeyboard.getClass().equals(this.keyboard.getClass())) {
            this.keyboard = newKeyboard;
        } else {
            newKeyboard = this.keyboard;
        }

        Keyboard.Config patchedConfig = patchKeyboardConfig(config);
        // 支持临时修改左右手模式
        if (this.keyboardHandMode != null) {
            patchedConfig.setHandMode(this.keyboardHandMode);
        }
        // 支持临时禁用对用户输入的记录
        if (this.disableUserInputData != null) {
            patchedConfig.setUserInputDataDisabled(this.disableUserInputData);
        }

        newKeyboard.setConfig(patchedConfig);

        if (oldKeyboard != newKeyboard) {
            if (oldKeyboard != null) {
                oldKeyboard.destroy();
            }

            bindKeyboard(newKeyboard);
        } else {
            newKeyboard.reset();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (this.disableSharedPreferencesUpdatedListener) {
            return;
        }

        Keyboard.Config oldConfig = this.keyboard.getConfig();
        Keyboard.Config newConfig = patchKeyboardConfig(oldConfig);

        updateKeyboardConfig(newConfig);
    }

    private void updateKeyboardConfig(Keyboard.Config newConfig) {
        Keyboard.Config oldConfig = this.keyboard.getConfig();
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
    }

    private void reset() {
        if (this.keyboard != null) {
            this.inputMsgListeners.forEach(this.keyboard::removeInputMsgListener);
        }

        resetInputCompletionsPopup();

        if (this.keyboardView != null) {
            this.keyboardView.reset();
        }
        if (this.inputListView != null) {
            this.inputListView.reset();
        }
        if (this.inputCompletionsView != null) {
            this.inputCompletionsView.reset();
        }

        this.inputMsgListeners.remove(this);
        this.inputMsgListeners.remove(this.inputListView);
        this.inputMsgListeners.remove(this.inputCompletionsView);
    }

    private void relayoutViews() {
        reset();
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        Keyboard.Config config = getKeyboardConfig();
        Keyboard.ThemeType theme = config.getTheme();
        int themeResId = Keyboard.Config.getThemeResId(getContext(), theme);

        View rootView = inflateWithTheme(R.layout.ime_input_view_layout, themeResId);

        rootView.findViewById(R.id.settings).setOnClickListener(this::onShowPreferences);

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.inputListView = rootView.findViewById(R.id.input_list);

        this.inputCompletionsView = inflateWithTheme(R.layout.input_completions_view, themeResId, false);
        prepareInputCompletionsPopup(this.inputCompletionsView);

        this.inputListView.updateInputList(this.inputList);
        this.inputCompletionsView.setInputList(this.inputList);

        addInputMsgListener(this);
        addInputMsgListener(this.inputListView);
        addInputMsgListener(this.inputCompletionsView);

        bindKeyboard(this.keyboard);
    }

    private boolean needToRelayoutViews(Keyboard.Config oldConfig, Keyboard.Config newConfig) {
        return oldConfig.getTheme() != newConfig.getTheme()
               || oldConfig.isDesktopSwipeUpGestureAdapted() != newConfig.isDesktopSwipeUpGestureAdapted();
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
        Keyboard.Config config = keyboard != null ? keyboard.getConfig() : null;
        // Note：仅竖屏模式下需要添加底部空白
        addBottomSpacing(this,
                         config != null
                         && config.isDesktopSwipeUpGestureAdapted()
                         && config.getOrientation() == Keyboard.Orientation.Portrait);

        if (keyboard != null) {
            keyboard.setInputList(this.inputList);
            keyboard.start();

            this.inputMsgListeners.forEach(keyboard::addInputMsgListener);

            this.keyboardView.updateKeyboard(keyboard);
        }
    }

    private Keyboard.Config patchKeyboardConfig(Keyboard.Config config) {
        Keyboard.Config patchedConfig = new Keyboard.Config(config.getType(), config);

        boolean disableUserInputData = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_user_input_data,
                                                                   false);
        patchedConfig.setUserInputDataDisabled(disableUserInputData);

        boolean disableKeyClickedAudio = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_key_clicked_audio,
                                                                     false);
        patchedConfig.setKeyClickedAudioDisabled(disableKeyClickedAudio);

        boolean disableKeyAnimation = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_key_animation,
                                                                  false);
        patchedConfig.setKeyAnimationDisabled(disableKeyAnimation);

        boolean disableInputCandidatesPagingAudio
                = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_input_candidates_paging_audio, false);
        patchedConfig.setPagingAudioDisabled(disableInputCandidatesPagingAudio);

        boolean desktopSwipeUpGestureAdapted = Keyboard.Config.isDesktopSwipeUpGestureAdapted(this.preferences);
        patchedConfig.setDesktopSwipeUpGestureAdapted(desktopSwipeUpGestureAdapted);

        Keyboard.HandMode handMode = Keyboard.Config.getHandMode(this.preferences);
        patchedConfig.setHandMode(handMode);

        Keyboard.ThemeType theme = Keyboard.Config.getTheme(this.preferences);
        patchedConfig.setTheme(theme);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            patchedConfig.setOrientation(Keyboard.Orientation.Landscape);
        } else {
            patchedConfig.setOrientation(Keyboard.Orientation.Portrait);
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

    private void prepareInputCompletionsPopup(InputCompletionsView completionsView) {
        resetInputCompletionsPopup();

        PopupWindow window = this.inputCompletionsPopup;

        window.setClippingEnabled(false);
        window.setBackgroundDrawable(null);
        window.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        window.setContentView(completionsView);
    }

    private void showInputCompletionsPopup(boolean shown) {
        PopupWindow window = this.inputCompletionsPopup;

        if (!shown) {
            // Note：必须延后关闭窗口，否则，
            // 若直接关闭，点击事件会被再次发送，
            // 造成 CompletionViewGestureListener 触发两次补全事件
            post(window::dismiss);
            return;
        } else if (window.isShowing()) {
            return;
        }

        // https://android.googlesource.com/platform/packages/inputmethods/PinyinIME/+/40056ae7c2757681d88d2e226c4681281bd07129/src/com/android/inputmethod/pinyin/PinyinIME.java#1247
        // https://stackoverflow.com/questions/3514392/android-ime-showing-a-custom-pop-up-dialog-like-swype-keyboard-which-can-ente#answer-32858312
        // Note：自动补全视图的高度和宽度是固定的，故而，只用取一次
        int width = getMeasuredWidth();
        int height = (int) ScreenUtils.pxFromDimension(getContext(), R.dimen.input_completions_view_height);

        window.setWidth(width);
        window.setHeight(height);

        // 放置于被布局的键盘之上
        View parent = this;
        int[] location = new int[2];
        parent.getLocationInWindow(location);

        int x = location[0];
        int y = location[1] - window.getHeight();

        post(() -> window.showAtLocation(parent, Gravity.START | Gravity.TOP, x, y));
    }

    private void resetInputCompletionsPopup() {
        if (this.inputCompletionsPopup != null) {
            this.inputCompletionsPopup.dismiss();
        } else {
            this.inputCompletionsPopup = new PopupWindow();
        }
    }

    private void onShowPreferences(View v) {
        SystemUtils.showAppPreferences(getContext());
    }

    private void onCleanInputList(View v) {
        this.inputList.reset(true);
    }

    private void onCancelCleanInputList(View v) {
        this.inputList.cancelDelete();
    }

    private void toggleShowInputListCleanBtn() {
        if (this.inputList.isEmpty()) {
            if (this.inputList.canCancelDelete()) {
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
}
