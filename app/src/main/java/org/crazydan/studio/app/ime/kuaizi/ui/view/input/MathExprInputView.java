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

package org.crazydan.studio.app.ime.kuaizi.ui.view.input;

import android.view.View;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ViewUtils;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.input.InputViewData;
import org.crazydan.studio.app.ime.kuaizi.pane.input.MathExprInput;
import org.crazydan.studio.app.ime.kuaizi.ui.view.InputListViewReadonly;

/**
 * {@link MathExprInput} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-15
 */
public class MathExprInputView extends InputView<Input<?>> {
    private final InputListViewReadonly inputListView;
    private final View markerView;

    public MathExprInputView(@NonNull View itemView) {
        super(itemView);

        this.inputListView = itemView.findViewById(R.id.input_list);
        this.markerView = itemView.findViewById(R.id.marker);
    }

    public void bind(InputViewData data, boolean selected) {
        super.bind(data.input);

        addLeftSpaceMargin(this.itemView, data.gapSpaces);

        InputList inputList = ((MathExprInput) data.input).getInputList();
        this.inputListView.update(inputList.getInputFactory(), selected, false);

        setBackgroundColorByAttrId(this.markerView,
                                   selected
                                   ? R.attr.input_char_math_expr_border_highlight_color
                                   : R.attr.input_char_math_expr_border_color);
        ViewUtils.visible(this.markerView, !inputList.isEmpty());
    }
}
