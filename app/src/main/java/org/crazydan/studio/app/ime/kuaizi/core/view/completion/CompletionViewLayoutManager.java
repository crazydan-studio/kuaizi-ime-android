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

package org.crazydan.studio.app.ime.kuaizi.core.view.completion;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.core.input.CompletionInput;

/**
 * {@link CompletionInput} 的{@link RecyclerView}布局器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-12
 */
public class CompletionViewLayoutManager extends LinearLayoutManager {
    private boolean scrollEnabled = true;

    public CompletionViewLayoutManager(Context context) {
        super(context, RecyclerView.HORIZONTAL, false);
    }

    @Override
    public boolean canScrollHorizontally() {
        return this.scrollEnabled;
    }

    public void enableScroll(boolean enabled) {
        this.scrollEnabled = enabled;
    }
}
