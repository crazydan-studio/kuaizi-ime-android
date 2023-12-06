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

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拼音树
 * <p/>
 * 按拼音的字母组合逐层分解
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-06
 */
public class PinyinTree {
    /** 从 root 到当前节点所构成的字母组合是否为有效拼音 */
    public final boolean pinyin;

    /** 后继字母及其子树：按字母顺序升序排序 */
    public final Map<String, PinyinTree> children = new LinkedHashMap<>();

    private PinyinTree(boolean pinyin) {
        this.pinyin = pinyin;
    }

    public static PinyinTree create(Collection<String> pinyinList) {
        PinyinTree root = new PinyinTree(false);

        pinyinList.stream()
                  // 必须先按长度升序排序，以确保没有后继字母的拼音最先被加入树结构中
                  .sorted(Comparator.comparing(String::length)) //
                  .sorted(String::compareTo) //
                  .forEach(pinyin -> {
                      String start;
                      String[] ends;
                      if (pinyin.startsWith("ch") || pinyin.startsWith("sh") || pinyin.startsWith("zh")) {
                          start = pinyin.substring(0, 2);
                          ends = pinyin.substring(2).split("");
                      } else {
                          start = pinyin.substring(0, 1);
                          ends = pinyin.length() > 1 ? pinyin.substring(1).split("") : new String[0];
                      }

                      root.add("", start, ends, pinyinList);
                  });

        return root;
    }

    public boolean isFinal() {
        return this.children.isEmpty();
    }

    private void add(String start, String next, String[] ends, Collection<String> pinyinList) {
        boolean isPinyin = ends.length == 0 || pinyinList.contains(start + next);
        PinyinTree child = this.children.computeIfAbsent(next, (k) -> new PinyinTree(isPinyin));

        if (ends.length > 0) {
            child.add(start + next,
                      ends[0],
                      ends.length == 1 ? new String[0] : Arrays.copyOfRange(ends, 1, ends.length),
                      pinyinList);
        }
    }
}
