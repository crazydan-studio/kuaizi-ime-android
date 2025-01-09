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

package org.crazydan.studio.app.ime.kuaizi.core;

/**
 * {@link Key} 生成器
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-30
 */
public interface KeyFactory {

    /**
     * 获取当前状态下的二维矩阵 {@link Key}
     * <p/>
     * 元素可为<code>null</code>，表示该位置不放置任何按键
     */
    Key[][] getKeys();

    /** 无动效的生成器 */
    interface NoAnimation extends KeyFactory {}

    /** 左手模式的生成器 */
    interface LeftHandMode extends KeyFactory {}

    /** {@link NoAnimation} 与 {@link LeftHandMode} 的组合 */
    interface LeftHandMode_NoAnimation extends NoAnimation, LeftHandMode {}
}
