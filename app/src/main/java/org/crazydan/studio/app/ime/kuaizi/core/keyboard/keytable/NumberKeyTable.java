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

import java.util.function.Consumer;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.key.CharKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.key.XPadKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.NumberKeyboard;

/**
 * {@link Keyboard.Type#Number} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class NumberKeyTable extends KeyTable {

    protected NumberKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static NumberKeyTable create(KeyTableConfig config) {
        return new NumberKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 创建{@link NumberKeyboard 数字键盘}按键 */
    public Key[][] createGrid(boolean showExitKey) {
        if (this.config.xInputPadEnabled) {
            return createXPadGrid();
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

        Key[][] gridKeys = createEmptyGrid();

        int index_begin = getGridFirstColumnIndex();
        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][index_begin] = ctrlKey(CtrlKey.Type.Switch_HandMode);
        gridKeys[4][index_begin] = switcherCtrlKey(Keyboard.Type.Symbol);

        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);
        gridKeys[3][index_end] = enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);

        if (showExitKey) {
            gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);
        }

        GridCoord[][] keyCoords = getKeyCoords();
        fillGridLevelKeysByCoord(gridKeys, keyCoords, keys);

        return gridKeys;
    }

    @Override
    protected XPadKey createXPadKey() {
        return xPadKey(Keyboard.Type.Number, new Key[][][] {
                new Key[][] {
                        new Key[] {
                                null, symbolKey("."), symbolKey("#"),
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
                                null, symbolKey(":"), symbolKey(";"),
                                }, //
                        new Key[] { null, numberKey("1"), numberKey("2"), },
                        }, //
                new Key[][] {
                        new Key[] { null, symbolKey(","), symbolKey("%"), }, //
                        new Key[] {
                                null, numberKey("3"), numberKey("4"),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, symbolKey("*"), symbolKey("/"),
                                }, //
                        new Key[] {
                                null, numberKey("5"), numberKey("6"),
                                },
                        }, //
                new Key[][] {
                        new Key[] {
                                null, symbolKey("+"), symbolKey("-"),
                                }, //
                        new Key[] {
                                null, numberKey("7"), numberKey("8"),
                                },
                        },
                });
    }

    private GridCoord[][] getKeyCoords() {
        return new GridCoord[][] {
                // level 1
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
                // level 2
                new GridCoord[] {
                        coord(3, 1), coord(2, 1),
                        //
                        coord(3, 0), coord(4, 1),
                        //
                        coord(2, 2), coord(3, 2),
                        //
                        coord(4, 2), coord(1, 0),
                        //
                        coord(1, 1), coord(1, 2),
                        },
                };
    }

    @Override
    public CharKey numberKey(String value, Consumer<CharKey.Builder> c) {
        return super.numberKey(value, (b) -> {
            b.color(key_char_color);
            c.accept(b);
        });
    }

    @Override
    public CharKey symbolKey(String value, Consumer<CharKey.Builder> c) {
        return super.symbolKey(value, (b) -> {
            b.color(key_char_color);
            c.accept(b);
        });
    }
}
