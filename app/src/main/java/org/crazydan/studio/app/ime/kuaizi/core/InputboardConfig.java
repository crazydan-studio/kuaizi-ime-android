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

package org.crazydan.studio.app.ime.kuaizi.core;

import org.crazydan.studio.app.ime.kuaizi.conf.Config;
import org.crazydan.studio.app.ime.kuaizi.conf.ConfigKey;

/**
 * {@link Inputboard} 的配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-15
 */
public class InputboardConfig {
    /** 是否优先使用候选字的变体：主要针对拼音输入的候选字 */
    public final boolean useCandidateVariantFirst;

    /** 通过 {@link Config} 构造 {@link InputboardConfig} */
    public static InputboardConfig from(Config config) {
        return new InputboardConfig(config);
    }

    InputboardConfig(Config config) {
        this.useCandidateVariantFirst = config.bool(ConfigKey.enable_candidate_variant_first);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InputboardConfig that = (InputboardConfig) o;
        return this.useCandidateVariantFirst == that.useCandidateVariantFirst;
    }
}
