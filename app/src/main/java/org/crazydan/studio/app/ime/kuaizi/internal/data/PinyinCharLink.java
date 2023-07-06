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

import java.util.Objects;

import androidx.annotation.NonNull;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class PinyinCharLink {
    private final boolean undirected;
    private final String source;
    private final String target;

    public PinyinCharLink(boolean undirected, String source, String target) {
        this.undirected = undirected;
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return this.source;
    }

    public String getTarget() {
        return this.target;
    }

    @NonNull
    @Override
    public String toString() {
        return this.source + " -> " + this.target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PinyinCharLink that = (PinyinCharLink) o;
        return (this.source.equals(that.source) && this.target.equals(that.target))
               //
               || (this.undirected && this.source.equals(that.target) && this.target.equals(that.source));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.source, this.target)
               //
               + (this.undirected ? Objects.hash(this.target, this.source) : 0);
    }
}
