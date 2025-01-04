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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-10-09
 */
public class UserInputData {
    public final List<List<PinyinWord>> phrases;
    public final List<InputWord> emojis;
    public final List<String> latins;

    public UserInputData(List<List<PinyinWord>> phrases, List<InputWord> emojis, List<String> latins) {
        this.phrases = phrases;
        this.emojis = emojis;
        this.latins = latins;
    }

    public boolean isEmpty() {
        return this.phrases.isEmpty() && this.emojis.isEmpty() && this.latins.isEmpty();
    }
}
