/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyboardConfig;

/**
 * {@link KeyTable} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class KeyTableConfig {
    /** 是否存在输入 */
    public final boolean hasInputs;
    /** 是否有待撤回输入 */
    public final boolean hasRevokingInputs;
    /** 是否已选中字符输入 */
    public final boolean charInputSelected;

    /** 键盘配置 */
    public final KeyboardConfig keyboard;

    public static KeyTableConfig from(KeyboardConfig keyboard) {
        return from(keyboard, null);
    }

    public static KeyTableConfig from(KeyboardConfig keyboard, InputList inputList) {
        return new KeyTableConfig(keyboard, inputList);
    }

    KeyTableConfig(KeyboardConfig keyboard, InputList inputList) {
        this.hasInputs = inputList != null && !inputList.isEmpty();
        this.hasRevokingInputs = inputList != null && inputList.canRevokeCommit();
        this.charInputSelected = inputList != null && !inputList.isGapSelected();

        this.keyboard = keyboard;
    }
}
