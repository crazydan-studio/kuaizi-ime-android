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

package org.crazydan.studio.app.ime.kuaizi.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.InputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserInputMsgListener;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsg;
import org.crazydan.studio.app.ime.kuaizi.internal.msg.UserKeyMsgData;

/**
 * 键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface Keyboard extends UserInputMsgListener {

    Config getConfig();

    void setConfig(Config config);

    KeyFactory getKeyFactory();

    void setInputList(InputList inputList);

    /** 启动 */
    void start();

    /** 重置状态 */
    void reset();

    /** 销毁 */
    void destroy();

    /** 键盘主题已更新 */
    void onThemeUpdated();

    /** 处理{@link UserKeyMsg 按键消息} */
    void onUserKeyMsg(UserKeyMsg msg, UserKeyMsgData data);

    /**
     * 添加{@link InputMsg 输入消息监听}
     * <p/>
     * 忽略重复加入的监听，且执行顺序与添加顺序无关
     */
    void addInputMsgListener(InputMsgListener listener);

    /** 移除{@link InputMsg 输入消息监听} */
    void removeInputMsgListener(InputMsgListener listener);

    /** 键盘类型 */
    enum Type {
        /** 汉语拼音键盘 */
        Pinyin,
        /** 算数键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,
    }

    /** 键盘布局方向 */
    enum Orientation {
        /** 纵向 */
        Portrait,
        /** 横向 */
        Landscape,
    }

    /** 左右手模式 */
    enum HandMode {
        /** 左手模式 */
        Left,
        /** 右手模式 */
        Right,
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
        private static final String pref_key_adapt_desktop_swipe_up_gesture = "adapt_desktop_swipe_up_gesture";
        private static final String pref_key_hand_mode = "hand_mode";
        private static final String pref_key_theme = "theme";

        /** 键盘类型 */
        private final Type type;

        /** 当前键盘是从哪个类型的键盘切换过来的，以便于退出时切换到原键盘 */
        private Type switchFromType;
        /** 是否为单行输入 */
        private boolean singleLineInput;
        /** 键盘布局方向 */
        private Orientation orientation = Orientation.Portrait;
        /** 左右手模式 */
        private HandMode handMode = HandMode.Right;
        /** 主题类型 */
        private ThemeType theme;

        private boolean userInputDataDisabled;
        private boolean keyClickedAudioDisabled;
        private boolean keyAnimationDisabled;
        private boolean pagingAudioDisabled;
        private boolean desktopSwipeUpGestureAdapted;

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

                this.userInputDataDisabled = config.userInputDataDisabled;
                this.keyClickedAudioDisabled = config.keyClickedAudioDisabled;
                this.keyAnimationDisabled = config.keyAnimationDisabled;
                this.pagingAudioDisabled = config.pagingAudioDisabled;
                this.desktopSwipeUpGestureAdapted = config.desktopSwipeUpGestureAdapted;
            }
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
            return getHandMode() == HandMode.Left;
        }

        public ThemeType getTheme() {
            return this.theme;
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

        public boolean isDesktopSwipeUpGestureAdapted() {
            return this.desktopSwipeUpGestureAdapted;
        }

        public void syncWith(SharedPreferences preferences) {
            boolean disableUserInputData = Keyboard.Config.isUserInputDataDisabled(preferences);
            setUserInputDataDisabled(disableUserInputData);

            this.keyClickedAudioDisabled = Keyboard.Config.isKeyClickedAudioDisabled(preferences);
            this.keyAnimationDisabled = Keyboard.Config.isKeyAnimationDisabled(preferences);
            this.pagingAudioDisabled = Keyboard.Config.isPagingAudioDisabled(preferences);
            this.desktopSwipeUpGestureAdapted = Keyboard.Config.isDesktopSwipeUpGestureAdapted(preferences);

            Keyboard.HandMode handMode = Keyboard.Config.getHandMode(preferences);
            setHandMode(handMode);

            this.theme = Keyboard.Config.getTheme(preferences);
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

        public static ThemeType getTheme(SharedPreferences preferences) {
            String theme = preferences.getString(Keyboard.Config.pref_key_theme, null);

            return theme != null ? ThemeType.valueOf(theme) : null;
        }

        public static HandMode getHandMode(SharedPreferences preferences) {
            String handMode = preferences.getString(Keyboard.Config.pref_key_hand_mode, "right");

            if ("left".equals(handMode)) {
                return Keyboard.HandMode.Left;
            } else {
                return Keyboard.HandMode.Right;
            }
        }

        public static boolean isDesktopSwipeUpGestureAdapted(SharedPreferences preferences) {
            return preferences.getBoolean(Keyboard.Config.pref_key_adapt_desktop_swipe_up_gesture, false);
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
                    int themeMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
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
}
