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
    private final TextView spellView;
    private final TextView wordView;

    public CharInputViewHolder(@NonNull View itemView) {
        super(itemView);

        this.spellView = itemView.findViewById(R.id.spell_view);
        this.wordView = itemView.findViewById(R.id.word_view);
    }

    public void bind(InputViewData data, boolean selected) {
        addLeftSpaceMargin(this.itemView, data.gapSpaces);
        setSelectedBgColor(this.itemView, selected);

        bind(data.text, data.spell, selected);
    }

    public void bind(String text, String spell, boolean selected) {
        whenViewReady(this.wordView, (view) -> {
            setSelectedTextColor(view, selected);
            view.setText(text);
        });

        whenViewReady(this.spellView, (view) -> {
            boolean shown = !CharUtils.isBlank(spell);
            if (shown) {
                setSelectedTextColor(view, selected);
                view.setText(spell);
            }
            ViewUtils.visible(view, shown);
        });
    }
}
