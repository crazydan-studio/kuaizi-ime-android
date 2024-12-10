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
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.hexworks.mixite.core.api.HexagonOrientation;

/**
 * {@link Keyboard 键盘}切换 输入拼写 的{@link CtrlKey 控制按键}视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-11
 */
public class CtrlToggleInputSpellKeyView extends KeyView<CtrlKey, View> {
    private final TextView sourceView;
    private final TextView targetView;

    public CtrlToggleInputSpellKeyView(@NonNull View itemView) {
        super(itemView);

        this.sourceView = itemView.findViewById(R.id.source_view);
        this.targetView = itemView.findViewById(R.id.target_view);
    }

    public void bind(CtrlKey key, HexagonOrientation orientation) {
        super.bind(key, orientation);

        String[] splits = key.getLabel().split(",");
        this.sourceView.setText(splits[0]);
        this.targetView.setText(splits[1]);
    }
}
