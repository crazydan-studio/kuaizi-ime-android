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

import java.util.List;

import android.graphics.Point;
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
        int size = 0;
        for (Point[] level : getLevelKeyCoords()) {
            size += level.length;
        }
        return size;
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

        int index_mid = getGridMiddleColumnIndexForHandMode();
        int index_end = getGridLastColumnIndexForHandMode();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = this.config.isCharInputSelected()
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);

        Point[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < groups.size(); i++, j++) {
            Point keyCoord = groupKeyCoords[i];
            String group = groups.get(j);
            boolean selected = group.equals(selectedGroup);

            int x = keyCoord.x;
            int y = keyCoord.y;
            CtrlKey.Option<?> option = new CtrlKey.TextOption(group);

            gridKeys[x][y] = ctrlKey(CtrlKey.Type.Toggle_Emoji_Group).setOption(option)
                                                                     .setLabel(group)
                                                                     .setDisabled(selected);
        }

        int dataIndex = startIndex;
        Point[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            Point[] keyCoords = levelKeyCoords[level];

            for (Point keyCoord : keyCoords) {
                int x = keyCoord.x;
                int y = keyCoord.y;

                if (dataIndex < dataSize) {
                    InputWord word = words.get(dataIndex);

                    if (word != null) {
                        KeyColor color = key_char_emoji_color;
                        InputWordKey key = InputWordKey.create(word).setColor(color);

                        gridKeys[x][y] = key;
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
        int size = 0;
        for (Point[] level : getLevelKeyCoords()) {
            size += level.length;
        }
        return size;
    }

    /** 创建标点符号按键 */
    public Key<?>[][] createSymbolKeys(SymbolGroup symbolGroup, int startIndex) {
        Key<?>[][] gridKeys = createEmptyGrid();

        int dataSize = symbolGroup.symbols.length;
        int pageSize = getEmojiKeysPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_mid = getGridMiddleColumnIndexForHandMode();
        int index_end = getGridLastColumnIndexForHandMode();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[2][index_end] = ctrlKey(CtrlKey.Type.Exit);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.LocateInputCursor);
        gridKeys[3][index_end] = this.config.hasInputs() ? ctrlKey(CtrlKey.Type.CommitInputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = this.config.isCharInputSelected()
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);

        Point[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < SymbolGroup.values().length; i++, j++) {
            Point keyCoord = groupKeyCoords[i];
            String group = SymbolGroup.values()[j].name;
            boolean selected = group.equals(symbolGroup.name);

            int x = keyCoord.x;
            int y = keyCoord.y;
            CtrlKey.Option<?> option = new CtrlKey.SymbolGroupOption(SymbolGroup.values()[j]);

            gridKeys[x][y] = ctrlKey(CtrlKey.Type.Toggle_Symbol_Group).setOption(option)
                                                                      .setLabel(group)
                                                                      .setDisabled(selected);
        }

        int dataIndex = startIndex;
        Point[][] levelKeyCoords = getLevelKeyCoords();

        for (int level = 0; level < levelKeyCoords.length && dataSize > 0; level++) {
            Point[] keyCoords = levelKeyCoords[level];

            for (Point keyCoord : keyCoords) {
                int x = keyCoord.x;
                int y = keyCoord.y;

                if (dataIndex < dataSize) {
                    Symbol data = symbolGroup.symbols[dataIndex];

                    if (data != null) {
                        KeyColor color = latin_key_char_alphabet_level_colors[level];

                        SymbolKey key = SymbolKey.create(data).setLabel(data.text).setColor(color);
                        gridKeys[x][y] = key;
                    }
                } else {
                    break;
                }

                dataIndex += 1;
            }
        }

        return gridKeys;
    }

    private Point[] getGroupKeyCoords() {
        if (this.config.isLeftHandMode()) {
            return new Point[] {
                    point(1, 0), point(0, 1), point(0, 2), point(0, 3), point(0, 4),
                    //
                    point(0, 5), point(0, 6), point(0, 7), point(1, 7), point(2, 7),
                    };
        }

        return new Point[] {
                point(1, 7), point(0, 7), point(0, 6), point(0, 5), point(0, 4),
                //
                point(0, 3), point(0, 2), point(0, 1), point(1, 0), point(2, 0),
                };
    }

    private Point[][] getLevelKeyCoords() {
        return new Point[][] {
                // level 1
                new Point[] {
                        coord(1, 6), coord(1, 5), coord(1, 4), coord(1, 3), coord(1, 2), coord(1, 1),
                        },
                // level 2
                new Point[] {
                        coord(2, 6), coord(2, 5), coord(2, 4), coord(2, 3), coord(2, 2), coord(2, 1),
                        },
                // level 3
                new Point[] {
                        coord(3, 6), coord(3, 5), coord(3, 3), coord(3, 2), coord(3, 1),
                        },
                // level 4
                new Point[] {
                        coord(4, 6), coord(4, 5), coord(4, 4), coord(4, 3), coord(4, 2), coord(4, 1),
                        },
                // level 5
                new Point[] {
                        coord(5, 6), coord(5, 5), coord(5, 4), coord(5, 3), coord(5, 2), coord(5, 1),
                        },
                };
    }
}
