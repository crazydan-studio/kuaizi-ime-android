/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.dict;

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
