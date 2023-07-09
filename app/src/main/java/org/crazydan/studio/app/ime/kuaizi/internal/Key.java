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

/**
 * {@link Keyboard 键盘}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-01
 */
public interface Key<K extends Key<?>> {

    /** 隐藏按键 */
    K hide();

    /** 显示按键 */
    K show();

    /** 是否隐藏 */
    boolean isHidden();

    /** 获取背景色属性 id */
    int bgColorAttrId();

    /** 设置背景色属性 id */
    K bgColorAttrId(int bgColorAttrId);
}
