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
public class CharInput extends Input {
    private CharInput pair;

    public static CharInput from(List<Key> keys) {
        CharInput input = new CharInput();

        input.replaceKeys(keys);

        return input;
    }

    public static CharInput from(Key... keys) {
        return from(Arrays.asList(keys));
    }

    // ======================= Start: 配对符号输入 ======================

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

    // ======================= End: 配对符号输入 ======================

    // ======================= Start: 拼音输入转换 ======================

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
        String keyText = key.value;

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
        String keyText = key.value;

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
        String keyValue = key.value;

        if (keyValue.startsWith("n")) {
            keyValue = "l" + keyValue.substring(1);
        } else if (keyValue.startsWith("l")) {
            keyValue = "n" + keyValue.substring(1);
        }

        replaceCharKeyText(key, 0, keyValue);
    }

    protected void replaceCharKeyText(CharKey key, int keyIndex, String keyValue) {
        CharKey newKey = CharKey.build((b) -> b.from(key).value(keyValue).label(keyValue));

        getKeys().set(keyIndex, newKey);
    }

    // ======================= End: 拼音输入转换 ======================
}
