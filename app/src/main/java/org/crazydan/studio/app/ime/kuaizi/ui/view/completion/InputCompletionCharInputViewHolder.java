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

package org.crazydan.studio.app.ime.kuaizi.ui.view.completion;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.completion.InputCompletion;
import org.crazydan.studio.app.ime.kuaizi.ui.view.input.InputViewHolder;

/**
 * {@link InputCompletion.CharInputViewData} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-14
 */
public class InputCompletionCharInputViewHolder extends InputViewHolder {
    private final TextView wordSpellView;
    private final TextView wordView;
    private final TextView latinView;

    public InputCompletionCharInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.wordSpellView = itemView.findViewById(R.id.word_spell_view);
        this.wordView = itemView.findViewById(R.id.word_view);
        this.latinView = itemView.findViewById(R.id.latin_view);
    }

    public void bind(InputCompletion.CharInputViewData data) {
        boolean hasSpell = !CharUtils.isBlank(data.spell);

        whenViewReady(this.wordView, (view) -> {
            view.setText(hasSpell ? data.text : null);
            ViewUtils.visible(view, hasSpell);
        });
        whenViewReady(this.latinView, (view) -> {
            view.setText(!hasSpell ? data.text : null);
            ViewUtils.visible(view, !hasSpell);
        });

        whenViewReady(this.wordSpellView, (view) -> {
            view.setText(hasSpell ? data.spell : null);
            ViewUtils.visible(view, hasSpell);
        });
    }
}
