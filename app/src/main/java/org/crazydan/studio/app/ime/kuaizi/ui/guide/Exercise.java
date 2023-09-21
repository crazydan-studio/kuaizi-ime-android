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
public class Exercise {
    public final Mode mode;
    public final String title;
    public final List<ExerciseStep> steps = new ArrayList<>();

    public static Exercise free(String title) {
        return new Exercise(Mode.free, title);
    }

    public static Exercise normal(String title) {
        return new Exercise(Mode.normal, title);
    }

    private Exercise(Mode mode, String title) {
        this.mode = mode;
        this.title = title;
    }

    public ExerciseStep addStep(String content) {
        return addStep(content, null);
    }

    public ExerciseStep addStep(String content, ExerciseStep.Action action) {
        return addStep(ExerciseStep.create(content, action));
    }

    public ExerciseStep addStep(ExerciseStep step) {
        this.steps.add(step);
        return step;
    }

    public enum Mode {
        free,
        normal,
    }
}
