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

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-25
 */
public class ApiUtils {

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
}
