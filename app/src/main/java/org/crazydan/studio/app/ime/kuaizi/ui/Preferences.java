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

import java.io.File;
import java.io.OutputStream;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.SystemUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.Alert;

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
        String fileName = "kuaizi_user_data_backup.db";
        // https://stackoverflow.com/questions/59103133/how-to-directly-download-a-file-to-download-directory-on-android-q-android-10#answer-64357198
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            try (OutputStream output = resolver.openOutputStream(uri)) {
                PinyinDict.instance().saveUserDB(context, output);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/" + fileName);

            try (OutputStream output = FileUtils.newOutput(dir)) {
                PinyinDict.instance().saveUserDB(context, output);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static void openFeedbackUrl(Activity context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pref_key = "user_feedback_with_extra_info_enabled";
        String extraInfoEnabledStr = preferences.getString(pref_key, null);
        boolean extraInfoEnabled = extraInfoEnabledStr != null && Boolean.parseBoolean(extraInfoEnabledStr);

        Point screenSize = ScreenUtils.getScreenSize();
        PackageInfo pkgInfo = SystemUtils.getPackageInfo(context);
        String clientInfo = String.format(Locale.getDefault(),
                                          "%s %s / Android %s (API %s) / %dx%d",
                                          Build.MANUFACTURER,
                                          Build.MODEL,
                                          Build.VERSION.RELEASE,
                                          Build.VERSION.SDK_INT,
                                          screenSize.x,
                                          screenSize.y);
        String appInfo = pkgInfo.packageName + ":" + pkgInfo.versionName;

        String feedbackUrl = extraInfoEnabled
                             ? Preferences.createFeedbackUrl(preferences, clientInfo, appInfo)
                             : Preferences.createFeedbackUrl(preferences, null, null);

        if (extraInfoEnabledStr != null) {
            SystemUtils.openLink(context, feedbackUrl);
            return;
        }

        Alert.with(context)
             .setView(R.layout.guide_alert_view)
             .setCancelable(true)
             .setTitle(R.string.title_tips)
             .setRawMessage(R.raw.text_about_suggestion, clientInfo, appInfo)
             .setNegativeButton(R.string.btn_feedback_open_link_without_extra_info, (dialog, which) -> {
                 savePreference(preferences, pref_key, Boolean.FALSE.toString());

                 SystemUtils.openLink(context, feedbackUrl);
             })
             .setPositiveButton(R.string.btn_feedback_open_link_with_extra_info, (dialog, which) -> {
                 savePreference(preferences, pref_key, Boolean.TRUE.toString());

                 SystemUtils.openLink(context, feedbackUrl);
             })
             .show();
    }

    private static String createFeedbackUrl(SharedPreferences preferences, String clientInfo, String appInfo) {
        String title = Uri.encode("[Android] ");
        String body = clientInfo != null && appInfo != null
                      //
                      ? Uri.encode(String.format("系统信息：%s\n" + "应用信息：%s\n" + "---\n\n", clientInfo, appInfo))
                      : "";

        // Note: 可用的 label 需提前建好
        return "https://github.com/crazydan-studio/kuaizi-ime/issues/new" //
               + "?labels=android,feedback&title=" + title + "&body=" + body;
    }

    private static void savePreference(SharedPreferences preferences, String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_preferences_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey);

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
    }
}
