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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeConfigListView extends RecyclerView {

    public ThemeConfigListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        RecyclerView.Adapter<ThemeConfigView> adapter = new ThemeConfigViewAdapter(createData());
        setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        setLayoutManager(layoutManager);
    }

    private List<ThemeConfig> createData() {
        List<CharInput> samples = new ArrayList<>();

        InputWord[] words = new InputWord[] {
                new InputWord("kuai", "筷", "kuài"),
                new InputWord("zi", "字", "zì"),
                new InputWord("shu", "输", "shū"),
                new InputWord("ru", "入", "rù"),
                new InputWord("fa", "法", "fǎ"),
                };
        for (InputWord word : words) {
            CharInput input = CharInput.from(InputWordKey.create(word));
            input.setWord(word);

            samples.add(input);
        }

        List<ThemeConfig> data = new ArrayList<>();

        ThemeConfig theme = new ThemeConfig(Keyboard.ThemeType.follow_system);
        theme.setSamples(samples);
        data.add(theme);

        theme = new ThemeConfig(Keyboard.ThemeType.light);
        theme.setSamples(samples);
        data.add(theme);

        theme = new ThemeConfig(Keyboard.ThemeType.night);
        theme.setSamples(samples);
        data.add(theme);

        return data;
    }
}
