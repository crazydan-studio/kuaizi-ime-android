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

import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ScreenUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link CharKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class CharKeyViewHolder extends KeyViewHolder<TextView> {

    public CharKeyViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(Key key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        whenViewReady(this.fgView, (view) -> {
            setTextColorByAttrId(view, key.getColor().fg);

            int textDimen;
            if (key.isSymbol()) {
                textDimen = R.dimen.char_symbol_key_text_size;
            } else {
                switch (key.getLabel().length()) {
                    case 6:
                        textDimen = R.dimen.char_key_text_size_4d;
                        break;
                    case 5:
                    case 4:
                        textDimen = R.dimen.char_key_text_size_3d;
                        break;
                    case 3:
                        textDimen = R.dimen.char_key_text_size_2d;
                        break;
                    case 2:
                        textDimen = R.dimen.char_key_text_size_1d;
                        break;
                    default:
                        textDimen = R.dimen.char_key_text_size;
                }
            }

            float textSize = ScreenUtils.pxFromDimension(getContext(), textDimen);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            String label = key.getLabel();
            view.setText(label);
        });
    }
}
