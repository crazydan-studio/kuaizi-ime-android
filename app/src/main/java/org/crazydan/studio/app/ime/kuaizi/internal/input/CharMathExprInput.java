/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2023 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.internal.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.crazydan.studio.app.ime.kuaizi.internal.InputList;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 数学表达式输入
 * <p/>
 * 内嵌独立的{@link InputList 输入列表}，以单独处理表达式中的符号和数字
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-09-15
 */
public class CharMathExprInput extends CharInput {
    private final InputList inputList;

    public CharMathExprInput() {
        this(new InputList());
    }

    private CharMathExprInput(InputList inputList) {
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
        return this.inputList.getText();
    }

    @Override
    public CharInput copy() {
        return new CharMathExprInput(getInputList().copy());
    }

    @Override
    public boolean isMathExpr() {
        return true;
    }

    // <<<<<<<<< CharInput 接口覆盖
    @Override
    public boolean isLatin() {return false;}

    @Override
    public boolean isPinyin() {return false;}

    @Override
    public boolean isSymbol() {return false;}

    @Override
    public boolean isEmoji() {return false;}

    @Override
    public List<Key<?>> getKeys() {return new ArrayList<>();}

    @Override
    public Key<?> getFirstKey() {return null;}

    @Override
    public Key<?> getLastKey() {return null;}

    @Override
    public void appendKey(Key<?> key) {}

    @Override
    public void dropLastKey() {}

    @Override
    public void replaceKeyAfterLevel(Key.Level level, Key<?> newKey) {}

    @Override
    public void replaceLatestKey(Key<?> oldKey, Key<?> newKey) {}

    @Override
    public void replaceLastKey(Key<?> newKey) {}

    @Override
    public List<String> getChars() {return new ArrayList<>();}

    @Override
    public boolean isTextOnlyWordNotation(Option option) {return false;}

    @Override
    public boolean hasWord() {return false;}

    @Override
    public InputWord getWord() {return null;}

    @Override
    public void setWord(InputWord word) {}
    // >>>>>>>

    // <<<<<<<<< 相同性检查
    @Override
    public boolean isSameWith(Object o) {
        return equals(o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CharMathExprInput that = (CharMathExprInput) o;
        return this.inputList.equals(that.inputList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inputList);
    }
    // >>>>>>>>
}
