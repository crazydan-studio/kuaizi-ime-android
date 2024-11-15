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

package org.crazydan.studio.app.ime.kuaizi.ui.guide.view;

import android.view.View;
import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsg;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgData;
import org.crazydan.studio.app.ime.kuaizi.ui.guide.Exercise;
import org.crazydan.studio.app.ime.kuaizi.ui.view.ImeInputView;

/**
 * {@link Exercise 练习题}视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-19
 */
public class ExerciseIntroduceView extends ExerciseView {

    public ExerciseIntroduceView(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    public void withIme(ImeInputView ime) {
        // keep it empty
    }

    public void onMsg(Keyboard keyboard, InputMsg msg, InputMsgData msgData) {
        // keep it empty
    }

    @Override
    public void bind(Exercise exercise, int position) {
        super.bind(exercise);

        String title = createTitle(exercise, position);
        this.titleView.setText(title);

        updateSteps();
    }
}
