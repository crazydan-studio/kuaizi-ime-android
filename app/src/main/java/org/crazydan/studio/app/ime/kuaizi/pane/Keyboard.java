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

import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.InputMsgListener;
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

    KeyFactory getKeyFactory(InputList inputList);

    /** 启动 */
    void start(InputList inputList);

    /** 重置 */
    void reset();

    /** 销毁 */
    void destroy();

    /** 注册 {@link InputMsg} 消息监听 */
    void setListener(InputMsgListener listener);

    /** 响应来自 {@link InputList} 的 {@link InputMsg} 消息 */
    void onMsg(InputList inputList, InputMsg msg);

    /** 响应 {@link UserKeyMsg} 消息 */
    void onMsg(InputList inputList, UserKeyMsg msg);

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
        light(R.string.value_theme_light),
        night(R.string.value_theme_night),
        follow_system(R.string.value_theme_follow_system),
        ;

        private final int labelResId;

        Theme(int labelResId) {
            this.labelResId = labelResId;
        }

        public int getLabelResId() {
            return this.labelResId;
        }
    }
}
