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

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.utils.ScreenUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link CharKey 字符按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class CharKeyView extends KeyView<CharKey, TextView> {

    public CharKeyView(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(CharKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        String label = key.getLabel();
        this.fgView.setText(label);

        float textSize = ScreenUtils.pxFromDimension(getContext(),
                                                     key.isSymbol()
                                                     ? R.dimen.char_symbol_key_text_size
                                                     : key.getText().length() == 4
                                                       ? R.dimen.char_key_text_size_3d
                                                       : key.getText().length() == 3
                                                         ? R.dimen.char_key_text_size_2d
                                                         : key.getText().length() == 2
                                                           ? R.dimen.char_key_text_size_1d
                                                           : R.dimen.char_key_text_size);
        this.fgView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        setTextColorByAttrId(this.fgView, key.getFgColorAttrId());
    }
}
