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
import android.view.View;
import android.widget.FrameLayout;
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
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;
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

    public KeyboardView keyboardView;
    public InputListView inputListView;
    private View inputListCleanBtnView;
    private View inputListCleanCancelBtnView;

    private final InputList inputList;
    private Keyboard keyboard;
    private Keyboard.HandMode keyboardHandMode;
    private Boolean disableUserInputData;

    public ImeInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        PinyinDictDB.getInstance().init(getContext());

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.preferences.registerOnSharedPreferenceChangeListener(this);

        this.inputList = new InputList();

        bindViews();
    }

    public void setDisableUserInputData(boolean disableUserInputData) {
        this.disableUserInputData = disableUserInputData;

        if (this.keyboard != null) {
            this.keyboard.getConfig().setUserInputDataDisabled(disableUserInputData);
        }
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
    public void updateKeyboard(Keyboard.Config config) {
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
        Keyboard.Config oldConfig = this.keyboard.getConfig();
        Keyboard.Config newConfig = patchKeyboardConfig(oldConfig);
        this.keyboard.setConfig(newConfig);

        // 主题发生变化，重新绑定视图
        if (oldConfig.getThemeResId() != newConfig.getThemeResId()) {
            bindViews();
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

        if (this.keyboardView != null) {
            this.keyboardView.reset();
        }
        if (this.inputListView != null) {
            this.inputListView.reset();
        }

        this.inputMsgListeners.remove(this);
        this.inputMsgListeners.remove(this.inputListView);
    }

    private void bindViews() {
        reset();
        // 必须先清除已有的子视图，否则，重复 inflate 会无法即时生效
        removeAllViews();

        int themeResId = this.keyboard != null ? this.keyboard.getConfig().getThemeResId() : getThemeResId();
        View rootView = inflateWithTheme(R.layout.ime_input_view_layout, themeResId);

        rootView.findViewById(R.id.settings).setOnClickListener(this::onShowPreferences);

        this.inputListCleanBtnView = rootView.findViewById(R.id.clean_input_list);
        this.inputListCleanCancelBtnView = rootView.findViewById(R.id.cancel_clean_input_list);
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.inputListView = rootView.findViewById(R.id.input_list);

        this.inputListView.updateInputList(this.inputList);

        addInputMsgListener(this);
        addInputMsgListener(this.inputListView);

        bindKeyboard(this.keyboard);
    }

    private void bindKeyboard(Keyboard keyboard) {
        if (keyboard != null) {
            keyboard.setInputList(this.inputList);
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

        Keyboard.HandMode handMode = getHandMode();
        patchedConfig.setHandMode(handMode);

        int themeResId = getThemeResId();
        patchedConfig.setThemeResId(themeResId);

        return patchedConfig;
    }

    public Keyboard.Config getKeyboardConfig() {
        return this.keyboard != null ? this.keyboard.getConfig() : null;
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        return ThemeUtils.inflate(this, resId, themeResId, true);
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

    private int getThemeResId() {
        String theme = this.preferences.getString(Keyboard.Config.pref_key_theme, "night");

        int themeResId = R.style.Theme_Kuaizi_IME_Light;
        switch (theme) {
            case "night":
                themeResId = R.style.Theme_Kuaizi_IME_Night;
                break;
            case "follow_system":
                int themeMode = getContext().getResources().getConfiguration().uiMode
                                & Configuration.UI_MODE_NIGHT_MASK;
                switch (themeMode) {
                    case Configuration.UI_MODE_NIGHT_NO:
                        themeResId = R.style.Theme_Kuaizi_IME_Light;
                        break;
                    case Configuration.UI_MODE_NIGHT_YES:
                        themeResId = R.style.Theme_Kuaizi_IME_Night;
                        break;
                }
                break;
        }

        return themeResId;
    }

    private Keyboard.HandMode getHandMode() {
        String handMode = this.preferences.getString(Keyboard.Config.pref_key_hand_mode, "right");

        if ("left".equals(handMode)) {
            return Keyboard.HandMode.Left;
        } else {
            return Keyboard.HandMode.Right;
        }
    }
}
