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

package org.crazydan.studio.app.ime.kuaizi.internal.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 最佳候选字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-20
 */
public class BestCandidateWords {
    /** 列表元素为 link_word_with_pinyin 表的 id */
    public final List<String> words;
    /**
     * 匹配到的最佳短语列表，列表内为短语中的字倒序排列的数组，
     * 数组内为 link_word_with_pinyin 表的 id
     * <p/>
     * 如果前序输入中存在已确认的字，则匹配的短语列表的对应位置的 id
     * 也与已确认的字是相同的
     */
    public final List<String[]> phrases;

    public BestCandidateWords() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public BestCandidateWords(List<String> words, List<String[]> phrases) {
        this.words = words;
        this.phrases = phrases;
    }
}
