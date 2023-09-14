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

import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.KeyColor;
import org.crazydan.studio.app.ime.kuaizi.internal.Symbol;
import org.crazydan.studio.app.ime.kuaizi.internal.data.SymbolGroup;
import org.crazydan.studio.app.ime.kuaizi.internal.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.InputWordKey;
import org.crazydan.studio.app.ime.kuaizi.internal.key.SymbolKey;
import org.crazydan.studio.app.ime.kuaizi.internal.keyboard.KeyTable;

/**
 * 表情、标点符号按键表
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class SymbolEmojiKeyTable extends KeyTable {

    public static SymbolEmojiKeyTable create(Config config) {
        return new SymbolEmojiKeyTable(config);
    }

    protected SymbolEmojiKeyTable(Config config) {
        super(config);
    }

    @Override
    protected Key<?>[][] initGrid() {
        return new Key[6][8];
    }

    /** 表情符号按键的分页大小 */
    public int getEmojiKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建表情符号按键 */
    public Key<?>[][] createEmojiKeys(
            List<String> groups, List<InputWord> words, String selectedGroup, int startIndex
    ) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getEmojiKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);

        gridKeys[2][index_end] = this.config.isCharInputSelected()
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        GridCoord[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < groups.size(); i++, j++) {
            GridCoord keyCoord = groupKeyCoords[i];
            String group = groups.get(j);
            boolean selected = group.equals(selectedGroup);

            int row = keyCoord.row;
            int column = keyCoord.column;
            CtrlKey.Option<?> option = new CtrlKey.TextOption(group);

            gridKeys[row][column] = ctrlKey(CtrlKey.Type.Toggle_Emoji_Group).setOption(option)
                                                                            .setLabel(group)
                                                                            .setDisabled(selected);
        }

        int dataIndex = startIndex;
        GridCoord[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            GridCoord[] keyCoords = levelKeyCoords[level];

            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                if (dataIndex < dataSize) {
                    InputWord word = words.get(dataIndex);

                    if (word != null) {
                        KeyColor color = key_input_word_level_colors[level];
                        InputWordKey key = InputWordKey.create(word).setColor(color);

                        gridKeys[row][column] = key;
                    }
                } else {
                    break;
                }

                dataIndex += 1;
            }
        }

        return gridKeys;
    }

    /** 标点符号按键的分页大小 */
    public int getSymbolKeysPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建标点符号按键 */
    public Key<?>[][] createSymbolKeys(SymbolGroup symbolGroup, boolean onlyPair, int startIndex) {
        Key<?>[][] gridKeys = createEmptyGrid();

        Symbol[] symbols = onlyPair ? Arrays.stream(symbolGroup.symbols)
                                            .filter(symbol -> symbol instanceof Symbol.Pair)
                                            .toArray(Symbol[]::new) : symbolGroup.symbols;

        int dataSize = symbols.length;
        int pageSize = getEmojiKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);

        gridKeys[2][index_end] = this.config.isCharInputSelected()
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        GridCoord[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < SymbolGroup.values().length; i++, j++) {
            GridCoord keyCoord = groupKeyCoords[i];
            String group = SymbolGroup.values()[j].name;
            boolean selected = group.equals(symbolGroup.name);

            int row = keyCoord.row;
            int column = keyCoord.column;
            CtrlKey.Option<?> option = new CtrlKey.SymbolGroupOption(SymbolGroup.values()[j]);

            gridKeys[row][column] = ctrlKey(CtrlKey.Type.Toggle_Symbol_Group).setOption(option)
                                                                             .setLabel(group)
                                                                             .setDisabled(selected);
        }

        int dataIndex = startIndex;
        GridCoord[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            GridCoord[] keyCoords = levelKeyCoords[level];

            for (GridCoord keyCoord : keyCoords) {
                int row = keyCoord.row;
                int column = keyCoord.column;

                if (dataIndex < dataSize) {
                    Symbol data = symbols[dataIndex];

                    if (data != null) {
                        KeyColor color = key_input_word_level_colors[level];

                        SymbolKey key = SymbolKey.create(data).setLabel(data.text).setColor(color);
                        gridKeys[row][column] = key;
                    }
                } else {
                    break;
                }

                dataIndex += 1;
            }
        }

        return gridKeys;
    }

    private GridCoord[] getGroupKeyCoords() {
        return new GridCoord[] {
                coord(1, 7), coord(0, 7), coord(0, 6), coord(0, 5), coord(0, 4),
                //
                coord(0, 3), coord(0, 2), coord(0, 1), coord(1, 0), coord(2, 0),
                };
    }

    private GridCoord[][] getLevelKeyCoords() {
        return new GridCoord[][] {
                // level 1
                new GridCoord[] {
                        coord(1, 6), coord(1, 5), coord(1, 4), coord(1, 3), coord(1, 2), coord(1, 1),
                        },
                // level 2
                new GridCoord[] {
                        coord(2, 6), coord(2, 5), coord(2, 4), coord(2, 3), coord(2, 2), coord(2, 1),
                        },
                // level 3
                new GridCoord[] {
                        coord(3, 6), coord(3, 5), coord(3, 3), coord(3, 2), coord(3, 1),
                        },
                // level 4
                new GridCoord[] {
                        coord(4, 6), coord(4, 5), coord(4, 4), coord(4, 3), coord(4, 2), coord(4, 1),
                        },
                // level 5
                new GridCoord[] {
                        coord(5, 6), coord(5, 5), coord(5, 4), coord(5, 3), coord(5, 2), coord(5, 1),
                        },
                };
    }
}
