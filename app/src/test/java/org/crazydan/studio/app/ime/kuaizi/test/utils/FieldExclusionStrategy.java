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

package org.crazydan.studio.app.ime.kuaizi.test.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public class FieldExclusionStrategy implements ExclusionStrategy {
    private final Set<String> includes;
    private final Set<String> excludes;

    public FieldExclusionStrategy(String[] includes, String[] excludes) {
        this.includes = includes != null ? new HashSet<>(Arrays.asList(includes)) : null;
        this.excludes = excludes != null ? new HashSet<>(Arrays.asList(excludes)) : null;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return (this.includes != null && !this.includes.contains(f.getName()))
               //
               || (this.excludes != null && this.excludes.contains(f.getName()));
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
