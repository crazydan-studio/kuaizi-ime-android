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

package org.crazydan.studio.app.ime.kuaizi.common.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;

/**
 * 可显示 HTML 内容的 {@link android.widget.TextView TextView}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-27
 */
public class HtmlTextView extends androidx.appcompat.widget.AppCompatTextView {

    public HtmlTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // https://gist.github.com/carlol/ab791a5f21cf9e58028db2668619aabe
        setHtmlText(getText().toString());
    }

    public void setHtmlText(String text) {
        ViewUtils.setHtmlText(this, text);
    }
}
