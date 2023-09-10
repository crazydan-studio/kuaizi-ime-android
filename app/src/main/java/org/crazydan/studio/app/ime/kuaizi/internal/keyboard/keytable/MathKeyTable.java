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
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.MathKeyboard;

/**
 * {@link MathKeyboard 数学键盘}按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class MathKeyTable extends KeyTable {

    public static MathKeyTable create(Config config) {
        return new MathKeyTable(config);
    }

    protected MathKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link MathKeyboard 数学键盘}按键 */
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
                ctrlKey(CtrlKey.Type.Math_Equal).setLabel("="),
                ctrlKey(CtrlKey.Type.Math_Dot).setLabel("."),
                ctrlKey(CtrlKey.Type.Math_Plus).setLabel("+"),
                ctrlKey(CtrlKey.Type.Math_Minus).setLabel("-"),
                ctrlKey(CtrlKey.Type.Math_Multiply).setLabel("×"),
                ctrlKey(CtrlKey.Type.Math_Divide).setLabel("÷"),
                ctrlKey(CtrlKey.Type.Math_Brackets).setLabel("( )"),
                ctrlKey(CtrlKey.Type.Math_Percent).setLabel("%"),
                };

        Key<?>[][] gridKeys = createEmptyGrid();

        int index_begin = getGridFirstColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.SwitchHandMode);
        gridKeys[1][index_end] = ctrlKey(CtrlKey.Type.LocateInputCursor).setColor(key_char_color);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Backspace);

        int dataIndex = 0;
        GridCoord[][] levelKeyCoords = getKeyCoords();
        for (GridCoord[] keyCoords : levelKeyCoords) {
            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                Key<?> key = keys[dataIndex++];
                KeyColor color = key.isNumber() ? key_char_color : key_char_special_color;

                key.setColor(color);
                gridKeys[row][column] = key;
            }
        }

        return gridKeys;
    }

    private GridCoord[][] getKeyCoords() {
        return new GridCoord[][] {
                // number
                new GridCoord[] {
                        coord(3, 5), coord(2, 6), coord(2, 5), coord(2, 4), coord(2, 3),
                        //
                        coord(4, 6), coord(4, 5), coord(4, 4), coord(4, 3), coord(3, 3),
                        },
                // expression
                new GridCoord[] {
                        coord(3, 4), coord(1, 5), coord(1, 4), coord(1, 3),
                        //
                        coord(0, 6), coord(0, 5), coord(0, 4), coord(0, 3),
                        },
                };
    }
}
