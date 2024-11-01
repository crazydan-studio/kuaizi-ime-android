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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.InputWord;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v0;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v2_to_v3;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.Async;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWords;
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
        List<PinyinInputWord> wordList = getAllPinyinInputWords(db, pinyinCharsId, this.userPhraseBaseWeight);

        patchPinyinWordVariantInCandidates(db, wordList);

        return wordList.stream().map(w -> (InputWord) w).collect(Collectors.toList());
    }

    /**
     * 获取各分组中的所有表情
     *
     * @param groupGeneralCount
     *         {@link Emojis#GROUP_GENERAL} 分组中的表情数量
     */
    public Emojis getAllEmojis(int groupGeneralCount) {
        SQLiteDatabase db = getDB();

        return getAllGroupedEmojis(db, groupGeneralCount);
    }

    /** 根据拼音输入分析得出最靠前的 <code>top</code> 个匹配的表情 */
    public List<InputWord> findTopBestEmojisMatchedPhrase(CharInput input, int top, List<InputWord> prevPhrase) {
        return findEmojisMatchedPhraseFromAppDB(input, top, prevPhrase);
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

        SQLiteDatabase db = getDB();

        List<String> pinyinCharsIdList = prevPhrase.stream()
                                                   .map((word) -> ((PinyinInputWord) word).getCharsId())
                                                   .collect(Collectors.toList());
        List<String[]> phraseWordsList = predictPinyinPhrase(db, pinyinCharsIdList, this.userPhraseBaseWeight, top);
        if (phraseWordsList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> wordIdSet = new HashSet<>();
        phraseWordsList.forEach((wordIds) -> wordIdSet.addAll(Set.of(wordIds)));

        Map<String, PinyinInputWord> wordMap = getPinyinInputWords(db, wordIdSet);
        if (variantFirst) {
            patchPinyinWordVariant(wordMap.values());
        }

        return phraseWordsList.stream()
                              .map((wordIds) -> Arrays.stream(wordIds)
                                                      .map(wordMap::get)
                                                      .map((word) -> (InputWord) word)
                                                      .collect(Collectors.toList()))
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
    private void patchPinyinWordVariantInCandidates(SQLiteDatabase db, Collection<PinyinInputWord> candidates) {
        Map<String, InputWord> tradWordMap = new HashMap<>();
        Map<String, InputWord> simpleWordMap = new HashMap<>();

        candidates.forEach((word) -> {
            String wordId = word.getWordId();
            if (word.isTraditional()) {
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
    private void patchPinyinWordVariant(Collection<PinyinInputWord> candidates) {
        SQLiteDatabase db = getDB();

        Map<String, InputWord> tradWordMap = new HashMap<>();
        Map<String, InputWord> simpleWordMap = new HashMap<>();

        candidates.forEach((word) -> {
            String wordId = word.getWordId();
            if (word.isTraditional()) {
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
}
