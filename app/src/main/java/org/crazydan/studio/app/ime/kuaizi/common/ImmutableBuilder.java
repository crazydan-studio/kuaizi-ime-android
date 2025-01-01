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

package org.crazydan.studio.app.ime.kuaizi.common;

import java.util.function.Consumer;

/**
 * 不可变对象的构建器
 * <p/>
 * 不可变对象的所有属性都需定义为 <code>public final</code>
 * <p/>
 * 该构建器以单例模式暂存不可变对象的属性值，并在 {@link #build()}
 * 后重置以实现复用，因此，其不是线程安全的
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-31
 */
public abstract class ImmutableBuilder<B extends ImmutableBuilder<B, O>, O> {

    /** 在入参函数中添加构建配置，再根据其配置创建不可变对象 */
    public static <O, B extends ImmutableBuilder<B, O>> O build(B b, Consumer<B> c) {
        // Note: 构建器为单例复用，在使用前必须重置
        b.reset();

        c.accept(b);

        return b.build();
    }

    /** 为便于构建器作为单例复用，必须在 {@link #build} 返回之前，重置所有的构建配置 */
    protected abstract void reset();

    /**
     * 在不可变对象的构造函数中根据构建器的配置为只读属性赋初始值
     * <p/>
     * 注意，相关属性的值转换和处理操作需在传给不可变对象的构造函数之前完成，
     * 以使其构造函数内仅需直接引用构建器的属性值，确保二者的 {@link #hashCode()} 是相同的
     */
    protected abstract O build();
}
