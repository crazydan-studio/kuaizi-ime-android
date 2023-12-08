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

package org.crazydan.studio.app.ime.kuaizi.core.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.Input;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;

/**
 * 字符{@link Input 输入}
 * <p/>
 * 任意可见字符的单次输入
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-06
 */
public class CharInput extends BaseInput<CharInput> {
    private CharInput pair;

    /** 输入补全 */
    private List<CompletionInput> completions = new ArrayList<>();

    public static CharInput from(List<Key<?>> keys) {
        CharInput input = new CharInput();

        input.replaceKeys(keys);

        return input;
    }

    public static CharInput from(Key<?>... keys) {
        return from(Arrays.asList(keys));
    }

    public CharInput getPair() {
        return this.pair;
    }

    public void setPair(CharInput pair) {
        if (pair == null) {
            clearPair();
            return;
        }

        this.pair = pair;
        // 同步设置对端关联
        this.pair.pair = this;
    }

    public void clearPair() {
        if (this.pair != null) {
            this.pair.pair = null;
        }
        this.pair = null;
    }

    public boolean hasPair() {
        return this.pair != null;
    }

    public List<CompletionInput> getCompletions() {
        return this.completions;
    }

    public void addCompletion(CompletionInput completion) {
        this.completions.add(completion);
    }

    public boolean hasCompletions() {
        return !this.completions.isEmpty();
    }

    public void clearCompletions() {
        this.completions = new ArrayList<>();
    }

    public void applyCompletion(CompletionInput completion) {
        CharInput input = completion.inputs.get(0);

        replaceKeys(input.getKeys());
        setWord(input.getWord());
    }

    @Override
    public void confirm() {
        clearCompletions();
    }

    /** 是否为拼音 平/翘舌 开头 */
    public boolean is_Pinyin_SCZ_Starting() {
        List<String> chars = getChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("s") || ch.startsWith("c") || ch.startsWith("z");
    }

    /** 切换拼音输入的平翘舌 */
    public void toggle_Pinyin_SCZ_Starting() {
        CharKey key = (CharKey) getFirstKey();
        String keyText = key.getText();

        if (keyText.startsWith("sh") || keyText.startsWith("ch") || keyText.startsWith("zh")) {
            keyText = keyText.charAt(0) + keyText.substring(2);
        } else if (keyText.startsWith("s") || keyText.startsWith("c") || keyText.startsWith("z")) {
            keyText = keyText.charAt(0) + "h" + keyText.substring(1);
        }

        replaceCharKeyText(key, 0, keyText);
    }

    /** 是否为拼音 前/后鼻韵 */
    public boolean is_Pinyin_NG_Ending() {
        List<String> chars = getChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(chars.size() - 1);
        return ch.endsWith("eng") || ch.endsWith("ing") || ch.endsWith("ang") //
               || ch.endsWith("en") || ch.endsWith("in") || ch.endsWith("an");
    }

    /** 切换拼音输入的前/后鼻韵 */
    public void toggle_Pinyin_NG_Ending() {
        CharKey key = (CharKey) getLastKey();
        String keyText = key.getText();

        if (keyText.endsWith("eng") || keyText.endsWith("ing") || keyText.endsWith("ang")) {
            keyText = keyText.substring(0, keyText.length() - 1);
        } else if (keyText.endsWith("en") || keyText.endsWith("in") || keyText.endsWith("an")) {
            keyText += "g";
        }

        replaceCharKeyText(key, getKeys().size() - 1, keyText);
    }

    /** 是否为拼音 n/l 开头 */
    public boolean is_Pinyin_NL_Starting() {
        List<String> chars = getChars();
        if (chars.isEmpty()) {
            return false;
        }

        String ch = chars.get(0);
        return ch.startsWith("n") || ch.startsWith("l");
    }

    /** 切换拼音输入的 n/l */
    public void toggle_Pinyin_NL_Starting() {
        CharKey key = (CharKey) getFirstKey();
        String keyText = key.getText();

        if (keyText.startsWith("n")) {
            keyText = "l" + keyText.substring(1);
        } else if (keyText.startsWith("l")) {
            keyText = "n" + keyText.substring(1);
        }

        replaceCharKeyText(key, 0, keyText);
    }

    protected void replaceCharKeyText(CharKey key, int keyIndex, String keyText) {
        getKeys().remove(keyIndex);
        getKeys().add(keyIndex, CharKey.create(key.getType(), keyText).setLabel(keyText));
    }
}
