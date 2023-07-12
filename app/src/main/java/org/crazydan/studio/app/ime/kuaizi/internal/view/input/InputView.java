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

package org.crazydan.studio.app.ime.kuaizi.internal.view.input;

import android.view.View;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.view.RecyclerViewHolder;
import org.crazydan.studio.app.ime.kuaizi.utils.ColorUtils;

/**
 * {@link Keyboard 键盘}{@link Input 输入}的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public abstract class InputView<I extends Input> extends RecyclerViewHolder {
    private I input;

    public InputView(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(I input, boolean selected) {
        this.input = input;

        int bgColor;
        if (selected) {
            bgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_selection_bg_color);
        } else {
            bgColor = ColorUtils.getByAttrId(getContext(), R.attr.input_bg_color);
        }
        this.itemView.setBackgroundColor(bgColor);
    }
}