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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseStep {
    public final String content;
    public final Action action;
    public final List<ExerciseStep> subs = new ArrayList<>();

    public static ExerciseStep create(String content, Action action) {
        return new ExerciseStep(content, action);
    }

    private ExerciseStep(String content, Action action) {
        this.content = content;
        this.action = action;
    }

    /** 返回当前对象本身 */
    public ExerciseStep subStep(String content) {
        return subStep(content, null);
    }

    /** 返回当前对象本身 */
    public ExerciseStep subStep(String content, Action action) {
        ExerciseStep step = create(content, action);
        this.subs.add(step);

        return this;
    }

    public interface Action {
        void begin();

        boolean end();
    }
}
