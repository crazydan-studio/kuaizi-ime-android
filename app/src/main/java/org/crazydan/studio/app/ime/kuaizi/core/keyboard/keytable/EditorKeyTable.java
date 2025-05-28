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
    public Key[][] createGrid() {
        Key[][] gridKeys = createEmptyGrid();

        int index_end = getGridLastColumnIndex();

        if (this.config.hasInputs) {
            gridKeys[1][index_end] = ctrlKey(CtrlKey.Type.Commit_InputList);
        }

        gridKeys[2][2] = ctrlKey(CtrlKey.Type.Editor_Range_Selector);
        gridKeys[2][5] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_end] = enterCtrlKey();
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

    public Key[][] createGrid(CtrlKey.Type type) {
        Key[][] gridKeys = createEmptyGrid();

        int index_mid = getGridMiddleColumnIndex();

        gridKeys[3][index_mid] = ctrlKey(type);

        return gridKeys;
    }

    public CtrlKey editCtrlKey(EditorAction action) {
        String label = action.label;
        CtrlKey.Option<EditorAction> option = new CtrlKey.Option<>(action);

        return ctrlKey(CtrlKey.Type.Edit_Editor, (b) -> b.option(option).label(label));
    }
}
