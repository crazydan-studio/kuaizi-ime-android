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

package org.crazydan.studio.app.ime.kuaizi.dict.db;

import java.util.Date;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputFavorite;
import org.crazydan.studio.app.ime.kuaizi.core.input.InputTextType;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.createSQLiteArgHolders;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-26
 */
public class UserInputFavoriteDBHelper {

    /** 新增 {@link InputFavorite} */
    public static InputFavorite saveInputFavorite(SQLiteDatabase db, InputFavorite favorite) {
        String clause = "insert into meta_favorite ("
                        + "   type_, text_, html_, shortcut_, created_at_,"
                        + "   used_count_, used_at_"
                        + " ) values (?, ?, ?, ?, ?, 0, 0)";

        execSQLite(db, clause, new Object[] {
                favorite.type, favorite.text, favorite.html, favorite.shortcut, favorite.createdAt.getTime()
        });

        List<InputFavorite> result = queryInputFavorites(db, favorite.text);
        return result.get(0);
    }

    /** 更新 {@link InputFavorite} 的使用情况 */
    public static InputFavorite updateInputFavoriteUsage(SQLiteDatabase db, InputFavorite favorite) {
        String clause = "update meta_favorite set used_count_ = ?, used_at_ = ? where id_ = ?";

        InputFavorite newFavorite = favorite.copy((b) -> b.usedCount(favorite.usedCount + 1).usedAt(new Date()));

        execSQLite(db, clause, new Object[] { newFavorite.usedCount, newFavorite.usedAt.getTime(), newFavorite.id });

        return newFavorite;
    }

    /** 获取全部的 {@link InputFavorite}，并按创建时间、最近使用、使用次数降序排序 */
    public static List<InputFavorite> getAllInputFavorites(SQLiteDatabase db) {
        return queryInputFavorites(db, null);
    }

    /** 删除指定的 {@link InputFavorite} */
    public static void removeInputFavorites(SQLiteDatabase db, List<Integer> ids) {
        String argHolders = createSQLiteArgHolders(ids);
        String clause = "delete from meta_favorite where id_ in (" + argHolders + ")";

        execSQLite(db, clause, ids.toArray());
    }

    /** 清空 {@link InputFavorite} 数据 */
    public static void clearAllInputFavorites(SQLiteDatabase db) {
        execSQLite(db, "delete from meta_favorite");
    }

    /** 是否存在相同文本的 {@link InputFavorite} */
    public static boolean existSameTextInputFavorite(SQLiteDatabase db, String text) {
        List<InputFavorite> result = queryInputFavorites(db, text);

        return !result.isEmpty();
    }

    private static List<InputFavorite> queryInputFavorites(SQLiteDatabase db, String text) {
        return querySQLite(db, new SQLiteQueryParams<InputFavorite>() {{
            this.table = "meta_favorite";
            this.columns = new String[] { "id_", "type_", "text_", "used_count_", "created_at_", "used_at_" };

            this.where = text != null ? "text_ = ?" : null;
            this.params = text != null ? new String[] { text } : null;
            this.orderBy = text != null ? null : "created_at_ desc, used_at_ desc, used_count_ desc";

            this.reader = UserInputFavoriteDBHelper::createInputFavorite;
        }});
    }

    private static InputFavorite createInputFavorite(DBUtils.SQLiteRow row) {
        Integer id = row.getInt("id_");
        InputTextType type = InputTextType.valueByName(row.getString("type_"));
        String text = row.getString("text_");
        int usedCount = row.getInt("used_count_");
        long createdAt = row.getLong("created_at_");
        long usedAt = row.getLong("used_at_");

        return type == null
               ? null
               : InputFavorite.build((b) -> b.id(id)
                                             .type(type)
                                             .text(text)
                                             .createdAt(new Date(createdAt))
                                             .usedCount(usedCount)
                                             .usedAt(usedAt > 0 ? new Date(usedAt) : null));
    }
}
