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

package org.crazydan.studio.app.ime.kuaizi;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewConfiguration;
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
public class IMEConfig extends Config.Mutable implements SharedPreferences.OnSharedPreferenceChangeListener {
    private ConfigChangeListener listener;

    private Runnable cleaner;

    public static IMEConfig create(Context context) {
        IMEConfig config = new IMEConfig();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        config.syncWith(preferences);

        ViewConfiguration viewConfig = ViewConfiguration.get(context);
        config.set(ConfigKey.scaled_touch_slop, viewConfig.getScaledTouchSlop());

        return config;
    }

    IMEConfig() {
        // 其源配置为 系统配置
        super(new Mutable());
    }

    public void destroy() {
        this.cleaner.run();

        this.listener = null;
        this.cleaner = null;
    }

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

        // Note: 监听器不能为 lambda 函数，否则，会造成监听仅对第一次变更有效
        preferences.registerOnSharedPreferenceChangeListener(this);
        this.cleaner = () -> {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        };
    }

    /** 同步对系统配置的更新 */
    public void onSharedPreferenceChanged(SharedPreferences preferences, String keyName) {
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
