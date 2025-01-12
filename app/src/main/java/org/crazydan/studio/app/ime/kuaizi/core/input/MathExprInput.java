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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;

import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.InputList;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.MathOpKey;

/**
 * 数学表达式输入
 * <p/>
 * 内嵌独立的{@link InputList 输入列表}，以单独处理表达式中的符号和数字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-15
 */
public class MathExprInput extends CharInput {
    private final InputList inputList;

    public MathExprInput() {
        this(new InputList());
    }

    private MathExprInput(InputList inputList) {
        this.inputList = inputList;
    }

    public InputList getInputList() {
        return this.inputList;
    }

    @Override
    public boolean isEmpty() {
        return this.inputList.isEmpty();
    }

    @Override
    public StringBuilder getText(Option option) {
        List<CharInput> inputs = this.inputList.getCharInputs();
        boolean hasFirstEqual = isEqualOp(CollectionUtils.first(inputs));
        boolean hasLastEqual = isEqualOp(CollectionUtils.last(inputs));

        Double result = null;
        if (hasFirstEqual || hasLastEqual) {
            if (hasFirstEqual) {
                inputs.remove(0);
            } else {
                inputs.remove(inputs.size() - 1);
            }

            result = calculate(inputs);
        }

        if (result == null) {
            return this.inputList.getText(option);
        }

        // Note: 涉及 ‱，因此最小精度至少需为小数点后 4 位
        String text = String.format(Locale.getDefault(), "%.4f", result);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");

        StringBuilder sb;
        if (hasLastEqual) {
            sb = this.inputList.getText(option);
            sb.append(" ");
        } else {
            sb = new StringBuilder();
        }
        sb.append(text);

        return sb;
    }

    @Override
    public Input copy() {
        // Note：
        // - 输入列表直接复用，以确保与视图绑定的输入对象实例保持不变
        // - 只有新建 pending 时才会做复制操作，
        //   此时，对算术表达式的原输入或 pending 做修改操作都是等效的，
        //   不需要通过副本规避
        return new MathExprInput(getInputList());
    }

    @Override
    public void confirm() {
        this.inputList.confirmPending();
    }

    // ========================= Start: 覆盖父类接口 ========================

    @Override
    public boolean isSpace() {return false;}

    @Override
    public boolean isLatin() {return false;}

    @Override
    public boolean isPinyin() {return false;}

    @Override
    public boolean isSymbol() {return false;}

    @Override
    public boolean isEmoji() {return false;}

    @Override
    public List<Key> getKeys() {return new ArrayList<>();}

    @Override
    public Key getFirstKey() {return null;}

    @Override
    public Key getLastKey() {return null;}

    @Override
    public void appendKey(Key key) {}

    @Override
    public void dropLastKey() {}

    @Override
    public void replaceKeyAfterLevel(CharKey.Level level, Key newKey) {}

    @Override
    public void replaceLatestKey(Key oldKey, Key newKey) {}

    @Override
    public void replaceLastKey(Key newKey) {}

    @Override
    public List<String> getChars() {return new ArrayList<>();}

    @Override
    public boolean isTextOnlyWordSpell(Option option) {return false;}

    @Override
    public boolean hasWord() {return false;}

    @Override
    public InputWord getWord() {return null;}

    @Override
    public void setWord(InputWord word) {}

    // ========================= End: 覆盖父类接口 ========================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MathExprInput that = (MathExprInput) o;
        return this.inputList.equals(that.inputList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inputList);
    }

    /** @return 若计算式有效，则返回计算结果，否则，返回 null */
    private static Double calculate(List<CharInput> inputs) {
        List<Expr> exprs = new ArrayList<>(inputs.size());

        for (int i = 0; i < inputs.size(); i++) {
            CharInput input = inputs.get(i);

            Expr expr = null;
            MathOpKey.Type type = getOpType(input);
            if (type == null) {
                expr = Num.create(input);
            } else {
                switch (type) {
                    // 若有多余的小数点和等号，则为无效算式
                    case Equal:
                    case Dot:
                        break;
                    case Brackets: {
                        int fromIndex = i;
                        int toIndex = CollectionUtils.indexOfRef(inputs, input.getPair(), fromIndex + 1);
                        if (toIndex < 0) {
                            break;
                        }

                        List<CharInput> subInputs = CollectionUtils.subList(inputs, fromIndex + 1, toIndex);
                        Double number = calculate(subInputs);
                        expr = Num.create(number);

                        i = toIndex;
                        break;
                    }
                    default:
                        expr = Op.create(type);
                }
            }

            if (expr == null) {
                return null;
            }

            exprs.add(expr);
        }

        return doCalculate(exprs);
    }

    private static Double doCalculate(List<Expr> exprs) {
        List<Expr> rpnExprs = toRPN(exprs);

        // 后缀表达式的计算过程：
        // https://zh.wikipedia.org/wiki/%E9%80%86%E6%B3%A2%E5%85%B0%E8%A1%A8%E7%A4%BA%E6%B3%95
        Stack<Num> values = new Stack<>();
        for (Expr expr : rpnExprs) {
            if (expr instanceof Num) {
                values.push((Num) expr);
            } else if (expr instanceof Op) {
                Op op = (Op) expr;
                List<Num> numbers = new ArrayList<>(op.args);

                for (int i = op.args; !values.isEmpty() && i > 0; i--) {
                    Num num = values.pop();
                    numbers.add(num);
                }

                if (numbers.size() != op.args) {
                    return null;
                } else {
                    Collections.reverse(numbers);

                    Double result = op.call(numbers);
                    if (result == null) {
                        return null;
                    }

                    values.add(Num.create(result));
                }
            }
        }

        return values.size() == 1 ? values.pop().value : null;
    }

    /**
     * 中缀表达式 转 后缀表达式：
     * <p/>
     * https://zh.wikipedia.org/wiki/%E8%B0%83%E5%BA%A6%E5%9C%BA%E7%AE%97%E6%B3%95
     */
    private static List<Expr> toRPN(List<Expr> exprs) {
        List<Expr> output = new ArrayList<>(exprs.size());
        Stack<Op> operators = new Stack<>();

        for (int i = 0; i < exprs.size(); i++) {
            Expr expr = exprs.get(i);

            if (expr instanceof Num) {
                output.add(expr);
            } else if (expr instanceof Op) {
                Op o1 = (Op) expr;
                Op o2;
                while ((o2 = !operators.isEmpty() ? operators.peek() : null) != null //
                       && o2.isPriorTo(o1)) {
                    operators.pop();
                    output.add(o2);
                }

                operators.push(o1);
            }
        }

        while (!operators.isEmpty()) {
            Op o2 = operators.pop();
            output.add(o2);
        }

        return output;
    }

    private static MathOpKey.Type getOpType(CharInput input) {
        Key key = input != null && input.getKeys().size() == 1 ? input.getFirstKey() : null;

        return key instanceof MathOpKey ? ((MathOpKey) key).type : null;
    }

    private static boolean isEqualOp(CharInput input) {
        return getOpType(input) == MathOpKey.Type.Equal;
    }

    private interface Expr {}

    private static class Num implements Expr {
        public final Double value;

        private Num(Double value) {this.value = value;}

        public static Num create(Double number) {
            return number != null ? new Num(number) : null;
        }

        public static Num create(CharInput input) {
            StringBuilder sb = new StringBuilder();

            boolean hasDot = false;
            List<Key> keys = input.getKeys();
            int keyCount = keys.size();
            for (int i = 0; i < keyCount; i++) {
                Key prevKey = i > 0 ? keys.get(i - 1) : null;
                Key key = keys.get(i);
                Key postKey = i < keyCount - 1 ? keys.get(i + 1) : null;

                // 第一个按键必须是 小数点 或 数字
                boolean isDotKey = MathOpKey.Type.Dot.match(key);
                if (prevKey == null //
                    && !(isDotKey || CharKey.Type.Number.match(key))) {
                    return null;
                }

                if (isDotKey) {
                    if (hasDot) {
                        return null;
                    }

                    hasDot = true;
                    // 小数点 的后继必须为数字，否则，无需添加 小数点
                    if (CharKey.Type.Number.match(postKey)) {
                        if (prevKey == null) {
                            sb.append("0");
                        }

                        sb.append(".");
                    }
                } else if (CharKey.Type.Number.match(key)) {
                    sb.append(key.value);
                } else {
                    return null;
                }
            }

            if (sb.length() == 0) {
                return null;
            }

            double number = Double.parseDouble(sb.toString());
            return create(number);
        }
    }

    private static class Op implements Expr {
        public final MathOpKey.Type type;
        /** 参数数量 */
        public final int args;
        /** 符号优先级 */
        public final int priority;
        /** 结合性 */
        public final Associativity associativity;

        private Op(MathOpKey.Type type, int args, int priority, Associativity associativity) {
            this.type = type;
            this.args = args;
            this.priority = priority;
            this.associativity = associativity;
        }

        public static Op create(MathOpKey.Type type) {
            int args = 0;
            int priority = 10;
            Associativity associativity = Associativity.left;

            switch (type) {
                case Plus:
                case Minus:
                    args = 2;
                    break;
                case Multiply:
                case Divide:
                    args = 2;
                    priority = 20;
                    break;
                case Percent:
                case Permill:
                case Permyriad:
                    args = 1;
                    priority = 30;
                    break;
            }
            return new Op(type, args, priority, associativity);
        }

        /** 当前运算符 是否 优先于 指定的运算符 */
        public boolean isPriorTo(Op other) {
            return (other.associativity == Associativity.left //
                    && other.priority <= this.priority) //
                   || (other.associativity == Associativity.right //
                       && other.priority < this.priority);
        }

        public Double call(List<Num> numbers) {
            if (numbers.size() != this.args) {
                return null;
            }

            Double result = null;
            Num first = numbers.get(0);
            Num second = numbers.size() > 1 ? numbers.get(1) : Num.create(0d);
            switch (this.type) {
                case Plus: {
                    result = first.value + second.value;
                    break;
                }
                case Minus: {
                    result = first.value - second.value;
                    break;
                }
                case Multiply: {
                    result = first.value * second.value;
                    break;
                }
                case Divide: {
                    if (second.value != 0) {
                        result = first.value / second.value;
                    }
                    break;
                }
                case Percent: {
                    result = first.value * 0.01;
                    break;
                }
                case Permill: {
                    result = first.value * 0.001;
                    break;
                }
                case Permyriad: {
                    result = first.value * 0.0001;
                    break;
                }
            }
            return result;
        }

        public enum Associativity {
            left,
            right,
        }
    }
}
