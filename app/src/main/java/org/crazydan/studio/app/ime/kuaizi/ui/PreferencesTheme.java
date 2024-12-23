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

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.conf.Conf;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class PreferencesTheme extends FollowSystemThemeActivity {
    private ImeInputView imeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_preferences_theme_activity);

        this.imeView = findViewById(R.id.ime_view);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.settings, new SettingsFragment(this.imeView))
                                       .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        prepareImeInput();
    }

    private void prepareImeInput() {
        InputList inputList = this.imeView.getInputList();
        inputList.reset(false);

        InputWord[] words = new InputWord[] {
                new InputWord(100, "筷", "kuài"),
                new InputWord(101, "字", "zì"),
                new InputWord(102, "输", "shū"),
                new InputWord(103, "入", "rù"),
                new InputWord(104, "法", "fǎ"),
                };
        for (InputWord word : words) {
            CharInput input = CharInput.from(InputWordKey.create(word));
            input.setWord(word);

            inputList.selectLast();
            inputList.withPending(input);
            inputList.confirmPending();
        }

        this.imeView.startInput(Keyboard.Type.Pinyin, false);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final ImeInputView imeView;

        public SettingsFragment(ImeInputView imeView) {this.imeView = imeView;}

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences_theme, rootKey);

            SwitchPreferenceCompat xPad = findPreference(Conf.enable_x_input_pad.name());
            Preference latinUsePinyinKeys = findPreference(Conf.enable_latin_use_pinyin_keys_in_x_input_pad.name());
            Preference adaptGesture = findPreference(Conf.adapt_desktop_swipe_up_gesture.name());

            xPad.setOnPreferenceClickListener(pref -> {
                latinUsePinyinKeys.setEnabled(xPad.isChecked());
                adaptGesture.setEnabled(!xPad.isChecked());
                return true;
            });
            latinUsePinyinKeys.setEnabled(xPad.isChecked());
            adaptGesture.setEnabled(!xPad.isChecked());

            Keyboard.Subtype subtype = SystemUtils.getImeSubtype(getContext());
            PreferenceCategory xPadCategory = findPreference("preference_x_input_pad");
            xPadCategory.setVisible(subtype == Keyboard.Subtype.hans);

            // 显示配置后的拉丁文键盘布局
            latinUsePinyinKeys.setOnPreferenceClickListener(pref -> {
                this.imeView.startInput(Keyboard.Type.Latin, false);
                return true;
            });
        }
    }
}
