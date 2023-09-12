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
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.NumberKeyboard;

/**
 * {@link NumberKeyboard 数字键盘}按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class NumberKeyTable extends KeyTable {

    public static NumberKeyTable create(Config config) {
        return new NumberKeyTable(config);
    }

    protected NumberKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link NumberKeyboard 数字键盘}按键 */
    public Key<?>[][] createKeys() {
        Key<?>[] keys = new Key[] {
                numberKey("0"),
                numberKey("1"),
                numberKey("2"),
                numberKey("3"),
                numberKey("4"),
                numberKey("5"),
                numberKey("6"),
                numberKey("7"),
                numberKey("8"),
                numberKey("9"),
                symbolKey("."),
                symbolKey("+"),
                symbolKey("-"),
                symbolKey("*"),
                symbolKey("/"),
                symbolKey("#"),
                symbolKey("%"),
                symbolKey(","),
                symbolKey(";"),
                symbolKey(":"),
                };

        Key<?>[][] gridKeys = createEmptyGrid();

        int index_begin = getGridFirstColumnIndex();
        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.SwitchHandMode);
        gridKeys[4][index_begin] = ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);

        int dataIndex = 0;
        GridCoord[][] levelKeyCoords = getKeyCoords();
        for (GridCoord[] keyCoords : levelKeyCoords) {
            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                KeyColor color = key_char_color;
                Key<?> key = keys[dataIndex++].setColor(color);

                gridKeys[row][column] = key;
            }
        }

        return gridKeys;
    }

    private GridCoord[][] getKeyCoords() {
        return new GridCoord[][] {
                // level 1
                new GridCoord[] {
                        coord(3, 5), coord(2, 6), coord(2, 5), coord(2, 4), coord(2, 3),
                        //
                        coord(4, 6), coord(4, 5), coord(4, 4), coord(4, 3), coord(3, 3),
                        },
                // level 2
                new GridCoord[] {
                        coord(3, 1), coord(2, 1), coord(3, 0), coord(4, 1), coord(2, 2),
                        //
                        coord(3, 2), coord(4, 2), coord(1, 0), coord(1, 1), coord(1, 2),
                        },
                };
    }
}
