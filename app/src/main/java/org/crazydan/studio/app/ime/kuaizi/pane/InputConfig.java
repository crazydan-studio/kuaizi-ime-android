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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.content.SharedPreferences;
import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;

/**
 * 输入配置
 * <p/>
 * 包括临时性和持久性配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-05
 */
public class InputConfig implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Map<Key, Object> vars = new HashMap<>();

    private ChangeListener listener;

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }

    public boolean has(Key key) {
        return this.vars.containsKey(key);
    }

    public <T> T get(Key key) {
        return (T) this.vars.get(key);
    }

    public Boolean bool(Key key) {
        return get(key);
    }

    /**
     * @param value
     *         若为 null，则删除配置项 conf
     */
    public <T> boolean set(Key key, T value) {
        return set(key, value, false);
    }

    /**
     * @param value
     *         若为 null，则删除配置项 conf
     * @param ignoreNull
     *         是否忽略为 null 的值
     */
    public <T> boolean set(Key key, T value, boolean ignoreNull) {
        T oldValue = get(key);
        if (Objects.equals(oldValue, value) || (ignoreNull && value == null)) {
            return false;
        }

        if (value == null) {
            this.vars.remove(key);
        } else {
            this.vars.put(key, value);
        }
        return true;
    }

    // ======================= Start: 消息处理 ======================

    /** 与 {@link SharedPreferences} 同步数据，并监听其变更 */
    public void syncWith(SharedPreferences preferences) {
        Map<String, ?> all = preferences.getAll();

        // 遍历所有枚举项以便于为各项赋默认值
        for (Key key : Key.values()) {
            Object value = all.get(key.name());
            value = key.parse(value);

            set(key, value);
        }

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String keyName) {
        for (Key key : Key.values()) {
            if (!key.name().equals(keyName)) {
                continue;
            }

            // Note：暂时不对赋值为 null 或移除配置项的情况做处理
            Object oldValue = get(key);
            Object newValue = preferences.getAll().get(keyName);
            newValue = key.parse(newValue);

            if (set(key, newValue)) {
                this.listener.onChanged(key, oldValue, newValue);
            }
            break;
        }
    }

    // ======================= End: 消息处理 ======================

    /** 配置变更监听器 */
    public interface ChangeListener {
        /** 仅当新旧配置值不相等时才会触发 */
        void onChanged(Key key, Object oldValue, Object newValue);
    }

    /** 配置项 */
    public enum Key {
        // ====================== Start: 临时性配置 =====================
        /** IME 子类型 */
        ime_subtype(ImeSubtype.class, ImeSubtype.hans),
        /** 键盘布局方向 */
        orientation(Keyboard.Orientation.class, Keyboard.Orientation.portrait),
        /** 是否为单行输入 */
        single_line_input(Boolean.class, false),
        /** 是否重置输入，即，清空已输入内容 */
        reset_inputting(Boolean.class, false),
        /** 原键盘类型 */
        prev_keyboard_type(Keyboard.Type.class, null),
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
        ;

        public final Class<?> type;
        private final Object defaultValue;

        Key(Class<?> type, Object defaultValue) {
            this.type = type;
            this.defaultValue = defaultValue;
        }

        /** 解析数据 */
        private <T> T parse(Object value) {
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
}
