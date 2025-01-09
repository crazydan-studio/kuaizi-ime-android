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

import android.content.res.Configuration;
import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * 支持根据系统暗黑主题设置窗口主题
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public abstract class FollowSystemThemeActivity extends ActionBarSupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int themeResId = getDefaultTheme();
        setTheme(themeResId);

        super.onCreate(savedInstanceState);
    }

    protected int getDefaultTheme() {
        return chooseTheme(R.style.Theme_Kuaizi_App_Light, R.style.Theme_Kuaizi_App_Night);
    }

    protected int chooseTheme(int lightThemeResId, int nightThemeResId) {
        int themeMode = getApplicationContext().getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        int themeResId = lightThemeResId;
        if (themeMode == Configuration.UI_MODE_NIGHT_YES) {
            themeResId = nightThemeResId;
        }

        return themeResId;
    }
}
