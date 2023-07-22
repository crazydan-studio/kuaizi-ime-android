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

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.Input;

/**
 * 字符{@link Input 输入}
 * <p/>
 * 任意可见字符的单次输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class CharInput extends BaseInput {
    private boolean emotion;

    public CharInput copy() {
        CharInput input = new CharInput();
        getKeys().forEach(input::appendKey);

        input.emotion = this.emotion;
        input.setWord(getWord());

        return input;
    }

    @Override
    public boolean isEmotion() {
        return this.emotion;
    }

    public CharInput setEmotion(boolean emotion) {
        this.emotion = emotion;
        return this;
    }

    /** 是否为拼音 平/翘舌 */
    public boolean isPinyinTongue() {
        List<String> chars = getChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("s") || ch.startsWith("c") || ch.startsWith("z");
    }

    /** 是否为拼音 前/后鼻韵 */
    public boolean isPinyinRhyme() {
        String s = String.join("", getChars());

        return s.endsWith("eng") || s.endsWith("ing") || s.endsWith("ang") //
               || s.endsWith("en") || s.endsWith("in") || s.endsWith("an");
    }

    /** 是否为拼音 n/l */
    public boolean isPinyinNL() {
        List<String> chars = getChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("n") || ch.startsWith("l");
    }
}
