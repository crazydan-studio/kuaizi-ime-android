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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard;

import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.KeyFactory;
import org.crazydan.studio.app.ime.kuaizi.core.KeyboardContext;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable.LatinKeyTable;
import org.crazydan.studio.app.ime.kuaizi.dict.UserInputDataDict;

/**
 * {@link Type#Latin 拉丁文键盘}
 * <p/>
 * 含字母、数字，逐字直接录入目标输入组件
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-29
 */
public class LatinKeyboard extends DirectInputKeyboard {

    @Override
    public Type getType() {return Type.Latin;}

    @Override
    public boolean isMaster() {return true;}

    @Override
    public KeyFactory buildKeyFactory(KeyboardContext context) {
        KeyTableConfig keyTableConf = createKeyTableConfig(context);
        LatinKeyTable keyTable = LatinKeyTable.create(keyTableConf);

        return keyTable::createGrid;
    }

    @Override
    protected List<String> getTopBestMatchedLatins(KeyboardContext context, String text) {
        UserInputDataDict dict = context.dict.useUserInputDataDict();

        return dict.findTopBestMatchedLatins(text, 5);
    }
}
