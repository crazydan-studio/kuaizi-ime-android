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

package org.crazydan.studio.app.ime.kuaizi.internal.input;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.data.PinyinCharTree;

/**
 * 字符{@link Input 输入}
 * <p/>
 * 任意可见字符的单次输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class CharInput extends BaseInput {
    private PinyinCharTree.Word word;
    private List<PinyinCharTree.Word> candidates;

    public PinyinCharTree.Word word() {
        return this.word;
    }

    public void word(PinyinCharTree.Word word) {
        this.word = word;
    }

    public List<PinyinCharTree.Word> candidates() {
        return this.candidates;
    }

    public void candidates(List<PinyinCharTree.Word> candidates) {
        this.candidates = candidates;
    }
}
