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

package org.crazydan.studio.app.ime.kuaizi.pane;

import android.content.Context;
import android.content.SharedPreferences;
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * {@link InputPane 键盘}配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class KeyboardConfig {
    private static final String pref_key_disable_user_input_data = "disable_user_input_data";
    private static final String pref_key_disable_key_clicked_audio = "disable_key_clicked_audio";
    private static final String pref_key_disable_key_animation = "disable_key_animation";
    private static final String pref_key_disable_input_candidates_paging_audio
            = "disable_input_candidates_paging_audio";
    private static final String pref_key_disable_input_key_popup_tips = "disable_input_key_popup_tips";
    private static final String pref_key_disable_gesture_slipping_trail = "disable_gesture_slipping_trail";
    private static final String pref_key_adapt_desktop_swipe_up_gesture = "adapt_desktop_swipe_up_gesture";
    private static final String pref_key_enable_candidate_variant_first = "enable_candidate_variant_first";
    private static final String pref_key_enable_x_input_pad = "enable_x_input_pad";
    private static final String pref_key_enable_latin_use_pinyin_keys_in_x_input_pad
            = "enable_latin_use_pinyin_keys_in_x_input_pad";
    private static final String pref_key_hand_mode = "hand_mode";
    private static final String pref_key_theme = "theme";

    /** 键盘类型 */
    private final Keyboard.Type type;

    /** 当前键盘是从哪个类型的键盘切换过来的，以便于退出时切换到原键盘 */
    private Keyboard.Type switchFromType;
    /** 是否为单行输入 */
    private boolean singleLineInput;
    /** 键盘布局方向 */
    private Keyboard.Orientation orientation = Keyboard.Orientation.portrait;
    /** 左右手模式 */
    private Keyboard.HandMode handMode = Keyboard.HandMode.right;
    /** 主题类型 */
    private Keyboard.Theme theme;
    private ImeSubtype subtype;

    private boolean userInputDataDisabled;
    private boolean keyClickedAudioDisabled;
    private boolean keyAnimationDisabled;
    private boolean pagingAudioDisabled;
    private boolean inputKeyPopupTipsDisabled;
    private boolean gestureSlippingTrailDisabled;
    private boolean desktopSwipeUpGestureAdapted;
    private boolean candidateVariantFirstEnabled;
    private boolean xInputPadEnabled;
    private boolean latinUsePinyinKeysInXInputPadEnabled;

    public KeyboardConfig(Keyboard.Type type) {
        this.type = type;
    }

    public KeyboardConfig(Keyboard.Type type, KeyboardConfig config) {
        this(type);

        if (config != null) {
            this.switchFromType = config.switchFromType;
            this.singleLineInput = config.singleLineInput;
            this.orientation = config.orientation;
            this.handMode = config.handMode;
            this.subtype = config.subtype;

            this.userInputDataDisabled = config.userInputDataDisabled;
            this.keyClickedAudioDisabled = config.keyClickedAudioDisabled;
            this.keyAnimationDisabled = config.keyAnimationDisabled;
            this.pagingAudioDisabled = config.pagingAudioDisabled;
            this.inputKeyPopupTipsDisabled = config.inputKeyPopupTipsDisabled;
            this.gestureSlippingTrailDisabled = config.gestureSlippingTrailDisabled;
            this.desktopSwipeUpGestureAdapted = config.desktopSwipeUpGestureAdapted;
            this.candidateVariantFirstEnabled = config.candidateVariantFirstEnabled;
            this.xInputPadEnabled = config.xInputPadEnabled;
            this.latinUsePinyinKeysInXInputPadEnabled = config.latinUsePinyinKeysInXInputPadEnabled;
        }
    }

    public static boolean isUserInputDataDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_user_input_data, false);
    }

    public static boolean isKeyClickedAudioDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_key_clicked_audio, false);
    }

    public static boolean isKeyAnimationDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_key_animation, false);
    }

    public static boolean isPagingAudioDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_input_candidates_paging_audio, false);
    }

    public static boolean isInputKeyPopupTipsDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_input_key_popup_tips, false);
    }

    public static boolean isGestureSlippingTrailDisabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_disable_gesture_slipping_trail, false);
    }

    public static Keyboard.Theme getTheme(SharedPreferences preferences) {
        String theme = preferences.getString(KeyboardConfig.pref_key_theme, null);

        return theme != null ? Keyboard.Theme.valueOf(theme) : null;
    }

    public static Keyboard.HandMode getHandMode(SharedPreferences preferences) {
        String handMode = preferences.getString(KeyboardConfig.pref_key_hand_mode, "right");

        if ("left".equals(handMode)) {
            return Keyboard.HandMode.left;
        } else {
            return Keyboard.HandMode.right;
        }
    }

    public static boolean isDesktopSwipeUpGestureAdapted(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_adapt_desktop_swipe_up_gesture, false);
    }

    public static boolean isCandidateVariantFirstEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_enable_candidate_variant_first, false);
    }

    public static boolean isXInputPadEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_enable_x_input_pad, false);
    }

    public static boolean isLatinUsePinyinKeysInXInputPadEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(KeyboardConfig.pref_key_enable_latin_use_pinyin_keys_in_x_input_pad, false);
    }

    public static int getThemeResId(Context context, Keyboard.Theme theme) {
        int themeResId = R.style.Theme_Kuaizi_IME_Light;
        if (theme == null) {
            return themeResId;
        }

        switch (theme) {
            case night:
                themeResId = R.style.Theme_Kuaizi_IME_Night;
                break;
            case follow_system:
                int themeMode = context.getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                switch (themeMode) {
                    case android.content.res.Configuration.UI_MODE_NIGHT_NO:
                        themeResId = R.style.Theme_Kuaizi_IME_Light;
                        break;
                    case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                        themeResId = R.style.Theme_Kuaizi_IME_Night;
                        break;
                }
                break;
        }
        return themeResId;
    }

    public Keyboard.Type getType() {
        return this.type;
    }

    public Keyboard.Type getSwitchFromType() {
        return this.switchFromType;
    }

    public void setSwitchFromType(Keyboard.Type switchFromType) {
        this.switchFromType = switchFromType;
    }

    public boolean isSingleLineInput() {
        return this.singleLineInput;
    }

    public void setSingleLineInput(boolean singleLineInput) {
        this.singleLineInput = singleLineInput;
    }

    public Keyboard.Orientation getOrientation() {
        return this.orientation;
    }

    public void setOrientation(Keyboard.Orientation orientation) {
        this.orientation = orientation;
    }

    public Keyboard.HandMode getHandMode() {
        return this.handMode;
    }

    public void setHandMode(Keyboard.HandMode handMode) {
        this.handMode = handMode;
    }

    public boolean isLeftHandMode() {
        return getHandMode() == Keyboard.HandMode.left;
    }

    public Keyboard.Theme getTheme() {
        return this.theme;
    }

    public ImeSubtype getSubtype() {
        return this.subtype;
    }

    public void setSubtype(ImeSubtype subtype) {
        this.subtype = subtype;
    }

    public boolean isUserInputDataDisabled() {
        return this.userInputDataDisabled;
    }

    public void setUserInputDataDisabled(boolean userInputDataDisabled) {
        this.userInputDataDisabled = userInputDataDisabled;
    }

    public boolean isKeyClickedAudioDisabled() {
        return this.keyClickedAudioDisabled;
    }

    public boolean isKeyAnimationDisabled() {
        return this.keyAnimationDisabled;
    }

    public boolean isPagingAudioDisabled() {
        return this.pagingAudioDisabled;
    }

    public boolean isInputKeyPopupTipsDisabled() {
        return this.inputKeyPopupTipsDisabled;
    }

    public void setInputKeyPopupTipsDisabled(boolean inputKeyPopupTipsDisabled) {
        this.inputKeyPopupTipsDisabled = inputKeyPopupTipsDisabled;
    }

    public boolean isGestureSlippingTrailDisabled() {
        return this.gestureSlippingTrailDisabled;
    }

    public boolean isDesktopSwipeUpGestureAdapted() {
        return this.desktopSwipeUpGestureAdapted;
    }

    public boolean isCandidateVariantFirstEnabled() {
        return this.candidateVariantFirstEnabled;
    }

    public void setCandidateVariantFirstEnabled(boolean candidateVariantFirstEnabled) {
        this.candidateVariantFirstEnabled = candidateVariantFirstEnabled;
    }

    public boolean isXInputPadEnabled() {
        return this.xInputPadEnabled;
    }

    public void setXInputPadEnabled(boolean xInputPadEnabled) {
        this.xInputPadEnabled = xInputPadEnabled;
    }

    public boolean isLatinUsePinyinKeysInXInputPadEnabled() {
        // Note：仅汉字输入环境才支持将拉丁文键盘与拼音键盘的按键布局设置为相同的
        return this.latinUsePinyinKeysInXInputPadEnabled && this.subtype == ImeSubtype.hans;
    }

    public void syncWith(SharedPreferences preferences) {
        this.userInputDataDisabled = isUserInputDataDisabled(preferences);
        this.keyClickedAudioDisabled = isKeyClickedAudioDisabled(preferences);
        this.keyAnimationDisabled = isKeyAnimationDisabled(preferences);
        this.pagingAudioDisabled = isPagingAudioDisabled(preferences);
        this.inputKeyPopupTipsDisabled = isInputKeyPopupTipsDisabled(preferences);
        this.gestureSlippingTrailDisabled = isGestureSlippingTrailDisabled(preferences);
        this.desktopSwipeUpGestureAdapted = isDesktopSwipeUpGestureAdapted(preferences);
        this.candidateVariantFirstEnabled = isCandidateVariantFirstEnabled(preferences);
        this.xInputPadEnabled = isXInputPadEnabled(preferences);
        this.latinUsePinyinKeysInXInputPadEnabled = isLatinUsePinyinKeysInXInputPadEnabled(preferences);

        this.handMode = getHandMode(preferences);
        this.theme = getTheme(preferences);
    }
}
