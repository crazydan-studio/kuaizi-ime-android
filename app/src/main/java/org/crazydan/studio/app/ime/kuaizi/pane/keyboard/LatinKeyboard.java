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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.pane.InputList;
import org.crazydan.studio.app.ime.kuaizi.pane.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable.LatinKeyTable;

/**
 * {@link Type#Latin 拉丁文键盘}
 * <p/>
 * 含字母、数字，逐字直接录入目标输入组件
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-29
 */
public class LatinKeyboard extends DirectInputKeyboard {
    private final PinyinDict dict;

    public LatinKeyboard(PinyinDict dict) {
        this.dict = dict;
    }

    @Override
    public Type getType() {
        return Type.Latin;
    }

    @Override
    public KeyFactory getKeyFactory(InputList inputList) {
        LatinKeyTable keyTable = LatinKeyTable.create(createKeyTableConfig(inputList));

        return keyTable::createKeys;
    }

    @Override
    protected List<String> getTopBestMatchedLatins(String text) {
        return this.dict.findTopBestMatchedLatins(text, 5);
    }
}
