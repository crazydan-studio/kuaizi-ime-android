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

package org.crazydan.studio.app.ime.kuaizi.ui.theme;

import android.view.View;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewHolder;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeConfigView extends RecyclerViewHolder<ThemeConfig> {
    private final ImeInputView imeView;

    public ThemeConfigView(@NonNull View itemView) {
        super(itemView);

        this.imeView = itemView.findViewById(R.id.ime_view);
    }

    @Override
    public void bind(ThemeConfig data) {
        super.bind(data);

        InputList inputList = this.imeView.getInputList();
        inputList.reset(false);
        data.getSamples().forEach((input) -> {
            inputList.selectLast();
            inputList.withPending(input);
            inputList.confirmPending();
        });

        Keyboard.Config initConfig = new Keyboard.Config(Keyboard.Type.Pinyin);
        this.imeView.startInput(initConfig, false);

        Keyboard.Config newConfig = new Keyboard.Config(initConfig.getType(), initConfig);
        newConfig.setTheme(data.getType());
        this.imeView.updateKeyboardConfig(newConfig);
    }
}
