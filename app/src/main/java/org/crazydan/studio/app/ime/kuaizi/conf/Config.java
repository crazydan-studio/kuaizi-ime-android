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

package org.crazydan.studio.app.ime.kuaizi.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 配置
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-12-05
 */
public interface Config {

    /** 检查配置项是否存在 */
    boolean has(ConfigKey key);

    /** 获取配置项值 */
    <T> T get(ConfigKey key);

    /** 获取 bool 类型的配置项值 */
    default Boolean bool(ConfigKey key) {
        return get(key);
    }

    /** 不可变 {@link Config}：只能读取源配置的数据，不可做配置变更 */
    class Immutable implements Config {
        protected final Config source;

        public Immutable(Config source) {
            this.source = source;
        }

        @Override
        public boolean has(ConfigKey key) {
            return this.source != null && this.source.has(key);
        }

        @Override
        public <T> T get(ConfigKey key) {
            return this.source != null ? this.source.get(key) : null;
        }
    }

    /** 可变 {@link Config}：仅在当前配置中记录已变更的配置项，其余配置项从源配置中获取 */
    class Mutable extends Immutable {
        private final Map<ConfigKey, Object> vars = new HashMap<>();

        public Mutable() {
            this(null);
        }

        public Mutable(Config source) {
            super(source);
        }

        @Override
        public boolean has(ConfigKey key) {
            return this.vars.containsKey(key) || super.has(key);
        }

        /** 优先获取当前配置所记录的数据，再从源配置中获取数据 */
        @Override
        public <T> T get(ConfigKey key) {
            return this.vars.containsKey(key) ? (T) this.vars.get(key) : super.get(key);
        }

        /**
         * @param value
         *         若为 null，则删除配置项 conf
         */
        public <T> boolean set(ConfigKey key, T value) {
            return set(key, value, false);
        }

        /**
         * @param value
         *         若为 null，则删除配置项 conf
         * @param ignoreNull
         *         是否忽略为 null 的值
         */
        public <T> boolean set(ConfigKey key, T value, boolean ignoreNull) {
            T oldValue = get(key);
            if (Objects.equals(oldValue, value) || (ignoreNull && value == null)) {
                return false;
            }

            if (value == null) {
                this.vars.remove(key);
            } else {
                this.vars.put(key, value);
            }
            return true;
        }
    }
}
