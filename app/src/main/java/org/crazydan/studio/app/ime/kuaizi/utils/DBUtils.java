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

package org.crazydan.studio.app.ime.kuaizi.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-20
 */
public class DBUtils {

    public static SQLiteDatabase openSQLite(File file, boolean readonly) {
        if (!file.exists() && !readonly) {
            return SQLiteDatabase.openOrCreateDatabase(file, null);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            SQLiteDatabase.OpenParams.Builder builder = new SQLiteDatabase.OpenParams.Builder();

            if (!readonly) {
                builder.setOpenFlags(SQLiteDatabase.OPEN_READWRITE);
            } else {
                builder.setOpenFlags(SQLiteDatabase.OPEN_READONLY);
            }

            return SQLiteDatabase.openDatabase(file, builder.build());
        } else {
            return SQLiteDatabase.openDatabase(file.getPath(),
                                               null,
                                               readonly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);
        }
    }

    public static void closeSQLite(SQLiteDatabase db) {
        if (db != null) {
            db.close();
        }
    }

    public static void copySQLite(Context context, File targetDBFile, int dbRawResId) {
        try {
            FileUtils.copy(context, dbRawResId, targetDBFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copySQLite(Context context, File targetDBFile, int dbRawResId, int dbHashRawResId) {
        String dbHash = FileUtils.read(context, dbHashRawResId, true);

        File targetDBHashFile = new File(targetDBFile.getPath() + ".hash");
        String targetHash = FileUtils.read(targetDBHashFile, true);

        if (dbHash != null && Objects.equals(dbHash, targetHash)) {
            return;
        }

        copySQLite(context, targetDBFile, dbRawResId);

        if (dbHash != null) {
            try {
                FileUtils.write(targetDBHashFile, dbHash);
            } catch (IOException ignore) {
            }
        }
    }

    public static void configSQLite(SQLiteDatabase db) {
        String[] clauses = new String[] {
                "pragma cache_size = 2500;", "pragma temp_store = memory;",
                };
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    public static void execSQLite(SQLiteDatabase db, String... clauses) {
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    public static <T> List<T> querySQLite(SQLiteDatabase db, SQLiteQueryParams<T> params) {
        try (
                // Note：在有 group by 时，只能通过 having 过滤结果，而不能使用 where
                Cursor cursor = db.query(params.table,
                                         params.columns,
                                         params.where,
                                         params.params,
                                         params.groupBy,
                                         params.having,
                                         params.orderBy,
                                         params.limit)
        ) {
            List<T> list = new ArrayList<>(cursor.getCount());

            while (cursor.moveToNext()) {
                T data = params.creator.apply(cursor);

                if (data != null) {
                    list.add(data);
                }
            }
            return list;
        }
    }

    public static void saveSQLite(SQLiteDatabase db, String sql_1, String sql_2, SQLiteSaver saver) {
        db.beginTransaction();
        try {
            SQLiteStatement sql_1_statement = sql_1 != null ? db.compileStatement(sql_1) : null;
            SQLiteStatement sql_2_statement = sql_2 != null ? db.compileStatement(sql_2) : null;

            saver.save(sql_1_statement, sql_2_statement);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public interface SQLiteSaver {
        void save(SQLiteStatement statement_1, SQLiteStatement statement_2);
    }

    public static class SQLiteQueryParams<T> {
        public String table;
        public String[] columns;

        public String where;
        public String[] params;

        public String groupBy;
        public String having;

        public String orderBy;
        public String limit;

        public Function<Cursor, T> creator;
    }
}
