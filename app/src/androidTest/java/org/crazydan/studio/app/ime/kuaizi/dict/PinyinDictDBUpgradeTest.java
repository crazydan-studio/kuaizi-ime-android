/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2024 Crazydan Studio
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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.crazydan.studio.app.ime.kuaizi.PinyinDictBaseTest;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.rawQuerySQLite;

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
                "create table" //
                + " tmp_meta_emoji_1 (" //
                + "   id_ integer not null primary key," //
                + "   weight_user_ integer not null" //
                + " )",
                //
                "create table" //
                + " tmp_meta_emoji_2 (" //
                + "   id_ integer not null primary key," //
                + "   weight_ integer not null" //
                + " )",
                // 待测试 SQL
                // Note: SQLite 3.33.0 版本才支持 update-from
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
                //
                "insert into tmp_meta_emoji_1"
                + "   (id_, weight_user_)"
                + " select"
                + "   user_.id_, user_.weight_"
                + " from tmp_meta_emoji_2 user_",
                };

        for (String clause : clauses) {
            try {
                execSQLite(db, clause);
                Log.i(LOG_TAG, "[OK] SQL: " + clause);
            } catch (Exception e) {
                Log.e(LOG_TAG, "[ERROR] SQL: " + clause, e);
            }
        }
    }
}