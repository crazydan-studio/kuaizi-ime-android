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

package org.crazydan.studio.app.ime.kuaizi.pane.key;

import java.util.Objects;

import androidx.annotation.NonNull;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;

/**
 * {@link Symbol 标点符号}按键
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-29
 */
public class SymbolKey extends BaseKey<SymbolKey> {
    private final Symbol symbol;

    private SymbolKey(Symbol symbol) {
        this.symbol = symbol;
    }

    public static SymbolKey create(Symbol symbol) {
        return new SymbolKey(symbol);
    }

    public Symbol getSymbol() {
        return this.symbol;
    }

    public boolean isPair() {
        return this.symbol instanceof Symbol.Pair;
    }

    @Override
    public boolean isSymbol() {
        return true;
    }

    @Override
    public String getText() {
        return this.symbol.text;
    }

    @Override
    public boolean isSameWith(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        SymbolKey that = (SymbolKey) o;
        return this.symbol.equals(that.symbol);
    }

    @NonNull
    @Override
    public String toString() {
        return this.symbol.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SymbolKey that = (SymbolKey) o;
        return this.symbol.equals(that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.symbol);
    }
}
