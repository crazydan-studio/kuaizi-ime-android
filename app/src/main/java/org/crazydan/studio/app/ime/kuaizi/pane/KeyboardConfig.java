/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane;

import android.content.Context;
import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * {@link Keyboard} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class KeyboardConfig {
    /** X 输入面板是否已启用 */
    public final boolean xInputPadEnabled;

    /** 通过 {@link InputConfig} 构造 {@link KeyboardConfig} */
    public static KeyboardConfig from(InputConfig inputConfig) {
        return new KeyboardConfig(inputConfig);
    }

    KeyboardConfig(InputConfig inputConfig) {
    }

    public static int getThemeResId(Context context, Keyboard.Theme theme) {
        int themeResId = R.style.Theme_Kuaizi_IME_Light;
        if (theme == null) {
            return themeResId;
        }

        switch (theme) {
            case night:
                themeResId = R.style.Theme_Kuaizi_IME_Night;
                break;
            case follow_system:
                int themeMode = context.getResources().getConfiguration().uiMode
                                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                switch (themeMode) {
                    case android.content.res.Configuration.UI_MODE_NIGHT_NO:
                        themeResId = R.style.Theme_Kuaizi_IME_Light;
                        break;
                    case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                        themeResId = R.style.Theme_Kuaizi_IME_Night;
                        break;
                }
                break;
        }
        return themeResId;
    }
}
