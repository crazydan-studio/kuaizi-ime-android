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

import java.util.Arrays;
import java.util.List;

import org.crazydan.studio.app.ime.kuaizi.core.Key;
import org.crazydan.studio.app.ime.kuaizi.core.Keyboard;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.key.CtrlKey;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTable;
import org.crazydan.studio.app.ime.kuaizi.core.keyboard.KeyTableConfig;
import org.crazydan.studio.app.ime.kuaizi.dict.Symbol;
import org.crazydan.studio.app.ime.kuaizi.dict.SymbolGroup;

/**
 * {@link Keyboard.Type#Symbol} 和 {@link Keyboard.Type#Emoji} 的按键布局
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-08-31
 */
public class SymbolEmojiKeyTable extends KeyTable {

    protected SymbolEmojiKeyTable(KeyTableConfig config) {
        super(config);
    }

    public static SymbolEmojiKeyTable create(KeyTableConfig config) {
        return new SymbolEmojiKeyTable(config);
    }

    @Override
    protected Key[][] initGrid() {
        return new Key[6][8];
    }

    /** 表情符号按键的分页大小 */
    public int getEmojiGridPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建表情符号按键 */
    public Key[][] createEmojiGrid(
            List<String> groups, List<InputWord> words, String selectedGroup, int startIndex
    ) {
        Key[][] gridKeys = createEmptyGrid();

        int dataSize = words.size();
        int pageSize = getEmojiGridPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[4][0] = switcherCtrlKey(Keyboard.Type.Symbol);

        gridKeys[2][index_end] = this.config.charInputSelected
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);
        gridKeys[3][index_end] = this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        GridCoord[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < groups.size(); i++, j++) {
            GridCoord keyCoord = groupKeyCoords[i];
            String group = groups.get(j);
            boolean selected = group.equals(selectedGroup);

            CtrlKey.Option<String> option = new CtrlKey.Option<>(group);

            CtrlKey key = ctrlKey(CtrlKey.Type.Toggle_Emoji_Group,
                                  (b) -> b.option(option).label(group).disabled(selected));
            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }

        GridCoord[][] levelKeyCoords = getLevelKeyCoords();
        fillGridLevelKeysByCoord(gridKeys, levelKeyCoords, words, startIndex, this::inputWordKey);

        return gridKeys;
    }

    /** 标点符号按键的分页大小 */
    public int getSymbolGridPageSize() {
        return countGridSize(getLevelKeyCoords());
    }

    /** 创建标点符号按键 */
    public Key[][] createSymbolGrid(SymbolGroup symbolGroup, boolean onlyPair, int startIndex) {
        Key[][] gridKeys = createEmptyGrid();

        Symbol[] symbols = onlyPair ? Arrays.stream(symbolGroup.symbols)
                                            .filter(symbol -> symbol instanceof Symbol.Pair)
                                            .toArray(Symbol[]::new) : symbolGroup.symbols;

        int dataSize = symbols.length;
        int pageSize = getEmojiGridPageSize();
        int currentPage = dataSize == 0 ? 0 : startIndex / pageSize + 1;
        int totalPage = (int) Math.ceil(dataSize / (pageSize * 1.0));

        int index_mid = getGridMiddleColumnIndex();
        int index_end = getGridLastColumnIndex();

        gridKeys[0][0] = noopCtrlKey(currentPage + "/" + totalPage);
        gridKeys[3][0] = switcherCtrlKey(Keyboard.Type.Emoji);

        gridKeys[2][index_end] = this.config.charInputSelected
                                 ? ctrlKey(CtrlKey.Type.DropInput)
                                 : ctrlKey(CtrlKey.Type.Backspace);
        gridKeys[3][index_mid] = ctrlKey(CtrlKey.Type.Editor_Cursor_Locator);
        gridKeys[3][index_end] = this.config.hasInputs ? ctrlKey(CtrlKey.Type.Commit_InputList) : enterCtrlKey();
        gridKeys[4][index_end] = ctrlKey(CtrlKey.Type.Space);
        gridKeys[5][index_end] = ctrlKey(CtrlKey.Type.Exit);

        GridCoord[] groupKeyCoords = getGroupKeyCoords();
        for (int i = 0, j = 0; i < groupKeyCoords.length && j < SymbolGroup.values().length; i++, j++) {
            GridCoord keyCoord = groupKeyCoords[i];
            SymbolGroup group = SymbolGroup.values()[j];
            boolean selected = group == symbolGroup;

            CtrlKey.Option<SymbolGroup> option = new CtrlKey.Option<>(group);

            CtrlKey key = ctrlKey(CtrlKey.Type.Toggle_Symbol_Group,
                                  (b) -> b.option(option).label(group.name).disabled(selected));
            fillGridKeyByCoord(gridKeys, keyCoord, key);
        }

        GridCoord[][] levelKeyCoords = getLevelKeyCoords();
        fillGridLevelKeysByCoord(gridKeys, levelKeyCoords, List.of(symbols), startIndex, this::symbolKey);

        return gridKeys;
    }

    private GridCoord[] getGroupKeyCoords() {
        return new GridCoord[] {
                coord(1, 7), coord(0, 7),
                //
                coord(0, 6), coord(0, 5),
                //
                coord(0, 4), coord(0, 3),
                //
                coord(0, 2), coord(0, 1),
                //
                coord(1, 0), coord(2, 0),
                };
    }
}
