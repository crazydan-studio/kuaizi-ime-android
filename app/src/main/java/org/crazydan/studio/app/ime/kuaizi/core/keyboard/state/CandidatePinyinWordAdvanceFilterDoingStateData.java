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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

/**
 * {@link State.Type#InputCandidate_AdvanceFilter_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-27
 */
public class CandidatePinyinWordAdvanceFilterDoingStateData extends PagingStateData<PinyinInputWord.Radical> {
    private final CharInput target;
    private final Map<PinyinInputWord.Spell, List<WordRadical>> spellAndRadicalsMap;

    private PinyinInputWord.Filter filter;

    public CandidatePinyinWordAdvanceFilterDoingStateData(
            CharInput target, List<InputWord> candidates, int pageSize
    ) {
        super(pageSize);

        this.target = target;

        this.spellAndRadicalsMap = initSpellAndRadicalsMap(candidates);
    }

    /** 获取可用的过滤部首列表 */
    @Override
    public List<PinyinInputWord.Radical> getPagingData() {
        return getRadicals();
    }

    public CharInput getTarget() {
        return this.target;
    }

    public PinyinInputWord.Filter getFilter() {
        return new PinyinInputWord.Filter(this.filter);
    }

    public void setFilter(PinyinInputWord.Filter filter) {
        this.filter = new PinyinInputWord.Filter(filter);
    }

    public List<PinyinInputWord.Spell> getSpells() {
        return this.spellAndRadicalsMap.keySet()
                                       .stream()
                                       .sorted(Comparator.comparing(s -> s.id))
                                       .collect(Collectors.toList());
    }

    public List<PinyinInputWord.Radical> getRadicals() {
        Map<PinyinInputWord.Radical, Integer> map = new HashMap<>();

        this.spellAndRadicalsMap.forEach((spell, radicals) -> {
            if (!this.filter.spells.isEmpty() && !this.filter.spells.contains(spell)) {
                return;
            }

            radicals.forEach((radical) -> {
                map.compute(radical.value, (k, v) -> (v == null ? 0 : v) + radical.weight);
            });
        });

        return map.keySet().stream().map((value) -> new WordRadical(value, map.get(value))).sorted((a, b) -> {
            int diff = b.weight - a.weight;
            if (diff == 0) {
                diff = a.value.strokeCount - b.value.strokeCount;
            }
            return diff;
        }).map((radical) -> radical.value).collect(Collectors.toList());
    }

    private Map<PinyinInputWord.Spell, List<WordRadical>> initSpellAndRadicalsMap(List<InputWord> candidates) {
        Map<PinyinInputWord.Spell, Map<PinyinInputWord.Radical, Integer>> map = new HashMap<>();

        candidates.stream()
                  .filter((word) -> word instanceof PinyinInputWord)
                  .map((word) -> (PinyinInputWord) word)
                  .forEach((word) -> {
                      PinyinInputWord.Spell spell = word.getSpell();
                      PinyinInputWord.Radical radical = word.getRadical();

                      map.computeIfAbsent(spell, (k) -> new HashMap<>())
                         .compute(radical, (k, v) -> (v == null ? 0 : v) + 1);
                  });

        Map<PinyinInputWord.Spell, List<WordRadical>> result = new HashMap<>();
        map.forEach((spell, radicalMap) -> {
            List<WordRadical> radicals = new ArrayList<>();

            radicalMap.forEach((radical, weight) -> {
                radicals.add(new WordRadical(radical, weight));
            });

            result.put(spell, radicals);
        });

        return result;
    }

    private static class WordRadical {
        private final PinyinInputWord.Radical value;
        private final int weight;

        private WordRadical(PinyinInputWord.Radical value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }
}
