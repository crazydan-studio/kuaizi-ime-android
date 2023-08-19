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

package org.crazydan.studio.app.ime.kuaizi.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-26
 */
public class CollectionUtils {

    public static <T> T first(Collection<T> c) {
        return c == null || c.isEmpty() ? null : c.iterator().next();
    }

    public static <T> T last(Collection<T> c) {
        return c == null || c.isEmpty() ? null //
                                        : c.size() == 1 //
                                          ? first(c) //
                                          : c.stream().reduce((prev, next) -> next).orElse(null);
    }

    /**
     * 从 <code>source</code> 向 <code>target</code> 补充元素，
     * 直到元素数量小于或等于 <code>top</code> 数
     */
    public static <T> List<T> topPatch(List<T> target, int top, Supplier<Collection<T>> supplier) {
        int lostCount = top - target.size();

        if (lostCount > 0) {
            Iterator<T> it = supplier.get().iterator();
            while (it.hasNext() && lostCount > 0) {
                T e = it.next();
                if (target.contains(e)) {
                    continue;
                }

                target.add(e);
                lostCount--;
            }
        } else if (lostCount < 0) {
            return target.subList(0, top);
        }
        return target;
    }
}
