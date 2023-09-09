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

package org.crazydan.studio.app.ime.kuaizi.internal.keyboard.keytable;

import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;

/**
 * 光标定位功能 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class LocatorKeyTable extends KeyTable {

    public static LocatorKeyTable create(Config config) {
        return new LocatorKeyTable(config);
    }

    protected LocatorKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建定位按键 */
    public Key<?>[][] createKeys() {
        Key<?>[][] gridKeys = createEmptyGrid();

        int index_end = getGridLastColumnIndex();

        gridKeys[2][2] = ctrlKey(CtrlKey.Type.LocateInputCursor_Selector);
        gridKeys[2][5] = ctrlKey(CtrlKey.Type.LocateInputCursor_Locator);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Backspace);

        gridKeys[3][3] = ctrlKey(CtrlKey.Type.Cut).setLabel("剪切");
        gridKeys[4][2] = ctrlKey(CtrlKey.Type.Redo).setLabel("重做");
        gridKeys[4][3] = ctrlKey(CtrlKey.Type.Undo).setLabel("撤销");
        gridKeys[4][4] = ctrlKey(CtrlKey.Type.Paste).setLabel("粘贴");
        gridKeys[4][5] = ctrlKey(CtrlKey.Type.Copy).setLabel("复制");

        return gridKeys;
    }
}
