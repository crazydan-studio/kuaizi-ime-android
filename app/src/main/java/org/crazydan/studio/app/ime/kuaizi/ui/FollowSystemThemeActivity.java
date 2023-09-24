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
