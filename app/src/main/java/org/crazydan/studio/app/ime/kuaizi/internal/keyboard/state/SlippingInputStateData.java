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

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.State;

/**
 * {@link State.Type#Input_Slipping}的状态数据
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-15
 */
public class SlippingInputStateData implements State.Data {
    private Key.Level keyLevel = Key.Level.level_0;
    private Key<?> level0Key;
    private Collection<String> availableLevel1NextChars;
    private Collection<String> availableLevel2NextChars;

    private Collection<String> actualLevel1NextChars;
    private Collection<String> actualLevel2NextChars;

    public Key.Level getKeyLevel() {
        return this.keyLevel;
    }

    public Key<?> getLevel0Key() {
        return this.level0Key;
    }

    public void setLevel0Key(Key<?> level0Key) {
        this.level0Key = level0Key;
    }

    public Collection<String> getAvailableLevel1NextChars() {
        return this.availableLevel1NextChars != null ? this.availableLevel1NextChars : new ArrayList<>();
    }

    public void setAvailableLevel1NextChars(Collection<String> availableLevel1NextChars) {
        this.availableLevel1NextChars = availableLevel1NextChars;
    }

    public Collection<String> getAvailableLevel2NextChars() {
        return this.availableLevel2NextChars != null ? this.availableLevel2NextChars : new ArrayList<>();
    }

    public void setAvailableLevel2NextChars(Collection<String> availableLevel2NextChars) {
        this.availableLevel2NextChars = availableLevel2NextChars;
    }

    public Collection<String> getActualLevel1NextChars() {
        return this.actualLevel1NextChars;
    }

    public void setActualLevel1NextChars(Collection<String> actualLevel1NextChars) {
        this.actualLevel1NextChars = actualLevel1NextChars;
    }

    public Collection<String> getActualLevel2NextChars() {
        return this.actualLevel2NextChars;
    }

    public void setActualLevel2NextChars(Collection<String> actualLevel2NextChars) {
        this.actualLevel2NextChars = actualLevel2NextChars;
    }

    public void nextKeyLevel() {
        Key.Level nextKeyLevel = Key.Level.level_0;

        switch (this.keyLevel) {
            case level_0: {
                nextKeyLevel = Key.Level.level_1;
                break;
            }
            case level_1: {
                nextKeyLevel = Key.Level.level_2;
                break;
            }
        }

        this.keyLevel = nextKeyLevel;
    }
}
