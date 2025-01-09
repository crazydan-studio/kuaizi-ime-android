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

package org.crazydan.studio.app.ime.kuaizi.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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

    /** 回收无用空间 */
    public static void vacuumSQLite(SQLiteDatabase db) {
        execSQLite(db, "vacuum;");
    }

    public static void execSQLite(SQLiteDatabase db, String... clauses) {
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    /**
     * @param argsList
     *         参数列表为空时，不执行 <code>clause</code>
     */
    public static void execSQLite(SQLiteDatabase db, String clause, Collection<String[]> argsList) {
        if (CollectionUtils.isEmpty(argsList)) {
            return;
        }

        withTransaction(db, () -> {
            try (SQLiteStatement statement = db.compileStatement(clause);) {
                for (String[] args : argsList) {
                    statement.bindAllArgsAsStrings(args);
                    statement.execute();
                }
            }
        });
    }

    /**
     * @param args
     *         参数为空时，不执行 <code>clause</code>
     */
    public static void execSQLite(SQLiteDatabase db, String clause, String[] args) {
        if (CollectionUtils.isEmpty(args)) {
            return;
        }

        try (SQLiteStatement statement = db.compileStatement(clause);) {
            statement.bindAllArgsAsStrings(args);
            statement.execute();
        }
    }

    /**
     * 模拟 [upsert](https://www.sqlite.org/lang_upsert.html) 功能，
     * 即，先尝试执行 update 语句，若无数据更新，则视为新增，改为执行 insert 语句
     */
    public static void upsertSQLite(SQLiteDatabase db, SQLiteRawUpsertParams params) {
        // Note: SQLite 3.24.0 版本才支持 upsert
        // https://www.sqlite.org/lang_upsert.html#history
        withTransaction(db, () -> {
            try (
                    SQLiteStatement update = db.compileStatement(params.updateSQL);
                    SQLiteStatement insert = db.compileStatement(params.insertSql);
            ) {
                // insert 参数与 update 参数的数量需相同
                for (int i = 0; i < params.insertParamsList.size(); i++) {
                    String[] updateParams = params.updateParamsGetter != null
                                            ? params.updateParamsGetter.apply(i)
                                            : params.updateParamsList.get(i);

                    update.bindAllArgsAsStrings(updateParams);
                    if (update.executeUpdateDelete() > 0) {
                        continue;
                    }

                    insert.bindAllArgsAsStrings(params.insertParamsList.get(i));
                    insert.executeInsert();
                }
            }
        });
    }

    public static <T> List<T> querySQLite(SQLiteDatabase db, SQLiteQueryParams<T> params) {
        try (
                Cursor cursor = db.query(params.table,
                                         params.columns,
                                         params.where,
                                         params.params,
                                         params.groupBy,
                                         params.having,
                                         params.orderBy,
                                         params.limit)
        ) {
            return doQuerySQLite(cursor, params.reader, params.voidReader);
        }
    }

    public static <T> List<T> rawQuerySQLite(SQLiteDatabase db, SQLiteRawQueryParams<T> params) {
        try (
                Cursor cursor = db.rawQuery(params.sql, params.params)
        ) {
            return doQuerySQLite(cursor, params.reader, params.voidReader);
        }
    }

    private static <T> List<T> doQuerySQLite(
            Cursor cursor, Function<SQLiteRow, T> reader, Consumer<SQLiteRow> voidReader
    ) {
        // 通过 voidReader 避免无用的空间预设
        List<T> list = voidReader != null ? null : new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            SQLiteRow row = new SQLiteRow(cursor);

            if (voidReader != null) {
                voidReader.accept(row);
            } else {
                T data = reader.apply(row);
                if (data != null) {
                    list.add(data);
                }
            }
        }
        return list;
    }

    private static void withTransaction(SQLiteDatabase db, Runnable call) {
        db.beginTransaction();
        try {
            call.run();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static class BaseSQLiteQueryParams<T> {
        public String[] params;

        /** 行读取函数：有返回值 */
        public Function<SQLiteRow, T> reader;
        /** 行读取函数：无返回值，优先于 {@link #reader} */
        public Consumer<SQLiteRow> voidReader;
    }

    public static class SQLiteQueryParams<T> extends BaseSQLiteQueryParams<T> {
        public String table;
        public String[] columns;

        /** Note：在有 group by 时，只能通过 having 过滤结果，而不能使用 where */
        public String where;

        public String groupBy;
        public String having;

        public String orderBy;
        public String limit;
    }

    public static class SQLiteRawQueryParams<T> extends BaseSQLiteQueryParams<T> {
        public String sql;
    }

    public static class SQLiteRawUpsertParams {
        public String updateSQL;
        public String insertSql;

        public List<String[]> updateParamsList;
        public List<String[]> insertParamsList;

        /** 获取指定序号的更新参数 */
        public Function<Integer, String[]> updateParamsGetter;
    }

    public static class SQLiteRow {
        private final Cursor cursor;

        public SQLiteRow(Cursor cursor) {
            this.cursor = cursor;
        }

        public String getString(String columnName) {
            return get(columnName, this.cursor::getString);
        }

        public int getInt(String columnName) {
            return get(columnName, this.cursor::getInt);
        }

        private <T> T get(String columnName, Function<Integer, T> getter) {
            int columnIndex = this.cursor.getColumnIndexOrThrow(columnName);

            return getter.apply(columnIndex);
        }
    }
}
