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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.Service;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseMain;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.Alert;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;

/**
 * 使用指南
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class Guide extends FollowSystemThemeActivity {
    private static final String pref_key_confirmed_alpha_user_agreement = "confirmed_alpha_user_agreement";

    private SharedPreferences preferences;

    @Override
    protected boolean isActionBarEnabled() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_activity);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // https://stackoverflow.com/questions/18486130/detect-if-input-method-has-been-selected#answer-18487989
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(Intent.ACTION_INPUT_METHOD_CHANGED)) {
                    updateSwitcher();
                }
            }
        }, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateSwitcher();

        MaterialButton btnShowPreferences = findViewById(R.id.btn_guide_show_preferences);
        MaterialButton btnTryExercises = findViewById(R.id.btn_guide_try_exercises);

        btnShowPreferences.setOnClickListener(this::showPreferences);
        btnTryExercises.setOnClickListener(this::tryExercises);

        if (!isAlphaUserAgreementConfirmed()) {
            showAlphaAgreementConfirmWindow();
        }
    }

    private boolean isImeEnabled() {
        Context context = getApplicationContext();
        return SystemUtils.isEnabledIme(context, Service.class);
    }

    private boolean isImeDefault() {
        Context context = getApplicationContext();
        return SystemUtils.isDefaultIme(context, Service.class);
    }

    private void updateSwitcher() {
        SwitchCompat switcher = findViewById(R.id.switcher);
        switcher.setChecked(isImeEnabled() && isImeDefault());

        switcher.setOnClickListener(this::switchIme);
    }

    private void switchIme(View v) {
        // 消除点击造成的切换影响
        updateSwitcher();

        if (isImeEnabled()) {
            Context context = getApplicationContext();

            SystemUtils.switchIme(context);
        } else {
            String appName = getResources().getString(R.string.app_name);
            String appNameShown = getResources().getString(R.string.app_name_shown);

            Alert.with(this)
                 .setView(R.layout.guide_alert_view)
                 .setTitle(R.string.title_tips)
                 .setMessage(R.string.msg_ime_should_be_enabled_first, appName, appNameShown)
                 .setNegativeButton(R.string.btn_enable_later, (dialog, which) -> {})
                 .setPositiveButton(R.string.btn_enable_right_now, (dialog, which) -> showImeSettings())
                 .show();
        }
    }

    private void showImeSettings() {
        Context context = getApplicationContext();

        SystemUtils.showImeSettings(context);
    }

    private void showPreferences(View v) {
        Context context = getApplicationContext();

        SystemUtils.showAppPreferences(context);
    }

    private void tryExercises(View v) {
        Context context = getApplicationContext();

        Intent intent = new Intent();
        intent.setClass(context, ExerciseMain.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private void showAlphaAgreementConfirmWindow() {
        String appName = getResources().getString(R.string.app_name);

        Alert.with(this)
             .setView(R.layout.guide_alert_view)
             .setTitle(R.string.title_about_alpha_user_agreement)
             .setRawMessage(R.raw.text_about_alpha_user_agreement, appName)
             .setNegativeButton(R.string.btn_guide_reject_alpha_user_agreement, (dialog, which) -> {
                 // 关闭窗口
                 // https://gist.github.com/goliver79/8878498
                 finish();
             })
             .setPositiveButton(R.string.btn_guide_confirm_alpha_user_agreement, (dialog, which) -> {
                 confirmAlphaUserAgreement();
             })
             .show();
    }

    private boolean isAlphaUserAgreementConfirmed() {
        if (!SystemUtils.isAlphaVersion()) {
            return true;
        }
        return this.preferences.getBoolean(pref_key_confirmed_alpha_user_agreement, false);
    }

    private void confirmAlphaUserAgreement() {
        SharedPreferences.Editor editor = this.preferences.edit();

        editor.putBoolean(pref_key_confirmed_alpha_user_agreement, true);
        editor.apply();
    }
}
