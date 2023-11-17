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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-26
 */
public class CollectionUtils {

    public static <T> boolean contains(T[] array, T el) {
        if (array == null || el == null) {
            return false;
        }

        for (T t : array) {
            if (Objects.equals(t, el)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T first(Collection<T> c) {
        return c == null || c.isEmpty() ? null : c.iterator().next();
    }

    public static <T> T last(Collection<T> c) {
        return c == null || c.isEmpty() ? null //
                                        : c.size() == 1 //
                                          ? first(c) //
                                          : c.stream().reduce((prev, next) -> next).orElse(null);
    }

    /** 通过 对象引用 是否相等确定指定元素在列表中的位置 */
    public static <T> int indexOfRef(List<T> list, T element) {
        return indexOfRef(list, element, 0);
    }

    /** 通过 对象引用 是否相等确定指定元素在列表中的位置 */
    public static <T> int indexOfRef(List<T> list, T element, int fromIndex) {
        for (int i = fromIndex; i < list.size(); i++) {
            if (list.get(i) == element) {
                return i;
            }
        }
        return -1;
    }

    /** 返回的为新建的 List 实例，可直接做元素增删 */
    public static <T> List<T> subList(List<T> list, int start, int end) {
        if (start < 0 || end < 0 || start >= list.size() || start >= end) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(start, Math.min(end, list.size())));
    }

    /**
     * 向列表填充指定元素直到满足指定大小
     *
     * @return 填充后的原始列表
     */
    public static <E, T extends Collection<E>> T fillToSize(T c, E e, int untilSize) {
        int size = c.size();

        if (size < untilSize) {
            for (int i = size; i < untilSize; i++) {
                c.add(e);
            }
        }
        return c;
    }

    /**
     * 从 <code>source</code> 向 <code>target</code> 补充元素，
     * 直到元素数量小于或等于 <code>top</code> 数
     * <p/>
     * 直接修改 <code>target</code>，且返回值也为该参数本身
     */
    public static <T> List<T> topPatch(List<T> target, int top, Supplier<Collection<T>> supplier) {
        if (top == 0) {
            target.clear();
            return target;
        }

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
            Iterator<T> it = target.iterator();
            int total = target.size();
            for (int i = 0; i < total && it.hasNext(); i++) {
                it.next();

                if (i >= top) {
                    it.remove();
                }
            }
            return target;
        }
        return target;
    }
}
