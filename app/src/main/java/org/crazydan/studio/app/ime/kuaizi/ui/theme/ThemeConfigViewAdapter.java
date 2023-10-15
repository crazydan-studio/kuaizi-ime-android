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

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.widget.recycler.RecyclerViewAdapter;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-15
 */
public class ThemeConfigViewAdapter extends RecyclerViewAdapter<ThemeConfigView> {
    private final List<ThemeConfig> data;

    public ThemeConfigViewAdapter(List<ThemeConfig> data) {
        this.data = data;
    }

    public void update() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.data.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeConfigView holder, int position) {
        ThemeConfig theme = this.data.get(position);

        holder.bind(theme);
    }

    @NonNull
    @Override
    public ThemeConfigView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ThemeConfigView(inflateItemView(parent, R.layout.app_theme_view));
    }
}
