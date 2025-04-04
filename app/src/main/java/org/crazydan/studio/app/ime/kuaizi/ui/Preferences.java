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

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import org.crazydan.studio.app.ime.kuaizi.BuildConfig;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDict;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.PreferencesUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.DialogAlert;
import org.crazydan.studio.app.ime.kuaizi.ui.common.FollowSystemThemeActivity;

/**
 * 输入法配置界面
 * <p/>
 * 在视图内通过 {@link androidx.preference.Preference}
 * 组件读写应用的 {@link SharedPreferences} 配置数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-06
 */
public class Preferences extends FollowSystemThemeActivity {

    public static void backupUserData(Activity context) {
        String filename = "Kuaizi_IME_User_Dict.db.bak";

        FileUtils.saveToDownload(context, filename, (output) -> IMEditorDict.instance().saveUserDB(context, output));
    }

    public static void openFeedbackUrl(Activity context) {
        String pref_key = "user_feedback_with_extra_info_enabled";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        PreferencesUtils.update(preferences, (editor) -> editor.remove(pref_key));

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        PackageInfo pkgInfo = SystemUtils.getPackageInfo(context);
        String clientInfo = String.format(Locale.getDefault(),
                                          "%s %s / Android %s (API %s) / %dx%d (DPI %d)",
                                          Build.MANUFACTURER,
                                          Build.MODEL,
                                          Build.VERSION.RELEASE,
                                          Build.VERSION.SDK_INT,
                                          metrics.widthPixels,
                                          metrics.heightPixels,
                                          metrics.densityDpi);
        String appInfo = pkgInfo.packageName + ":" + pkgInfo.versionName;

        DialogAlert.with(context)
                   .setView(R.layout.guide_alert_view)
                   .setCancelable(true)
                   .setTitle(R.string.title_tips)
                   .setRawMessage(R.raw.text_about_suggestion, clientInfo, appInfo)
                   .setNegativeButton(R.string.btn_feedback_open_link_without_extra_info, (dialog, which) -> {
                       String feedbackUrl = Preferences.createFeedbackUrl(null, null);

                       SystemUtils.openLink(context, feedbackUrl);
                   })
                   .setPositiveButton(R.string.btn_feedback_open_link_with_extra_info, (dialog, which) -> {
                       String feedbackUrl = Preferences.createFeedbackUrl(clientInfo, appInfo);

                       SystemUtils.openLink(context, feedbackUrl);
                   })
                   .show();
    }

    private static String createFeedbackUrl(String clientInfo, String appInfo) {
        String title = Uri.encode("[Android] ");
        String body = clientInfo != null && appInfo != null
                      //
                      ? Uri.encode(String.format("系统信息：%s\n" + "应用信息：%s\n" + "---\n\n", clientInfo, appInfo))
                      : "";

        // Note: 可用的 label 需提前建好
        return "https://github.com/crazydan-studio/kuaizi-ime/issues/new" //
               + "?labels=android,feedback&title=" + title + "&body=" + body;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_preferences_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .replace(R.id.btn_open_settings, new SettingsFragment())
                                       .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey);

            PreferenceScreen rootPref = getPreferenceManager().getPreferenceScreen();
            updateIntentActionPlaceholderDeeply(rootPref);

            if (!SystemUtils.isAlphaVersion()) {
                // https://stackoverflow.com/questions/2240326/remove-hide-a-preference-from-the-screen#answer-45274037
                PreferenceCategory category = findPreference("preference_about");
                PreferenceScreen alpha = findPreference("about_alpha_user_agreement");

                if (category != null && alpha != null) {
                    category.removePreference(alpha);
                }
            }

            Preference feedback = findPreference("about_user_feedback");
            if (feedback != null) {
                feedback.setOnPreferenceClickListener(preference -> {
                    openFeedbackUrl(getActivity());
                    return true;
                });
            }

            Preference backup = findPreference("user_data_backup");
            if (backup != null) {
                backup.setOnPreferenceClickListener(preference -> {
                    backupUserData(getActivity());
                    return true;
                });
            }
        }

        /** 更新 Intent action 名字中的占位符 */
        private void updateIntentActionPlaceholderDeeply(PreferenceGroup preference) {
            int total = preference.getPreferenceCount();
            for (int i = 0; i < total; i++) {
                Preference pref = preference.getPreference(i);
                if (!(pref instanceof PreferenceGroup)) {
                    continue;
                }

                Intent intent = pref.getIntent();
                if (intent != null && intent.getAction() != null //
                    && intent.getAction().contains("${applicationId}")) {
                    String action = intent.getAction().replace("${applicationId}", BuildConfig.APPLICATION_ID);
                    intent.setAction(action);
                }

                updateIntentActionPlaceholderDeeply((PreferenceGroup) pref);
            }
        }
    }
}
