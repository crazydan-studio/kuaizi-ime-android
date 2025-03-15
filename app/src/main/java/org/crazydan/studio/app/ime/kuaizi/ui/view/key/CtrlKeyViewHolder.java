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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link CtrlKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public class CtrlKeyViewHolder extends KeyViewHolder<ImageView> {
    private final TextView fgTextView;

    public CtrlKeyViewHolder(@NonNull View itemView) {
        super(itemView);

        this.fgTextView = itemView.findViewById(R.id.fg_text);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        whenViewReady(this.fgView, (view) -> {
            boolean shown = key.icon != null;

            if (shown) {
                view.setImageResource(key.icon);
            }
            ViewUtils.visible(view, shown);
        });

        whenViewReady(this.fgTextView, (view) -> {
            boolean shown = key.icon == null && key.label != null;

            if (shown) {
                updateKeyTextView(key, view, key.label);
            }
            ViewUtils.visible(view, shown);
        });
    }
}
