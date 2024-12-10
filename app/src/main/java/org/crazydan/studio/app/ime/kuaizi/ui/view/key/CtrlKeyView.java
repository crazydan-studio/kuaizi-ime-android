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
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}{@link CtrlKey 控制按键}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public class CtrlKeyView extends KeyView<CtrlKey, ImageView> {
    private final TextView fgTextView;

    public CtrlKeyView(@NonNull View itemView) {
        super(itemView);

        this.fgTextView = itemView.findViewById(R.id.fg_text_view);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        if (key.getIconResId() != null) {
            ViewUtils.hide(this.fgTextView);
            ViewUtils.show(this.fgView).setImageResource(key.getIconResId());
        } else if (key.getLabel() != null) {
            ViewUtils.hide(this.fgView);
            ViewUtils.show(this.fgTextView).setText(key.getLabel());

            setTextColorByAttrId(this.fgTextView, key.getColor().fg);
        } else {
            // 存在复用的情况，故，需在其他情况对其进行重置
            ViewUtils.hide(this.fgTextView);
            ViewUtils.hide(this.fgView);
        }
    }
}
