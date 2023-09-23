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

import android.content.pm.PackageInfo;
import android.widget.ImageView;
import android.widget.TextView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public abstract class HtmlSupportActivity extends FollowSystemThemeActivity {

    protected void setText(int viewResId, int textResId, Object... args) {
        // https://developer.android.com/guide/topics/resources/string-resource#formatting-strings
        String viewText = getResources().getString(textResId, args);

        TextView view = findViewById(viewResId);
        view.setText(viewText);
    }

    protected void setHtmlText(int viewResId, int htmlRawResId, Object... args) {
        String text = ResourceUtils.readString(getApplicationContext(), htmlRawResId, args);
        TextView view = findViewById(viewResId);

        ViewUtils.setHtmlText(view, text);
    }

    protected void setIcon(int viewResId, int iconResId) {
        ImageView view = findViewById(viewResId);
        view.setImageResource(iconResId);
    }

    protected String getAppName() {
        return getResources().getString(R.string.app_name);
    }

    protected String getAppVersion() {
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
