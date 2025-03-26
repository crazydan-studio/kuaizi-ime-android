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

package org.crazydan.studio.app.ime.kuaizi.dict.upgrade;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDictDBType;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.vacuumSQLite;
import static org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v0.initFavoriteTables;

/**
 * 从 v3 版本升级到 v4 版本
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-26
 */
public class From_v3_to_v4 extends Upgrader {

    @Override
    public void upgrade(Context context, PinyinDict dict) {
        File userDBFile = dict.getDBFile(context, PinyinDictDBType.user);

        try (SQLiteDatabase targetDB = openSQLite(userDBFile, false)) {
            initFavoriteTables(targetDB);

            vacuumSQLite(targetDB);
        }
    }
}
