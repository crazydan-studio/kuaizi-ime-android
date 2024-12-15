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

import org.crazydan.studio.app.ime.kuaizi.pane.InputConfig;

/**
 * {@link KeyTable} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class KeyTableConfig {
    private final boolean hasInputs;
    /** 是否有待撤回输入 */
    private final boolean hasRevokingInputs;
    /** 是否已选中字符输入 */
    private final boolean charInputSelected;

    private final boolean leftHandMode;
    private final boolean singleLineInput;
    private final boolean xInputPadEnabled;
    private final boolean latinUsePinyinKeysInXInputPadEnabled;

    public KeyTableConfig(InputConfig inputConfig) {
        this(inputConfig, false, false, false);
    }

    public KeyTableConfig(
            InputConfig inputConfig, boolean hasInputs, boolean hasRevokingInputs, boolean charInputSelected
    ) {
        this.hasInputs = hasInputs;
        this.hasRevokingInputs = hasRevokingInputs;
        this.charInputSelected = charInputSelected;

        this.leftHandMode = inputConfig.isLeftHandMode();
        this.singleLineInput = inputConfig.bool(InputConfig.Key.single_line_input);
        this.xInputPadEnabled = inputConfig.isXInputPadEnabled();
        this.latinUsePinyinKeysInXInputPadEnabled = inputConfig.isLatinUsePinyinKeysInXInputPadEnabled();
    }

    public boolean hasInputs() {
        return this.hasInputs;
    }

    public boolean hasRevokingInputs() {
        return this.hasRevokingInputs;
    }

    public boolean isCharInputSelected() {
        return this.charInputSelected;
    }

    public boolean isLeftHandMode() {
        return this.leftHandMode;
    }

    public boolean isSingleLineInput() {
        return this.singleLineInput;
    }

    public boolean isXInputPadEnabled() {
        return this.xInputPadEnabled;
    }

    public boolean isLatinUsePinyinKeysInXInputPadEnabled() {
        return this.latinUsePinyinKeysInXInputPadEnabled;
    }
}
