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

package org.crazydan.studio.app.ime.kuaizi.ui.view.clip;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.widget.ShadowDrawable;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputClip;

/**
 * {@link InputClip} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class InputClipViewHolder extends RecyclerViewHolder {
    private final TextView textView;

    public InputClipViewHolder(@NonNull View itemView) {
        super(itemView);

        this.textView = itemView.findViewById(R.id.clip_text);

        String shadow = getStringByAttrId(R.attr.input_completions_shadow_style);
        ShadowDrawable bg = new ShadowDrawable(this.itemView.getBackground(), shadow);
        this.itemView.setBackground(bg);
    }

    public void bind(InputClip data) {
        whenViewReady(this.textView, (view) -> {
            int resId = R.string.title_clip_past_content;
            switch (data.type) {
                case url: {
                    resId = R.string.title_clip_past_url;
                    break;
                }
                case captcha: {
                    resId = R.string.title_clip_past_captcha;
                    break;
                }
                case phone: {
                    resId = R.string.title_clip_past_phone;
                    break;
                }
                case email: {
                    resId = R.string.title_clip_past_email;
                    break;
                }
            }
            this.textView.setText(resId);
        });
    }
}
