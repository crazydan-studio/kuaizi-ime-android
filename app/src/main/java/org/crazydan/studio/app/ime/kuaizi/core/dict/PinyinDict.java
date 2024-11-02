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
import org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v0;
import org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v2_to_v3;
import org.crazydan.studio.app.ime.kuaizi.core.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.core.input.PinyinInputWord;
import org.crazydan.studio.app.ime.kuaizi.utils.Async;
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.ResourceUtils;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.attachVariantToPinyinInputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWords;
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

        // 附加拼音字的繁/简字
        attachVariantToPinyinInputWord(db, wordList);

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
        if (!(input.getWord() instanceof PinyinInputWord)) {
            return List.of();
        }

        List<String> phraseWordIdList = prevPhrase.stream()
                                                  .map((word) -> ((PinyinInputWord) word).getWordId())
                                                  .collect(Collectors.toList());
        phraseWordIdList.add(((PinyinInputWord) input.getWord()).getWordId());

        int tries = 4;
        int total = phraseWordIdList.size();
        List<String[]> keywordIdsList = new ArrayList<>(tries);
        for (int i = total - 1; i >= 0 && i >= total - tries; i--) {
            String[] keywordIds = phraseWordIdList.subList(i, total).toArray(new String[0]);
            keywordIdsList.add(keywordIds);
        }

        SQLiteDatabase db = getDB();

        return getEmojisByKeyword(db, keywordIdsList, top).stream()
                                                          .map((word) -> (InputWord) word)
                                                          .collect(Collectors.toList());
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
            attachVariantToPinyinInputWord(db, wordMap.values());
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
            saveUsedEmojis(data.emojis, false);
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
            saveUsedEmojis(data.emojis, true);
            undoSaveUsedLatins(data.latins);
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
    private void saveUsedEmojis(List<InputWord> emojis, boolean reverse) {
        SQLiteDatabase db = getDB();

        PinyinDictDBHelper.saveUsedEmojis(db,
                                          emojis.stream().map(InputWord::getUid).collect(Collectors.toList()),
                                          reverse);
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
