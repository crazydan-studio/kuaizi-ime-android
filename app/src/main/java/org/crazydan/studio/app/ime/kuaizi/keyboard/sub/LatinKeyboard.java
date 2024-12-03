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

package org.crazydan.studio.app.ime.kuaizi.keyboard.sub;

import org.crazydan.studio.app.ime.kuaizi.keyboard.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.keyboard.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.keyboard.sub.keytable.LatinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.keyboard.msg.InputMsgListener;

/**
 * {@link Keyboard.Subtype#Latin 拉丁文键盘}
 * <p/>
 * 含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-29
 */
public class LatinKeyboard extends DirectInputKeyboard {

    public LatinKeyboard(InputMsgListener listener, Keyboard.Subtype prevType) {super(listener, prevType);}

    @Override
    public Keyboard.Subtype getType() {
        return Keyboard.Subtype.Latin;
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        LatinKeyTable keyTable = LatinKeyTable.create(createKeyTableConfig());

        return keyTable::createKeys;
    }
}
