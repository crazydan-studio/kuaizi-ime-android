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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard.state;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.State;

/**
 * {@link State.Type#InputCandidate_Advance_Filter_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-27
 */
public class PinyinCandidateAdvanceFilterStateData extends PinyinCandidateFilterStateData<PinyinWord.Radical> {
    private final Map<PinyinWord.Spell, List<WordRadical>> spellAndRadicalsMap;

    public PinyinCandidateAdvanceFilterStateData(
            CharInput input, List<InputWord> candidates, int pageSize
    ) {
        super(input, pageSize);

        this.spellAndRadicalsMap = initSpellAndRadicalsMap(candidates);
    }

    /** 获取可用的过滤部首列表 */
    @Override
    public List<PinyinWord.Radical> getPagingData() {
        return getRadicals();
    }

    public List<PinyinWord.Spell> getSpells() {
        return this.spellAndRadicalsMap.keySet()
                                       .stream()
                                       .sorted(Comparator.comparing(s -> s.id))
                                       .collect(Collectors.toList());
    }

    public List<PinyinWord.Radical> getRadicals() {
        Map<PinyinWord.Radical, Integer> map = new HashMap<>();

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

    private Map<PinyinWord.Spell, List<WordRadical>> initSpellAndRadicalsMap(List<InputWord> candidates) {
        Map<PinyinWord.Spell, Map<PinyinWord.Radical, Integer>> map = new HashMap<>();

        candidates.stream()
                  .filter((word) -> word instanceof PinyinWord)
                  .map((word) -> (PinyinWord) word)
                  .forEach((word) -> {
                      PinyinWord.Spell spell = word.getSpell();
                      PinyinWord.Radical radical = word.getRadical();

                      map.computeIfAbsent(spell, (k) -> new HashMap<>())
                         .compute(radical, (k, v) -> (v == null ? 0 : v) + 1);
                  });

        Map<PinyinWord.Spell, List<WordRadical>> result = new HashMap<>();
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
        private final PinyinWord.Radical value;
        private final int weight;

        private WordRadical(PinyinWord.Radical value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }
}
