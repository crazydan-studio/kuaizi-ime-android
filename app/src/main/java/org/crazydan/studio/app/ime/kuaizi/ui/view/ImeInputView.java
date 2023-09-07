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
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
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
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardHandModeSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.input.KeyboardSwitchingMsgData;
import org.crazydan.studio.app.ime.kuaizi.internal.view.InputListView;
import org.crazydan.studio.app.ime.kuaizi.internal.view.KeyboardView;
import org.crazydan.studio.app.ime.kuaizi.ui.Preferences;

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

    private final InputList inputList;
    private Keyboard keyboard;
    private Keyboard.HandMode keyboardHandMode;

    public ImeInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        PinyinDictDB.getInstance().init(getContext());

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.preferences.registerOnSharedPreferenceChangeListener(this);

        this.inputList = new InputList();

        bindViews();
    }

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    public void addInputMsgListener(InputMsgListener listener) {
        this.inputMsgListeners.add(listener);
    }

    /** 开始输入 */
    public void startInput(Keyboard.Config config, boolean resetInputList) {
        if (resetInputList) {
            this.inputList.reset();
        }
        updateKeyboard(config);
    }

    /** 结束输入 */
    public void finishInput() {
        this.keyboard.reset();
    }

    /** 响应键盘输入消息 */
    @Override
    public void onInputMsg(InputMsg msg, InputMsgData data) {
        switch (msg) {
            case Keyboard_Switching: {
                Keyboard.Type source = ((KeyboardSwitchingMsgData) data).source;
                Keyboard.Type target = ((KeyboardSwitchingMsgData) data).target;

                Keyboard.Config config = new Keyboard.Config(target, this.keyboard.getConfig());
                config.setSwitchFromType(source);

                updateKeyboard(config);
                break;
            }
            case HandMode_Switching: {
                this.keyboardHandMode = ((KeyboardHandModeSwitchingMsgData) data).mode;
                break;
            }
            default: {
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
        toggleShowInputListCleanBtn();

        this.keyboardView = rootView.findViewById(R.id.keyboard);
        this.inputListView = rootView.findViewById(R.id.input_list);

        this.inputListView.setInputList(this.inputList);

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

        boolean disablePinyinGlidingInputAnimation
                = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_pinyin_gliding_input_animation, false);
        patchedConfig.setGlidingInputAnimationDisabled(disablePinyinGlidingInputAnimation);

        boolean disableInputCandidatesPagingAudio
                = this.preferences.getBoolean(Keyboard.Config.pref_key_disable_input_candidates_paging_audio, false);
        patchedConfig.setPagingAudioDisabled(disableInputCandidatesPagingAudio);

        Keyboard.HandMode handMode = getHandMode();
        patchedConfig.setHandMode(handMode);

        int themeResId = getThemeResId();
        patchedConfig.setThemeResId(themeResId);

        return patchedConfig;
    }

    private <T extends View> T inflateWithTheme(int resId, int themeResId) {
        // 通过 Context Theme 仅对键盘自身的视图设置主题样式，
        // 以避免通过 AppCompatDelegate.setDefaultNightMode 对配置等视图造成影响
        // https://stackoverflow.com/questions/65433795/unable-to-update-the-day-and-night-modes-in-android-with-window-manager-screens#answer-67340930
        Context context = new ContextThemeWrapper(getContext(), themeResId);

        return (T) LayoutInflater.from(context).inflate(resId, this);
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
        Context context = getContext();

        Intent intent;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//            // Settings.ACTION_LOCALE_SETTINGS: 打开语言设置
//            // Settings.ACTION_INPUT_METHOD_SETTINGS: 打开输入法设置
//            intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
//        } else {
        // https://stackoverflow.com/questions/32822101/how-can-i-programmatically-open-the-permission-screen-for-a-specific-app-on-andr#answer-43707264
        intent = new Intent(context, Preferences.class);
//        }

        // If set then opens Settings Screen(Activity) as new activity.
        // Otherwise, it will be opened in currently running activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);
    }

    private void onCleanInputList(View v) {
        this.inputList.reset();
    }

    private void toggleShowInputListCleanBtn() {
        if (this.inputList.isEmpty()) {
            this.inputListCleanBtnView.setAlpha(0);
            this.inputListCleanBtnView.setOnClickListener(null);
        } else {
            this.inputListCleanBtnView.setAlpha(1);
            this.inputListCleanBtnView.setOnClickListener(this::onCleanInputList);
        }
    }

    private int getThemeResId() {
        String theme = this.preferences.getString(Keyboard.Config.pref_key_theme, "night");

        int themeResId = R.style.Theme_KuaiziIME_Night;
        switch (theme) {
            case "light":
                themeResId = R.style.Theme_KuaiziIME_Light;
                break;
            case "follow_system":
                int themeMode = getContext().getResources().getConfiguration().uiMode
                                & Configuration.UI_MODE_NIGHT_MASK;
                switch (themeMode) {
                    case Configuration.UI_MODE_NIGHT_NO:
                        themeResId = R.style.Theme_KuaiziIME_Light;
                        break;
                    case Configuration.UI_MODE_NIGHT_YES:
                        themeResId = R.style.Theme_KuaiziIME_Night;
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
