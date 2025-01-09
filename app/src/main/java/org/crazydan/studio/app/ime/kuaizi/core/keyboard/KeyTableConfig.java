/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;

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
