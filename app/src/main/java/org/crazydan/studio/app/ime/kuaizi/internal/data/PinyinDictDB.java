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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.internal.InputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.Key;
import org.crazydan.studio.app.ime.kuaizi.internal.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.internal.input.EmojiInputWord;
import org.crazydan.studio.app.ime.kuaizi.internal.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.CharUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;

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

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private Future<Boolean> dbInited;
    private Future<Boolean> dbOpened;

    /** 内置字典数据库 */
    private SQLiteDatabase appDB;
    /** 用户字典数据库 */
    private SQLiteDatabase userDB;

    // <<<<<<<<<<<<< 缓存常量数据
    private Map<String, String> pinyinCharsAndIdCache;
    // >>>>>>>>>>>>>

    private PinyinDictDB() {
    }

    public static PinyinDictDB getInstance() {
        return instance;
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
                // Note：在有 group by 时，只能通过 having 过滤结果，而不能使用 where
                Cursor cursor = db.query(table,
                                         columns,
                                         groupBy != null ? null : where,
                                         params,
                                         groupBy,
                                         groupBy != null ? where : null,
                                         orderBy,
                                         limit)
        ) {
            if (cursor == null) {
                return new ArrayList<>();
            }

            List<T> list = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                T data = creator.apply(cursor);

                if (data != null) {
                    list.add(data);
                }
            }

            return list;
        }
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
        if (isInited()) {
            return;
        }

        this.dbInited = this.executor.submit(() -> {
            doInit(context);
            return true;
        });
    }

    /** 在任意需要启用输入法的情况下调用该开启接口 */
    public synchronized void open(Context context) {
        if (isOpened()) {
            return;
        }

        this.dbOpened = this.executor.submit(() -> {
            // 等待初始化完成后，再开启数据库
            if (isInited()) {
                doOpen(context);
            }
            return true;
        });
    }

    /** 在任意存在完全退出的情况下调用该关闭接口 */
    public synchronized void close() {
        if (isOpened()) {
            doClose();
        }
        this.dbOpened = null;
    }

    public boolean isInited() {
        return Boolean.TRUE.equals(value(this.dbInited));
    }

    public boolean isOpened() {
        return Boolean.TRUE.equals(value(this.dbOpened));
    }

    public void saveUserDB(Context context, OutputStream output) {
        File userDBFile = getUserDBFile(context);

        try (InputStream input = FileUtils.newInput(userDBFile)) {
            ResourceUtils.copy(input, output);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 判断指定的输入是否为有效拼音 */
    public boolean hasValidPinyin(CharInput input) {
        return getPinyinCharsId(input) != null;
    }

    /** 通过拼音字符 id 获取拼音字符 */
    public String getPinyinCharsById(String charsId) {
        for (Map.Entry<String, String> entry : this.pinyinCharsAndIdCache.entrySet()) {
            if (entry.getValue().equals(charsId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 查找指定{@link Key.Level 级别}的拼音后继字母
     *
     * @return 参数为<code>null</code>或为空时，返回<code>null</code>
     */
    public Collection<String> findPinyinNextChar(Key.Level keyLevel, String startChar) {
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

    /**
     * 查找以指定字母开头的拼音
     *
     * @return 参数为<code>null</code>或为空时，返回空集合
     */
    public Collection<String> findPinyinCharsStartsWith(String startChar) {
        if (startChar == null || startChar.isEmpty()) {
            return new ArrayList<>();
        }

        return this.pinyinCharsAndIdCache.keySet().stream().filter(chars -> {
            if (chars.length() >= startChar.length() //
                && chars.startsWith(startChar)) {
                // 平翘舌需相同
                return !(chars.startsWith("ch") || chars.startsWith("sh") || chars.startsWith("zh"))
                       || startChar.startsWith(chars.substring(0, 2));
            }
            return false;
        }).collect(Collectors.toList());
    }

    /** 获取指定拼音的候选字 */
    public List<InputWord> getPinyinCandidateWords(CharInput input) {
        String inputPinyinCharsId = getPinyinCharsId(input);
        if (inputPinyinCharsId == null) {
            return new ArrayList<>();
        }

        return queryPinyinWordsFromAppDB("spell_chars_id_ = ?",//
                                         new String[] { inputPinyinCharsId },//
                                         // Note：拼音的 id 排序即为其字母排序
                                         // 按拼音使用频率（weight_）、拼音内字形相似性（glyph_weight_）、拼音字母顺序（spell_id_）排序
                                         "weight_ desc, glyph_weight_ desc, spell_id_ asc");
    }

    /** 根据前序输入分析得出最靠前的 <code>top</code> 个拼音候选字 */
    public BestCandidateWords findTopBestPinyinCandidateWords(
            CharInput input, int top, List<InputWord> prevPhrase, boolean userDataDisabled
    ) {
        String inputPinyinCharsId = getPinyinCharsId(input);
        if (inputPinyinCharsId == null) {
            return new BestCandidateWords();
        }

        Future<BestCandidateWords> userBestFuture = userDataDisabled
                                                    ? null
                                                    : this.executor.submit(() -> findTopBestPinyinWordsFromUserDB(
                                                            inputPinyinCharsId,
                                                            top,
                                                            prevPhrase));
        Future<BestCandidateWords> appBestFuture = this.executor.submit(() -> findTopBestPinyinWordsFromAppDB(
                inputPinyinCharsId,
                top,
                prevPhrase));

        BestCandidateWords topBest = value(userBestFuture);
        BestCandidateWords appBest = value(appBestFuture);

        if (topBest == null) {
            topBest = appBest;
        } else {
            // 用户字典的常用字优先，不够时，再合并内置字典的高频字
            CollectionUtils.topPatch(topBest.words, top, () -> appBest.words);
            // 短语直接合并两个字典的数据：二者的权重算法不一样，无法直接比较
            topBest.phrases.addAll(appBest.phrases);
        }

        return topBest;
    }

    /** 根据拼音输入分析得出最靠前的 <code>top</code> 个匹配的表情 */
    public List<InputWord> findTopBestEmojisMatchedPhrase(CharInput input, int top, List<InputWord> prevPhrase) {
        return findEmojisMatchedPhraseFromAppDB(input, top, prevPhrase);
    }

    /** 查找最靠前的 <code>top</code> 各表情符号 */
    public Emojis findTopBestEmojis(int top) {
        SQLiteDatabase userDB = getUserDB();
        SQLiteDatabase appDB = getAppDB();

        Map<String, List<InputWord>> groups = new LinkedHashMap<>();
        // 高优先级的分组先占位
        groups.put(Emojis.GROUP_GENERAL, new ArrayList<>());

        Map<String, InputWord> idAndDataMap = new HashMap<>();
        doSQLiteQuery(appDB, "group_emoji", new String[] {
                              "id_", "value_", "group_"
                      }, //
                      null, //
                      null, //
                      "group_ asc, id_ asc", //
                      (cursor) -> {
                          String uid = cursor.getString(0);
                          String value = cursor.getString(1);
                          String group = cursor.getString(2);

                          if (CharUtils.isPrintable(value)) {
                              InputWord emoji = new EmojiInputWord(uid, value);

                              idAndDataMap.put(uid, emoji);
                              groups.computeIfAbsent(group, (k) -> new ArrayList<>(500)).add(emoji);
                          }

                          return null;
                      });

        List<InputWord> used = doSQLiteQuery(userDB, "used_emoji", new String[] {
                                                     "id_"
                                             }, //
                                             null, //
                                             null, //
                                             "weight_ desc", //
                                             String.valueOf(top), //
                                             (cursor) -> idAndDataMap.get(cursor.getString(0)));
        groups.put(Emojis.GROUP_GENERAL, used);

        return new Emojis(groups);
    }

    /** 查找以指定参数开头的最靠前的 <code>top</code> 个拉丁文 */
    public List<String> findTopBestMatchedLatin(String text, int top) {
        if (text == null || text.length() < 3) {
            return new ArrayList<>();
        }

        SQLiteDatabase db = getUserDB();

        List<String> matched = doSQLiteQuery(db, "used_latin", //
                                             new String[] { "value_" }, //
                                             // like 为大小写不敏感的匹配，glob 为大小写敏感匹配
                                             // https://www.sqlitetutorial.net/sqlite-glob/
                                             "weight_ > 0 and value_ glob ?", //
                                             new String[] { text + "*" }, //
                                             "weight_ desc", //
                                             String.valueOf(top), //
                                             (cursor) -> cursor.getString(0));

        return matched;
    }

    /** 根据前序输入的字词，查找最靠前的 <code>top</code> 个拼音短语 */
    public List<List<InputWord>> findTopBestMatchedPinyinPhrase(List<InputWord> prevPhrase, int top) {
        if (prevPhrase == null || prevPhrase.size() < 2) {
            return new ArrayList<>();
        }

        List<List<String>> phraseWordsList = findTopBestMatchedPinyinPhraseWordFromUserDB(prevPhrase, top);
        if (phraseWordsList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> wordIdSet = new HashSet<>();
        phraseWordsList.forEach(wordIdSet::addAll);

        Map<String, InputWord> wordMap = getPinyinWordsFromAppDB(wordIdSet);
        return phraseWordsList.stream()
                              .map((wordIdList) -> wordIdList.stream().map(wordMap::get).collect(Collectors.toList()))
                              //.sorted(Comparator.comparingInt(List::size))
                              .collect(Collectors.toList());
    }

    /** 保存使用数据信息，含短语、单字、表情符号等：异步处理 */
    public void saveUserInputData(UserInputData data) {
        if (data.isEmpty()) {
            return;
        }

        this.executor.execute(() -> {
            data.phrases.forEach(this::doSaveUsedPinyinPhrase);
            doSaveUsedEmojis(data.emojis);
            doSaveUsedLatins(data.latins);
        });
    }

    /** 对 {@link #saveUserInputData} 的撤销处理（异步） */
    public void revokeSavedUserInputData(UserInputData data) {
        if (data.isEmpty()) {
            return;
        }

        this.executor.execute(() -> {
            data.phrases.forEach(this::undoSaveUsedPinyinPhrase);
            undoSaveUsedEmojis(data.emojis);
            undoSaveUsedLatins(data.latins);
        });
    }

    private BestCandidateWords findTopBestPinyinWordsFromUserDB(
            String inputPinyinCharsId, int top, List<InputWord> prevPhrase
    ) {
        return findTopBestPinyinWordsFromDB(getUserDB(),
                                            "used_pinyin_word",
                                            "used_pinyin_phrase",
                                            inputPinyinCharsId,
                                            top,
                                            prevPhrase);
    }

    private BestCandidateWords findTopBestPinyinWordsFromAppDB(
            String inputPinyinCharsId, int top, List<InputWord> prevPhrase
    ) {
        return findTopBestPinyinWordsFromDB(getAppDB(),
                                            "link_word_with_pinyin",
                                            "pinyin_phrase",
                                            inputPinyinCharsId,
                                            top,
                                            prevPhrase);
    }

    /**
     * @param top
     *         为 0 时，{@link BestCandidateWords#words} 为空，
     *         但 {@link BestCandidateWords#phrases} 依然是按权重降序排序后的最佳短语
     */
    private BestCandidateWords findTopBestPinyinWordsFromDB(
            SQLiteDatabase db, String wordTable, String phraseTable, String inputPinyinCharsId, int top,
            List<InputWord> prevPhrase
    ) {
        List<InputWord> pinyinWords = prevPhrase != null ? new ArrayList<>(prevPhrase) : new ArrayList<>();
        Collections.reverse(pinyinWords);

        // 匹配短语中的常用字：倒序分析
        List<String> inputPhraseWordCharsIdList = pinyinWords.stream()
                                                             .map((word) -> ((PinyinInputWord) word).getCharsId())
                                                             .collect(Collectors.toList());
        inputPhraseWordCharsIdList.add(0, inputPinyinCharsId);

        // 已确认的拼音字 id
        List<String> confirmedPhraseWordIdList = pinyinWords.stream()
                                                            .map(word -> word.isConfirmed() ? word.getUid() : null)
                                                            .collect(Collectors.toList());
        confirmedPhraseWordIdList.add(0, null);

        Set<String> invalidPhraseIdSet = new HashSet<>();

        Map<String, List<String[]>> bestPhraseMap = new LinkedHashMap<>();
        doSQLiteQuery(db, phraseTable, new String[] {
                              "source_id_", "target_id_", "target_spell_chars_id_", "target_index_"
                      }, //
                      "weight_ > 0 and target_spell_chars_id_ in (" //
                      + inputPhraseWordCharsIdList.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
                      + ")", //
                      inputPhraseWordCharsIdList.toArray(new String[0]), //
                      "weight_ desc, source_id_ asc" +
                      // Note：只有一个字时，应该将其视为短语的开头（升序排序短语中的字），
                      // 否则，视其为短语的结尾（降序排序短语中的字）
                      (", target_index_ " + (inputPhraseWordCharsIdList.size() == 1 ? "asc" : "desc")), //
                      (cursor) -> {
                          String phraseId = cursor.getString(0);
                          if (invalidPhraseIdSet.contains(phraseId)) {
                              return null;
                          }

                          String phraseWordId = cursor.getString(1);
                          String phraseWordCharsId = cursor.getString(2);
                          String phraseWordIndex = cursor.getString(3);

                          List<String[]> phraseWords = bestPhraseMap.computeIfAbsent(phraseId,
                                                                                     (k) -> new ArrayList<>());

                          int phraseWordSize = phraseWords.size();
                          String[] prev = CollectionUtils.last(phraseWords);
                          if ( // 去掉 搜索的字 在 短语 中 不相邻 的数据：对短语内的字顺序做了降序处理，故而，prev 的序号应该比当前字的序号更大
                                  (prev != null //
                                   && Integer.parseInt(prev[1]) - Integer.parseInt(phraseWordIndex) != 1)
                                  // 去掉与 查询短语 在 相同位置 读音（或已确认的字）不匹配 的数据
                                  || (phraseWordSize < inputPhraseWordCharsIdList.size() //
                                      && (!inputPhraseWordCharsIdList.get(phraseWordSize).equals(phraseWordCharsId) //
                                          || (confirmedPhraseWordIdList.get(phraseWordSize) != null //
                                              && !confirmedPhraseWordIdList.get(phraseWordSize)
                                                                           .equals(phraseWordId))))) {
                              invalidPhraseIdSet.add(phraseId);

                              // Note：及时删除有助于匹配某个短语内部的部分组合
                              bestPhraseMap.remove(phraseId);

                              return null;
                          }

                          phraseWords.add(new String[] { phraseWordId, phraseWordIndex, phraseWordCharsId });

                          return null;
                      });

        List<String[]> bestPhraseWordIdsList = bestPhraseMap.values()
                                                            .stream()
                                                            .map(tupleList -> tupleList.stream()
                                                                                       .map(tuple -> tuple[0])
                                                                                       .toArray(String[]::new))
                                                            // 根据匹配短语长度排序，长度越长，其匹配性最佳。
                                                            // 长度相同的，再看其权重大小。
                                                            // 不过，原结果已经是权重排序结果，故而，长度相同的保持位置不变即可
                                                            .sorted((a1, a2) -> Integer.compare(a2.length, a1.length))
                                                            .collect(Collectors.toList());
        List<String[]> bestPhrases = bestPhraseWordIdsList.stream()
                                                          .filter(phrase -> phrase.length > 1)
                                                          .collect(Collectors.toList());

        // Note：短语中的最佳候选字，仅针对短语长度大于 1 的情况，等于 1 的就应该使用高频的单字
        Set<String> firstWordIdInBestPhrasesSet = bestPhrases.stream()
                                                             .map(phrase -> phrase[0])
                                                             .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> bestWords = CollectionUtils.topPatch(new ArrayList<>(firstWordIdInBestPhrasesSet), top,
                                                          // 匹配高频字
                                                          () -> doSQLiteQuery(db, wordTable,
                                                                              //
                                                                              new String[] { "id_" },
                                                                              //
                                                                              "weight_ > 0 and target_chars_id_ = ?",
                                                                              //
                                                                              new String[] { inputPinyinCharsId },
                                                                              //
                                                                              "weight_ desc", String.valueOf(top),
                                                                              //
                                                                              (cursor) -> cursor.getString(0)));

        return new BestCandidateWords(bestWords, bestPhrases);
    }

    private List<List<String>> findTopBestMatchedPinyinPhraseWordFromUserDB(List<InputWord> prevPhrase, int top) {
        SQLiteDatabase db = getUserDB();

        String prevPhraseWordIds = prevPhrase.stream().map(InputWord::getUid).collect(Collectors.joining(",")) + ",";

        Map<String, String> bestPhraseMap = new LinkedHashMap<>();
        doSQLiteQuery(db, "used_pinyin_phrase", new String[] {
                              "source_id_", "group_concat(target_id_) as target_ids_",
                              }, //
                      "weight_ > 0 and (target_ids_ like ? or target_ids_ like ?)", //
                      new String[] { prevPhraseWordIds + "%", "%," + prevPhraseWordIds + "%" }, //
                      "source_id_", //
                      // Note：视图 used_pinyin_phrase 已默认按 target_id_ 升序排序
                      "weight_ desc", //
                      String.valueOf(top), //
                      (cursor) -> {
                          String phraseId = cursor.getString(0);
                          String phraseWordIds = cursor.getString(1);

                          bestPhraseMap.put(phraseId, phraseWordIds);
                          return null;
                      });

        Set<List<String>> matchedPhraseWordsSet = new LinkedHashSet<>();
        bestPhraseMap.forEach((k, phraseWordIds) -> {
            // Note：匹配位置不是在开头就是在中间
            int index = phraseWordIds.indexOf("," + prevPhraseWordIds);
            String matchedPhraseWordIds = phraseWordIds.substring(index + 1);

            matchedPhraseWordsSet.add(Arrays.asList(matchedPhraseWordIds.split(",")));
        });

        return new ArrayList<>(matchedPhraseWordsSet);
    }

    private Map<String, InputWord> getPinyinWordsFromAppDB(Collection<String> wordIds) {
        List<InputWord> wordList = queryPinyinWordsFromAppDB("id_ in ("
                                                             + wordIds.stream()
                                                                      .map((id) -> "?")
                                                                      .collect(Collectors.joining(", "))
                                                             + ")", //
                                                             wordIds.toArray(new String[0]), //
                                                             null);

        return wordList.stream().collect(Collectors.toMap(InputWord::getUid, Function.identity()));
    }

    /** 查找拼音候选字字的变体 */
    private Map<String, List<String>> findPinyinWordVariants(
            SQLiteDatabase db, String table, Collection<String> sourceIds
    ) {
        if (sourceIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<String>> map = new HashMap<>(sourceIds.size());
        doSQLiteQuery(db, table, new String[] { "source_id_", "target_id_" },
                      //
                      "source_id_ in (" //
                      + sourceIds.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
                      + ")", //
                      sourceIds.toArray(new String[0]), //
                      (cursor) -> {
                          String sourceId = cursor.getString(0);
                          String targetId = cursor.getString(1);
                          map.computeIfAbsent(sourceId, (k) -> new ArrayList<>()).add(targetId);

                          return null;
                      });
        return map;
    }

    private List<InputWord> findEmojisMatchedPhraseFromAppDB(CharInput input, int top, List<InputWord> prevPhrase) {
        SQLiteDatabase db = getAppDB();

        List<String> phraseWordUidList = prevPhrase.stream().map(InputWord::getUid).collect(Collectors.toList());
        Collections.reverse(phraseWordUidList);

        phraseWordUidList.add(0, input.getWord().getUid());

        Set<String> invalidEmojiKeywordIdSet = new HashSet<>();
        Map<String, List<String[]>> keywordAndWordIndexesMap = new HashMap<>();

        Map<String, InputWord> emojiMap = new HashMap<>();
        doSQLiteQuery(db, "emoji", new String[] {
                              "id_", "value_", "keyword_index_", "keyword_word_spell_link_id_", "keyword_word_index_"
                      }, //
                      "keyword_word_spell_link_id_ in (" //
                      + phraseWordUidList.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
                      + ")", //
                      phraseWordUidList.toArray(new String[0]), //
                      "id_ asc, keyword_index_ asc, keyword_word_index_ desc", //
                      (cursor) -> {
                          String emojiId = cursor.getString(0);
                          String emojiValue = cursor.getString(1);
                          // 表情的关键字唯一标识由 表情 id 和 关键字的序号 组合而成
                          String emojiKeywordId = emojiId + ":" + cursor.getString(2);
                          String emojiKeywordWordUid = cursor.getString(3);
                          String emojiKeywordWordIndex = cursor.getString(4);

                          if (!CharUtils.isPrintable(emojiValue) //
                              || invalidEmojiKeywordIdSet.contains(emojiKeywordId)) {
                              return null;
                          }

                          List<String[]> keywordAndWordIndexes
                                  = keywordAndWordIndexesMap.computeIfAbsent(emojiKeywordId, (k) -> new ArrayList<>());

                          int keywordWordSize = keywordAndWordIndexes.size();
                          String[] prev = CollectionUtils.last(keywordAndWordIndexes);
                          if ( // 去掉 关键字 在 短语 中 不相邻 的数据
                                  (prev != null //
                                   && Integer.parseInt(prev[1]) - Integer.parseInt(emojiKeywordWordIndex) != 1)
                                  // 去掉与 短语 在 相同位置的字 不匹配 的数据
                                  || (keywordWordSize < phraseWordUidList.size() //
                                      && (!phraseWordUidList.get(keywordWordSize).equals(emojiKeywordWordUid)))) {
                              invalidEmojiKeywordIdSet.add(emojiKeywordId);

                              keywordAndWordIndexesMap.remove(emojiKeywordId);

                              return null;
                          }

                          keywordAndWordIndexesMap.computeIfAbsent(emojiKeywordId, (k) -> new ArrayList<>())
                                                  .add(new String[] {
                                                          emojiKeywordWordUid, emojiKeywordWordIndex, emojiId
                                                  });

                          if (!emojiMap.containsKey(emojiId)) {
                              InputWord emoji = new EmojiInputWord(emojiId, emojiValue);
                              emojiMap.put(emojiId, emoji);
                          }

                          return null;
                      });

        // 按关键字匹配到的输入长度排序，匹配越长的越靠前
        Map<String, Integer> emojiAndWeightMap = new HashMap<>();
        keywordAndWordIndexesMap.forEach((keywordId, tupleList) -> {
            tupleList.forEach((tuple) -> {
                String emojiId = tuple[2];
                int weight = emojiAndWeightMap.getOrDefault(emojiId, 0);

                emojiAndWeightMap.put(emojiId, tupleList.size() > weight ? tupleList.size() : weight);
            });
        });

        List<InputWord> emojiList = emojiAndWeightMap.keySet().stream().map(emojiMap::get).sorted((a1, a2) -> {
            int a1Weight = emojiAndWeightMap.get(a1.getUid());
            int a2Weight = emojiAndWeightMap.get(a2.getUid());
            int order = Integer.compare(a2Weight, a1Weight);
            // Note：相邻 id 的表情更有可能在同一个分组内，
            // 故而，对匹配度相同的表情采用 id 排序
            return order != 0 //
                   ? order //
                   : Integer.compare(Integer.parseInt(a1.getUid()), Integer.parseInt(a2.getUid()));
        }).collect(Collectors.toList());

        return CollectionUtils.subList(emojiList, 0, top);
    }

    private List<InputWord> queryPinyinWordsFromAppDB(String where, String[] params, String orderBy) {
        SQLiteDatabase db = getAppDB();

        List<InputWord> wordList = doSQLiteQuery(db, "pinyin_word", //
                                                 new String[] {
                                                         "id_",
                                                         "word_",
                                                         "spell_",
                                                         "word_id_",
                                                         "spell_chars_id_",
                                                         "traditional_",
                                                         "stroke_order_",
                                                         }, //
                                                 where, params, orderBy, //
                                                 (cursor) -> {
                                                     String uid = cursor.getString(0);
                                                     String value = cursor.getString(1);
                                                     String notation = cursor.getString(2);
                                                     String wordId = cursor.getString(3);
                                                     String pinyinCharsId = cursor.getString(4);
                                                     boolean traditional = cursor.getInt(5) > 0;
                                                     String strokeOrder = cursor.getString(6);

                                                     return (InputWord) new PinyinInputWord(uid,
                                                                                            value,
                                                                                            notation,
                                                                                            wordId,
                                                                                            pinyinCharsId,
                                                                                            traditional,
                                                                                            strokeOrder);
                                                 });

        // 查找繁/简字
        patchPinyinWordVariantFromAppDB(wordList);

        return wordList;
    }

    private void patchPinyinWordVariantFromAppDB(Collection<InputWord> words) {
        SQLiteDatabase db = getAppDB();

        Map<String, InputWord> tradWordMap = new HashMap<>();
        Map<String, InputWord> simpleWordMap = new HashMap<>();

        words.forEach((word) -> {
            String wordId = ((PinyinInputWord) word).getWordId();
            if (((PinyinInputWord) word).isTraditional()) {
                tradWordMap.put(wordId, word);
            } else {
                simpleWordMap.put(wordId, word);
            }
        });

        // 查找繁/简字
        Future<Map<String, List<String>>> tradWithSimpleWordIdMapFuture = tradWordMap.isEmpty()
                                                                          ? null
                                                                          : this.executor.submit(() -> findPinyinWordVariants(
                                                                                  db,
                                                                                  "link_word_with_simple_word",
                                                                                  tradWordMap.keySet()));
        Future<Map<String, List<String>>> simpleWithTradWordIdMapFuture = simpleWordMap.isEmpty()
                                                                          ? null
                                                                          : this.executor.submit(() -> findPinyinWordVariants(
                                                                                  db,
                                                                                  "link_word_with_traditional_word",
                                                                                  simpleWordMap.keySet()));

        value(tradWithSimpleWordIdMapFuture, new HashMap<>()).forEach((sourceId, targetIds) -> {
            InputWord sourceWord = tradWordMap.get(sourceId);
            if (sourceWord == null || sourceWord.getVariant() != null) {
                return;
            }

            targetIds.forEach((targetId) -> {
                InputWord targetWord = simpleWordMap.get(targetId);
                // 读音需一致
                if (targetWord != null && targetWord.getNotation().equals(sourceWord.getNotation())) {
                    sourceWord.setVariant(targetWord.getValue());
                }
            });
        });

        value(simpleWithTradWordIdMapFuture, new HashMap<>()).forEach((sourceId, targetIds) -> {
            InputWord sourceWord = simpleWordMap.get(sourceId);
            if (sourceWord == null || sourceWord.getVariant() != null) {
                return;
            }

            targetIds.forEach((targetId) -> {
                InputWord targetWord = tradWordMap.get(targetId);
                // 读音需一致
                if (targetWord != null && targetWord.getNotation().equals(sourceWord.getNotation())) {
                    sourceWord.setVariant(targetWord.getValue());
                }
            });
        });
    }

    private void doSaveUsedPinyinPhrase(List<InputWord> phrase) {
        if (phrase.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedWordInPinyinPhrase(db, phrase, true);

        if (phrase.size() < 2) {
            return;
        }

        String phraseCode = calculatePinyinPhraseCode(phrase);
        boolean isNew = doSaveUsedPinyinPhrase(db, phraseCode, true);
        if (!isNew) {
            return;
        }

        // 保存短语中的字
        doSQLiteSave(db,
                     "insert into"
                     + " used_phrase_pinyin_word"
                     + " (source_id_, target_id_, target_spell_chars_id_, target_index_)"
                     + " values ((select id_ from used_phrase where value_ = ?), ?, ?, ?)",
                     null,
                     (insert, _ignore) -> {
                         for (int i = 0; i < phrase.size(); i++) {
                             InputWord word = phrase.get(i);

                             insert.bindAllArgsAsStrings(new String[] {
                                     phraseCode, word.getUid(), ((PinyinInputWord) word).getCharsId(), String.valueOf(i)
                             });
                             insert.execute();
                         }
                     });
    }

    /** 保存表情的使用频率等信息 */
    private void doSaveUsedEmojis(List<InputWord> emojis) {
        if (emojis.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedEmojis(db, emojis, true);
    }

    /** 保存拉丁文的使用频率等信息 */
    private void doSaveUsedLatins(Set<String> latins) {
        // 仅针对长单词
        Set<String> validLatins = latins.stream().filter(this::isValidUsedLatin).collect(Collectors.toSet());
        if (validLatins.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedLatins(db, validLatins, true);
    }

    /** 撤销 {@link #doSaveUsedPinyinPhrase} */
    private void undoSaveUsedPinyinPhrase(List<InputWord> phrase) {
        if (phrase.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedWordInPinyinPhrase(db, phrase, false);

        if (phrase.size() < 2) {
            return;
        }

        String phraseCode = calculatePinyinPhraseCode(phrase);
        doSaveUsedPinyinPhrase(db, phraseCode, false);
    }

    /** 撤销 {@link #doSaveUsedEmojis} */
    private void undoSaveUsedEmojis(List<InputWord> emojis) {
        if (emojis.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedEmojis(db, emojis, false);
    }

    /** 撤销 {@link #doSaveUsedLatins} */
    private void undoSaveUsedLatins(Set<String> latins) {
        if (latins.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getUserDB();
        doSaveUsedLatins(db, latins, false);
    }

    /** @return 若为新增，则返回 <code>true</code>，否则，返回 <code>false</code> */
    private boolean doSaveUsedPinyinPhrase(SQLiteDatabase db, String phraseCode, boolean shouldIncreaseWeight) {
        Map<String, Integer> oldDataMap = doUpdateDataWeight(db,
                                                             "used_phrase",
                                                             "value_",
                                                             Collections.singleton(phraseCode),
                                                             null,
                                                             "insert into used_phrase (weight_, value_) values (?, ?)",
                                                             "update used_phrase set weight_ = ? where value_ = ?",
                                                             new String[] {
                                                                     "delete from used_phrase_pinyin_word"
                                                                     + " where source_id_ in (select id_ from used_phrase where weight_ = 0)",
                                                                     "delete from used_phrase where weight_ = 0"
                                                             },
                                                             shouldIncreaseWeight);
        return !oldDataMap.containsKey(phraseCode);
    }

    /** 保存拼音短语中字的使用频率等信息 */
    private void doSaveUsedWordInPinyinPhrase(SQLiteDatabase db, List<InputWord> phrase, boolean shouldIncreaseWeight) {
        Map<String, String> idAndTargetCharsIdMap = phrase.stream()
                                                          .collect(Collectors.toMap(InputWord::getUid,
                                                                                    (word) -> ((PinyinInputWord) word).getCharsId(),
                                                                                    (v1, v2) -> v1));

        doUpdateDataWeight(db,
                           "used_pinyin_word",
                           "id_",
                           idAndTargetCharsIdMap.keySet(),
                           (id) -> new String[] { idAndTargetCharsIdMap.get(id) },
                           "insert into used_pinyin_word (weight_, id_, target_chars_id_) values (?, ?, ?)",
                           "update used_pinyin_word set weight_ = ? where id_ = ?",
                           new String[] { "delete from used_pinyin_word where weight_ = 0" },
                           shouldIncreaseWeight);
    }

    private void doSaveUsedEmojis(SQLiteDatabase db, List<InputWord> emojis, boolean shouldIncreaseWeight) {
        Set<String> dataIdSet = emojis.stream().map(InputWord::getUid).collect(Collectors.toSet());

        doUpdateDataWeight(db,
                           "used_emoji",
                           "id_",
                           dataIdSet,
                           null,
                           "insert into used_emoji (weight_, id_) values (?, ?)",
                           "update used_emoji set weight_ = ? where id_ = ?",
                           new String[] { "delete from used_emoji where weight_ = 0" },
                           shouldIncreaseWeight);
    }

    private void doSaveUsedLatins(SQLiteDatabase db, Set<String> latins, boolean shouldIncreaseWeight) {
        doUpdateDataWeight(db,
                           "used_latin",
                           "value_",
                           latins,
                           null,
                           "insert into used_latin (weight_, value_) values (?, ?)",
                           "update used_latin set weight_ = ? where value_ = ?",
                           new String[] { "delete from used_latin where weight_ = 0" },
                           shouldIncreaseWeight);
    }

    private String getPinyinCharsId(CharInput input) {
        String pinyinChars = String.join("", input.getChars());

        return this.pinyinCharsAndIdCache.get(pinyinChars);
    }

    private String calculatePinyinPhraseCode(List<InputWord> phrase) {
        int sum = 0;
        for (int i = 0; i < phrase.size(); i++) {
            InputWord word = phrase.get(i);
            int code = Integer.parseInt(word.getUid() + i);
            sum += code;
        }

        return String.valueOf(sum) + phrase.size();
    }

    private boolean isValidUsedLatin(String text) {
        return text != null && text.length() > 3;
    }

    /**
     * 更新数据权重，并将权重为 0 的数据删除
     *
     * @return 返回更新前的数据唯一标识和权重映射数据
     */
    private Map<String, Integer> doUpdateDataWeight(
            SQLiteDatabase db, String weightTable, String dataIdField, Collection<String> dataIds,
            Function<String, String[]> insertExtraArgsSupplier, String insertSQL, String updateSQL, String[] deleteSQLs,
            boolean shouldIncreaseWeight
    ) {
        Map<String, Integer> existDataMap = new HashMap<>();
        doSQLiteQuery(db,
                      weightTable,
                      new String[] { dataIdField, "weight_" },
                      dataIdField + " in (" + dataIds.stream().map((id) -> "?").collect(Collectors.joining(", ")) + ")",
                      dataIds.toArray(new String[0]),
                      (cursor) -> {
                          existDataMap.put(cursor.getString(0), cursor.getInt(1));
                          return null;
                      });

        doSQLiteSave(db, insertSQL, updateSQL, (insert, update) -> {
            dataIds.forEach((dataId) -> {
                int dataWeight = existDataMap.getOrDefault(dataId, 0);

                if (shouldIncreaseWeight) {
                    dataWeight += 1;
                } else {
                    dataWeight = Math.max(0, dataWeight - 1);
                }

                if (existDataMap.containsKey(dataId)) {
                    update.bindAllArgsAsStrings(new String[] { String.valueOf(dataWeight), dataId });
                    update.execute();
                } else {
                    String[] args = new String[] { String.valueOf(dataWeight), dataId };
                    if (insertExtraArgsSupplier != null) {
                        String[] extraArgs = insertExtraArgsSupplier.apply(dataId);

                        args = Stream.concat(Arrays.stream(args), Arrays.stream(extraArgs)).toArray(String[]::new);
                    }

                    insert.bindAllArgsAsStrings(args);
                    insert.execute();
                }
            });
        });

        for (String deleteSQL : deleteSQLs) {
            doSQLiteSave(db, deleteSQL, null, (delete, _ignore) -> delete.execute());
        }

        return existDataMap;
    }

    private void doSQLiteSave(SQLiteDatabase db, String sql_1, String sql_2, SQLiteSaver saver) {
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

    private SQLiteDatabase getAppDB() {
        return isOpened() ? this.appDB : null;
    }

    private SQLiteDatabase getUserDB() {
        return isOpened() ? this.userDB : null;
    }

    private void doInit(Context context) {
        File appDBFile = getAppDBFile(context);
        copySQLite(context, appDBFile, R.raw.pinyin_dict, R.raw.pinyin_dict_db_hash);

        try (SQLiteDatabase appDB = openSQLite(appDBFile, false);) {
            initAppDB(appDB);
        }

        File userDBFile = getUserDBFile(context);
        try (SQLiteDatabase userDB = openSQLite(userDBFile, false);) {
            initUserDB(userDB);
        }
    }

    private void doOpen(Context context) {
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
                      null, null, (cursor) -> {
                    // Note: android sqlite 从 0 开始取，与 jdbc 的规范不一样
                    this.pinyinCharsAndIdCache.put(cursor.getString(1), cursor.getString(0));
                    return null;
                });
    }

    private void doClose() {
        closeSQLite(this.appDB);
        closeSQLite(this.userDB);

        this.appDB = null;
        this.userDB = null;
        this.pinyinCharsAndIdCache = null;
    }

    private void configSQLite(SQLiteDatabase db) {
        String[] clauses = new String[] {
                "PRAGMA cache_size = 2500;", "PRAGMA temp_store = MEMORY;",
                };
        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    private void initUserDB(SQLiteDatabase db) {
        String[] clauses = new String[] {
                //
                "CREATE TABLE\n"
                + "    IF NOT EXISTS used_pinyin_word (\n"
                // -- id_, target_chars_id_ 与内置字典中的 link_word_with_pinyin 表的数据保持一致
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                + "        target_chars_id_ INTEGER NOT NUll,\n"
                + "        weight_ INTEGER DEFAULT 0\n"
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_pinyin_word ON used_pinyin_word (weight_, target_chars_id_);",
                //
                "CREATE TABLE\n" //
                + "    IF NOT EXISTS used_phrase (\n" //
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                // -- 短语内容标识：由 used_phrase_pinyin_word 中的 target_id_ 拼接而成
                + "        value_ TEXT NOT NULL,\n"
                // -- 按使用频率等排序的权重
                + "        weight_ INTEGER DEFAULT 0,\n" //
                + "        UNIQUE (value_)\n" //
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_phrase ON used_phrase (weight_, value_);",
                "CREATE TABLE\n"
                + "    IF NOT EXISTS used_phrase_pinyin_word (\n"
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                // -- used_phrase 中的 id_
                + "        source_id_ INTEGER NOT NULL,\n"
                // -- 与内置字典中的 link_word_with_pinyin 表的 id_ 一致
                + "        target_id_ INTEGER NOT NULL,\n"
                // -- 拼音字母组合 id
                + "        target_spell_chars_id_ INTEGER NOT NULL,\n"
                // -- 字在词中的序号
                + "        target_index_ INTEGER NOT NULL,\n"
                + "        UNIQUE (source_id_, target_id_, target_index_),\n"
                + "        FOREIGN KEY (source_id_) REFERENCES used_phrase (id_)\n"
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_phrase_pinyin_word"
                + " ON used_phrase_pinyin_word (target_spell_chars_id_, source_id_, target_index_);",
                //
                //"DROP VIEW IF EXISTS used_pinyin_phrase;",
                "CREATE VIEW\n"
                + "    IF NOT EXISTS used_pinyin_phrase (\n"
                + "        id_,\n"
                + "        weight_,\n"
                + "        source_id_,\n"
                + "        target_id_,\n"
                + "        target_index_,\n"
                + "        target_spell_chars_id_\n"
                + "    ) AS\n"
                + "SELECT\n"
                + "    lnk_.id_,\n"
                + "    phrase_.weight_,\n"
                + "    lnk_.source_id_,\n"
                + "    lnk_.target_id_,\n"
                + "    lnk_.target_index_,\n"
                + "    lnk_.target_spell_chars_id_\n"
                + "FROM\n"
                + "    used_phrase phrase_\n"
                + "    INNER JOIN used_phrase_pinyin_word lnk_ on lnk_.source_id_ = phrase_.id_"
                + "-- Note: group by 不能对组内元素排序，故，只能在视图内先排序\n"
                + "ORDER BY\n"
                + "    lnk_.target_index_ asc;",
                //
                "CREATE TABLE\n" //
                + "    IF NOT EXISTS used_emoji (\n" //
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n" //
                // -- 按使用频率等排序的权重
                + "        weight_ INTEGER DEFAULT 0\n" //
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_emoji ON used_emoji (weight_);",
                //
                "CREATE TABLE\n" //
                + "    IF NOT EXISTS used_latin (\n" //
                + "        id_ INTEGER NOT NULL PRIMARY KEY,\n"
                // -- 拉丁文内容
                + "        value_ TEXT NOT NULL,\n"
                // -- 按使用频率等排序的权重
                + "        weight_ INTEGER DEFAULT 0,\n" //
                + "        UNIQUE (value_)\n" //
                + "    );",
                "CREATE INDEX IF NOT EXISTS idx_used_latin ON used_latin (weight_, value_);",
                };

        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    private void initAppDB(SQLiteDatabase db) {
        String[] clauses = new String[] {
                // 创建索引以加速查询
                "CREATE INDEX IF NOT EXISTS idx_link_word_with_pinyin"
                + " ON link_word_with_pinyin"
                + " (target_chars_id_, weight_, glyph_weight_, target_id_);",
                //
                "CREATE INDEX IF NOT EXISTS idx_meta_phrase ON meta_phrase (weight_);",
                "CREATE INDEX IF NOT EXISTS idx_link_phrase_with_pinyin_word"
                + " ON link_phrase_with_pinyin_word"
                + " (target_spell_chars_id_, source_id_, target_index_);",
                };

        for (String clause : clauses) {
            db.execSQL(clause);
        }
    }

    private <T> T value(Future<T> f) {
        return value(f, null);
    }

    private <T> T value(Future<T> f, T defaultVale) {
        try {
            return f != null ? f.get() : defaultVale;
        } catch (Exception e) {
            return defaultVale;
        }
    }

    private interface SQLiteSaver {
        void save(SQLiteStatement statement_1, SQLiteStatement statement_2);
    }
}
