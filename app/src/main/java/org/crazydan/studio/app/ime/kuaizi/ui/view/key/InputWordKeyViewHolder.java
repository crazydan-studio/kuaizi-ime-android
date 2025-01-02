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

package org.crazydan.studio.app.ime.kuaizi.ui.view.key;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
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
