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

package org.crazydan.studio.app.ime.kuaizi.internal;

import java.util.List;

/**
 * {@link Keyboard 键盘}输入对象，包含零个或多个字符
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public interface Input {

    /** 输入按键列表 */
    List<Key> keys();

    /** 当前按键 */
    Key currentKey();

    /** 追加输入按键 */
    void append(Key key);

    /** 获取输入字符列表 */
    List<String> chars();

    /** 是否有可输入字 */
    boolean hasWord();

    /**
     * 已选择候选字
     *
     * @return 若为<code>null</code>，则表示未选择
     */
    InputWord word();

    /**
     * 可选候选字列表
     *
     * @return 若无候选字，则返回空集合
     */
    List<InputWord> candidates();
}
