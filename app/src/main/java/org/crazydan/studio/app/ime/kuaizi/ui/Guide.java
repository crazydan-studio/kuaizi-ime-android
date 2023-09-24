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
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.Service;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.ExerciseMain;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;

/**
 * 使用指南
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class Guide extends FollowSystemThemeActivity {

    @Override
    protected boolean isActionBarEnabled() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_activity);

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

        MaterialButton btnEnableIme = findViewById(R.id.btn_guide_enable_ime);
        MaterialButton btnTryExercises = findViewById(R.id.btn_guide_try_exercises);

        if (isImeEnabled()) {
            btnEnableIme.setText(R.string.btn_guide_disable_ime);
        } else {
            btnEnableIme.setText(R.string.btn_guide_enable_ime);
        }

        btnEnableIme.setOnClickListener(this::showImeSettings);
        btnTryExercises.setOnClickListener(this::tryExercises);

        tryExercises();
    }

    private String getImeId() {
        // 输入法 Id 组成：<application package name>/<service android:name>
        return String.format("%s/.%s", getApplicationContext().getPackageName(), Service.class.getSimpleName());
    }

    private boolean isImeEnabled() {
        String imeId = getImeId();
        Context context = getApplicationContext();

        return SystemUtils.isEnabledIme(context, imeId);
    }

    private boolean isImeDefault() {
        String imeId = getImeId();
        Context context = getApplicationContext();

        return SystemUtils.isDefaultIme(context, imeId);
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
            String msgText = getResources().getString(R.string.msg_ime_should_be_enabled_first, appName).trim();

            // Note: AlertDialog 的 context 必须为 activity，不能是应用的 context
            // https://stackoverflow.com/questions/27087983/unable-to-add-window-token-null-is-not-valid-is-your-activity-running#answer-50716727
            Context context = this; //new ContextThemeWrapper(this, themeResId);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

            builder.setTitle(R.string.title_tips)
                   .setMessage(msgText)
                   .setCancelable(false)
                   .setNegativeButton(R.string.btn_enable_later, (dialog, which) -> {})
                   .setPositiveButton(R.string.btn_enable_right_now, (dialog, which) -> showImeSettings())
                   .create()
                   .show();
        }
    }

    private void showImeSettings() {
        showImeSettings(null);
    }

    private void showImeSettings(View v) {
        Context context = getApplicationContext();

        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        // If set then opens Settings Screen(Activity) as new activity.
        // Otherwise, it will be opened in currently running activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private void tryExercises() {
        tryExercises(null);
    }

    private void tryExercises(View v) {
        Context context = getApplicationContext();

        Intent intent = new Intent();
        intent.setClass(context, ExerciseMain.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }
}
