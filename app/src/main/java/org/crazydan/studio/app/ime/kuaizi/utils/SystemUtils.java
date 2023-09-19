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

package org.crazydan.studio.app.ime.kuaizi.utils;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import org.crazydan.studio.app.ime.kuaizi.ui.Preferences;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-25
 */
public class SystemUtils {

    /** 获取设备支持的 Unicode 版本 */
    public static String supportedUnicodeVersion() {
        String version = "6.0";

        int sdk = android.os.Build.VERSION.SDK_INT;
        // https://developer.android.com/guide/topics/resources/internationalization
        switch (sdk) {
            case 18:
            case 19:
            case 20:
                version = "6.2";
                break;
            case 21:
            case 22:
                version = "6.3";
                break;
            case 23:
                version = "7.0";
                break;
            case 24:
            case 25:
                version = "8.0";
                break;
            case 26:
            case 27:
                version = "9.0";
                break;
            case 28:
                version = "10.0";
                break;
            case 29:
                version = "11.0";
                break;
            case 30:
            case 31:
                version = "13.0";
                break;
            default:
                if (sdk > 31) {
                    version = "14.0";
                }
                break;
        }

        return version;
    }

    /** 检查输入法是否为系统默认输入法 */
    public static boolean isDefaultIme(Context context, String imeId) {
        // https://stackoverflow.com/questions/2744729/how-to-determine-the-current-ime-in-android#answer-4256571
        String defaultImeId = Settings.Secure.getString(context.getContentResolver(),
                                                        Settings.Secure.DEFAULT_INPUT_METHOD);

        return defaultImeId.equals(imeId);
    }

    /** 检查输入法是否已启用 */
    public static boolean isEnabledIme(Context context, String imeId) {
        InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        for (InputMethodInfo info : manager.getEnabledInputMethodList()) {
            if (info.getId().equals(imeId)) {
                return true;
            }
        }
        return false;
    }

    /** 切换输入法 */
    public static void switchIme(Context context) {
        // https://stackoverflow.com/questions/16684482/android-switch-to-a-different-ime-programmatically#answer-16684491
        InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showInputMethodPicker();
        }
    }

    /** 显示输入法的配置页面 */
    public static void showImePreferences(Context context) {
        Intent intent;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//            // Settings.ACTION_LOCALE_SETTINGS: 打开语言设置
//            // Settings.ACTION_INPUT_METHOD_SETTINGS: 打开输入法设置
//            intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
//        } else {
        // https://stackoverflow.com/questions/32822101/how-can-i-programmatically-open-the-permission-screen-for-a-specific-app-on-andr#answer-43707264
        intent = new Intent(context, Preferences.class);
//        }

        // If set then opens Settings Screen(Activity) as new activity.
        // Otherwise, it will be opened in currently running activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);
    }
}
