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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import java.util.Date;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.common.widget.recycler.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;

/**
 * {@link InputFavorite} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-10
 */
public class InputFavoriteViewHolder extends RecyclerViewHolder {
    private final CheckBox checkboxView;
    private final TextView textView;

    private final TextView textTypeView;
    private final TextView createdAtView;
    private final TextView usedCountView;

    public InputFavoriteViewHolder(@NonNull View itemView) {
        super(itemView);

        this.checkboxView = itemView.findViewById(R.id.checkbox);
        this.textView = itemView.findViewById(R.id.text);

        this.textTypeView = itemView.findViewById(R.id.text_type);
        this.createdAtView = itemView.findViewById(R.id.created_at);
        this.usedCountView = itemView.findViewById(R.id.used_count);
    }

    public void bind(InputFavorite data, boolean selected, Runnable onCheck) {
        whenViewReady(this.checkboxView, (v) -> {
            v.setChecked(selected);
            v.setOnClickListener((vv) -> onCheck.run());
        });

        whenViewReady(this.textView, (v) -> {
            v.setText(data.text);
        });

        whenViewReady(this.textTypeView, (v) -> {
            String type = getContext().getString(data.type.labelResId);
            String text = getContext().getString(R.string.label_text_type, type);

            v.setText(text);
        });
        whenViewReady(this.createdAtView, (v) -> {
            String date = format(data.createdAt, false);
            ViewUtils.setText(v, R.string.label_created_at, date);
        });
        whenViewReady(this.usedCountView, (v) -> {
            String date = format(data.usedAt, true);
            ViewUtils.setText(v, R.string.label_used_count, data.usedCount + (data.usedCount > 0 ? " @ " + date : ""));
        });
    }

    private String format(Date date, boolean withTime) {
        if (date == null) {
            return null;
        }

        int flags = FORMAT_SHOW_YEAR | FORMAT_SHOW_DATE | FORMAT_NUMERIC_DATE;
        return DateUtils.formatDateTime(getContext(), date.getTime(), withTime ? flags | FORMAT_SHOW_TIME : flags);
    }
}
