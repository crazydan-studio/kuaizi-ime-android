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
    public final static Builder builder = new Builder();

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

    /** 构建 {@link InputViewData} 列表 */
    public static List<InputViewData> build(InputList inputList, Input.Option option) {
        return doBuild(builder, inputList, option);
    }

    /** 构建 {@link InputViewData} 列表 */
    private static List<InputViewData> doBuild(Builder b, InputList inputList, Input.Option option) {
        int total = inputList.getInputs().size();
        List<InputViewData> dataList = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            int position = i;
            InputViewData data = Builder.build(b, (bld) -> doBuild(bld, inputList, option, position));

            dataList.add(data);
        }
        return dataList;
    }

    /** 构建 {@link InputViewData} */
    private static void doBuild(Builder b, InputList inputList, Input.Option option, int position) {
        Input input = inputList.getInput(position);
        Input preInput = inputList.getInput(position - 1);

        CharInput pending = inputList.getPendingOn(input);
        CharInput prePending = inputList.getPendingOn(preInput);

        // 前序正在输入的 Gap 位为算术待输入，则当前位置需多加一个空白位
//        boolean preGapIsMathExprInput = Input.isGap(preInput) && !Input.isEmpty(prePending) && prePending.isMathExpr();
//        int gapSpaces = needGapSpace ? preGapIsMathExprInput ? 2 : 1 : 0;

        MathExprInput mathExprInput = tryGetMathExprInput(inputList, input);
        if (mathExprInput != null) {
//            // 第一个普通输入不需要添加空白，
//            // 但是对于第一个不为空的算术待输入则需要提前添加，
//            // 因为，在输入过程中，算术待输入的前面没有 Gap 占位，
//            // 输入完毕后才会添加 Gap 占位
//            if (position == 0) {
//                gapSpaces = !Input.isEmpty(mathExprInput) ? 1 : 0;
//            }
//            // 算术输入 在输入完毕后会在其内部的开头位置添加一个 Gap 占位，从而导致该输入发生后移，
//            // 为避免视觉干扰，故在该算术的待输入之前先多附加一个空白
//            else if (needGapSpace && input.isGap() && !Input.isEmpty(mathExprInput)) {
//                gapSpaces = 2;
//            }

            // Note: 由于 Builder 是单例的，故而，不能嵌套复用其实例，否则，在外层设置的数据会被下层覆盖。
            // 并且，在构建嵌套的 InputList 时，不需要缓存，由最上层缓存整体即可
            List<InputViewData> inputs = doBuild(new Builder(true), mathExprInput.getInputList(), option);
            b.type(Type.MathExpr).inputs(inputs);
        } else if (input.isGap()) {
            b.type(Input.isEmpty(pending) ? Type.Gap : Type.Char);
        } else if (input.isSpace()) {
            b.type(Type.Space);
        } else {
            b.type(Type.Char);
        }

        // Note: 在视图中，需确保 Gap 空格的单位宽度与 GapInput 的宽度相同，
        // 而针对 GapInput 所添加的 Gap 空格数需要均分至左右两侧
        float[] gapSpaces = inputList.needGapSpace(position) ? new float[] { 0.5f, 0.5f } : null;
        if (input.isGap() && !Input.isEmpty(pending)) {
            // 正在输入的 GapInput，需判断其待输入的左右两侧的空格数：至少需一个光标占位
            Input left = inputList.getInput(position - 1);
            Input right = inputList.getInput(position + 1);
            gapSpaces = new float[] {
                    inputList.needGapSpace(left, pending) ? 2f : 1f, //
                    inputList.needGapSpace(pending, right) ? 2f : 1f
            };
        }

        boolean shouldBeSelected = needToBeSelected(inputList, input);
        Input currInput = Input.isEmpty(pending) ? input : pending;
        String[] textAndSpell = getInputTextAndSpell(currInput, option);

        b.position(position)
         .pending(pending != null)
         .selected(shouldBeSelected)
         .gapSpaces(gapSpaces)
         .text(textAndSpell[0])
         .spell(textAndSpell[1]);
        // Note: 不缓存正在输入的 Input，其变动频率更高
        if (pending != null) {
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
