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
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link InputWordKey 候选字按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class InputWordKeyView extends KeyView<InputWordKey, View> {
    private final TextView spellView;
    private final TextView wordView;
    private final View traditionalMarkView;

    public InputWordKeyView(@NonNull View itemView) {
        super(itemView);

        this.spellView = this.fgView.findViewById(R.id.spell_view);
        this.wordView = this.fgView.findViewById(R.id.word_view);
        this.traditionalMarkView = itemView.findViewById(R.id.traditional_mark_view);
    }

    public void bind(InputWordKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        InputWord word = key.getWord();

        if (word instanceof PinyinWord && ((PinyinWord) word).isTraditional()) {
            ViewUtils.show(this.traditionalMarkView);
        } else {
            ViewUtils.hide(this.traditionalMarkView);
        }

        String value = word != null ? word.getValue() : "";
        String spell = word != null ? word.getSpell().value : null;
        this.wordView.setText(value);
        setTextColorByAttrId(this.wordView, key.getColor().fg);

        if (CharUtils.isBlank(spell)) {
            ViewUtils.hide(this.spellView);
        } else {
            ViewUtils.show(this.spellView).setText(spell);
            setTextColorByAttrId(this.spellView, key.getColor().fg);
        }
    }
}
