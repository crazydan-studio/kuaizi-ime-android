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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputList;

/**
 * {@link KeyTable} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class KeyTableConfig {
    /** 是否存在输入 */
    public final boolean hasInputs;
    /** 是否已选中字符输入 */
    public final boolean charInputSelected;

    /** 左右手使用模式 */
    public final Keyboard.HandMode keyboardHandMode;
    /** 是否采用单行输入模式 */
    public final boolean useSingleLineInputMode;

    /** 是否已启用 X 输入面板 */
    public final boolean xInputPadEnabled;
    /** 是否已启用在 X 输入面板中让拉丁文输入共用拼音输入的按键布局 */
    public final boolean latinUsePinyinKeysInXInputPadEnabled;

    /** 是否有可撤回的输入提交 */
    public final boolean hasRevokableInputsCommit;

    public KeyTableConfig(KeyboardContext context) {
        InputList inputList = context.inputList;

        this.hasInputs = inputList != null && !inputList.isEmpty();
        this.charInputSelected = inputList != null && !inputList.isGapSelected();

        this.keyboardHandMode = context.keyboardHandMode;
        this.useSingleLineInputMode = context.useSingleLineInputMode;

        this.xInputPadEnabled = context.xInputPadEnabled;
        this.latinUsePinyinKeysInXInputPadEnabled = context.latinUsePinyinKeysInXInputPadEnabled;

        this.hasRevokableInputsCommit = context.hasRevokableInputsCommit;
    }
}
