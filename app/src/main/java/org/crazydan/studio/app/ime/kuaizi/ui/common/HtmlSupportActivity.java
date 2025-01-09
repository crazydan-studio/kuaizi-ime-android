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

package org.crazydan.studio.app.ime.kuaizi.ui.common;

import android.content.pm.PackageInfo;
import android.widget.ImageView;
import android.widget.TextView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

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
