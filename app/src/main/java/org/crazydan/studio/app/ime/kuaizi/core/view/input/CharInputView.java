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

package org.crazydan.studio.app.ime.kuaizi.core.view.input;

import android.view.View;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;

/**
 * {@link CharInput} 的视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class CharInputView extends InputView<CharInput> {

    public CharInputView(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(
            Input.Option option, CharInput data, CharInput pending, boolean selected, int gapSpaceCount
    ) {
        bind(option, data, pending, selected, false, gapSpaceCount);
    }

    public void bind(
            Input.Option option, CharInput data, CharInput pending, boolean selected, boolean hideWordSpell,
            int gapSpaceCount
    ) {
        super.bind(data);
        addLeftSpaceMargin(this.itemView, gapSpaceCount);

        showWord(option, Input.isEmpty(pending) ? data : pending, selected, hideWordSpell);
        setSelectedBgColor(this.itemView, selected);
    }
}
