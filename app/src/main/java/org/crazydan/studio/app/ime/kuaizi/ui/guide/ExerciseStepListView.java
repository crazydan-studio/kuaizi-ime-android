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

package org.crazydan.studio.app.ime.kuaizi.ui.guide;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseStepListViewAdapter;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.view.ExerciseStepListViewLayoutManager;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStepListView extends RecyclerView {
    public final ExerciseStepListViewAdapter adapter;

    public ExerciseStepListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.adapter = new ExerciseStepListViewAdapter();
        setAdapter(this.adapter);

        ExerciseStepListViewLayoutManager layoutManager = new ExerciseStepListViewLayoutManager(context);
        setLayoutManager(layoutManager);
    }
}
