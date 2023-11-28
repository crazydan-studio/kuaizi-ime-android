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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#InputCandidate_AdvanceFilter_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-27
 */
public class InputCandidateAdvanceFilterDoingStateData extends PagingStateData<PinyinInputWord.Radical> {
    private final CharInput target;
    private final Map<PinyinInputWord.Spell, Set<PinyinInputWord.Radical>> spellAndRadicalsMap = new HashMap<>();

    public final List<PinyinInputWord.Spell> selectedSpells = new ArrayList<>();
    public final List<PinyinInputWord.Radical> selectedRadicals = new ArrayList<>();

    public InputCandidateAdvanceFilterDoingStateData(CharInput target, Collection<InputWord> inputWords, int pageSize) {
        super(pageSize);

        this.target = target;

        inputWords.forEach((word) -> {
            if (!(word instanceof PinyinInputWord)) {
                return;
            }

            PinyinInputWord.Spell spell = ((PinyinInputWord) word).getSpell();
            PinyinInputWord.Radical radical = ((PinyinInputWord) word).getRadical();

            this.spellAndRadicalsMap.computeIfAbsent(spell, (k) -> new HashSet<>()).add(radical);
        });
    }

    /** 获取可用的过滤部首列表 */
    @Override
    public List<PinyinInputWord.Radical> getPagingData() {
        return getRadicals();
    }

    public CharInput getTarget() {
        return this.target;
    }

    public List<PinyinInputWord.Spell> getSpells() {
        return this.spellAndRadicalsMap.keySet()
                                       .stream()
                                       .sorted(Comparator.comparing(s -> s.id))
                                       .collect(Collectors.toList());
    }

    public List<PinyinInputWord.Radical> getRadicals() {
        Set<PinyinInputWord.Radical> radicals = new HashSet<>();

        this.spellAndRadicalsMap.forEach((spell, set) -> {
            if (this.selectedSpells.isEmpty() || this.selectedSpells.contains(spell)) {
                radicals.addAll(set);
            }
        });

        return radicals.stream().sorted(Comparator.comparing(r -> r.strokeCount)).collect(Collectors.toList());
    }
}
