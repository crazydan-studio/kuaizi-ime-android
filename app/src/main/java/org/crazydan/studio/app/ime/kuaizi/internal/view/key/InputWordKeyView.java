/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal.view.key;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link InputWordKey 候选字按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-09
 */
public class InputWordKeyView extends KeyView<InputWordKey, View> {
    private final TextView notationView;
    private final TextView wordView;
    private final View traditionalMarkView;

    public InputWordKeyView(@NonNull View itemView) {
        super(itemView);

        this.notationView = this.fgView.findViewById(R.id.notation_view);
        this.wordView = this.fgView.findViewById(R.id.word_view);
        this.traditionalMarkView = itemView.findViewById(R.id.traditional_mark_view);
    }

    public void bind(InputWordKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        InputWord inputWord = key.getWord();

        if (inputWord != null) {
            this.wordView.setText(inputWord.getValue());
            this.notationView.setText(inputWord.getNotation());
        } else {
            this.wordView.setText("");
            this.notationView.setText("");
        }

        if (inputWord != null && inputWord.isTraditional()) {
            ViewUtils.show(this.traditionalMarkView);
        } else {
            ViewUtils.hide(this.traditionalMarkView);
        }

        setTextColorByAttrId(this.wordView, key.getFgColorAttrId());
        setTextColorByAttrId(this.notationView, key.getFgColorAttrId());
    }
}
