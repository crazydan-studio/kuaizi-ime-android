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

import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

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

    protected void setHtmlText(int viewResId, int htmlTextResId, Object... args) {
        // https://developer.android.com/guide/topics/resources/string-resource#StylingWithHTML
        String text = getResources().getString(htmlTextResId, args);
        Spanned viewText = Html.fromHtml(text, FROM_HTML_MODE_LEGACY);

        TextView view = findViewById(viewResId);
        view.setText(viewText);
    }
}
