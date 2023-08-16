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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;

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

    private final Handler dbHandler = new Handler();

    private boolean inited = false;
    private boolean closed = true;

    /** 内置字典数据库 */
    private SQLiteDatabase appDB;
    /** 用户字典数据库 */
    private SQLiteDatabase userDB;

    // <<<<<<<<<<<<< 缓存常量数据
    private Map<String, Integer> pinyinCharsAndIdCache;
    // >>>>>>>>>>>>>

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

        File appDBFile = new File(context.getFilesDir(), file_app_dict_db);
        copySQLite(context, appDBFile, R.raw.pinyin_dict);

        File userDBFile = new File(context.getFilesDir(), file_user_dict_db);
        SQLiteDatabase userDB = openSQLite(userDBFile, false);
        initUserDB(userDB);

        this.inited = true;
    }

    /** 在任意需要启用输入法的情况下调用该开启接口 */
    public synchronized void open(Context context) {
        if (!this.closed) {
            return;
        }

        File appDBFile = new File(context.getFilesDir(), file_app_dict_db);
        File userDBFile = new File(context.getFilesDir(), file_user_dict_db);

        this.appDB = openSQLite(appDBFile, true);
        this.userDB = openSQLite(userDBFile, false);

        configSQLite(this.appDB);
        configSQLite(this.userDB);

        this.pinyinCharsAndIdCache = new HashMap<>(600);
        doSQLiteQuery(this.appDB, "pinyin_chars_meta", new String[] {
                              "id_", "value_"
                      }, //
                      null, null, null, null, //
                      (cursor) -> {
                          // Note: android sqlite 从 0 开始取，与 jdbc 的规范不一样
                          this.pinyinCharsAndIdCache.put(cursor.getString(1), cursor.getInt(0));
                          return null;
                      });

        this.closed = false;
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

    /**
     * 查找指定{@link Key.Level 级别}的后继字母
     *
     * @return 参数为<code>null</code>或为空时，返回<code>null</code>
     */
    public Collection<String> findNextChar(Key.Level keyLevel, String startChar) {
        if (startChar == null || startChar.isEmpty()) {
            return null;
        }

        List<String> nextCharList = this.pinyinCharsAndIdCache.keySet().stream().filter(chars -> {
            if (chars.length() > startChar.length() //
                && chars.startsWith(startChar)) {
                // 平翘舌需相同
                return !(chars.startsWith("ch") || chars.startsWith("sh") || chars.startsWith("zh"))
                       || startChar.startsWith(chars.substring(0, 2));
            }
            return false;
        }).collect(Collectors.toList());

        return nextCharList.stream().map(chars -> {
            if (keyLevel == Key.Level.level_1) {
                String nextChar = chars.substring(startChar.length(), startChar.length() + 1);

                int startsWithCount = 0;
                for (String ch : nextCharList) {
                    if (ch.startsWith(startChar + nextChar)) {
                        startsWithCount += 1;
                    }
                }
                return startsWithCount == 1
                       // 只有一个可选拼音，则返回从直接后继字母开始的剩余部分
                       ? chars.substring(startChar.length())
                       // 否则，返回直接后继字母
                       : nextChar;
            }
            // Note: 第 2 级后继需包含第 1 级后继字母
            return chars.substring(startChar.length() - 1);
        }).collect(Collectors.toSet());
    }

    /** 获取指定拼音的候选字 */
    public List<InputWord> getCandidateWords(List<String> pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return new ArrayList<>();
        }

        String pinyinChars = String.join("", pinyin);
        Integer pinyinCharsId = this.pinyinCharsAndIdCache.get(pinyinChars);
        if (pinyinCharsId == null) {
            return new ArrayList<>();
        }

        Map<String, String[]> pinyinWordIdMap = new LinkedHashMap<>(1000);
        doSQLiteQuery(this.appDB, "pinyin_pinyin_meta", new String[] {
                              "id_", "value_", "word_id_"
                      }, //
                      "chars_id_ = ?", //
                      new String[] { String.valueOf(pinyinCharsId) }, //
                      "weight_ desc", //
                      null,//
                      (cursor) -> {
                          pinyinWordIdMap.put(cursor.getString(0),
                                              new String[] { cursor.getString(1), cursor.getString(2) });

                          return null;
                      });
        if (pinyinWordIdMap.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> wordIdSet = pinyinWordIdMap.values().stream().map(tuple -> tuple[1]).collect(Collectors.toSet());
        Map<String, String[]> wordMap = new HashMap<>(wordIdSet.size());
        doSQLiteQuery(this.appDB, "pinyin_word_meta", new String[] {
                              "id_", "value_", "simple_word_id_"
                      }, //
                      "id_ in (" + wordIdSet.stream().map(id -> "?").collect(Collectors.joining(", ")) + ")", //
                      wordIdSet.toArray(new String[0]), //
                      null, //
                      null, //
                      (cursor) -> {
                          wordMap.put(cursor.getString(0), new String[] { cursor.getString(1), cursor.getString(2) });

                          return null;
                      });

        List<InputWord> result = new ArrayList<>(pinyinWordIdMap.size());
        pinyinWordIdMap.forEach((id, pinyinTuple) -> {
            String[] wordTuple = wordMap.get(pinyinTuple[1]);
            InputWord pw = new InputWord(id, wordTuple[0], pinyinTuple[0], wordTuple[1] != null);

            result.add(pw);
        });

        return result;
    }

    /** 根据当前候选字列表和前序拼音，分析得出最佳候选字 */
    public InputWord findBestCandidateWord(List<InputWord> candidateWords, List<InputWord> prePhrase) {
        if (candidateWords == null || candidateWords.isEmpty()) {
            return null;
        }

        Set<String> candidatePinyinIds = extractPinyinId(candidateWords);
        Set<String> prePhrasePinyinIds = extractPinyinId(prePhrase);

        String bestPinyinId = null;
        // 查找用户字典的适配短语
        if (!prePhrasePinyinIds.isEmpty()) {
            List<String> params = new ArrayList<>(candidatePinyinIds);
            params.add(CollectionUtils.last(prePhrasePinyinIds));

            List<String> postPinyinIds = doSQLiteQuery(this.userDB, "used_phrase_meta", new String[] {
                                                               "pre_pinyin_id_", "post_pinyin_id_"
                                                       }, //
                                                       "post_pinyin_id_ in ("
                                                       + candidatePinyinIds.stream()
                                                                           .map(id -> "?")
                                                                           .collect(Collectors.joining(", "))
                                                       + ") and pre_pinyin_id_ = ?", //
                                                       params.toArray(new String[0]), //
                                                       "weight_ desc", //
                                                       "1", //
                                                       (cursor) -> cursor.getString(1));
            if (!postPinyinIds.isEmpty()) {
                bestPinyinId = CollectionUtils.first(postPinyinIds);
            }
        }
        // 若未从短语中匹配到，则按单字使用情况匹配
        if (bestPinyinId == null) {
            List<String> pinyinIds = doSQLiteQuery(this.userDB, "used_pinyin_meta", new String[] {
                                                           "pinyin_id_"
                                                   }, //
                                                   "pinyin_id_ in (" + candidatePinyinIds.stream()
                                                                                         .map(id -> "?")
                                                                                         .collect(Collectors.joining(
                                                                                                 ", ")) + ")", //
                                                   candidatePinyinIds.toArray(new String[0]), //
                                                   "weight_ desc", //
                                                   "1", //
                                                   (cursor) -> cursor.getString(0));
            if (!pinyinIds.isEmpty()) {
                bestPinyinId = CollectionUtils.first(pinyinIds);
            }
        }
        // 最后，查找内置字典的适配短语
        if (bestPinyinId == null && !prePhrasePinyinIds.isEmpty()) {
            List<String> params = new ArrayList<>(candidatePinyinIds);
            params.add(CollectionUtils.last(prePhrasePinyinIds));

            List<String> postPinyinIds = doSQLiteQuery(this.appDB, "pinyin_phrase_meta", new String[] {
                                                               "pre_pinyin_id_", "post_pinyin_id_"
                                                       }, //
                                                       "post_pinyin_id_ in ("
                                                       + candidatePinyinIds.stream()
                                                                           .map(id -> "?")
                                                                           .collect(Collectors.joining(", "))
                                                       + ") and pre_pinyin_id_ = ?", //
                                                       params.toArray(new String[0]), //
                                                       "weight_ desc", //
                                                       "1", //
                                                       (cursor) -> cursor.getString(1));
            if (!postPinyinIds.isEmpty()) {
                bestPinyinId = CollectionUtils.first(postPinyinIds);
            }
        }

        List<InputWord> candidates = candidateWords;
        if (bestPinyinId != null) {
            String pinyinId = bestPinyinId;
            candidates = candidateWords.stream().filter(w -> w.getOid().equals(pinyinId)).collect(Collectors.toList());
        }

        return CollectionUtils.first(candidates);
    }

    /** 保存已使用的短语：异步处理 */
    public void saveUsedPhrases(List<List<InputWord>> phrases) {
        if (phrases == null || phrases.isEmpty()) {
            return;
        }

        this.dbHandler.post(() -> phrases.forEach(this::doSaveUsedPhrase));
    }

    private void doSaveUsedPhrase(List<InputWord> phrase) {
        Set<String> phrasePinyinIds = extractPinyinId(phrase);

        Map<String, String[]> existUsedPinyinMap = new HashMap<>();
        doSQLiteQuery(this.userDB, "used_pinyin_meta", new String[] {
                              "id_", "pinyin_id_", "weight_"
                      }, //
                      null, null, null, null, //
                      (cursor) -> {
                          existUsedPinyinMap.put(cursor.getString(1),
                                                 new String[] { cursor.getString(0), cursor.getString(2) });
                          return null;
                      });

        Map<String, String[]> existUsedPhraseMap = new HashMap<>();
        doSQLiteQuery(this.userDB, "used_phrase_meta", new String[] {
                              "id_", "pre_pinyin_id_", "post_pinyin_id_", "weight_"
                      }, //
                      null, null, null, null, //
                      (cursor) -> {
                          existUsedPhraseMap.put(cursor.getString(1) + ":" + cursor.getString(2),
                                                 new String[] { cursor.getString(0), cursor.getString(3) });
                          return null;
                      });

        this.userDB.beginTransaction();
        try {
            SQLiteStatement insert = this.userDB.compileStatement(
                    "insert into used_pinyin_meta (pinyin_id_, weight_) values (?, ?)");
            SQLiteStatement update = this.userDB.compileStatement(
                    "update used_pinyin_meta set pinyin_id_ = ?, weight_ = ? where id_ = ?");
            for (String pinyinId : phrasePinyinIds) {
                String[] exist = existUsedPinyinMap.get(pinyinId);
                if (exist == null) {
                    insert.bindAllArgsAsStrings(new String[] { pinyinId, "1" });
                    insert.execute();
                } else {
                    update.bindAllArgsAsStrings(new String[] {
                            pinyinId, String.valueOf(Integer.parseInt(exist[1]) + 1), exist[0]
                    });
                    update.execute();
                }
            }

            insert = this.userDB.compileStatement(
                    "insert into used_phrase_meta (pre_pinyin_id_, post_pinyin_id_, weight_) values (?, ?, ?)");
            update = this.userDB.compileStatement(
                    "update used_phrase_meta set pre_pinyin_id_ = ?, post_pinyin_id_ = ?, weight_ = ? where id_ = ?");
            Iterator<String> it = phrasePinyinIds.iterator();
            String prePinyinId = it.next();
            while (it.hasNext()) {
                String postPinyinId = it.next();
                String[] exist = existUsedPhraseMap.get(prePinyinId + ":" + postPinyinId);
                if (exist == null) {
                    insert.bindAllArgsAsStrings(new String[] { prePinyinId, postPinyinId, "1" });
                    insert.execute();
                } else {
                    update.bindAllArgsAsStrings(new String[] {
                            prePinyinId, postPinyinId, String.valueOf(Integer.parseInt(exist[1]) + 1), exist[0]
                    });
                    update.execute();
                }

                prePinyinId = postPinyinId;
            }

            this.userDB.setTransactionSuccessful();
        } finally {
            this.userDB.endTransaction();
        }
    }

    private Set<String> extractPinyinId(List<InputWord> words) {
        // Note: 必须保证结果的顺序与输入的顺序一致
        return words == null || words.isEmpty()
               ? new LinkedHashSet<>()
               : new LinkedHashSet<>(words.stream().map(InputWord::getOid).collect(Collectors.toList()));
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
            String limit, Function<Cursor, T> creator
    ) {
        try (
                Cursor cursor = db.query(table, columns, where, params, null, null, orderBy, limit)
        ) {
            if (cursor == null) {
                return new ArrayList<>();
            }

            List<T> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                T data = creator.apply(cursor);
                list.add(data);
            }

            return list;
        }
    }

    private void configSQLite(SQLiteDatabase db) {
        String[] clauses = new String[] {
                "PRAGMA cache_size = 5000;", "PRAGMA temp_store = MEMORY;",
                };
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    private void initUserDB(SQLiteDatabase db) {
        String[] clauses = new String[] {
                "CREATE TABLE\n"
                + "    IF NOT EXISTS used_pinyin_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        pinyin_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0,\n"
                + "        UNIQUE (pinyin_id_)"
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_py_py_id ON used_pinyin_meta (pinyin_id_);",
                "CREATE TABLE\n"
                + "    IF NOT EXISTS used_phrase_meta (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        pre_pinyin_id_ INTEGER NOT NULL,\n"
                + "        post_pinyin_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0,\n"
                + "        UNIQUE (pre_pinyin_id_, post_pinyin_id_)"
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_ph_py_id ON used_phrase_meta (pre_pinyin_id_, post_pinyin_id_);",
                };

        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }
}
