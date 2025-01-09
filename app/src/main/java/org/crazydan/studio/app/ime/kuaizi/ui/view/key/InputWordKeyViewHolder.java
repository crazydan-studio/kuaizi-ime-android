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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.InputWordKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link InputWordKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class InputWordKeyViewHolder extends KeyViewHolder<View> {
    private final TextView spellView;
    private final TextView wordView;
    private final View traditionalMarkView;

    public InputWordKeyViewHolder(@NonNull View itemView) {
        super(itemView);

        this.spellView = this.fgView.findViewById(R.id.spell_view);
        this.wordView = this.fgView.findViewById(R.id.word_view);
        this.traditionalMarkView = itemView.findViewById(R.id.traditional_mark_view);
    }

    public void bind(InputWordKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        InputWord word = key.word;

        whenViewReady(this.traditionalMarkView, (view) -> {
            boolean shown = word instanceof PinyinWord && ((PinyinWord) word).traditional;

            ViewUtils.visible(view, shown);
        });

        String value = word != null ? word.value : "";
        whenViewReady(this.wordView, (view) -> {
            updateKeyTextView(key, view, value);
        });

        String spell = word instanceof PinyinWord ? ((PinyinWord) word).spell.value : null;
        whenViewReady(this.spellView, (view) -> {
            boolean shown = !CharUtils.isBlank(spell);

            if (shown) {
                updateKeyTextView(key, view, spell);
            }
            ViewUtils.visible(view, shown);
        });
    }
}
