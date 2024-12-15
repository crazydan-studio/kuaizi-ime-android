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

package org.crazydan.studio.app.ime.kuaizi.pane.keyboard.keytable;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.pane.msg.EditorEditAction;

/**
 * 编辑按键布局
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
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建定位按键 */
    public Key<?>[][] createKeys() {
        Key<?>[][] gridKeys = createEmptyGrid();

        int index_end = getGridLastColumnIndex();

        gridKeys[2][2] = ctrlKey(CtrlKey.Type.Editor_Range_Selector);
        gridKeys[2][5] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_end] = this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        gridKeys[3][3] = editCtrlKey(EditorEditAction.cut);
        gridKeys[4][2] = editCtrlKey(EditorEditAction.redo);
        gridKeys[4][3] = editCtrlKey(EditorEditAction.undo);
        gridKeys[4][4] = editCtrlKey(EditorEditAction.paste);
        gridKeys[4][5] = editCtrlKey(EditorEditAction.copy);
        gridKeys[5][3] = editCtrlKey(EditorEditAction.select_all);

        return gridKeys;
    }

    public CtrlKey editCtrlKey(EditorEditAction action) {
        CtrlKey.Type type = CtrlKey.Type.Edit_Editor;
        CtrlKey.Option<?> option = new CtrlKey.EditorEditOption(action);

        String label = null;
        switch (action) {
            case select_all:
                label = "全选";
                break;
            case copy:
                label = "复制";
                break;
            case paste:
                label = "粘贴";
                break;
            case cut:
                label = "剪切";
                break;
            case undo:
                label = "撤销";
                break;
            case redo:
                label = "重做";
                break;
        }

        return ctrlKey(type).setOption(option).setLabel(label);
    }
}
