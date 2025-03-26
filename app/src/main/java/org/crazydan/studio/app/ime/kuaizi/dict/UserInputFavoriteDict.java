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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;

/**
 * {@link InputFavorite} 字典
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-26
 */
public class UserInputFavoriteDict {
    private final SQLiteDatabase db;
    private final ThreadPoolExecutor executor;

    UserInputFavoriteDict(SQLiteDatabase db, ThreadPoolExecutor executor) {
        this.db = db;
        this.executor = executor;
    }

    public void save(InputFavorite favorite) {
    }

    public void updateUsage(InputFavorite favorite) {
    }

    public List<InputFavorite> getAll() {

    }

    public void remove(List<Integer> ids) {

    }

    public void clearAll() {

    }

    public boolean exists(String text) {

    }
}
