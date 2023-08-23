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

    void setConfig(Config config);

    Config getConfig();

    KeyFactory getKeyFactory();

    void setInputList(InputList inputList);

    /** 重置状态 */
    void reset();

    /** 销毁 */
    void destroy();

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
        /** 数学键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,
        /** 标点符号键盘：各类符号可切换 */
        Symbol,
        /** 表情符号键盘 */
        Emotion,
    }

    /** 键盘布局方向 */
    enum Orientation {
        /** 纵向 */
        Portrait,
        /** 横向 */
        Landscape,
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

    /** 键盘配置 */
    class Config {
        public static final String pref_key_disable_user_input_data = "disable_user_input_data";
        public static final String pref_key_disable_key_clicked_audio = "disable_key_clicked_audio";
        public static final String pref_key_disable_pinyin_gliding_input_animation
                = "disable_pinyin_gliding_input_animation";
        public static final String pref_key_disable_input_candidates_paging_audio
                = "disable_input_candidates_paging_audio";
        public static final String pref_key_hand_mode = "hand_mode";
        public static final String pref_key_theme = "theme";

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
        /** 主题资源 id */
        private int themeResId;

        private boolean userInputDataDisabled;
        private boolean keyClickedAudioDisabled;
        private boolean glidingInputAnimationDisabled;
        private boolean pagingAudioDisabled;

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
                this.glidingInputAnimationDisabled = config.glidingInputAnimationDisabled;
                this.pagingAudioDisabled = config.pagingAudioDisabled;
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

        public int getThemeResId() {
            return this.themeResId;
        }

        public void setThemeResId(int themeResId) {
            this.themeResId = themeResId;
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

        public void setKeyClickedAudioDisabled(boolean keyClickedAudioDisabled) {
            this.keyClickedAudioDisabled = keyClickedAudioDisabled;
        }

        public boolean isGlidingInputAnimationDisabled() {
            return this.glidingInputAnimationDisabled;
        }

        public void setGlidingInputAnimationDisabled(boolean glidingInputAnimationDisabled) {
            this.glidingInputAnimationDisabled = glidingInputAnimationDisabled;
        }

        public boolean isPagingAudioDisabled() {
            return this.pagingAudioDisabled;
        }

        public void setPagingAudioDisabled(boolean pagingAudioDisabled) {
            this.pagingAudioDisabled = pagingAudioDisabled;
        }
    }

    /** 左右手模式 */
    enum HandMode {
        /** 左手模式 */
        Left,
        /** 右手模式 */
        Right,
    }
}
