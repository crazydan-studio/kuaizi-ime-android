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

package org.crazydan.studio.app.ime.kuaizi.conf;

import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;

/**
 * 枚举的配置项
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-05
 */
public enum Conf {
    /** IME 子类型 */
    ime_subtype(ImeSubtype.class, ImeSubtype.hans),
    //
    /** 键盘布局方向 */
    orientation(Keyboard.Orientation.class, Keyboard.Orientation.portrait),
    /** 是否为单行输入 */
    single_line_input(Boolean.class, false),
    //
    /** 主题样式 */
    theme(Keyboard.Theme.class, Keyboard.Theme.light),
    /** 左右手使用模式 */
    hand_mode(Keyboard.HandMode.class, Keyboard.HandMode.right),
    //
    disable_user_input_data(Boolean.class, false),
    disable_key_clicked_audio(Boolean.class, false),
    disable_key_animation(Boolean.class, false),
    disable_input_candidates_paging_audio(Boolean.class, false),
    disable_input_key_popup_tips(Boolean.class, false),
    disable_gesture_slipping_trail(Boolean.class, false),
    //
    enable_candidate_variant_first(Boolean.class, false),
    enable_x_input_pad(Boolean.class, false),
    enable_latin_use_pinyin_keys_in_x_input_pad(Boolean.class, false),
    //
    adapt_desktop_swipe_up_gesture(Boolean.class, false),
    ;

    public final Class<?> type;
    private final Object defaultValue;

    Conf(Class<?> type, Object defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public <T> T parse(Object value) {
        if (value != null) {
            if (value.getClass() == this.type) {
                return (T) value;
            } else if (this.type.isEnum()) {
                for (Object o : this.type.getEnumConstants()) {
                    if (o.toString().equals(value.toString())) {
                        return (T) o;
                    }
                }
            }
        }

        return (T) this.defaultValue;
    }
}
