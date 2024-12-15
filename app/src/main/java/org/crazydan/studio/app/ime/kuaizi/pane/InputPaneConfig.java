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

import android.content.SharedPreferences;

/**
 * {@link InputPane} 的配置
 * <p/>
 * 同时处理系统配置和运行时配置，且运行时的配置优先
 * <p/>
 * 其将自动与系统配置的变更进行同步
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
class InputPaneConfig extends InputConfig {
    /** 系统配置：与系统设置做同步更新 */
    private final InputConfig system = new InputConfig();
    /** 运行时配置：记录运行期间更新的配置 */
    private final InputConfig runtime = new InputConfig();

    public InputListConfig createInputListConfig() {
        return InputListConfig.from(this);
    }

    public KeyboardConfig createKeyboardConfig() {
        return KeyboardConfig.from(this);
    }

    @Override
    public boolean has(Key key) {
        return this.runtime.has(key) || this.system.has(key);
    }

    /** 获取配置值，且以运行时的配置优先 */
    @Override
    public <T> T get(Key key) {
        return this.runtime.has(key) ? this.runtime.get(key) : this.system.get(key);
    }

    /** 更新运行时配置 */
    @Override
    public <T> boolean set(Key key, T value, boolean ignoreNull) {
        return this.runtime.set(key, value, ignoreNull);
    }

    /** 监听系统配置的变化 */
    @Override
    public void setListener(ChangeListener listener) {
        this.system.setListener(listener);
    }

    /** 同步系统配置数据 */
    @Override
    public void syncWith(SharedPreferences preferences) {
        this.system.syncWith(preferences);
    }
}
