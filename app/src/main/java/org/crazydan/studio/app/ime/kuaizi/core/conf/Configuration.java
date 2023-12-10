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

package org.crazydan.studio.app.ime.kuaizi.core.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import android.content.SharedPreferences;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;

/**
 * 配置数据
 * <p/>
 * 包括临时性和持久性配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-05
 */
public class Configuration implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Listener listener;
    private final Map<Conf, Object> vars = new HashMap<>();

    public Configuration() {this(null);}

    public Configuration(Listener listener) {this.listener = listener;}

    public boolean has(Conf conf) {
        return this.vars.containsKey(conf);
    }

    public <T> T get(Conf conf) {
        return (T) this.vars.get(conf);
    }

    public Boolean bool(Conf conf) {
        return get(conf);
    }

    /**
     * @param value
     *         若为 null，则删除配置项 conf
     */
    public <T> boolean set(Conf conf, T value) {
        T oldValue = get(conf);
        if (Objects.equals(oldValue, value)) {
            return false;
        }

        if (value == null) {
            this.vars.remove(conf);
        } else {
            this.vars.put(conf, value);
        }
        return true;
    }

    public void merge(Configuration other) {
        other.vars.forEach(this::set);
    }

    /** 创建副本，副本仅记录发生了更新的配置项，未变更的直接从源配置中取值 */
    public Configuration copy() {
        return new Configuration.Copied(this);
    }

    public void bind(SharedPreferences preferences) {
        Map<String, ?> all = preferences.getAll();

        // 遍历所有枚举项以便于为各项赋默认值
        for (Conf conf : Conf.values()) {
            Object value = all.get(conf.name());
            value = conf.parse(value);

            set(conf, value);
        }

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        for (Conf conf : Conf.values()) {
            if (!conf.name().equals(key)) {
                continue;
            }

            // Note：暂时不对赋值为 null 或移除配置项的情况做处理
            Object oldValue = get(conf);
            Object newValue = preferences.getAll().get(key);
            newValue = conf.parse(newValue);

            if (set(conf, newValue)) {
                this.listener.onChanged(conf, oldValue, newValue);
            }
            break;
        }
    }

    // ==============================================================

    /** 仅汉字输入环境才支持将拉丁文键盘与拼音键盘的按键布局设置为相同的 */
    public boolean isLatinUsePinyinKeysInXInputPadEnabled() {
        return bool(Conf.enable_latin_use_pinyin_keys_in_x_input_pad) && get(Conf.subtype) == Keyboard.Subtype.hans;
    }

    public boolean isLeftHandMode() {
        return get(Conf.hand_mode) == Keyboard.HandMode.left;
    }

    public boolean isUserInputDataDisabled() {
        return bool(Conf.disable_user_input_data);
    }

    public boolean isCandidateVariantFirstEnabled() {
        return bool(Conf.enable_candidate_variant_first);
    }

    public boolean isXInputPadEnabled() {
        return bool(Conf.enable_x_input_pad);
    }

    // ==============================================================
    public interface Listener {
        /** 仅当新旧配置值不相等时才会触发 */
        void onChanged(Conf conf, Object oldValue, Object newValue);
    }

    private static class Copied extends Configuration {
        private final Configuration source;

        private Copied(Configuration source) {this.source = source;}

        @Override
        public <T> T get(Conf conf) {
            // 若副本内没有，则从源配置中查找
            if (!has(conf)) {
                return this.source.get(conf);
            } else {
                return super.get(conf);
            }
        }
    }
}
