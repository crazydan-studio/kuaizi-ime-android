/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.conf;

import org.crazydan.studio.app.ime.kuaizi.IMESubtype;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;

/** 配置项 */
public enum ConfigKey {
    // ====================== Start: 临时性配置 =====================
    /** IME 子类型 */
    ime_subtype(IMESubtype.class, IMESubtype.hans),
    /** 键盘布局方向 */
    orientation(Keyboard.Orientation.class, Keyboard.Orientation.portrait),
    /** 是否为单行输入 */
    single_line_input(Boolean.class, false),
    /** 原键盘类型 */
    prev_keyboard_type(Keyboard.Type.class, null),
    /** 是否禁用字典库 */
    disable_dict_db(Boolean.class, false),
    /** 是否禁用配置按钮 */
    disable_settings_btn(Boolean.class, false),
    /** 是否禁用 IME 切换按钮 */
    disable_switch_ime_btn(Boolean.class, false),
    /** 是否禁用键盘关闭按钮 */
    disable_close_keyboard_btn(Boolean.class, false),
    // ====================== End: 临时性配置 =====================

    /** 主题样式 */
    theme(Keyboard.Theme.class, Keyboard.Theme.follow_system),
    /** 左右手使用模式 */
    hand_mode(Keyboard.HandMode.class, Keyboard.HandMode.right),

    /** 禁用对用户输入的数据存储 */
    disable_user_input_data(Boolean.class, false),
    /** 禁用按键点击音效 */
    disable_key_clicked_audio(Boolean.class, false),
    /** 禁用按键动画 */
    disable_key_animation(Boolean.class, false),
    /** 禁用输入候选字翻页音效 */
    disable_input_candidates_paging_audio(Boolean.class, false),
    /** 禁用输入按键气泡提示 */
    disable_input_key_popup_tips(Boolean.class, false),
    /** 禁用滑屏轨迹 */
    disable_gesture_slipping_trail(Boolean.class, false),

    /** 启用候选字变体优先：主要针对拼音字的繁/简体 */
    enable_candidate_variant_first(Boolean.class, false),
    /** 启用 X 输入面板 */
    enable_x_input_pad(Boolean.class, false),
    /** 启用在 X 输入面板中让拉丁文输入共用拼音输入的按键布局 */
    enable_latin_use_pinyin_keys_in_x_input_pad(Boolean.class, false),

    /** 适配移动端的上滑手势 */
    adapt_desktop_swipe_up_gesture(Boolean.class, false),

    /** 是否禁用剪贴数据提示 */
    disable_input_clip_popup_tips(Boolean.class, false),
    /** 已使用的剪贴数据标识 */
    used_input_clip_code(String.class, null),
    ;

    public final Class<?> type;
    private final Object defaultValue;

    ConfigKey(Class<?> type, Object defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /** 解析数据 */
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
