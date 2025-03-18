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

        /** 创建可变副本，该副本可对配置项进行覆盖更新，但其更新不影响源配置，且未被覆盖的配置项依然可取到源配置的最新数据 */
        public Mutable mutable() {
            return new Mutable(this);
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

        /** 创建不可变副本，该副本始终可取到源配置的最新数据，但不可做配置更新 */
        public Immutable immutable() {
            return new Immutable(this);
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
