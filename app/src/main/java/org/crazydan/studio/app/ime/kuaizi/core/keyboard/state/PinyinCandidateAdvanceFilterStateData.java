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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.State;

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
        return this.spellAndRadicalsMap.keySet().stream().sorted(this::compareSpell).collect(Collectors.toList());
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

        candidates.stream().filter((w) -> w instanceof PinyinWord).map((w) -> (PinyinWord) w).forEach((w) -> {
            PinyinWord.Spell spell = w.spell;
            PinyinWord.Radical radical = w.radical;

            map.computeIfAbsent(spell, (k) -> new HashMap<>()).compute(radical, (k, v) -> (v == null ? 0 : v) + 1);
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
