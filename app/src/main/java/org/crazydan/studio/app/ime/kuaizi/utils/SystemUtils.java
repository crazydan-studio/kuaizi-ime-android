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
import org.crazydan.studio.app.ime.kuaizi.BuildConfig;
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

    /** 当前应用是否为 alpha 版本 */
    public static boolean isAlphaVersion() {
        return "alpha".equals(BuildConfig.BUILD_TYPE);
    }

    /** 检查输入法是否为系统默认输入法 */
    public static boolean isDefaultIme(Context context, Class<?> imeServiceCls) {
        // Note：当应用的 id 与服务的包名相同时，输入法的 id 将采用简称模式，否则，为全路径模式
        String imeId = String.format("%s/.%s", context.getPackageName(), imeServiceCls.getSimpleName());
        String imeIdFull = String.format("%s/%s", context.getPackageName(), imeServiceCls.getName());

        // https://stackoverflow.com/questions/2744729/how-to-determine-the-current-ime-in-android#answer-4256571
        String defaultImeId = Settings.Secure.getString(context.getContentResolver(),
                                                        Settings.Secure.DEFAULT_INPUT_METHOD);

        return defaultImeId.equals(imeId) || defaultImeId.equals(imeIdFull);
    }

    /** 检查输入法是否已启用 */
    public static boolean isEnabledIme(Context context, Class<?> imeServiceCls) {
        String pkgName = context.getPackageName();
        InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        for (InputMethodInfo info : manager.getEnabledInputMethodList()) {
            String serviceName = info.getServiceName();
            if (info.getPackageName().equals(pkgName) //
                && (serviceName.equals(imeServiceCls.getSimpleName()) //
                    || serviceName.equals(imeServiceCls.getName()))) {
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

    /** 显示输入法的系统配置页面 */
    public static void showImeSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    /** 显示应用的配置页面 */
    public static void showAppPreferences(Context context) {
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
