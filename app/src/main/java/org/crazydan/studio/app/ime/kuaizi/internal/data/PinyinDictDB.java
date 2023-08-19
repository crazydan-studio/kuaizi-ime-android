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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

/**
 * 拼音字典（数据库版）
 * <p/>
 * 应用内置的拼音字典数据库的表结构和数据生成见
 * <a href="https://github.com/crazydan-studio/kuaizi-ime/blob/master/tools/pinyin-dict/src/generate/sqlite/ime/index.mjs">kuaizi-ime/tools/pinyin-dict</a>
 * <p/>
 * 采用单例方式读写数据，以确保可以支持在 Guide 和 InputMethodService 中进行数据库的开启和关闭，
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
    private Map<String, String> pinyinCharsAndIdCache;
    // >>>>>>>>>>>>>

    public static PinyinDictDB getInstance() {
        return instance;
    }

    private PinyinDictDB() {
    }

    private File getAppDBFile(Context context) {
        return new File(context.getFilesDir(), file_app_dict_db);
    }

    private File getUserDBFile(Context context) {
        return new File(context.getFilesDir(), file_user_dict_db);
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

        File appDBFile = getAppDBFile(context);
        copySQLite(context, appDBFile, R.raw.pinyin_dict, R.raw.pinyin_dict_db_hash);

        File userDBFile = getUserDBFile(context);
        SQLiteDatabase userDB = openSQLite(userDBFile, false);
        initUserDB(userDB);

        this.inited = true;
    }

    /** 在任意需要启用输入法的情况下调用该开启接口 */
    public synchronized void open(Context context) {
        if (!this.closed) {
            return;
        }

        File appDBFile = getAppDBFile(context);
        File userDBFile = getUserDBFile(context);

        this.appDB = openSQLite(appDBFile, true);
        this.userDB = openSQLite(userDBFile, false);

        configSQLite(this.appDB);
        configSQLite(this.userDB);

        this.pinyinCharsAndIdCache = new HashMap<>(600);
        doSQLiteQuery(this.appDB, "meta_pinyin_chars", new String[] {
                              "id_", "value_"
                      }, //
                      null, null, null, null, //
                      (cursor) -> {
                          // Note: android sqlite 从 0 开始取，与 jdbc 的规范不一样
                          this.pinyinCharsAndIdCache.put(cursor.getString(1), cursor.getString(0));
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
    public List<InputWord> getCandidateWords(CharInput input) {
        String inputPinyinCharsId = getPinyinCharsId(input);
        if (inputPinyinCharsId == null) {
            return new ArrayList<>();
        }

        Map<String, Set<String[]>> wordAndPinyinIdMap = new LinkedHashMap<>(1000);
        doSQLiteQuery(this.appDB, "link_word_with_pinyin", new String[] {
                              "id_", "source_id_", "target_id_"
                      }, //
                      "target_chars_id_ = ?", //
                      new String[] { inputPinyinCharsId }, //
                      "target_id_ asc", // Note：拼音的 id 排序即为其字母排序
                      (cursor) -> {
                          String oid = cursor.getString(0);
                          String wordId = cursor.getString(1);
                          String wordPinyinId = cursor.getString(2);

                          wordAndPinyinIdMap.computeIfAbsent(wordId, (k) -> new LinkedHashSet<>())
                                            .add(new String[] { oid, wordPinyinId });

                          return null;
                      });
        if (wordAndPinyinIdMap.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> pinyinIdSet = new HashSet<>(wordAndPinyinIdMap.size());
        wordAndPinyinIdMap.values().forEach((set) -> {
            set.forEach((tuple) -> {
                pinyinIdSet.add(tuple[1]);
            });
        });

        Map<String, String> pinyinMap = new HashMap<>(pinyinIdSet.size());
        doSQLiteQuery(this.appDB, "meta_pinyin", new String[] {
                              "id_", "value_"
                      }, //
                      "id_ in (" + pinyinIdSet.stream().map(id -> "?").collect(Collectors.joining(", ")) + ")", //
                      pinyinIdSet.toArray(new String[0]), //
                      (cursor) -> {
                          String id = cursor.getString(0);
                          String value = cursor.getString(1);

                          pinyinMap.put(id, value);

                          return null;
                      });

        Set<String> wordIdSet = wordAndPinyinIdMap.keySet();
        Map<String, String[]> wordMap = new LinkedHashMap<>(wordIdSet.size());
        doSQLiteQuery(this.appDB, "meta_word", new String[] {
                              "id_", "value_", "traditional_"
                      }, //
                      "id_ in (" + wordIdSet.stream().map(id -> "?").collect(Collectors.joining(", ")) + ")", //
                      wordIdSet.toArray(new String[0]), //
                      "weight_ desc", //
                      (cursor) -> {
                          String id = cursor.getString(0);
                          String value = cursor.getString(1);
                          String traditional = (cursor.getInt(2) != 0) + "";

                          wordMap.put(id, new String[] { value, traditional });

                          return null;
                      });

        // 返回按字形权重排序的结果
        List<InputWord> candidates = new ArrayList<>(wordAndPinyinIdMap.size() * 2);
        wordMap.forEach((wordId, wordTuple) -> {
            wordAndPinyinIdMap.get(wordId).forEach((pinyinTuple) -> {
                // Note：字与拼音存在唯一隐射，故以其关联 id 作为输入词的唯一标识
                String oid = pinyinTuple[0];
                String wordPinyinId = pinyinTuple[1];

                String word = wordTuple[0];
                boolean isTraditional = Boolean.getBoolean(wordTuple[1]);

                String wordPinyin = pinyinMap.get(wordPinyinId);

                InputWord iw = new InputWord(oid, inputPinyinCharsId, word, wordPinyin, isTraditional);

                candidates.add(iw);
            });
        });

        return candidates;
    }

    /** 根据前序输入分析得出最靠前的 <code>top</code> 个候选字 */
    public List<InputWord> findTopBestCandidateWords(
            CharInput input, int top, List<InputWord> inputCandidateWords, List<InputWord> preInputWords
    ) {
        String inputPinyinCharsId = getPinyinCharsId(input);
        if (inputPinyinCharsId == null) {
            return new ArrayList<>();
        }

        Map<String, InputWord> wordMap = inputCandidateWords.stream()
                                                            .collect(Collectors.toMap(InputWord::getOid,
                                                                                      Function.identity()));

        // Note：用户字典优先
        List<String> userTopBestPinyinWordIdList = findTopBestPinyinWordsFromUserDB(inputPinyinCharsId,
                                                                                    top,
                                                                                    preInputWords);
        List<InputWord> bestCandidates = userTopBestPinyinWordIdList.stream()
                                                                    .map(wordMap::get)
                                                                    .collect(Collectors.toList());

        return CollectionUtils.topPatch(bestCandidates,
                                        top,
                                        () -> findTopBestPinyinWordsFromAppDB(inputPinyinCharsId,
                                                                              top,
                                                                              preInputWords).stream()
                                                                                            .map(wordMap::get)
                                                                                            .collect(Collectors.toList()));
    }

    /** 保存已使用的短语：异步处理 */
    public void saveUsedPhrases(List<List<InputWord>> phrases) {
        if (phrases == null || phrases.isEmpty()) {
            return;
        }

        this.dbHandler.post(() -> phrases.forEach(this::doSaveUsedPhrase));
    }

    private List<String> findTopBestPinyinWordsFromUserDB(
            String inputPinyinCharsId, int top, List<InputWord> preInputWords
    ) {
        List<String> pinyinWordIdList = doSQLiteQuery(this.userDB, "used_pinyin_word", new String[] {
                                                              "oid_"
                                                      }, //
                                                      "chars_id_ = ?", //
                                                      new String[] { inputPinyinCharsId }, //
                                                      "weight_ desc", //
                                                      String.valueOf(top), //
                                                      (cursor) -> cursor.getString(0));
        return pinyinWordIdList;
    }

    private List<String> findTopBestPinyinWordsFromAppDB(
            String inputPinyinCharsId, int top, List<InputWord> preInputWords
    ) {
        // 匹配高频字
        List<String> pinyinWordIdList = doSQLiteQuery(this.appDB, "link_word_with_pinyin", new String[] {
                                                              "id_"
                                                      }, //
                                                      "target_chars_id_ = ?", //
                                                      new String[] { inputPinyinCharsId }, //
                                                      "weight_ desc", String.valueOf(top), //
                                                      (cursor) -> cursor.getString(0));

        // 匹配短语中的常用字
        List<String> phrasePinyinCharsIdList = preInputWords.stream()
                                                            .map(InputWord::getCharsId)
                                                            .collect(Collectors.toList());
        phrasePinyinCharsIdList.add(inputPinyinCharsId);
        Collections.reverse(phrasePinyinCharsIdList);

        Set<String> phrasePinyinCharsIdSet = new HashSet<>(phrasePinyinCharsIdList);
        Set<String> invalidPhraseIdSet = new HashSet<>();
        Map<String, List<String[]>> matchedPhraseMap = new HashMap<>(phrasePinyinCharsIdSet.size() * 10);
        doSQLiteQuery(this.appDB, "link_phrase_with_pinyin_word", new String[] {
                              "source_id_", "target_id_", "target_spell_chars_id_", "target_index_"
                      }, //
                      "target_spell_chars_id_ in (" + phrasePinyinCharsIdSet.stream()
                                                                            .map((id) -> "?")
                                                                            .collect(Collectors.joining(", ")) + ")", //
                      phrasePinyinCharsIdSet.toArray(new String[0]), //
                      "source_id_ asc, target_index_ " +
                      // Note：只有一个字时，应该将其视为短语的开头（升序排序短语中的字），
                      // 否则，视其为短语的结尾（降序排序短语中的字）
                      (phrasePinyinCharsIdList.size() == 1 ? "asc" : "desc"), //
                      (cursor) -> {
                          String phraseId = cursor.getString(0);
                          if (invalidPhraseIdSet.contains(phraseId)) {
                              return null;
                          }

                          String pinyinWordId = cursor.getString(1);
                          String pinyinCharsId = cursor.getString(2);
                          String pinyinWordIndex = cursor.getString(3);

                          List<String[]> list = matchedPhraseMap.computeIfAbsent(phraseId, (k) -> new ArrayList<>());

                          String[] prev = CollectionUtils.last(list);
                          if ( // 去掉 搜索的字 在 短语 中 不相邻 的数据
                                  (prev != null //
                                   && Integer.parseInt(pinyinWordIndex) - Integer.parseInt(prev[1]) != 1)
                                  // 去掉与 查询短语 在 相同位置 读音不匹配 的数据
                                  || (list.size() < phrasePinyinCharsIdList.size() //
                                      && !phrasePinyinCharsIdList.get(list.size()).equals(pinyinCharsId))) {
                              invalidPhraseIdSet.add(phraseId);
                              matchedPhraseMap.remove(phraseId);

                              return null;
                          }

                          list.add(new String[] { pinyinWordId, pinyinWordIndex, pinyinCharsId });

                          return null;
                      });

        if (matchedPhraseMap.isEmpty()) {
            return pinyinWordIdList;
        }

        List<String> sortedPhraseIdList = doSQLiteQuery(this.appDB, "meta_phrase", new String[] {
                                                                "id_"
                                                        }, //
                                                        "id_ in ()", //
                                                        new String[] { inputPinyinCharsId }, //
                                                        "weight_ desc", //
                                                        (cursor) -> cursor.getString(0));
        Set<String> pinyinWordIdInPhraseSet = new LinkedHashSet<>(sortedPhraseIdList.size());
        sortedPhraseIdList.forEach((phraseId) -> {
            String[] tuple = CollectionUtils.first(matchedPhraseMap.get(phraseId));

            pinyinWordIdInPhraseSet.add(tuple[0]);
        });

        return CollectionUtils.topPatch(new ArrayList<>(pinyinWordIdInPhraseSet), top, () -> pinyinWordIdList);
    }

    private void doSaveUsedPhrase(List<InputWord> phrase) {
        doSaveWordInUsedPhrase(phrase);

        Set<String> phrasePinyinIds = extractPinyinId(phrase);

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
                    "insert into used_phrase_meta (pre_pinyin_id_, post_pinyin_id_, weight_) values (?, ?, ?)");
            SQLiteStatement update = this.userDB.compileStatement(
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

    /** 保存短语中字的使用频率等信息 */
    private void doSaveWordInUsedPhrase(List<InputWord> phrase) {
        Map<String, String> oidAndCharsIdMap = phrase.stream()
                                                     .collect(Collectors.toMap(InputWord::getOid,
                                                                               InputWord::getCharsId));

        Map<String, Integer> existUsedPinyinWordMap = new HashMap<>();
        doSQLiteQuery(this.userDB,
                      "used_pinyin_word",
                      new String[] {
                              "id_", "oid_", "chars_id_", "weight_"
                      },
                      "oid_ in (" + oidAndCharsIdMap.keySet()
                                                    .stream()
                                                    .map((id) -> "?")
                                                    .collect(Collectors.joining(", ")) + ")",
                      oidAndCharsIdMap.keySet().toArray(new String[0]),
                      (cursor) -> {
                          existUsedPinyinWordMap.put(cursor.getString(1), cursor.getInt(3));
                          return null;
                      });

        this.userDB.beginTransaction();
        try {
            SQLiteStatement insert = this.userDB.compileStatement(
                    "insert into used_pinyin_word (weight_, oid_, chars_id_) values (?, ?, ?)");
            SQLiteStatement update = this.userDB.compileStatement(
                    "update used_pinyin_word set weight_ = ? where oid_ = ?");

            oidAndCharsIdMap.forEach((oid, charsId) -> {
                int weight = existUsedPinyinWordMap.getOrDefault(oid, 0) + 1;

                if (existUsedPinyinWordMap.containsKey(oid)) {
                    update.bindAllArgsAsStrings(new String[] { String.valueOf(weight), oid });
                    update.execute();
                } else {
                    insert.bindAllArgsAsStrings(new String[] { String.valueOf(weight), oid, charsId });
                    insert.execute();
                }
            });

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

    private String getPinyinCharsId(CharInput input) {
        String pinyinChars = String.join("", input.getChars());

        return this.pinyinCharsAndIdCache.get(pinyinChars);
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

    private static void copySQLite(Context context, File targetDBFile, int dbRawResId, int dbHashRawResId) {
        String dbHash = FileUtils.read(context, dbHashRawResId, true);

        File targetDBHashFile = new File(targetDBFile.getPath() + ".hash");
        String targetHash = FileUtils.read(targetDBHashFile, true);

        if (dbHash != null && Objects.equals(dbHash, targetHash)) {
            return;
        }

        try {
            FileUtils.copy(context, dbRawResId, targetDBFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (dbHash != null) {
            try {
                FileUtils.write(targetDBHashFile, dbHash);
            } catch (IOException ignore) {
            }
        }
    }

    private static <T> List<T> doSQLiteQuery(
            SQLiteDatabase db, String table, String[] columns, String where, String[] params,
            Function<Cursor, T> creator
    ) {
        return doSQLiteQuery(db, table, columns, where, params, null, creator);
    }

    private static <T> List<T> doSQLiteQuery(
            SQLiteDatabase db, String table, String[] columns, String where, String[] params, String orderBy,
            Function<Cursor, T> creator
    ) {
        return doSQLiteQuery(db, table, columns, where, params, orderBy, null, creator);
    }

    private static <T> List<T> doSQLiteQuery(
            SQLiteDatabase db, String table, String[] columns, String where, String[] params, String orderBy,
            String limit, Function<Cursor, T> creator
    ) {
        return doSQLiteQuery(db, table, columns, where, params, null, orderBy, limit, creator);
    }

    private static <T> List<T> doSQLiteQuery(
            SQLiteDatabase db, String table, String[] columns, String where, String[] params, String groupBy,
            String orderBy, String limit, Function<Cursor, T> creator
    ) {
        try (
                Cursor cursor = db.query(table, columns, where, params, groupBy, null, orderBy, limit)
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
                "PRAGMA cache_size = 100;", "PRAGMA temp_store = MEMORY;",
                };
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    private void initUserDB(SQLiteDatabase db) {
        String[] clauses = new String[] {
                "CREATE TABLE\n"
                + "    IF NOT EXISTS used_pinyin_word (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        oid_ INTEGER NOT NUll,\n"
                + "        chars_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0,\n"
                + "        UNIQUE (oid_)"
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_py_wrd_chars ON used_pinyin_word (chars_id_);",
                //
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
