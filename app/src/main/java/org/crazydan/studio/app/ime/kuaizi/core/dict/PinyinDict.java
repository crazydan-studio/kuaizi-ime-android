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
import java.util.function.BiFunction;
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
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.attachVariantToPinyinInputWord;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getAllPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getLatinsByStarts;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.getTopBestPinyinInputWords;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.PinyinDictDBHelper.saveUsedLatins;
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

    /** 获取指定拼音的候选字列表：已按拼音声调等排序 */
    public List<InputWord> getPinyinCandidateWords(CharInput input) {
        return queryPinyinCandidateWords(input, (db, pinyinCharsId) -> {
            List<PinyinInputWord> wordList = getAllPinyinInputWords(db, pinyinCharsId);

            // 附加拼音字的繁/简字
            attachVariantToPinyinInputWord(db, wordList);

            return wordList;
        });
    }

    /** 获取指定拼音的前 <code>top</code> 个高权重的候选字 */
    public List<InputWord> getTopBestPinyinCandidateWords(CharInput input, int top) {
        return queryPinyinCandidateWords(input, (db, pinyinCharsId) -> { //
            return getTopBestPinyinInputWords(db, pinyinCharsId, this.userPhraseBaseWeight, top);
        });
    }

    private List<InputWord> queryPinyinCandidateWords(
            CharInput input, BiFunction<SQLiteDatabase, String, List<PinyinInputWord>> consumer
    ) {
        String pinyinCharsId = this.pinyinTree.getPinyinCharsId(input);
        if (pinyinCharsId == null) {
            return List.of();
        }

        SQLiteDatabase db = getDB();
        List<PinyinInputWord> wordList = consumer.apply(db, pinyinCharsId);

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

    /** 根据拼音输入短语的后 4 个字作为关键字查询得到最靠前的 <code>top</code> 个表情 */
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
        if (text == null || text.length() < 2) {
            return List.of();
        }

        SQLiteDatabase db = getDB();

        return getLatinsByStarts(db, text, top);
    }

    /** 根据前序输入的字词，查找最靠前的 <code>top</code> 个拼音短语 */
    public List<List<InputWord>> findTopBestMatchedPhrase(
            List<InputWord> prevPhrase, int top, boolean variantFirst
    ) {
        if (prevPhrase == null || prevPhrase.size() < 2) {
            return List.of();
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
        phraseWordsList.forEach((wordIds) -> wordIdSet.addAll(List.of(wordIds)));

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
        doSaveUserInputData(data, false);
    }

    /** 对 {@link #saveUserInputData} 的撤销处理（异步） */
    public void revokeSavedUserInputData(UserInputData data) {
        doSaveUserInputData(data, true);
    }

    /** 保存使用数据信息，含短语、单字、表情符号等：异步处理 */
    private void doSaveUserInputData(UserInputData data, boolean reverse) {
        if (data.isEmpty()) {
            return;
        }

        Async.executor.execute(() -> {
            data.phrases.forEach((phrase) -> doSaveUsedPhrase(phrase, reverse));
            doSaveUsedEmojis(data.emojis, reverse);
            doSaveUsedLatins(data.latins, reverse);
        });
    }

    private void doSaveUsedPhrase(List<InputWord> phrase, boolean reverse) {
        SQLiteDatabase db = getDB();

        saveUsedPinyinPhrase(db,
                             phrase.stream().map((word) -> (PinyinInputWord) word).collect(Collectors.toList()),
                             reverse);
    }

    /** 保存表情的使用频率等信息 */
    private void doSaveUsedEmojis(List<InputWord> emojis, boolean reverse) {
        SQLiteDatabase db = getDB();

        saveUsedEmojis(db, emojis.stream().map(InputWord::getUid).collect(Collectors.toList()), reverse);
    }

    /** 保存拉丁文的使用频率等信息 */
    private void doSaveUsedLatins(List<String> latins, boolean reverse) {
        SQLiteDatabase db = getDB();

        // 仅针对长单词
        saveUsedLatins(db, latins.stream().filter((latin) -> latin.length() > 3).collect(Collectors.toList()), reverse);
    }

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
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
    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

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
