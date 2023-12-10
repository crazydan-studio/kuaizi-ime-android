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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.LatinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.msg.InputMsgListener;

/**
 * {@link Keyboard.Type#Latin 拉丁文键盘}
 * <p/>
 * 含字母、数字和英文标点（在内部切换按键），逐字直接录入目标输入组件
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-29
 */
public class LatinKeyboard extends DirectInputKeyboard {

    public LatinKeyboard(InputMsgListener listener, Type prevType) {super(listener, prevType);}

    @Override
    public Type getType() {
        return Type.Latin;
    }

    @Override
    protected KeyFactory doGetKeyFactory() {
        LatinKeyTable keyTable = LatinKeyTable.create(createKeyTableConfig());

        return keyTable::createKeys;
    }
}
