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

import android.os.Bundle;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.ui.HtmlSupportActivity;

/**
 * 关于开发团队
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-07
 */
public class AboutTeam extends HtmlSupportActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_team_activity);

        String appName = getAppName();

        setHtmlText(R.id.about_team, R.string.text_about_team, appName);
    }
}
