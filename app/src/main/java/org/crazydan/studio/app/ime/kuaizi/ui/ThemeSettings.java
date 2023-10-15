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

package org.crazydan.studio.app.ime.kuaizi.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.ui.theme.ThemeConfig;
import org.crazydan.studio.app.ime.kuaizi.ui.theme.ThemeConfigListView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeSettings extends FollowSystemThemeActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences preferences;

    private ThemeConfigListView themeConfigListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_theme_settings_activity);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.preferences.registerOnSharedPreferenceChangeListener(this);

        this.themeConfigListView = findViewById(R.id.theme_config_list_view);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateData();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        // Note：当前视图以外的配置项不会触发该监听，
        // 故而，无需对 key 做检查，触发该监听的，一定是对视图内的配置项的变更
        updateData();
    }

    private void updateData() {
        List<ThemeConfig> data = createData();
        this.themeConfigListView.updateData(data);
    }

    private List<ThemeConfig> createData() {
        boolean desktopSwipeUpGestureAdapted = Keyboard.Config.isDesktopSwipeUpGestureAdapted(this.preferences);
        Keyboard.HandMode handMode = Keyboard.Config.getHandMode(this.preferences);
        Keyboard.ThemeType theme = Keyboard.Config.getTheme(this.preferences);

        List<CharInput> samples = new ArrayList<>();

        InputWord[] words = new InputWord[] {
                new InputWord("kuai", "筷", "kuài"),
                new InputWord("zi", "字", "zì"),
                new InputWord("shu", "输", "shū"),
                new InputWord("ru", "入", "rù"),
                new InputWord("fa", "法", "fǎ"),
                };
        for (InputWord word : words) {
            CharInput input = CharInput.from(InputWordKey.create(word));
            input.setWord(word);

            samples.add(input);
        }

        List<ThemeConfig> data = new ArrayList<>();
        for (Keyboard.ThemeType themeType : new Keyboard.ThemeType[] {
                Keyboard.ThemeType.follow_system, Keyboard.ThemeType.light, Keyboard.ThemeType.night,
                }) {
            ThemeConfig config = new ThemeConfig(themeType);

            config.setSelected(themeType == theme);
            config.setSamples(samples);
            config.setDesktopSwipeUpGestureAdapted(desktopSwipeUpGestureAdapted);
            config.setHandMode(handMode);

            data.add(config);
        }

        return data;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_theme_settings, rootKey);
        }
    }
}
