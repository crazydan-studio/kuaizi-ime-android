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

import android.graphics.Point;
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

        int index_begin = getGridFirstColumnIndexForHandMode();
        int index_mid = getGridMiddleColumnIndexForHandMode();
        int index_end = getGridLastColumnIndexForHandMode();

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.SwitchHandMode);
        gridKeys[1][index_end] = ctrlKey(CtrlKey.Type.SwitchToSymbolKeyboard);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Backspace);

        int dataIndex = 0;
        Point[][] levelKeyCoords = getLevelKeyCoords();
        for (int level = 0; level < levelKeyCoords.length; level++) {
            Point[] keyCoords = levelKeyCoords[level];

            for (Point keyCoord : keyCoords) {
                int x = keyCoord.x;
                int y = keyCoord.y;

                KeyColor color = latin_key_char_alphabet_level_colors[level];
                Key<?> key = keys[dataIndex++].setColor(color);

                gridKeys[x][y] = key;
            }
        }

        return gridKeys;
    }

    private Point[][] getLevelKeyCoords() {
        if (this.config.isLeftHandMode()) {
            return new Point[][] {
                    // level 1
                    new Point[] {
                            point(3, 2), point(2, 2), point(2, 3), point(2, 4), point(2, 5),
                            //
                            point(4, 2), point(4, 3), point(4, 4), point(4, 5), point(3, 4),
                            },
                    // level 2
                    new Point[] {
                            point(3, 6), point(2, 7), point(3, 7), point(4, 7), point(2, 6),
                            //
                            point(3, 5), point(4, 6), point(1, 7), point(1, 6), point(1, 5),
                            },
                    };
        }
        return new Point[][] {
                // level 1
                new Point[] {
                        point(3, 5), point(2, 6), point(2, 5), point(2, 4), point(2, 3),
                        //
                        point(4, 6), point(4, 5), point(4, 4), point(4, 3), point(3, 3),
                        },
                // level 2
                new Point[] {
                        point(3, 1), point(2, 1), point(3, 0), point(4, 1), point(2, 2),
                        //
                        point(3, 2), point(4, 2), point(1, 0), point(1, 1), point(1, 2),
                        },
                };
    }
}