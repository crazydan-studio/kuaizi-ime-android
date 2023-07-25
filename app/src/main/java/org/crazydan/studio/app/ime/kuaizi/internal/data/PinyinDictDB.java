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

package org.crazydan.studio.app.ime.kuaizi.internal.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.R;

/**
 * 拼音字典（数据库版）
 * <p/>
 * 应用内置的拼音字典数据库的表结构和数据生成见单元测试用例 PinyinDataTest#writePinyinDictToSQLite
 * <p/>
 * 采用单例方式读写数据，以确保可以支持在 Demo 和 InputMethodService 中进行数据库的开启和关闭，
 * 且在两者同时启动时，不会重复开关数据库
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2023-07-24
 */
public class PinyinDictDB {
    private static final String file_app_dict_db = "pinyin_app_dict.db";
    private static final String file_user_dict_db = "pinyin_user_dict.db";

    private static final PinyinDictDB instance = new PinyinDictDB();

    private boolean inited = false;
    private boolean closed = true;

    private File appDBFile;
    private File userDBFile;
    /** 内置字典数据库 */
    private SQLiteDatabase appDB;
    /** 用户字典数据库 */
    private SQLiteDatabase userDB;

    public static PinyinDictDB getInstance() {
        return instance;
    }

    private PinyinDictDB() {
    }

    /**
     * 仅在确定的某个地方初始化一次
     * <p/>
     * 仅第一次调用起作用，后续调用均会被忽略
     */
    public synchronized void init(Context context) {
        if (this.inited) {
            return;
        }

        this.appDBFile = new File(context.getFilesDir(), file_app_dict_db);
        this.userDBFile = new File(context.getFilesDir(), file_user_dict_db);

        copySQLite(context, this.appDBFile, R.raw.pinyin_dict);

        this.inited = true;
    }

    /** 在任意存在完全退出的情况下调用该关闭接口 */
    public synchronized void close() {
        if (this.closed) {
            return;
        }

        closeSQLite(this.appDB);
        closeSQLite(this.userDB);

        this.closed = true;
    }

    /** 读写字典前需先确保数据库已被打开 */
    private synchronized void makeSureItIsOpen() {
        if (!this.closed) {
            return;
        }

        this.appDB = openSQLite(this.appDBFile, true);
        this.userDB = openSQLite(this.userDBFile, false);

        this.closed = false;
    }

    /**
     * 查找指定拼音的后继字母
     *
     * @return 参数为<code>null</code>或为空时，返回<code>null</code>
     */
    public Collection<String> findNextPinyinChar(List<String> pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return null;
        }
        makeSureItIsOpen();

        String chars = String.join("", pinyin);
        List<String> list = doSQLiteQuery(this.appDB, "pinyin_pinyin", new String[] {
                                                  "chars_"
                                          }, //
                                          "chars_ like ? and chars_ != ?", //
                                          new String[] { chars + "%", chars }, //
                                          null, //
                                          (cursor) ->
                                                  // Note: android sqlite 从 0 开始取，与 jdbc 的规范不一样
                                                  cursor.getString(0));

        return list.stream().map(s -> s.substring(chars.length(), chars.length() + 1)).collect(Collectors.toSet());
    }

    /** 查找指定拼音的候选字 */
    public List<PinyinWord> findCandidateWords(List<String> pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return new ArrayList<>();
        }
        makeSureItIsOpen();

        String chars = String.join("", pinyin);

        return doSQLiteQuery(this.appDB, "pinyin_pinyin", new String[] {
                                     "value_", "word_", "simple_word_"
                             }, //
                             "chars_ = ?", //
                             new String[] { chars }, //
                             "weight_ desc", //
                             (cursor) -> {
                                 // Note: android sqlite 从 0 开始取，与 jdbc 的规范不一样
                                 String py = cursor.getString(0);
                                 String word = cursor.getString(1);
                                 boolean traditional = cursor.getString(2) != null;

                                 return new PinyinWord(word, py, traditional);
                             });
    }

    private PinyinDict.Word queryWordByValue(SQLiteDatabase db, String value) {
        List<PinyinDict.Word> list = queryWord(db, "value_ = ?", new String[] { value }, null);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<PinyinDict.Word> queryWord(SQLiteDatabase db, String where, String[] params, String orderBy) {
        return doSQLiteQuery(db, "pinyin_word", new String[] {
                "id_", "value_", "stroke_count_", "stroke_order_", "simple_word_"
        }, where, params, orderBy, (cursor) -> {
            PinyinDict.Word data = new PinyinDict.Word();
            data.setId(cursor.getInt(0));
            data.setValue(cursor.getString(1));
            data.setStrokeCount(cursor.getInt(2));
            data.setStrokeOrder(cursor.getString(3));
            data.setSimpleWord(cursor.getString(4));

            return data;
        });
    }

    private PinyinTree.Pinyin queryPinyinByValue(SQLiteDatabase db, String value, String word) {
        List<PinyinTree.Pinyin> list = queryPinyin(db, "value_ = ? and word_ = ?", new String[] { value, word }, null);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<PinyinTree.Pinyin> queryPinyin(SQLiteDatabase db, String where, String[] params, String orderBy) {
        return doSQLiteQuery(db, "pinyin_pinyin", new String[] {
                "id_", "value_", "chars_", "weight_", "word_"
        }, where, params, orderBy, (cursor) -> {
            PinyinTree.Pinyin data = new PinyinTree.Pinyin();
            data.setId(cursor.getInt(0));
            data.setValue(cursor.getString(1));
            data.setChars(cursor.getString(2));
            data.setWeight(cursor.getInt(3));
            data.setWord(cursor.getString(4));

            return data;
        });
    }

    private ContentValues createContentValues(SQLiteDatabase db, PinyinDict.Word data) {
        ContentValues values = new ContentValues();
        values.put("value_", data.getValue());
        values.put("stroke_count_", data.getStrokeCount());
        values.put("stroke_order_", data.getStrokeOrder());
        values.put("simple_word_id_", (Integer) null);

        if (data.isTraditional()) {
            // Note: 需在繁体之前插入简体字，以确保繁体字的简体在之前已插入
            PinyinDict.Word simple = queryWordByValue(db, data.getSimpleWord());
            if (simple != null) {
                values.put("simple_word_id_", simple.getId());
            }
        }

        return values;
    }

    private ContentValues createContentValues(SQLiteDatabase db, PinyinTree.Pinyin data) {
        ContentValues values = new ContentValues();
        values.put("value_", data.getValue());
        values.put("chars_", data.getChars());
        values.put("weight_", data.getWeight());

        PinyinDict.Word word = queryWordByValue(db, data.getWord());
        values.put("word_id_", word.getId());

        return values;
    }

    private static SQLiteDatabase openSQLite(File file, boolean readonly) {
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

    private static void closeSQLite(SQLiteDatabase db) {
        if (db != null) {
            db.close();
        }
    }

    private static void copySQLite(Context context, File target, int dbRawResId) {
        try (
                InputStream input = context.getResources().openRawResource(dbRawResId);
                OutputStream output = Files.newOutputStream(target.toPath());
        ) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> List<T> doSQLiteQuery(
            SQLiteDatabase db, String table, String[] columns, String where, String[] params, String orderBy,
            Function<Cursor, T> creator
    ) {
        try (
                Cursor cursor = db.query(table, columns, where, params, null, null, orderBy)
        ) {
            if (cursor == null || cursor.getCount() == 0) {
                return new ArrayList<>();
            }

            List<T> list = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    T data = creator.apply(cursor);
                    list.add(data);
                } while (cursor.moveToNext());
            }

            return list;
        }
    }
}
