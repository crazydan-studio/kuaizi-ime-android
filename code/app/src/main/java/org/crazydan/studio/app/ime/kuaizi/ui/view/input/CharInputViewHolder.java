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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputViewData;

/**
 * {@link CharInput} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class CharInputViewHolder extends InputViewHolder {
    private final TextView wordSpellView;
    private final TextView wordView;

    public CharInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.wordSpellView = itemView.findViewById(R.id.word_spell);
        this.wordView = itemView.findViewById(R.id.word);
    }

    @Override
    public void bind(InputViewData data) {
        super.bind(data);

        setSelectedBgColor(this.itemView, data.selected);

        bind(data.text, data.spell, data.selected);
    }

    private void bind(String text, String spell, boolean selected) {
        whenViewReady(this.wordView, (view) -> {
            setSelectedTextColor(view, selected);
            view.setText(text);
        });

        whenViewReady(this.wordSpellView, (view) -> {
            boolean shown = !CharUtils.isBlank(spell);
            if (shown) {
                setSelectedTextColor(view, selected);
                view.setText(spell);
            }
            ViewUtils.visible(view, shown);
        });
    }
}
