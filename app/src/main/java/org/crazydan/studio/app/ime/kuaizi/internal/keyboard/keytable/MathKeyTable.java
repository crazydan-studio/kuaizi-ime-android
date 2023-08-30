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
    public Key<?>[][] createKeys(Key<?>[] keys) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int index_3 = getIndexForHandMode(3);
        int index_begin = getGridFirstColumnIndexForHandMode();
        int index_end = getGridLastColumnIndexForHandMode();

        gridKeys[3][index_3] = ctrlKey(CtrlKey.Type.Math_Equal).setLabel("=");

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.SwitchHandMode);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Backspace);

        int keyIndex = 0;
        Point[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length; level++) {
            Point[] keyCoords = levelKeyCoords[level];

            for (Point keyCoord : keyCoords) {
                int x = keyCoord.x;
                int y = keyCoord.y;

                if (keyIndex < keys.length) {
                    Key<?> key = keys[keyIndex];

                    KeyColor color = key_char_around_level_colors[level];
                    key.setColor(color);

                    gridKeys[x][y] = key;
                } else {
                    break;
                }

                keyIndex += 1;
            }
        }

        return gridKeys;
    }

    private Point[][] getLevelKeyCoords() {
        return new Point[][] {
                // level 1
                new Point[] {
                        coord(2, 4), coord(3, 4), coord(4, 4), coord(4, 3),
                        //
                        coord(3, 2), coord(2, 3), coord(1, 3),
                        },
                // level 2
                new Point[] {
                        coord(0, 4), coord(1, 4), coord(2, 5), coord(3, 5),
                        //
                        coord(4, 5), coord(4, 2), coord(3, 1), coord(2, 2),
                        //
                        coord(1, 2), coord(0, 3),
                        },
                };
    }
}
