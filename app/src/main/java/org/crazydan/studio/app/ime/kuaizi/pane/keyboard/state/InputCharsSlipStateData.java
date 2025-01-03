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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.State;

/**
 * {@link State.Type#InputChars_Slip_Doing} 的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-15
 */
public class InputCharsSlipStateData implements State.Data {
    private Key level0Key;
    private Key level1Key;
    private Key level2Key;

    private Map<Integer, List<String>> level2NextChars;

    public Key getLevel0Key() {
        return this.level0Key;
    }

    public void setLevel0Key(Key level0Key) {
        this.level0Key = level0Key;
    }

    public Key getLevel1Key() {
        return this.level1Key;
    }

    public void setLevel1Key(Key level1Key) {
        this.level1Key = level1Key;
    }

    public Key getLevel2Key() {
        return this.level2Key;
    }

    public void setLevel2Key(Key level2Key) {
        this.level2Key = level2Key;
    }

    public Map<Integer, List<String>> getLevel2NextChars() {
        return this.level2NextChars;
    }

    public void setLevel2NextChars(Collection<String> level2NextChars) {
        this.level2NextChars = new HashMap<>();

        for (String text : level2NextChars) {
            this.level2NextChars.computeIfAbsent(text.length(), (k) -> new ArrayList<>()).add(text);
        }
    }
}
