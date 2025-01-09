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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.common.Immutable;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.input.word.PinyinWord;

/**
 * {@link Input} 的视图数据，仅用于构造视图
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-12-09
 */
public class InputViewData extends Immutable {
    private final static Builder builder = new Builder();
    /**
     * 由于 Builder 是单例的，故而，不能对嵌套 {@link InputList} 复用其实例，否则，在外层配置的数据会被下层覆盖。
     * 并且，在构建嵌套的 {@link InputList} 时，不需要缓存，由最上层构建器缓存其整体即可
     * <p/>
     * 注意，每层嵌套均需单独实例化构建器，当前只有一层嵌套
     */
    private final static Builder mathBuilder = new Builder(true);

    public enum Type {
        /** 默认均为 {@link CharInput} */
        Char,
        /** 占位输入：{@link Input#isGap()} */
        Gap,
        /** 空格输入：{@link Input#isSpace()} */
        Space,
        /** 算术输入：{@link Input#isMathExpr()} */
        MathExpr
    }

    /** 当前输入在 {@link InputList} 中的位置（序号） */
    public final int position;
    /** 当前输入的类型 */
    public final Type type;

    /**
     * 当前输入是否为待输入
     * <p/>
     * 只有含有{@link InputList#getPending() 待输入}的才是待输入，
     * 并且，在 {@link InputList} 中必然存在唯一的待输入
     */
    public final boolean pending;
    /**
     * 当前输入是否已被选中
     * <p/>
     * 在配对符号中，若其中一个为{@link #pending 待输入}，
     * 则另一个也会被选中，否则，只有待输入才会被选中
     */
    public final boolean selected;

    /** 当前输入左右两侧所需的 Gap 空格数，二元数组，为 null 时，表示不需要添加空格 */
    public final float[] gapSpaces;
    /** 嵌套的输入列表：只有{@link MathExprInput 算术输入}才存在输入列表嵌套 */
    public final List<InputViewData> inputs;

    /** 当前输入的显示文本 */
    public final String text;
    /** 当前输入的读音（主要针对候选字） */
    public final String spell;

    InputViewData(Builder builder) {
        super(builder);

        this.position = builder.position;
        this.type = builder.type;

        this.pending = builder.pending;
        this.selected = builder.selected;

        this.gapSpaces = builder.gapSpaces;
        this.inputs = builder.inputs;

        this.text = builder.text;
        this.spell = builder.spell;
    }

    /** 清空已构建 {@link InputViewData} */
    public static void clearCachedBuilds() {
        builder.clear();
        mathBuilder.clear();
    }

    /** 构建 {@link InputViewData} 列表 */
    public static List<InputViewData> build(InputList inputList, Input.Option option) {
        return doBuild(builder, inputList, option, true);
    }

    /** 构建 {@link InputViewData} 列表 */
    private static List<InputViewData> doBuild(
            Builder b, InputList inputList, Input.Option option, boolean canBeSelected
    ) {
        int total = inputList.getInputs().size();
        List<InputViewData> dataList = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            int position = i;
            InputViewData data = Builder.build(b, (bld) -> doBuild(bld, inputList, option, position, canBeSelected));

            dataList.add(data);
        }
        return dataList;
    }

    /** 构建 {@link InputViewData} */
    private static void doBuild(
            Builder b, //
            InputList inputList, Input.Option option, //
            int position, boolean canBeSelected
    ) {
        Input input = inputList.getInput(position);
        CharInput pending = inputList.getPendingOn(input);

        boolean hasPending = pending != null;
        boolean hasEmptyPending = Input.isEmpty(pending);
        boolean shouldBeSelected = canBeSelected && needToBeSelected(inputList, input);

        MathExprInput mathExprInput = tryGetMathExprInput(inputList, input);
        if (mathExprInput != null) {
            // Note: 只有上层输入整体被选中时，下层的输入才能被独立选中
            List<InputViewData> inputs = doBuild(mathBuilder, mathExprInput.getInputList(), option, shouldBeSelected);

            b.type(Type.MathExpr).inputs(inputs);
        } else if (input.isGap()) {
            b.type(hasEmptyPending ? Type.Gap : Type.Char);
        } else if (input.isSpace()) {
            b.type(Type.Space);
        } else {
            b.type(Type.Char);
        }

        // Note: 在视图中，需确保 Gap 空格的单位宽度与 GapInput 的宽度相同，
        // 而针对 GapInput 所添加的 Gap 空格数需要均分至左右两侧
        float[] gapSpaces = inputList.needGapSpace(position) ? new float[] { 0.5f, 0.5f } : null;
        if (input.isGap() && !hasEmptyPending) {
            // 正在输入的 GapInput，需判断其待输入的左右两侧的空格数：至少需一个光标占位
            Input left = inputList.getInput(position - 1);
            Input right = inputList.getInput(position + 1);
            gapSpaces = new float[] {
                    inputList.needGapSpace(left, pending) ? 2f : 1f, //
                    inputList.needGapSpace(pending, right) ? 2f : 1f
            };
        }

        Input currInput = hasEmptyPending ? input : pending;
        String[] textAndSpell = getInputTextAndSpell(currInput, option);

        b.position(position)
         .pending(hasPending)
         .selected(shouldBeSelected)
         .gapSpaces(gapSpaces)
         .text(textAndSpell[0])
         .spell(textAndSpell[1]);

        // Note: 不缓存正在输入的 Input，其变动频率更高
        if (hasPending) {
            b.notCache();
        }
    }

    public static String[] getInputTextAndSpell(Input input, Input.Option option) {
        InputWord inputWord = input.getWord();

        String text;
        String spell = inputWord instanceof PinyinWord ? ((PinyinWord) inputWord).spell.value : null;
        if (option != null && inputWord != null) {
            text = input.getText(option).toString();

            // 若已携带读音，不再单独显示读音
            if (spell != null && text.contains(spell)) {
                spell = null;
            }
        } else {
            text = inputWord != null ? inputWord.value : input.getJoinedChars();
        }

        return new String[] { text, spell };
    }

    private static MathExprInput tryGetMathExprInput(InputList inputList, Input input) {
        CharInput pending = inputList.getPendingOn(input);

        return isMathExprInput(pending)
               // 待输入的算术不能为空，否则，原输入需为空，才能将待输入作为算术输入，
               // 从而确保在未修改非算术输入时能够正常显示原始输入
               && (!Input.isEmpty(pending) || Input.isEmpty(input)) //
               ? (MathExprInput) pending //
               : isMathExprInput(input)
                 // 若算术输入 没有 被替换为非算术输入，则返回其自身
                 && Input.isEmpty(pending) //
                 ? (MathExprInput) input : null;
    }

    private static boolean needToBeSelected(InputList inputList, Input input) {
        boolean selected = inputList.isSelected(input);

        // 若配对符号的另一侧符号被选中，则该侧符号也同样需被选中
        if (!selected && !input.isGap() && ((CharInput) input).hasPair()) {
            if (inputList.isSelected(((CharInput) input).getPair())) {
                selected = true;
            }
        }
        return selected;
    }

    private static boolean isMathExprInput(Input input) {
        return input != null && input.isMathExpr();
    }

    /** {@link InputViewData} 的构建器 */
    public static class Builder extends Immutable.CachableBuilder<InputViewData> {
        private int position;
        private Type type;

        private boolean pending;
        private boolean selected;

        private float[] gapSpaces;
        private List<InputViewData> inputs;

        private String text;
        private String spell;

        protected Builder() {
            this(false);
        }

        protected Builder(boolean disableCache) {
            super(disableCache ? 0 : 20);
        }

        // ===================== Start: 构建函数 ===================

        @Override
        protected InputViewData doBuild() {
            return new InputViewData(this);
        }

        @Override
        protected void reset() {
            this.position = 0;
            this.type = null;

            this.pending = false;
            this.selected = false;

            this.gapSpaces = null;
            this.inputs = null;

            this.text = null;
            this.spell = null;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.position,
                                this.type,
                                this.pending,
                                this.selected,
                                Arrays.hashCode(this.gapSpaces),
                                this.inputs,
                                this.text,
                                this.spell);
        }

        // ===================== End: 构建函数 ===================

        // ===================== Start: 构建配置 ===================

        /** @see InputViewData#position */
        public Builder position(int position) {
            this.position = position;
            return this;
        }

        /** @see InputViewData#type */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /** @see InputViewData#pending */
        public Builder pending(boolean pending) {
            this.pending = pending;
            return this;
        }

        /** @see InputViewData#selected */
        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        /** @see InputViewData#gapSpaces */
        public Builder gapSpaces(float[] gapSpaces) {
            this.gapSpaces = gapSpaces;
            return this;
        }

        /** @see InputViewData#inputs */
        public Builder inputs(List<InputViewData> inputs) {
            this.inputs = inputs;
            return this;
        }

        /** @see InputViewData#text */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /** @see InputViewData#spell */
        public Builder spell(String spell) {
            this.spell = spell;
            return this;
        }

        // ===================== End: 构建配置 ===================
    }
}
