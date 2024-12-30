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

package org.crazydan.studio.app.ime.kuaizi.pane;

import org.crazydan.studio.app.ime.kuaizi.ImeSubtype;
import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;

/**
 * {@link Keyboard} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-03
 */
public class KeyboardConfig {
    /** 原键盘类型 */
    public final Keyboard.Type prevType;

    /** 左右手使用模式 */
    public final Keyboard.HandMode handMode;
    /** 是否为单行输入 */
    public final boolean singleLineInput;

    /** 是否已启用 X 输入面板 */
    public final boolean xInputPadEnabled;
    /** 是否已启用在 X 输入面板中让拉丁文输入共用拼音输入的按键布局 */
    public final boolean latinUsePinyinKeysInXInputPadEnabled;

    /** 是否已禁用对用户输入数据的保存 */
    public final boolean userInputDataDisabled;

    /** 通过 {@link Config} 构造 {@link KeyboardConfig} */
    public static KeyboardConfig from(Config config) {
        return new KeyboardConfig(config);
    }

    KeyboardConfig(Config config) {
        this.prevType = config.get(ConfigKey.prev_keyboard_type);

        this.handMode = config.get(ConfigKey.hand_mode);
        this.singleLineInput = config.bool(ConfigKey.single_line_input);

        this.xInputPadEnabled = config.bool(ConfigKey.enable_x_input_pad);
        this.latinUsePinyinKeysInXInputPadEnabled = config.bool(ConfigKey.enable_latin_use_pinyin_keys_in_x_input_pad)
                                                    // Note: 仅汉字输入环境才支持将拉丁文键盘与拼音键盘的按键布局设置为相同的
                                                    && config.get(ConfigKey.ime_subtype) == ImeSubtype.hans;

        this.userInputDataDisabled = config.bool(ConfigKey.disable_user_input_data);
    }
}
