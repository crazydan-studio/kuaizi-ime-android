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

package org.crazydan.studio.app.ime.kuaizi.internal.key;

import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-02
 */
public abstract class BaseKey<K extends BaseKey<?>> implements Key<K> {
    private boolean hidden;
    private int bgColorAttrId;

    @Override
    public K hide() {
        this.hidden = true;
        return (K) this;
    }

    @Override
    public K show() {
        this.hidden = false;
        return (K) this;
    }

    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    @Override
    public int getBgColorAttrId() {
        return this.bgColorAttrId;
    }

    @Override
    public K setBgColorAttrId(int bgColorAttrId) {
        this.bgColorAttrId = bgColorAttrId;
        return (K) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseKey<?> that = (BaseKey<?>) o;
        return this.hidden == that.hidden && this.bgColorAttrId == that.bgColorAttrId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hidden, this.bgColorAttrId);
    }
}
