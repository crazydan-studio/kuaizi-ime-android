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

package org.crazydan.studio.app.ime.kuaizi.internal.key;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;

/**
 * 可输入字符{@link Key 按键}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-06-28
 */
public class CharKey implements Key {
    public enum Type {
        /** 字母按键 */
        Alphabet,
        /** 数字按键 */
        Number,
        /** 标点符号按键 */
        Punctuation,
        /** 颜文字按键 */
        Emotion,
    }

    public final Type type;
    public final String text;

    public static CharKey alphabet(String text) {
        return new CharKey(Type.Alphabet, text);
    }

    public static CharKey number(String text) {
        return new CharKey(Type.Number, text);
    }

    public static CharKey punctuation(String text) {
        return new CharKey(Type.Punctuation, text);
    }

    public static CharKey emotion(String text) {
        return new CharKey(Type.Emotion, text);
    }

    private CharKey(Type type, String text) {
        this.type = type;
        this.text = text;
    }
}
