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

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-31
 */
public class PinyinDictHelper {

    /** 获取拼音字母组合的 id 列表 */
    public static List<Integer> getPinyinCharsIdList(PinyinDict dict, String... pinyinCharsArray) {
        return getPinyinCharsIdList(dict, List.of(pinyinCharsArray));
    }

    /** 获取拼音字母组合的 id 列表 */
    public static List<Integer> getPinyinCharsIdList(PinyinDict dict, List<String> pinyinCharsList) {
        return pinyinCharsList.stream()
                              .map(chars -> dict.getPinyinCharsTree().getCharsId(chars))
                              .collect(Collectors.toList());
    }
}
