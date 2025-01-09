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

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.PinyinDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.common.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.rawQuerySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-11-05
 */
@RunWith(AndroidJUnit4.class)
public class PinyinDictDBUpgradeTest extends PinyinDictBaseTest {
    private static final String LOG_TAG = PinyinDictDBUpgradeTest.class.getSimpleName();

    @Before
    public void get_db_version() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        List<String> dbVersion = rawQuerySQLite(db, new DBUtils.SQLiteRawQueryParams<String>() {{
            this.sql = "SELECT sqlite_version() as version";
            this.reader = (row) -> row.getString("version");
        }});
        Log.i(LOG_TAG, "SQLite version: " + CollectionUtils.first(dbVersion));
    }

    @Test
    public void test_upgrade_sql_syntax() {
        PinyinDict dict = PinyinDict.instance();
        SQLiteDatabase db = dict.getDB();

        String[] clauses = new String[] {
                // 准备测试表
                "create table" //
                + " tmp_meta_emoji_1 (" //
                + "   id_ integer not null primary key," //
                + "   weight_user_ integer not null" //
                + " )",
                "create table" //
                + " tmp_meta_emoji_2 (" //
                + "   id_ integer not null primary key," //
                + "   weight_ integer not null" //
                + " )",
                // ====================================
                // 待测试 SQL
                // 基本都支持 insert-select
                "insert into tmp_meta_emoji_1"
                + "   (id_, weight_user_)"
                + " select"
                + "   user_.id_, user_.weight_"
                + " from tmp_meta_emoji_2 user_",
                // Note: SQLite 3.33.0 版本才支持 update-from，只能采用子查询方式
                // https://www.sqlite.org/lang_update.html#upfrom
                "update tmp_meta_emoji_1 as emoji_"
                + "   set weight_user_ = user_.weight_"
                + " from tmp_meta_emoji_2 user_"
                + " where user_.id_ = emoji_.id_",
                "update tmp_meta_emoji_1"
                + " set weight_user_ = ifnull(("
                + "   select weight_"
                + "   from tmp_meta_emoji_2 user_"
                + "   where user_.id_ = tmp_meta_emoji_1.id_"
                + " ), weight_user_)",
                // Note: 低版本不支持 where (a, b) in ((1, 2), (3, 4), ...) 形式，只能采用 or 形式
                "select * from tmp_meta_emoji_1" //
                + " where (id_, weight_user_) in ((1, 2), (3, 4))",
                "select * from tmp_meta_emoji_1" //
                + " where (id_, weight_user_) = (1, 2) or (id_, weight_user_) = (3, 4)",
                // Note: SQLite 3.44.0 版本才支持 concat 函数，低版本需采用 || 替代
                // https://sqlite.org/releaselog/3_44_0.html
                "select concat(id_, ':', weight_user_) from tmp_meta_emoji_1",
                "select id_ || ':' || weight_user_ from tmp_meta_emoji_1",
                };

        for (String clause : clauses) {
            try {
                execSQLite(db, clause);
                Log.i(LOG_TAG, "[OK] SQL: " + clause);
            } catch (Exception e) {
                Log.e(LOG_TAG, "[ERROR] SQL: " + clause + " ==> " + e.getMessage());
            }
        }
    }
}
