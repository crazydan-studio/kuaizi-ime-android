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

import java.util.ArrayList;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.Input;

/**
 * {@link Input} 的输入补全
 * <p/>
 * 用于自动补全正在输入的英文单词或中文句子等
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-01-15
 */
public class InputCompletions {
    /** 补全类型 */
    public final Type type;
    /**
     * 待应用补全的 {@link Input} 的范围
     * <p/>
     * 在输入列表中该范围内的输入均会被替换为 {@link InputCompletion#inputs} 中的输入，
     * 若 {@link InputCompletion#inputs} 的数量大于该范围内的输入数量，
     * 则多余的按新增挨个插入该范围之后
     * <p/>
     * 应用范围从起点序号开始，到终点序号结束，但不含终点序号
     */
    public final Range applyRange;

    /** 可应用的{@link InputCompletion 补全内容}的列表 */
    public final List<InputCompletion> data = new ArrayList<>();

    /** 有多个补全应用的输入位置 */
    public InputCompletions(Type type, int rangeStart, int rangeEnd) {
        this.type = type;
        this.applyRange = new Range(rangeStart, rangeEnd);
    }

    public void add(InputCompletion completion) {
        this.data.add(completion);
    }

    /** 补全类型 */
    public enum Type {
        /** 拉丁文补全：替换指定范围内拉丁文输入的{@link CharInput#getKeys() 全部输入字符} */
        Latin,
        /** 短语补全：替换或补充指定范围内的短语输入的{@link CharInput#getWord() 输入字} */
        Phrase_Word,
        ;
    }

    /** 补全的应用范围 */
    public static class Range {
        public final int start;
        public final int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
