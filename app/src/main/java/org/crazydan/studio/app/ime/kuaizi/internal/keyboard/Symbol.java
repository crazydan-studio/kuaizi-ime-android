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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-27
 */
public class Symbol {
    private final String text;
    private final boolean doubled;
    private final List<String> replacements;

    public Symbol(String text, boolean doubled) {
        this.text = text;
        this.doubled = doubled;
        this.replacements = new ArrayList<>();
    }

    public String getText() {
        return this.text;
    }

    public boolean isDoubled() {
        return this.doubled;
    }

    public List<String> getReplacements() {
        return this.replacements;
    }

    public Symbol withReplacements(String... replacements) {
        for (String s : replacements) {
            if (s != null && !s.isEmpty()) {
                this.replacements.add(s);
            }
        }
        return this;
    }
}
