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
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * 过滤 输入候选字 的{@link CtrlKey} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-27
 */
public class CtrlFilterInputWordKeyViewHolder extends KeyViewHolder<CtrlKey, View> {
    private final TextView fgTextView;
    private final TextView subTextView;

    public CtrlFilterInputWordKeyViewHolder(@NonNull View itemView) {
        super(itemView);

        this.fgTextView = itemView.findViewById(R.id.fg_text_view);
        this.subTextView = itemView.findViewById(R.id.sub_text_view);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        String[] splits = key.getLabel().split("/");

        whenViewReady(this.fgTextView, (view) -> {
            setTextColorByAttrId(view, key.getColor().fg);

            String text = splits[0];
            view.setText(text);
        });

        whenViewReady(this.subTextView, (view) -> {
            String subText = splits.length > 1 ? splits[1] : null;
            setTextColorByAttrId(view, subText != null ? key.getColor().fg : null);

            ViewUtils.visible(view, subText != null).setText(subText);
        });
    }
}
