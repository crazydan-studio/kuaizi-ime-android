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

package org.crazydan.studio.app.ime.kuaizi.common.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import org.crazydan.studio.app.ime.kuaizi.BuildConfig;
import org.crazydan.studio.app.ime.kuaizi.IMESubtype;
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

    public static IMESubtype getImeSubtype(Context context) {
        InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        InputMethodSubtype subtype = manager.getCurrentInputMethodSubtype();
        return parseImeSubtype(subtype);
    }

    public static IMESubtype parseImeSubtype(InputMethodSubtype subtype) {
        if (subtype != null //
            && ("en_US".equals(subtype.getLocale()) //
                || "en_US".equals(subtype.getLanguageTag()))) {
            return IMESubtype.latin;
        } else {
            return IMESubtype.hans;
        }
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
        showActivity(context, Preferences.class);
    }

    /** 显示指定的 activity */
    public static void showActivity(Context context, Class<?> activityCls) {
        Intent intent;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
//            // Settings.ACTION_LOCALE_SETTINGS: 打开语言设置
//            // Settings.ACTION_INPUT_METHOD_SETTINGS: 打开输入法设置
//            intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
//        } else {
        // https://stackoverflow.com/questions/32822101/how-can-i-programmatically-open-the-permission-screen-for-a-specific-app-on-andr#answer-43707264
        intent = new Intent(context, activityCls);
//        }

        // If set then opens Settings Screen(Activity) as new activity.
        // Otherwise, it will be opened in currently running activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);
    }

    /** 使用默认浏览器打开指定链接地址 */
    public static void openLink(Context context, String url) {
        // https://stackoverflow.com/questions/5026349/how-to-open-a-website-when-a-button-is-clicked-in-android-application#answer-16566120
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);

        // Note：在有多个浏览器时，该设置会要求选择某个浏览器，而不会使用默认浏览器
        //intent.addCategory(Intent.CATEGORY_BROWSABLE);

        intent.setData(Uri.parse(url));

        context.startActivity(intent);
    }

    public static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getAppVersion(Context context) {
        // https://developer.android.com/studio/publish/versioning
        // https://stackoverflow.com/questions/4616095/how-can-you-get-the-build-version-number-of-your-android-application#answer-6593822
        return getPackageInfo(context).versionName;
    }
}
