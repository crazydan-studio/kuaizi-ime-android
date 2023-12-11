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

package org.crazydan.studio.app.ime.kuaizi.core;

import java.util.function.Supplier;

import android.content.Context;
import android.content.SharedPreferences;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.conf.Configuration;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.UserKeyMsgData;

/**
 * 键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface Keyboard extends UserInputMsgListener {

    Type getType();

    Configuration getConfig();

    void setConfig(Supplier<Configuration> getter);

    KeyFactory getKeyFactory();

    void setInputList(Supplier<InputList> getter);

    /** 启动 */
    void start();

    /** 重置状态 */
    void reset();

    /** 销毁 */
    void destroy();

    /** 处理{@link UserKeyMsg 按键消息} */
    void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data);

    /** 键盘类型 */
    enum Type {
        /** 汉语拼音键盘 */
        Pinyin,
        /** 算术键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,
    }

    /** 键盘布局方向 */
    enum Orientation {
        /** 纵向 */
        portrait,
        /** 横向 */
        landscape,
    }

    /** 左右手模式 */
    enum HandMode {
        /** 左手模式 */
        left,
        /** 右手模式 */
        right,
    }

    enum ThemeType {
        light(R.string.value_theme_light),
        night(R.string.value_theme_night),
        follow_system(R.string.value_theme_follow_system),
        ;

        private final int labelResId;

        ThemeType(int labelResId) {
            this.labelResId = labelResId;
        }

        public int getLabelResId() {
            return this.labelResId;
        }
    }

    /** 键盘子类型 */
    enum Subtype {
        /** 拉丁文 */
        latin,
        /** 汉字 */
        hans,
    }

    /** 按键生成器 */
    interface KeyFactory {
        /**
         * 创建二维矩阵{@link Key 按键}
         * <p/>
         * 元素可为<code>null</code>，
         * 表示该位置不放置任何按键
         */
        Key<?>[][] create();
    }

    /** 无动效的按键生成器 */
    interface NoAnimationKeyFactory extends KeyFactory {}

    /** 键盘配置 */
    class Config {
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
        private final Type type;

        /** 当前键盘是从哪个类型的键盘切换过来的，以便于退出时切换到原键盘 */
        private Type switchFromType;
        /** 是否为单行输入 */
        private boolean singleLineInput;
        /** 键盘布局方向 */
        private Orientation orientation = Orientation.portrait;
        /** 左右手模式 */
        private HandMode handMode = HandMode.right;
        /** 主题类型 */
        private ThemeType theme;
        private Subtype subtype;

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

        public Config(Type type) {
            this.type = type;
        }

        public Config(Type type, Config config) {
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
            return preferences.getBoolean(Keyboard.Config.pref_key_disable_user_input_data, false);
        }

        public static boolean isKeyClickedAudioDisabled(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_disable_key_clicked_audio, false);
        }

        public static boolean isKeyAnimationDisabled(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_disable_key_animation, false);
        }

        public static boolean isPagingAudioDisabled(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_disable_input_candidates_paging_audio, false);
        }

        public static boolean isInputKeyPopupTipsDisabled(SharedPreferences preferences) {
            return preferences.getBoolean(Config.pref_key_disable_input_key_popup_tips, false);
        }

        public static boolean isGestureSlippingTrailDisabled(SharedPreferences preferences) {
            return preferences.getBoolean(Config.pref_key_disable_gesture_slipping_trail, false);
        }

        public static ThemeType getTheme(SharedPreferences preferences) {
            String theme = preferences.getString(Keyboard.Config.pref_key_theme, null);

            return theme != null ? ThemeType.valueOf(theme) : null;
        }

        public static HandMode getHandMode(SharedPreferences preferences) {
            String handMode = preferences.getString(Keyboard.Config.pref_key_hand_mode, "right");

            if ("left".equals(handMode)) {
                return Keyboard.HandMode.left;
            } else {
                return Keyboard.HandMode.right;
            }
        }

        public static boolean isDesktopSwipeUpGestureAdapted(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_adapt_desktop_swipe_up_gesture, false);
        }

        public static boolean isCandidateVariantFirstEnabled(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_enable_candidate_variant_first, false);
        }

        public static boolean isXInputPadEnabled(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_enable_x_input_pad, false);
        }

        public static boolean isLatinUsePinyinKeysInXInputPadEnabled(SharedPreferences preferences) {
            return preferences.getBoolean(Config.pref_key_enable_latin_use_pinyin_keys_in_x_input_pad, false);
        }

        public static int getThemeResId(Context context, Keyboard.ThemeType theme) {
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

        public Type getType() {
            return this.type;
        }

        public Type getSwitchFromType() {
            return this.switchFromType;
        }

        public void setSwitchFromType(Type switchFromType) {
            this.switchFromType = switchFromType;
        }

        public boolean isSingleLineInput() {
            return this.singleLineInput;
        }

        public void setSingleLineInput(boolean singleLineInput) {
            this.singleLineInput = singleLineInput;
        }

        public Orientation getOrientation() {
            return this.orientation;
        }

        public void setOrientation(Orientation orientation) {
            this.orientation = orientation;
        }

        public HandMode getHandMode() {
            return this.handMode;
        }

        public void setHandMode(HandMode handMode) {
            this.handMode = handMode;
        }

        public boolean isLeftHandMode() {
            return getHandMode() == HandMode.left;
        }

        public ThemeType getTheme() {
            return this.theme;
        }

        public Subtype getSubtype() {
            return this.subtype;
        }

        public void setSubtype(Subtype subtype) {
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
            return this.latinUsePinyinKeysInXInputPadEnabled && this.subtype == Subtype.hans;
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
}
