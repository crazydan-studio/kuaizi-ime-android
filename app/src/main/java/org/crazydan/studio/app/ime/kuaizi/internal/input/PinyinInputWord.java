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

import java.util.HashMap;
import java.util.Map;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;

/**
 * 拼音输入{@link InputWord 候选字}
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-26
 */
public class PinyinInputWord extends InputWord {
    /** 拼音字母组合 id */
    private final String charsId;
    /** 是否繁体 */
    private final boolean traditional;
    /** 笔画顺序 */
    private final Map<String, Integer> strokes;

    public PinyinInputWord(
            String uid, String value, String notation, String charsId, boolean traditional, String strokeOrder
    ) {
        super(uid, value, notation);
        this.charsId = charsId;
        this.traditional = traditional;
        this.strokes = new HashMap<>();

        if (strokeOrder != null) {
            for (int i = 0; i < strokeOrder.length(); i++) {
                String stroke = strokeOrder.charAt(i) + "";
                int count = this.strokes.getOrDefault(stroke, 0);
                this.strokes.put(stroke, count + 1);
            }
        }
    }

    public String getCharsId() {
        return this.charsId;
    }

    public boolean isTraditional() {
        return this.traditional;
    }

    public Map<String, Integer> getStrokes() {
        return this.strokes;
    }

    public static String[] getStrokeNames() {
        return new String[] { "一", "丨", "丿", "㇏", "\uD840\uDCCB" };
    }

    public static String getStrokeCode(String stroke) {
        String code = null;
        switch (stroke) {
            case "一":
                code = "1";
                break;
            case "丨":
                code = "2";
                break;
            case "丿":
                code = "3";
                break;
            case "㇏":
                code = "4";
                break;
            // 𠃋
            case "\uD840\uDCCB":
                code = "5";
                break;
        }
        return code;
    }
}
