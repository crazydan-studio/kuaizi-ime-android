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

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.pane.Key;
import org.crazydan.studio.app.ime.kuaizi.pane.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.MathOpKey;
import org.crazydan.studio.app.ime.kuaizi.pane.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.pane.keyboard.MathKeyboard;

/**
 * {@link MathKeyboard} 按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class MathKeyTable extends KeyTable {

    protected MathKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static MathKeyTable create(KeyTableConfig config) {
        return new MathKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建 {@link MathKeyboard} 按键 */
    public Key[][] createKeys() {
        if (this.config.keyboard.xInputPadEnabled) {
            return createKeysForXPad();
        }

        Key[] keys = new Key[] {
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
                //
                mathOpKey(MathOpKey.Type.Dot),
                mathOpKey(MathOpKey.Type.Equal),
                //
                mathOpKey(MathOpKey.Type.Plus),
                mathOpKey(MathOpKey.Type.Multiply),
                mathOpKey(MathOpKey.Type.Brackets),
                mathOpKey(MathOpKey.Type.Permill),
                //
                mathOpKey(MathOpKey.Type.Minus),
                mathOpKey(MathOpKey.Type.Divide),
                mathOpKey(MathOpKey.Type.Percent),
                mathOpKey(MathOpKey.Type.Permyriad),
                };

        Key[][] gridKeys = createEmptyGrid();

        int index_begin = getGridFirstColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.Switch_HandMode);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_end] = this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);

        int dataIndex = 0;
        GridCoord[][] levelKeyCoords = getKeyCoords();
        for (GridCoord[] keyCoords : levelKeyCoords) {
            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                Key key = keys[dataIndex++];
                gridKeys[row][column] = key;
            }
        }

        return gridKeys;
    }

    @Override
    protected XPadKey createXPadKey() {
        return xPadKey(Keyboard.Type.Math, new Key[][][] {
                new Key[][] {
                        new Key[] {
                                null, mathOpKey(MathOpKey.Type.Dot), mathOpKey(MathOpKey.Type.Brackets),
                                }, //
                        new Key[] {
                                null, ctrlKey(CtrlKey.Type.Space), ctrlKey(CtrlKey.Type.Backspace),
                                },
                        }, //
                new Key[][] {
                        new Key[] { null, null, null, }, //
                        new Key[] { null, numberKey("0"), numberKey("9"), },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, mathOpKey(MathOpKey.Type.Equal), mathOpKey(MathOpKey.Type.Permyriad),
                                }, //
                        new Key[] { null, numberKey("1"), numberKey("2"), },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, mathOpKey(MathOpKey.Type.Percent), mathOpKey(MathOpKey.Type.Permill),
                                }, //
                        new Key[] {
                                null, numberKey("3"), numberKey("4"),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, mathOpKey(MathOpKey.Type.Multiply), mathOpKey(MathOpKey.Type.Divide),
                                }, //
                        new Key[] {
                                null, numberKey("5"), numberKey("6"),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, mathOpKey(MathOpKey.Type.Plus), mathOpKey(MathOpKey.Type.Minus),
                                }, //
                        new Key[] {
                                null, numberKey("7"), numberKey("8"),
                                },
                        },
                });
    }

    private GridCoord[][] getKeyCoords() {
        return new GridCoord[][] {
                // number
                new GridCoord[] {
                        coord(3, 5), coord(2, 6),
                        //
                        coord(2, 5), coord(2, 4),
                        //
                        coord(2, 3), coord(4, 6),
                        //
                        coord(4, 5), coord(4, 4),
                        //
                        coord(4, 3), coord(3, 3),
                        },
                // expression
                new GridCoord[] {
                        coord(3, 4), coord(1, 7),
                        //
                        coord(1, 5), coord(1, 4),
                        //
                        coord(1, 3), coord(1, 2),
                        //
                        coord(0, 6), coord(0, 5),
                        //
                        coord(0, 4), coord(0, 3),
                        },
                };
    }

    @Override
    public CharKey numberKey(String value) {
        return numberKey(value, (b) -> b.color(key_char_color));
    }

    public MathOpKey mathOpKey(MathOpKey.Type type) {
        return mathOpKey(type, MathOpKey.Builder.noop);
    }

    public static MathOpKey bracketKey(String value) {
        return mathOpKey(MathOpKey.Type.Brackets, (b) -> b.value(value).label(value));
    }

    private static MathOpKey mathOpKey(MathOpKey.Type type, Consumer<MathOpKey.Builder> c) {
        return MathOpKey.build((b) -> {
            b.type(type).color(key_char_special_color);
            c.accept(b);
        });
    }
}
