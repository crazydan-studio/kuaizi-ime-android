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

package org.crazydan.studio.app.ime.kuaizi.keyboard;

import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.SubKeyboard;

/**
 * 键盘
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class Keyboard {

    /** 子键盘类型 */
    public enum Subtype {
        /** 汉语拼音键盘 */
        Pinyin,
        /** 算术键盘：支持数学计算 */
        Math,
        /** 拉丁文键盘：含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件 */
        Latin,
        /** 数字键盘：纯数字和 +、-、#、* 等符号 */
        Number,

        // 临时控制键盘切换
        /** 由 {@link org.crazydan.studio.app.ime.kuaizi.ImeSubtype ImeSubtype} 确定类型 */
        By_ImeSubtype,
        /** 保持当前键盘类型 */
        Keep_Current,
    }

    /** 键盘布局方向 */
    public enum Orientation {
        /** 纵向 */
        portrait,
        /** 横向 */
        landscape,
    }

    /** 左右手模式 */
    public enum HandMode {
        /** 左手模式 */
        left,
        /** 右手模式 */
        right,
    }

    /** 键盘主题样式 */
    public enum Theme {
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

    private SubKeyboard sub;
    private InputList inputList;

    /** 使用指定类型的子键盘 */
    public void use(Subtype subtype) {
        //
    }
}
