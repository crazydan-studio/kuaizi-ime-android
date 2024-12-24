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

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface KeyFactory {

    /**
     * 获取当前状态下的二维矩阵 {@link Key}
     * <p/>
     * 元素可为<code>null</code>，表示该位置不放置任何按键
     */
    Key<?>[][] getKeys();

    /** 无动效的生成器 */
    interface NoAnimation extends KeyFactory {}

    /** 左手模式的生成器 */
    interface LeftHandMode extends KeyFactory {}

    /** {@link NoAnimation} 与 {@link LeftHandMode} 的组合 */
    interface LeftHandMode_NoAnimation extends NoAnimation, LeftHandMode {}
}
