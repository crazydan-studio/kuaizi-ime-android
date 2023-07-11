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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CharKey;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public abstract class BaseInput implements Input {
    private final List<Key<?>> keys = new ArrayList<>();

    private InputWord word;
    private List<InputWord> candidates;

    @Override
    public List<Key<?>> getKeys() {
        return this.keys;
    }

    @Override
    public Key<?> getCurrentKey() {
        return this.keys.isEmpty() ? null : this.keys.get(this.keys.size() - 1);
    }

    @Override
    public void appendKey(Key<?> key) {
        this.keys.add(key);
    }

    @Override
    public List<String> getChars() {
        return this.keys.stream()
                        .filter(k -> k instanceof CharKey)
                        .map(k -> ((CharKey) k).getText())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    @Override
    public boolean hasWord() {
        return getWord() != null;
    }

    @Override
    public InputWord getWord() {
        return this.word;
    }

    public void word(InputWord candidate) {
        this.word = candidate;
    }

    @Override
    public List<InputWord> getWordCandidates() {
        return this.candidates == null ? new ArrayList<>() : this.candidates;
    }

    public void candidates(List<InputWord> candidates) {
        this.candidates = candidates;
    }

    /** 是否有多余 1 个的候选字 */
    public boolean hasExtraCandidates() {
        return getWordCandidates().size() > 1;
    }
}
