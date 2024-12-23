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
import org.crazydan.studio.app.ime.kuaizi.pane.key.SymbolKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link SymbolKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-29
 */
public class SymbolKeyViewHolder extends KeyViewHolder<SymbolKey, TextView> {

    public SymbolKeyViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void bind(SymbolKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        whenViewReady(this.fgView, (view) -> {
            setTextColorByAttrId(view, key.getColor().fg);

            int textSizeDimen = R.dimen.char_symbol_key_text_size;
            float textSize = ScreenUtils.pxFromDimension(getContext(), textSizeDimen);
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            String label = key.getLabel();
            view.setText(label);
        });
    }
}
