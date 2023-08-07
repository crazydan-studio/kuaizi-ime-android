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

package org.crazydan.studio.app.ime.kuaizi.ui.about;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.ui.HtmlSupportActivity;

/**
 * 关于输入法
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public class AboutApp extends HtmlSupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_app_activity);

        String appVersion = getVersion();
        String appName = getResources().getString(R.string.app_name);

        setText(R.id.app_name, R.string.app_name_with_version, appVersion);
        setHtmlText(R.id.about_app, R.string.text_about_app, appName);
    }

    private String getVersion() {
        // https://developer.android.com/studio/publish/versioning
        // https://stackoverflow.com/questions/4616095/how-can-you-get-the-build-version-number-of-your-android-application#answer-6593822
        try {
            PackageInfo pkgInfo = getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);

            return pkgInfo.versionName;
        } catch (Exception ignore) {
        }
        return null;
    }
}
