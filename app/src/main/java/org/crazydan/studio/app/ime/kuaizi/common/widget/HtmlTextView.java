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
