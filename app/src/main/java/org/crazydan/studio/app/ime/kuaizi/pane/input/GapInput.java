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

package org.crazydan.studio.app.ime.kuaizi.pane.input;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.pane.Input;
import org.crazydan.studio.app.ime.kuaizi.pane.Key;

/**
 * 间隙{@link Input 输入}
 * <p/>
 * 用于统一相邻两个{@link CharInput 字符输入}间的插入输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class GapInput extends BaseInput {

    @Override
    public Input copy() {
        return new GapInput();
    }

    @Override
    public boolean isGap() {
        return true;
    }

    @Override
    public void appendKey(Key key) {}

    @Override
    public void dropLastKey() {}

    @Override
    public List<String> getChars() {return new ArrayList<>();}

    @Override
    public boolean isSameWith(Object o) {return equals(o);}

    @NonNull
    @Override
    public String toString() {
        return "Gap";
    }

    @Override
    public boolean equals(Object o) {return this == o;}

    @Override
    public int hashCode() {return System.identityHashCode(this);}
}
