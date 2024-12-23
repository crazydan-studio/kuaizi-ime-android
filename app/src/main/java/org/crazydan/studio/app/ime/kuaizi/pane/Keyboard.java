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
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.UserKeyMsg;

/**
 * 键盘，做不同的输入处理
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface Keyboard {

    /** 获取当前键盘类型 */
    Type getType();

    /** 当前键盘是否为主键盘，即，常驻性键盘，其余类型的键盘均为临时性切换，其在退出后均需要回到切换前所在的主键盘上 */
    default boolean isMaster() {return false;}

    /**
     * 更新配置
     * <p/>
     * Note: 必须在 {@link #start} 之前做配置初始更新
     *
     * @return 若存在更新，则返回 true，否则，返回 false
     */
    boolean updateConfig(KeyboardConfig config);

    /** 获取当前的按键布局 */
    KeyFactory getKeyFactory(InputList inputList);

    // ==========================================================

    /** 启动 */
    void start(KeyboardContext context);

    /** 重置 */
    void reset(KeyboardContext context);

    // ==========================================================

    /** 响应来自 {@link InputList} 的 {@link InputMsg} 消息 */
    void onMsg(KeyboardContext context, InputMsg msg);

    /** 响应 {@link UserKeyMsg} 消息 */
    void onMsg(KeyboardContext context, UserKeyMsg msg);

    // ==========================================================

    /** 键盘类型 */
    enum Type {
        /** 拼音键盘 */
        Pinyin,
        /** 拼音候选字键盘 */
        Pinyin_Candidates,

        /** 算术键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字，逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,
        /** 符号键盘：标点符号等 */
        Symbol,
        /** 表情键盘 */
        Emoji,
        /** 文本编辑键盘：提供复制、粘贴等文本操作 */
        Editor,

        // 临时控制键盘切换
        /** 由 {@link ImeSubtype} 确定 */
        By_ImeSubtype,
        /** 保持当前类型的键盘不变 */
        Keep_Current,
    }

    /** 键盘布局方向 */
    enum Orientation {
        /** 纵向 */
        portrait,
        /** 横向 */
        landscape,
        ;

        /** 确定键盘布局方向 */
        public static Orientation from(Context context) {
            if (context.getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                return landscape;
            } else {
                return portrait;
            }
        }
    }

    /** 左右手模式 */
    enum HandMode {
        /** 左手模式 */
        left,
        /** 右手模式 */
        right,
    }

    /** 键盘主题样式 */
    enum Theme {
        light(R.style.Theme_Kuaizi_IME_Light),
        night(R.style.Theme_Kuaizi_IME_Night),
        follow_system(-1),
        ;

        private final int resId;

        Theme(int resId) {
            this.resId = resId;
        }

        /** 获取主题资源 id，若其为 {@link #follow_system}，则通过 {@link #from} 确定最终主题，再获取其资源 id */
        public int getResId(Context context) {
            if (this == follow_system) {
                return from(context).resId;
            }
            return this.resId;
        }

        /** 根据系统主题获取键盘主题，若系统不支持暗黑设置，则返回主题 {@link #light} */
        public static Theme from(Context context) {
            int themeMode = context.getResources().getConfiguration().uiMode
                            & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

            switch (themeMode) {
                case android.content.res.Configuration.UI_MODE_NIGHT_NO:
                    return light;
                case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                    return night;
            }
            return light;
        }
    }
}
