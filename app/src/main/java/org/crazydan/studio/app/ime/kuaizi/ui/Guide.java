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
import org.crazydan.studio.app.ime.kuaizi.BuildConfig;
import org.crazydan.studio.app.ime.kuaizi.IMEService;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.PreferencesUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.Alert;
import org.crazydan.studio.app.ime.kuaizi.ui.about.AboutDonate;
import org.crazydan.studio.app.ime.kuaizi.ui.common.FollowSystemThemeActivity;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseGuide;

/**
 * 使用指南
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class Guide extends FollowSystemThemeActivity {
    private static final String pref_key_confirmed_alpha_user_agreement = "confirmed_alpha_user_agreement";
    private static final String pref_key_confirmed_new_features_version = "confirmed_new_features_version";

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
        MaterialButton btnShowDonate = findViewById(R.id.btn_guide_show_donate);
        MaterialButton btnShowFeedback = findViewById(R.id.btn_guide_feedback);

        btnShowPreferences.setOnClickListener(this::showPreferences);
        btnTryExercises.setOnClickListener(this::tryExercises);
        btnShowDonate.setOnClickListener(this::showDonate);
        btnShowFeedback.setOnClickListener(this::showFeedback);

        if (!isAlphaUserAgreementConfirmed()) {
            showAlphaUserAgreementConfirmWindow();
        } else if (!isNewFeatureConfirmed()) {
            showNewFeatures();
        }

        //tryExercises(null);
    }

    private boolean isImeEnabled() {
        Context context = getApplicationContext();
        return SystemUtils.isEnabledIme(context, IMEService.class);
    }

    private boolean isImeDefault() {
        Context context = getApplicationContext();
        return SystemUtils.isDefaultIme(context, IMEService.class);
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

    private void showDonate(View v) {
        Context context = getApplicationContext();

        SystemUtils.showActivity(context, AboutDonate.class);
    }

    private void showFeedback(View v) {
        Preferences.openFeedbackUrl(this);
    }

    private void tryExercises(View v) {
        Context context = getApplicationContext();

        Intent intent = new Intent();
        intent.setClass(context, ExerciseGuide.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    // ====================================================================
    private void showAlphaUserAgreementConfirmWindow() {
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
                 showNewFeatures();
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
        PreferencesUtils.update(this.preferences, (editor) -> {
            editor.putBoolean(pref_key_confirmed_alpha_user_agreement, true);
        });
    }

    // ====================================================================
    private void showNewFeatures() {
        String appName = getResources().getString(R.string.app_name);

        Alert.with(this)
             .setView(R.layout.guide_alert_view)
             .setTitle(R.string.title_about_new_features)
             .setRawMessage(R.raw.text_about_new_features_v2,
                            appName,
                            getResources().getString(R.string.btn_guide_try_exercises),
                            getResources().getString(R.string.btn_guide_show_preferences),
                            getResources().getString(R.string.label_config_theme),
                            getResources().getString(R.string.label_enable_x_input_pad))
             .setPositiveButton(R.string.btn_guide_new_features_confirm, (dialog, which) -> {
                 confirmNewFeatures();
             })
             .show();
    }

    private boolean isNewFeatureConfirmed() {
        return BuildConfig.VERSION_CODE > 200
               || this.preferences.getInt(pref_key_confirmed_new_features_version, 0) >= 200;
    }

    private void confirmNewFeatures() {
        PreferencesUtils.update(this.preferences, (editor) -> {
            editor.putInt(pref_key_confirmed_new_features_version, BuildConfig.VERSION_CODE);
        });
    }
}
