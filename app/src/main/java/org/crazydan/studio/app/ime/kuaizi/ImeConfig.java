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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigChangeListener;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;

/**
 * IME {@link Config 配置}
 * <p/>
 * 通过 {@link #set} 直接更新的是运行时配置，其不会覆写系统配置，
 * 但在获取配置项的值时，以运行时的配置结果优先
 * <p/>
 * 若系统配置未被运行时配置覆盖，则可直接获取已更新的系统配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-16
 */
public class ImeConfig extends Config.Mutable {
    private ConfigChangeListener listener;

    private Runnable cleaner;

    public static ImeConfig create(Context context) {
        ImeConfig config = new ImeConfig();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        config.syncWith(preferences);

        return config;
    }

    ImeConfig() {
        // 其源配置为 系统配置
        super(new Mutable());
    }

    public void destroy() {
        this.cleaner.run();

        this.listener = null;
        this.cleaner = null;
    }

    // ======================= Start: 配置衍生 ======================

    /** 创建可变副本，该副本可对配置项进行覆盖更新，但其更新不影响源配置，且未被覆盖的配置项依然可取到源配置的最新数据 */
    public Mutable mutable() {
        return new Mutable(this);
    }

    /** 创建不可变副本，该副本始终可取到源配置的最新数据，但不可做配置更新 */
    public Immutable immutable() {
        return new Immutable(this);
    }

    // ======================= End: 配置衍生 ======================

    // ======================= Start: 消息处理 ======================

    public void setListener(ConfigChangeListener listener) {
        this.listener = listener;
    }

    /** 与 {@link SharedPreferences} 同步数据，并监听其变更 */
    private void syncWith(SharedPreferences preferences) {
        Map<String, ?> all = preferences.getAll();

        // 遍历所有枚举项以便于为各项赋默认值
        for (ConfigKey key : ConfigKey.values()) {
            Object value = all.get(key.name());
            value = key.parse(value);

            ((Mutable) this.source).set(key, value);
        }

        preferences.registerOnSharedPreferenceChangeListener(this::onSharedPreferenceChanged);

        this.cleaner = () -> {
            preferences.unregisterOnSharedPreferenceChangeListener(this::onSharedPreferenceChanged);
        };
    }

    /** 同步对系统配置的更新 */
    private void onSharedPreferenceChanged(SharedPreferences preferences, String keyName) {
        for (ConfigKey key : ConfigKey.values()) {
            if (!key.name().equals(keyName)) {
                continue;
            }

            // Note：暂时不对赋值为 null 或移除配置项的情况做处理
            Object oldValue = this.source.get(key);
            Object newValue = preferences.getAll().get(keyName);
            newValue = key.parse(newValue);

            if (((Mutable) this.source).set(key, newValue)) {
                this.listener.onChanged(key, oldValue, newValue);
            }
            break;
        }
    }

    // ======================= End: 消息处理 ======================
}
