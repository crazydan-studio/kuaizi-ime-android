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

package org.crazydan.studio.app.ime.kuaizi.core.keyboard.keytable;

import org.crazydan.studio.app.ime.kuaizi.common.widget.EditorAction;
import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;

/**
 * {@link Keyboard.Type#Editor} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class EditorKeyTable extends KeyTable {

    protected EditorKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static EditorKeyTable create(KeyTableConfig config) {
        return new EditorKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建定位按键 */
    public Key[][] createKeys() {
        Key[][] gridKeys = createEmptyGrid();

        int index_end = getGridLastColumnIndex();

        gridKeys[2][2] = ctrlKey(CtrlKey.Type.Editor_Range_Selector);
        gridKeys[2][5] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_end] = this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        gridKeys[3][3] = editCtrlKey(EditorAction.cut);
        gridKeys[4][2] = editCtrlKey(EditorAction.redo);
        gridKeys[4][3] = editCtrlKey(EditorAction.undo);
        gridKeys[4][4] = editCtrlKey(EditorAction.paste);
        gridKeys[4][5] = editCtrlKey(EditorAction.copy);
        gridKeys[5][3] = editCtrlKey(EditorAction.select_all);

        return gridKeys;
    }

    public CtrlKey editCtrlKey(EditorAction action) {
        String label = action.label;
        CtrlKey.Option<EditorAction> option = new CtrlKey.Option<>(action);

        return ctrlKey(CtrlKey.Type.Edit_Editor, (b) -> b.option(option).label(label));
    }
}
