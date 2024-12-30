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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link CtrlKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public class CtrlKeyViewHolder extends KeyViewHolder<ImageView> {
    private final TextView fgTextView;

    public CtrlKeyViewHolder(@NonNull View itemView) {
        super(itemView);

        this.fgTextView = itemView.findViewById(R.id.fg_text_view);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        whenViewReady(this.fgView, (view) -> {
            boolean shown = key.icon != null;
            if (shown) {
                view.setImageResource(key.icon);
            }
            ViewUtils.visible(view, shown);
        });

        whenViewReady(this.fgTextView, (view) -> {
            boolean shown = key.icon == null && key.label != null;
            if (shown) {
                setTextColorByAttrId(view, key.color.fg);
                view.setText(key.label);
            }
            ViewUtils.visible(view, shown);
        });
    }
}
