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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 候选字过滤数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-11-27
 */
public class CandidateFilters {
    private final Map<String, Set<Radical>> spellAndRadicalsMap;

    public CandidateFilters() {
        this(new HashMap<>());
    }

    public CandidateFilters(Map<String, Set<Radical>> spellAndRadicalsMap) {
        this.spellAndRadicalsMap = spellAndRadicalsMap;
    }

    public List<String> getSpells() {
        return new ArrayList<>(this.spellAndRadicalsMap.keySet());
    }

    public List<String> getRadicals(List<String> spells) {
        Set<Radical> radicals = new HashSet<>();

        this.spellAndRadicalsMap.forEach((spell, set) -> {
            if (spells.isEmpty() || spells.contains(spell)) {
                radicals.addAll(set);
            }
        });

        return radicals.stream()
                       .sorted(Comparator.comparing(r -> r.strokeCount))
                       .map(r -> r.value)
                       .collect(Collectors.toList());
    }

    public static class Radical {
        public final String value;
        public final int strokeCount;

        public Radical(String value, int strokeCount) {
            this.value = value;
            this.strokeCount = strokeCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Radical that = (Radical) o;
            return this.value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }
}
