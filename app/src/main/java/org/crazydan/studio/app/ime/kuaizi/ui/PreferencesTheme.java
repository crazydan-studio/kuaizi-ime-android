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

package org.crazydan.studio.app.ime.kuaizi.ui;

import java.util.function.Consumer;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.ui.common.ImeIntegratedActivity;

/**
 * 主题配置窗口
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class PreferencesTheme extends ImeIntegratedActivity {

    public PreferencesTheme() {
        super(R.layout.app_preferences_theme_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            SettingsFragment settings = new SettingsFragment(this::startKeyboard);
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, settings).commit();
        }

        // 禁用字典：仅做视图展示，无需实际操作
        this.imeConfig.set(ConfigKey.disable_dict_db, true);

        // Note: 在 SettingsFragment 中将自动触发键盘类型切换，故而，无需提前切换键盘
        prepareInputs(new String[] { "kuai", "筷", "kuài" },
                      new String[] { "zi", "字", "zì" },
                      new String[] { "shu", "输", "shū" },
                      new String[] { "ru", "入", "rù" },
                      new String[] { "fa", "法", "fǎ" });
    }

    /** Note: Fragment 必须为 public 的静态类 */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final Consumer<Keyboard.Type> keyboard;

        public SettingsFragment(Consumer<Keyboard.Type> keyboard) {
            this.keyboard = keyboard;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences_theme, rootKey);

            SwitchPreferenceCompat xPad = findPreference(ConfigKey.enable_x_input_pad.name());
            SwitchPreferenceCompat adaptGesture = findPreference(ConfigKey.adapt_desktop_swipe_up_gesture.name());

            SwitchPreferenceCompat latinUsePinyinKeys
                    = findPreference(ConfigKey.enable_latin_use_pinyin_keys_in_x_input_pad.name());

            assert xPad != null;
            assert adaptGesture != null;
            assert latinUsePinyinKeys != null;

            Preference.OnPreferenceClickListener listener = (pref) -> {
                adaptGesture.setEnabled(!xPad.isChecked());
                latinUsePinyinKeys.setEnabled(xPad.isChecked());

                if (latinUsePinyinKeys.isEnabled() && latinUsePinyinKeys.isChecked()) {
                    this.keyboard.accept(Keyboard.Type.Latin);
                } else {
                    this.keyboard.accept(Keyboard.Type.Pinyin);
                }
                return true;
            };
            // 触发第一次视图更新
            listener.onPreferenceClick(null);

            xPad.setOnPreferenceClickListener(listener);
            // 点击后，显示应用配置后的拉丁文键盘布局
            latinUsePinyinKeys.setOnPreferenceClickListener(pref -> {
                this.keyboard.accept(Keyboard.Type.Latin);
                return true;
            });
        }
    }
}
