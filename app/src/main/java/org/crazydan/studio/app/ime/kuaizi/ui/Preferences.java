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

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.SystemUtils;

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
    private static final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_preferences_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
    }

    public static void openFeedbackUrl(Context context) {
        String url = Preferences.createFeedbackUrl(context);

        SystemUtils.openLink(context, url);
    }

    private static String createFeedbackUrl(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pref_key = "user_feedback_info";
        String user_info = preferences.getString(pref_key, null);

        String openid = "";
        String nickname = "";
        String avatar = "";
        String user_signature = "";
        if (user_info == null) {
            int millis = (int) (System.currentTimeMillis() % 10000);
            int uid = millis + random.nextInt(10000) * random.nextInt(1000) + random.nextInt(100) * random.nextInt(10);

            openid = UUID.randomUUID().toString();
            nickname = "筷友" + String.format(Locale.getDefault(), "%06d", uid).substring(0, 6);
            avatar = "https://api.multiavatar.com/kuaizi_" + uid + ".png";
            user_signature = CharUtils.md5(openid + nickname + avatar + "bqMO5230");

            user_info = String.format("%s,%s,%s,%s", openid, nickname, avatar, user_signature);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(pref_key, user_info);
            editor.apply();
        } else {
            String[] splits = user_info.split(",");
            openid = splits[0];
            nickname = splits[1];
            avatar = splits[2];
            user_signature = splits[3];
        }

        Point screenSize = ScreenUtils.getScreenSize();
        PackageInfo pkgInfo = SystemUtils.getPackageInfo(context);
        String clientInfo = Build.MANUFACTURER
                            + " "
                            + Build.MODEL
                            + " / Android "
                            + Build.VERSION.RELEASE
                            + " (API "
                            + Build.VERSION.SDK_INT
                            + ") / "
                            + screenSize.x
                            + "x"
                            + screenSize.y;
        String customInfo = pkgInfo.packageName + ":" + pkgInfo.versionName;

        String url = "https://txc.qq.com/products/613302";

        return String.format("%s?openid=%s&nickname=%s&avatar=%s&user_signature=%s&clientInfo=%s&customInfo=%s",
                             url,
                             Uri.encode(openid),
                             Uri.encode(nickname),
                             Uri.encode(avatar),
                             Uri.encode(user_signature),
                             Uri.encode(clientInfo),
                             Uri.encode(customInfo));
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.app_preferences, rootKey);

            if (!SystemUtils.isAlphaVersion()) {
                // https://stackoverflow.com/questions/2240326/remove-hide-a-preference-from-the-screen#answer-45274037
                PreferenceCategory category = findPreference("category_about");
                PreferenceScreen about = findPreference("about_alpha_user_agreement");

                category.removePreference(about);
            }

            Preference feedback = findPreference("about_user_feedback");
            feedback.setOnPreferenceClickListener(preference -> {
                openFeedbackUrl(getContext());
                return true;
            });
        }
    }
}
