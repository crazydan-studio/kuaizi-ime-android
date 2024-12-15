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
    private final ChangeListener listener;
    private final Map<Key, Object> vars = new HashMap<>();

    public InputConfig() {
        this(null);
    }

    public InputConfig(ChangeListener listener) {
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

    public void merge(InputConfig other) {
        other.vars.forEach(this::set);
    }

    /** 创建副本，副本仅记录发生了更新的配置项，未变更的直接从源配置中取值 */
    public InputConfig copy() {
        return new InputConfig.Copied(this);
    }

    // ======================= Start: 消息处理 ======================

    /** 与 {@link SharedPreferences} 绑定，以便于监听系统配置的变更 */
    public void bind(SharedPreferences preferences) {
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

    // ========================== Start: 辅助方法 ========================

    /** 仅汉字输入环境才支持将拉丁文键盘与拼音键盘的按键布局设置为相同的 */
    public boolean isLatinUsePinyinKeysInXInputPadEnabled() {
        return bool(Key.enable_latin_use_pinyin_keys_in_x_input_pad) && get(Key.ime_subtype) == ImeSubtype.hans;
    }

    public boolean isLeftHandMode() {
        return get(Key.hand_mode) == Keyboard.HandMode.left;
    }

    public boolean isUserInputDataDisabled() {
        return bool(Key.disable_user_input_data);
    }

    public boolean isXInputPadEnabled() {
        return bool(Key.enable_x_input_pad);
    }

    // ========================== End: 辅助方法 ========================

    private static class Copied extends InputConfig {
        private final InputConfig source;

        private Copied(InputConfig source) {this.source = source;}

        @Override
        public <T> T get(Key key) {
            // 若副本内没有，则从源配置中查找
            if (!has(key)) {
                return this.source.get(key);
            } else {
                return super.get(key);
            }
        }
    }

    /** 配置变更监听器 */
    public interface ChangeListener {
        /** 仅当新旧配置值不相等时才会触发 */
        void onChanged(Key key, Object oldValue, Object newValue);
    }

    /** 配置项 */
    public enum Key {
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
