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
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.InputViewData;

/**
 * {@link CharInput} 视图的 {@link RecyclerView.ViewHolder}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-07
 */
public class CharInputViewHolder extends InputViewHolder {

    public CharInputViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(InputViewData data, boolean selected) {
        CharInput input = (CharInput) data.input;
        CharInput pending = data.pending;

        addLeftSpaceMargin(this.itemView, data.gapSpaces);
        setSelectedBgColor(this.itemView, selected);

        showWord(data.option, Input.isEmpty(pending) ? input : pending, selected, false);
    }

    public void bind(CharInput input) {
        addLeftSpaceMargin(this.itemView, 0);
        setSelectedBgColor(this.itemView, false);

        showWord(null, input, false, true);
    }
}
