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

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeConfigListView extends RecyclerView {

    public ThemeConfigListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // 滚动嵌套处理：
        // https://stackoverflow.com/questions/27083091/recyclerview-inside-scrollview-is-not-working#answer-37338715
        setNestedScrollingEnabled(true);

        RecyclerView.Adapter<ThemeConfigView> adapter = new ThemeConfigViewAdapter();
        setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        setLayoutManager(layoutManager);
    }

    public void updateData(List<ThemeConfig> data) {
        ((ThemeConfigViewAdapter) getAdapter()).updateData(data);
    }
}
