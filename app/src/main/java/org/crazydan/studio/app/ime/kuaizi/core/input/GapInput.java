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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.core.Input;

/**
 * 间隙{@link Input 输入}
 * <p/>
 * 用于统一相邻两个{@link CharInput 字符输入}间的插入输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class GapInput extends Input {

    @Override
    protected boolean isEmpty() {return true;}

    @Override
    public Input copy() {return new GapInput();}

    @Override
    public StringBuilder getText(Option option) {return new StringBuilder();}

    @NonNull
    @Override
    public String toString() {return "Gap";}

    @Override
    public boolean equals(Object o) {return this == o;}

    @Override
    public int hashCode() {return System.identityHashCode(this);}
}
