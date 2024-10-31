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

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v0;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v2_to_v3;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.Async;
import org.crazydan.studio.app.ime.kuaizi.utils.CollectionUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.queryPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.closeSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.configSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;

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
public class PinyinDict {
    public static final String VERSION_V0 = "v0";
    public static final String VERSION_V2 = "v2";
    public static final String VERSION_V3 = "v3";
    /** 最新版本号 */
    public static final String LATEST_VERSION = VERSION_V3;
    /** 首次安装版本号 */
    public static final String FIRST_INSTALL_VERSION = VERSION_V0;

    private static final String db_version_file = "pinyin_user_dict.version";

    private static final PinyinDict instance = new PinyinDict();

    /** 用户词组数据的基础权重，以确保用户输入权重大于应用词组数据 */
    private final int userPhraseBaseWeight = 500;

    private Future<Boolean> dbInited;
    private Future<Boolean> dbOpened;

    private String version;
    /** 用户库 */
    private SQLiteDatabase db;

    // <<<<<<<<<<<<< 缓存常量数据
    private PinyinTree pinyinTree;
    // >>>>>>>>>>>>>

    private PinyinDict() {
    }

    public static PinyinDict instance() {
        return instance;
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

        this.dbInited = Async.executor.submit(() -> {
            doInit(context);
            return true;
        });
    }

    /** 在任意需要启用输入法的情况下调用该开启接口 */
    public synchronized void open(Context context) {
        if (isOpened()) {
            return;
        }

        this.dbOpened = Async.executor.submit(() -> {
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

    private boolean isInited() {
        return Boolean.TRUE.equals(value(this.dbInited));
    }

    private boolean isOpened() {
        return Boolean.TRUE.equals(value(this.dbOpened));
    }

    public File getDBFile(Context context, PinyinDictDBType dbType) {
        return new File(context.getFilesDir(), dbType.fileName);
    }

    public void saveUserDB(Context context, OutputStream output) {
        File userDBFile = getUserDBFile(context);

        try (InputStream input = FileUtils.newInput(userDBFile)) {
            ResourceUtils.copy(input, output);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public PinyinTree getPinyinTree() {
        return this.pinyinTree;
    }

    /** 获取指定拼音的候选字列表：已按权重等排序 */
    public List<InputWord> getPinyinCandidateWords(CharInput input) {
        String pinyinCharsId = this.pinyinTree.getPinyinCharsId(input);
        if (pinyinCharsId == null) {
            return List.of();
        }

        SQLiteDatabase db = getDB();
        List<InputWord> wordList = queryPinyinInputWords(db, "py_.spell_chars_id_ = ?", new String[] {
                pinyinCharsId, PinyinDict.this.userPhraseBaseWeight + ""
        }, true).stream().map(w -> (InputWord) w).collect(Collectors.toList());

        patchPinyinWordVariantInCandidates(db, wordList);

        return wordList;
    }

    /** 根据前序输入分析得出最靠前的 <code>top</code> 个拼音候选字 */
    public BestCandidateWords findTopBestPinyinCandidateWords(
            CharInput input, int top, List<InputWord> prevPhrase, boolean userDataDisabled
    ) {
        String inputPinyinCharsId = this.pinyinTree.getPinyinCharsId(input);
        if (inputPinyinCharsId == null) {
            return new BestCandidateWords();
        }

        Future<BestCandidateWords> userBestFuture = userDataDisabled
                                                    ? null
                                                    : Async.executor.submit(() -> findTopBestPinyinWordsFromUserDB(
                                                            inputPinyinCharsId,
                                                            top,
                                                            prevPhrase));
        Future<BestCandidateWords> appBestFuture = Async.executor.submit(() -> findTopBestPinyinWordsFromAppDB(
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
//        SQLiteDatabase userDB = getUserDB();
//        SQLiteDatabase appDB = getAppDB();
//
//        Map<String, List<InputWord>> groups = new LinkedHashMap<>();
//        // 高优先级的分组先占位
//        groups.put(Emojis.GROUP_GENERAL, new ArrayList<>());
//
//        Map<String, InputWord> idAndDataMap = new HashMap<>();
//        doSQLiteQuery(appDB, "group_emoji", new String[] {
//                              "id_", "value_", "group_"
//                      }, //
//                      null, //
//                      null, //
//                      "group_ asc, id_ asc", //
//                      (cursor) -> {
//                          String uid = cursor.getString(0);
//                          String value = cursor.getString(1);
//                          String group = cursor.getString(2);
//
//                          if (CharUtils.isPrintable(value)) {
//                              InputWord emoji = new EmojiInputWord(uid, value);
//
//                              idAndDataMap.put(uid, emoji);
//                              groups.computeIfAbsent(group, (k) -> new ArrayList<>(500)).add(emoji);
//                          }
//
//                          return null;
//                      });
//
//        List<InputWord> used = doSQLiteQuery(userDB, "used_emoji", new String[] {
//                                                     "id_"
//                                             }, //
//                                             null, //
//                                             null, //
//                                             "weight_ desc", //
//                                             String.valueOf(top), //
//                                             (cursor) -> idAndDataMap.get(cursor.getString(0)));
//        groups.put(Emojis.GROUP_GENERAL, used);

        return new Emojis(new HashMap<>());
    }

    /** 查找以指定参数开头的最靠前的 <code>top</code> 个拉丁文 */
    public List<String> findTopBestMatchedLatin(String text, int top) {
        if (text == null || text.length() < 3) {
            return new ArrayList<>();
        }

        SQLiteDatabase db = getDB();

//        List<String> matched = doSQLiteQuery(db, "used_latin", //
//                                             new String[] { "value_" }, //
//                                             // like 为大小写不敏感的匹配，glob 为大小写敏感匹配
//                                             // https://www.sqlitetutorial.net/sqlite-glob/
//                                             "weight_ > 0 and value_ glob ?", //
//                                             new String[] { text + "*" }, //
//                                             "weight_ desc", //
//                                             String.valueOf(top), //
//                                             (cursor) -> cursor.getString(0));

        return List.of();
    }

    /** 根据前序输入的字词，查找最靠前的 <code>top</code> 个拼音短语 */
    public List<List<InputWord>> findTopBestMatchedPinyinPhrase(
            List<InputWord> prevPhrase, int top, boolean variantFirst
    ) {
        if (prevPhrase == null || prevPhrase.size() < 2) {
            return new ArrayList<>();
        }

        List<List<String>> phraseWordsList = findTopBestMatchedPinyinPhraseWord(prevPhrase, top);
        if (phraseWordsList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> wordIdSet = new HashSet<>();
        phraseWordsList.forEach(wordIdSet::addAll);

        Map<String, InputWord> wordMap = getPinyinWords(wordIdSet);
        if (variantFirst) {
            patchPinyinWordVariant(wordMap.values());
        }

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

        Async.executor.execute(() -> {
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

        Async.executor.execute(() -> {
            data.phrases.forEach(this::undoSaveUsedPinyinPhrase);
            undoSaveUsedEmojis(data.emojis);
            undoSaveUsedLatins(data.latins);
        });
    }

    private BestCandidateWords findTopBestPinyinWordsFromUserDB(
            String inputPinyinCharsId, int top, List<InputWord> prevPhrase
    ) {
        return findTopBestPinyinWordsFromDB(getDB(),
                                            "used_pinyin_word",
                                            "used_pinyin_phrase",
                                            inputPinyinCharsId,
                                            top,
                                            prevPhrase);
    }

    private BestCandidateWords findTopBestPinyinWordsFromAppDB(
            String inputPinyinCharsId, int top, List<InputWord> prevPhrase
    ) {
        return findTopBestPinyinWordsFromDB(getDB(),
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
//        List<InputWord> pinyinWords = prevPhrase != null ? new ArrayList<>(prevPhrase) : new ArrayList<>();
//        Collections.reverse(pinyinWords);
//
//        // 匹配短语中的常用字：倒序分析
//        List<String> inputPhraseWordCharsIdList = pinyinWords.stream()
//                                                             .map((word) -> ((PinyinInputWord) word).getCharsId())
//                                                             .collect(Collectors.toList());
//        inputPhraseWordCharsIdList.add(0, inputPinyinCharsId);
//
//        // 已确认的拼音字 id
//        List<String> confirmedPhraseWordIdList = pinyinWords.stream()
//                                                            .map(word -> word.isConfirmed() ? word.getUid() : null)
//                                                            .collect(Collectors.toList());
//        confirmedPhraseWordIdList.add(0, null);
//
//        Set<String> invalidPhraseIdSet = new HashSet<>();
//
//        Map<String, List<String[]>> bestPhraseMap = new LinkedHashMap<>();
//        doSQLiteQuery(db, phraseTable, new String[] {
//                              "source_id_", "target_id_", "target_spell_chars_id_", "target_index_"
//                      }, //
//                      "weight_ > 0 and target_spell_chars_id_ in (" //
//                      + inputPhraseWordCharsIdList.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
//                      + ")", //
//                      inputPhraseWordCharsIdList.toArray(new String[0]), //
//                      "weight_ desc, source_id_ asc" +
//                      // Note：只有一个字时，应该将其视为短语的开头（升序排序短语中的字），
//                      // 否则，视其为短语的结尾（降序排序短语中的字）
//                      (", target_index_ " + (inputPhraseWordCharsIdList.size() == 1 ? "asc" : "desc")), //
//                      (cursor) -> {
//                          String phraseId = cursor.getString(0);
//                          if (invalidPhraseIdSet.contains(phraseId)) {
//                              return null;
//                          }
//
//                          String phraseWordId = cursor.getString(1);
//                          String phraseWordCharsId = cursor.getString(2);
//                          String phraseWordIndex = cursor.getString(3);
//
//                          List<String[]> phraseWords = bestPhraseMap.computeIfAbsent(phraseId,
//                                                                                     (k) -> new ArrayList<>());
//
//                          int phraseWordSize = phraseWords.size();
//                          String[] prev = CollectionUtils.last(phraseWords);
//                          if ( // 去掉 搜索的字 在 短语 中 不相邻 的数据：对短语内的字顺序做了降序处理，故而，prev 的序号应该比当前字的序号更大
//                                  (prev != null //
//                                   && Integer.parseInt(prev[1]) - Integer.parseInt(phraseWordIndex) != 1)
//                                  // 去掉与 查询短语 在 相同位置 读音（或已确认的字）不匹配 的数据
//                                  || (phraseWordSize < inputPhraseWordCharsIdList.size() //
//                                      && (!inputPhraseWordCharsIdList.get(phraseWordSize).equals(phraseWordCharsId) //
//                                          || (confirmedPhraseWordIdList.get(phraseWordSize) != null //
//                                              && !confirmedPhraseWordIdList.get(phraseWordSize)
//                                                                           .equals(phraseWordId))))) {
//                              invalidPhraseIdSet.add(phraseId);
//
//                              // Note：及时删除有助于匹配某个短语内部的部分组合
//                              bestPhraseMap.remove(phraseId);
//
//                              return null;
//                          }
//
//                          phraseWords.add(new String[] { phraseWordId, phraseWordIndex, phraseWordCharsId });
//
//                          return null;
//                      });
//
//        List<String[]> bestPhraseWordIdsList = bestPhraseMap.values()
//                                                            .stream()
//                                                            .map(tupleList -> tupleList.stream()
//                                                                                       .map(tuple -> tuple[0])
//                                                                                       .toArray(String[]::new))
//                                                            // 根据匹配短语长度排序，长度越长，其匹配性最佳。
//                                                            // 长度相同的，再看其权重大小。
//                                                            // 不过，原结果已经是权重排序结果，故而，长度相同的保持位置不变即可
//                                                            .sorted((a1, a2) -> Integer.compare(a2.length, a1.length))
//                                                            .collect(Collectors.toList());
//        List<String[]> bestPhrases = bestPhraseWordIdsList.stream()
//                                                          .filter(phrase -> phrase.length > 1)
//                                                          .collect(Collectors.toList());
//
//        // Note：短语中的最佳候选字，仅针对短语长度大于 1 的情况，等于 1 的就应该使用高频的单字
//        Set<String> firstWordIdInBestPhrasesSet = bestPhrases.stream()
//                                                             .map(phrase -> phrase[0])
//                                                             .collect(Collectors.toCollection(LinkedHashSet::new));
//        List<String> bestWords = CollectionUtils.topPatch(new ArrayList<>(firstWordIdInBestPhrasesSet), top,
//                                                          // 匹配高频字
//                                                          () -> doSQLiteQuery(db, wordTable,
//                                                                              //
//                                                                              new String[] { "id_" },
//                                                                              //
//                                                                              "weight_ > 0 and target_chars_id_ = ?",
//                                                                              //
//                                                                              new String[] { inputPinyinCharsId },
//                                                                              //
//                                                                              "weight_ desc", String.valueOf(top),
//                                                                              //
//                                                                              (cursor) -> cursor.getString(0)));

//        return new BestCandidateWords(bestWords, bestPhrases);
        return new BestCandidateWords(new ArrayList<>(), new ArrayList<>());
    }

    private List<List<String>> findTopBestMatchedPinyinPhraseWord(List<InputWord> prevPhrase, int top) {
        SQLiteDatabase db = getDB();

        String prevPhraseWordIds = prevPhrase.stream().map(InputWord::getUid).collect(Collectors.joining(",")) + ",";

        Map<String, String> bestPhraseMap = new LinkedHashMap<>();
//        doSQLiteQuery(db, "used_pinyin_phrase", new String[] {
//                              "source_id_", "group_concat(target_id_) as target_ids_",
//                              }, //
//                      "weight_ > 0 and (target_ids_ like ? or target_ids_ like ?)", //
//                      new String[] { prevPhraseWordIds + "%", "%," + prevPhraseWordIds + "%" }, //
//                      "source_id_", //
//                      // Note：视图 used_pinyin_phrase 已默认按 target_id_ 升序排序
//                      "weight_ desc", //
//                      String.valueOf(top), //
//                      (cursor) -> {
//                          String phraseId = cursor.getString(0);
//                          String phraseWordIds = cursor.getString(1);
//
//                          bestPhraseMap.put(phraseId, phraseWordIds);
//                          return null;
//                      });

        Set<List<String>> matchedPhraseWordsSet = new LinkedHashSet<>();
        bestPhraseMap.forEach((k, phraseWordIds) -> {
            // Note：匹配位置不是在开头就是在中间
            int index = phraseWordIds.indexOf("," + prevPhraseWordIds);
            String matchedPhraseWordIds = phraseWordIds.substring(index + 1);

            matchedPhraseWordsSet.add(Arrays.asList(matchedPhraseWordIds.split(",")));
        });

        return new ArrayList<>(matchedPhraseWordsSet);
    }

    public Map<String, InputWord> getPinyinWords(Collection<String> wordIds) {
        SQLiteDatabase db = getDB();

        List<PinyinInputWord> wordList = queryPinyinInputWords(db,
                                                               "py_.id_ in ("
                                                               + wordIds.stream()
                                                                        .map((id) -> "?")
                                                                        .collect(Collectors.joining(", "))
                                                               + ")",
                                                               wordIds.toArray(new String[0]),
                                                               false);

        return wordList.stream().collect(Collectors.toMap(InputWord::getUid, Function.identity()));
    }

    /** 查找拼音候选字的变体 id */
    private Map<String, List<String>> findPinyinWordVariantIds(
            SQLiteDatabase db, String variantTable, Collection<String> sourceIds
    ) {
        if (sourceIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<String>> map = new HashMap<>(sourceIds.size());
        querySQLite(db, new SQLiteQueryParams<Void>() {{
            this.table = variantTable;
            this.columns = new String[] { "source_id_", "target_id_" };
            this.where = "source_id_ in (" //
                         + sourceIds.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
                         + ")";

            this.params = sourceIds.toArray(new String[0]);
            this.reader = (row) -> {
                String sourceId = row.getString("source_id_");
                String targetId = row.getString("target_id_");

                map.computeIfAbsent(sourceId, (k) -> new ArrayList<>()).add(targetId);
                return null;
            };
        }});

        return map;
    }

    /** 查找拼音字的变体 */
    private Map<String, List<String>> findPinyinWordVariants(
            SQLiteDatabase db, String table, Collection<String> sourceIds
    ) {
        if (sourceIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, List<String>> map = new HashMap<>(sourceIds.size());
//        doSQLiteQuery(db, table, new String[] { "id_", "target_value_" },
//                      //
//                      "id_ in (" //
//                      + sourceIds.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
//                      + ")", //
//                      sourceIds.toArray(new String[0]), //
//                      (cursor) -> {
//                          String sourceId = cursor.getString(0);
//                          String targetValue = cursor.getString(1);
//                          map.computeIfAbsent(sourceId, (k) -> new ArrayList<>()).add(targetValue);
//
//                          return null;
//                      });
        return map;
    }

    private List<InputWord> findEmojisMatchedPhraseFromAppDB(CharInput input, int top, List<InputWord> prevPhrase) {
//        SQLiteDatabase db = getAppDB();
//
//        List<String> phraseWordUidList = prevPhrase.stream().map(InputWord::getUid).collect(Collectors.toList());
//        Collections.reverse(phraseWordUidList);
//
//        phraseWordUidList.add(0, input.getWord().getUid());
//
//        Set<String> invalidEmojiKeywordIdSet = new HashSet<>();
//        Map<String, List<String[]>> keywordAndWordIndexesMap = new HashMap<>();
//
//        Map<String, InputWord> emojiMap = new HashMap<>();
//        doSQLiteQuery(db, "emoji", new String[] {
//                              "id_", "value_", "keyword_index_", "keyword_word_spell_link_id_", "keyword_word_index_"
//                      }, //
//                      "keyword_word_spell_link_id_ in (" //
//                      + phraseWordUidList.stream().map((id) -> "?").collect(Collectors.joining(", ")) //
//                      + ")", //
//                      phraseWordUidList.toArray(new String[0]), //
//                      "id_ asc, keyword_index_ asc, keyword_word_index_ desc", //
//                      (cursor) -> {
//                          String emojiId = cursor.getString(0);
//                          String emojiValue = cursor.getString(1);
//                          // 表情的关键字唯一标识由 表情 id 和 关键字的序号 组合而成
//                          String emojiKeywordId = emojiId + ":" + cursor.getString(2);
//                          String emojiKeywordWordUid = cursor.getString(3);
//                          String emojiKeywordWordIndex = cursor.getString(4);
//
//                          if (!CharUtils.isPrintable(emojiValue) //
//                              || invalidEmojiKeywordIdSet.contains(emojiKeywordId)) {
//                              return null;
//                          }
//
//                          List<String[]> keywordAndWordIndexes
//                                  = keywordAndWordIndexesMap.computeIfAbsent(emojiKeywordId, (k) -> new ArrayList<>());
//
//                          int keywordWordSize = keywordAndWordIndexes.size();
//                          String[] prev = CollectionUtils.last(keywordAndWordIndexes);
//                          if ( // 去掉 关键字 在 短语 中 不相邻 的数据
//                                  (prev != null //
//                                   && Integer.parseInt(prev[1]) - Integer.parseInt(emojiKeywordWordIndex) != 1)
//                                  // 去掉与 短语 在 相同位置的字 不匹配 的数据
//                                  || (keywordWordSize < phraseWordUidList.size() //
//                                      && (!phraseWordUidList.get(keywordWordSize).equals(emojiKeywordWordUid)))) {
//                              invalidEmojiKeywordIdSet.add(emojiKeywordId);
//
//                              keywordAndWordIndexesMap.remove(emojiKeywordId);
//
//                              return null;
//                          }
//
//                          keywordAndWordIndexesMap.computeIfAbsent(emojiKeywordId, (k) -> new ArrayList<>())
//                                                  .add(new String[] {
//                                                          emojiKeywordWordUid, emojiKeywordWordIndex, emojiId
//                                                  });
//
//                          if (!emojiMap.containsKey(emojiId)) {
//                              InputWord emoji = new EmojiInputWord(emojiId, emojiValue);
//                              emojiMap.put(emojiId, emoji);
//                          }
//
//                          return null;
//                      });
//
//        // 按关键字匹配到的输入长度排序，匹配越长的越靠前
//        Map<String, Integer> emojiAndWeightMap = new HashMap<>();
//        keywordAndWordIndexesMap.forEach((keywordId, tupleList) -> {
//            tupleList.forEach((tuple) -> {
//                String emojiId = tuple[2];
//                int weight = emojiAndWeightMap.getOrDefault(emojiId, 0);
//
//                emojiAndWeightMap.put(emojiId, tupleList.size() > weight ? tupleList.size() : weight);
//            });
//        });
//
//        List<InputWord> emojiList = emojiAndWeightMap.keySet().stream().map(emojiMap::get).sorted((a1, a2) -> {
//            int a1Weight = emojiAndWeightMap.get(a1.getUid());
//            int a2Weight = emojiAndWeightMap.get(a2.getUid());
//            int order = Integer.compare(a2Weight, a1Weight);
//            // Note：相邻 id 的表情更有可能在同一个分组内，
//            // 故而，对匹配度相同的表情采用 id 排序
//            return order != 0 //
//                   ? order //
//                   : Integer.compare(Integer.parseInt(a1.getUid()), Integer.parseInt(a2.getUid()));
//        }).collect(Collectors.toList());
//
//        return CollectionUtils.subList(emojiList, 0, top);
        return List.of();
    }

    /** 从拼音的候选字列表中查找各个字的繁/简形式 */
    private void patchPinyinWordVariantInCandidates(SQLiteDatabase db, Collection<InputWord> candidates) {
        Map<String, InputWord> tradWordMap = new HashMap<>();
        Map<String, InputWord> simpleWordMap = new HashMap<>();

        candidates.forEach((word) -> {
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
                                                                          : Async.executor.submit(() -> findPinyinWordVariantIds(
                                                                                  db,
                                                                                  "link_word_with_simple_word",
                                                                                  tradWordMap.keySet()));
        Future<Map<String, List<String>>> simpleWithTradWordIdMapFuture = simpleWordMap.isEmpty()
                                                                          ? null
                                                                          : Async.executor.submit(() -> findPinyinWordVariantIds(
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

    /** 查找各个字的繁/简形式 */
    private void patchPinyinWordVariant(Collection<InputWord> candidates) {
        SQLiteDatabase db = getDB();

        Map<String, InputWord> tradWordMap = new HashMap<>();
        Map<String, InputWord> simpleWordMap = new HashMap<>();

        candidates.forEach((word) -> {
            String wordId = ((PinyinInputWord) word).getWordId();
            if (((PinyinInputWord) word).isTraditional()) {
                tradWordMap.put(wordId, word);
            } else {
                simpleWordMap.put(wordId, word);
            }
        });

        Future<Map<String, List<String>>> tradWithSimpleWordMapFuture = tradWordMap.isEmpty()
                                                                        ? null
                                                                        : Async.executor.submit(() -> findPinyinWordVariants(
                                                                                db,
                                                                                "simple_word",
                                                                                tradWordMap.keySet()));
        Future<Map<String, List<String>>> simpleWithTradWordMapFuture = simpleWordMap.isEmpty()
                                                                        ? null
                                                                        : Async.executor.submit(() -> findPinyinWordVariants(
                                                                                db,
                                                                                "traditional_word",
                                                                                simpleWordMap.keySet()));

        value(tradWithSimpleWordMapFuture, new HashMap<>()).forEach((sourceId, variants) -> {
            InputWord sourceWord = tradWordMap.get(sourceId);
            if (sourceWord == null || sourceWord.getVariant() != null) {
                return;
            }

            String variant = variants.get(0);
            sourceWord.setVariant(variant);
        });

        value(simpleWithTradWordMapFuture, new HashMap<>()).forEach((sourceId, variants) -> {
            InputWord sourceWord = simpleWordMap.get(sourceId);
            if (sourceWord == null || sourceWord.getVariant() != null) {
                return;
            }

            String variant = variants.get(0);
            sourceWord.setVariant(variant);
        });
    }

    private void doSaveUsedPinyinPhrase(List<InputWord> phrase) {
        if (phrase.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
        doSaveUsedWordInPinyinPhrase(db, phrase, true);

        if (phrase.size() < 2) {
            return;
        }

        String phraseCode = calculatePinyinPhraseCode(phrase);
        boolean isNew = doSaveUsedPinyinPhrase(db, phraseCode, true);
        if (!isNew) {
            return;
        }

//        // 保存短语中的字
//        doSQLiteSave(db,
//                     "insert into"
//                     + " used_phrase_pinyin_word"
//                     + " (source_id_, target_id_, target_spell_chars_id_, target_index_)"
//                     + " values ((select id_ from used_phrase where value_ = ?), ?, ?, ?)",
//                     null,
//                     (insert, _ignore) -> {
//                         for (int i = 0; i < phrase.size(); i++) {
//                             InputWord word = phrase.get(i);
//
//                             insert.bindAllArgsAsStrings(new String[] {
//                                     phraseCode, word.getUid(), ((PinyinInputWord) word).getCharsId(), String.valueOf(i)
//                             });
//                             insert.execute();
//                         }
//                     });
    }

    /** 保存表情的使用频率等信息 */
    private void doSaveUsedEmojis(List<InputWord> emojis) {
        if (emojis.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
        doSaveUsedEmojis(db, emojis, true);
    }

    /** 保存拉丁文的使用频率等信息 */
    private void doSaveUsedLatins(Set<String> latins) {
        // 仅针对长单词
        Set<String> validLatins = latins.stream().filter(this::isValidUsedLatin).collect(Collectors.toSet());
        if (validLatins.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
        doSaveUsedLatins(db, validLatins, true);
    }

    /** 撤销 {@link #doSaveUsedPinyinPhrase} */
    private void undoSaveUsedPinyinPhrase(List<InputWord> phrase) {
        if (phrase.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
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

        SQLiteDatabase db = getDB();
        doSaveUsedEmojis(db, emojis, false);
    }

    /** 撤销 {@link #doSaveUsedLatins} */
    private void undoSaveUsedLatins(Set<String> latins) {
        if (latins.isEmpty()) {
            return;
        }

        SQLiteDatabase db = getDB();
        doSaveUsedLatins(db, latins, false);
    }

    /** @return 若为新增，则返回 <code>true</code>，否则，返回 <code>false</code> */
    private boolean doSaveUsedPinyinPhrase(SQLiteDatabase db, String phraseCode, boolean shouldIncreaseWeight) {
//        Map<String, Integer> oldDataMap = doUpdateDataWeight(db,
//                                                             "used_phrase",
//                                                             "value_",
//                                                             Collections.singleton(phraseCode),
//                                                             null,
//                                                             "insert into used_phrase (weight_, value_) values (?, ?)",
//                                                             "update used_phrase set weight_ = ? where value_ = ?",
//                                                             new String[] {
//                                                                     "delete from used_phrase_pinyin_word"
//                                                                     + " where source_id_ in (select id_ from used_phrase where weight_ = 0)",
//                                                                     "delete from used_phrase where weight_ = 0"
//                                                             },
//                                                             shouldIncreaseWeight);
//        return !oldDataMap.containsKey(phraseCode);
        return false;
    }

    /** 保存拼音短语中字的使用频率等信息 */
    private void doSaveUsedWordInPinyinPhrase(SQLiteDatabase db, List<InputWord> phrase, boolean shouldIncreaseWeight) {
        Map<String, String> idAndTargetCharsIdMap = phrase.stream()
                                                          .collect(Collectors.toMap(InputWord::getUid,
                                                                                    (word) -> ((PinyinInputWord) word).getCharsId(),
                                                                                    (v1, v2) -> v1));

//        doUpdateDataWeight(db,
//                           "used_pinyin_word",
//                           "id_",
//                           idAndTargetCharsIdMap.keySet(),
//                           (id) -> new String[] { idAndTargetCharsIdMap.get(id) },
//                           "insert into used_pinyin_word (weight_, id_, target_chars_id_) values (?, ?, ?)",
//                           "update used_pinyin_word set weight_ = ? where id_ = ?",
//                           new String[] { "delete from used_pinyin_word where weight_ = 0" },
//                           shouldIncreaseWeight);
    }

    private void doSaveUsedEmojis(SQLiteDatabase db, List<InputWord> emojis, boolean shouldIncreaseWeight) {
        Set<String> dataIdSet = emojis.stream().map(InputWord::getUid).collect(Collectors.toSet());

//        doUpdateDataWeight(db,
//                           "used_emoji",
//                           "id_",
//                           dataIdSet,
//                           null,
//                           "insert into used_emoji (weight_, id_) values (?, ?)",
//                           "update used_emoji set weight_ = ? where id_ = ?",
//                           new String[] { "delete from used_emoji where weight_ = 0" },
//                           shouldIncreaseWeight);
    }

    private void doSaveUsedLatins(SQLiteDatabase db, Set<String> latins, boolean shouldIncreaseWeight) {
//        doUpdateDataWeight(db,
//                           "used_latin",
//                           "value_",
//                           latins,
//                           null,
//                           "insert into used_latin (weight_, value_) values (?, ?)",
//                           "update used_latin set weight_ = ? where value_ = ?",
//                           new String[] { "delete from used_latin where weight_ = 0" },
//                           shouldIncreaseWeight);
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

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    private void doInit(Context context) {
        // <<<<<<<<<< 版本升级
        String version = getVersion(context);

        if (FIRST_INSTALL_VERSION.equals(version)) {
            From_v0.upgrade(context, this);
        } else if (VERSION_V2.equals(version) && LATEST_VERSION.equals(VERSION_V3)) {
            From_v2_to_v3.upgrade(context, this);
        }

        updateToLatestVersion(context);
        // >>>>>>>>>>>>>>
    }

    private void doOpen(Context context) {
        File userDBFile = getUserDBFile(context);

        this.db = openSQLite(userDBFile, false);
        configSQLite(this.db);

        Map<String, String> pinyinCharsAndIdMap = new HashMap<>(600);
        querySQLite(this.db, new DBUtils.SQLiteQueryParams<Void>() {{
            this.table = "meta_pinyin_chars";
            this.columns = new String[] { "id_", "value_" };
            this.reader = (row) -> {
                pinyinCharsAndIdMap.put(row.getString("value_"), row.getString("id_"));
                return null;
            };
        }});

        this.pinyinTree = PinyinTree.create(pinyinCharsAndIdMap);
    }

    private void doClose() {
        closeSQLite(this.db);

        this.db = null;
        this.pinyinTree = null;
    }

    private File getUserDBFile(Context context) {
        return getDBFile(context, PinyinDictDBType.user);
    }

    public SQLiteDatabase getDB() {
        return isOpened() ? this.db : null;
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    /** 获取应用本地的用户数据库的版本 */
    public String getVersion(Context context) {
        if (this.version == null) {
            File userDBFile = getDBFile(context, PinyinDictDBType.user);
            File versionFile = getVersionFile(context);

            if (!versionFile.exists()) {
                if (userDBFile.exists()) { // 应用 HMM 算法之前的版本
                    this.version = VERSION_V2;
                } else { // 首次安装
                    this.version = FIRST_INSTALL_VERSION;
                }
            } else { // 实际记录的版本号
                this.version = FileUtils.read(versionFile, true);
            }
        }

        return this.version;
    }

    private void updateToLatestVersion(Context context) {
        if (LATEST_VERSION.equals(this.version)) {
            return;
        }

        File file = getVersionFile(context);
        try {
            FileUtils.write(file, LATEST_VERSION);
            this.version = LATEST_VERSION;
        } catch (IOException ignore) {
        }
    }

    private File getVersionFile(Context context) {
        return new File(context.getFilesDir(), db_version_file);
    }
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

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
}
