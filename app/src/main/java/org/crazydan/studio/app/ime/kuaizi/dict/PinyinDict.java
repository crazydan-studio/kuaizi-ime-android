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

package org.crazydan.studio.app.ime.kuaizi.dict;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.common.utils.Async;
import org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.common.utils.ResourceUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v0;
import org.crazydan.studio.app.ime.kuaizi.dict.upgrade.From_v2_to_v3;
import org.crazydan.studio.app.ime.kuaizi.pane.InputWord;
import org.crazydan.studio.app.ime.kuaizi.pane.input.CharInput;
import org.crazydan.studio.app.ime.kuaizi.pane.input.PinyinWord;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.closeSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.predictPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.HmmDBHelper.saveUsedPinyinPhrase;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.enableAllPrintableEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getAllGroupedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getAllPinyinWordsByCharsId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getEmojisByKeyword;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getFirstBestPinyinWord;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getLatinsByStarts;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getPinyinWordsByWordId;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.getTopBestPinyinWordIds;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.saveUsedEmojis;
import static org.crazydan.studio.app.ime.kuaizi.dict.db.PinyinDictDBHelper.saveUsedLatins;

/**
 * 拼音字典（数据库版）
 * <p/>
 * 应用内置的拼音字典数据库的表结构和数据生成见
 * <a href="https://github.com/crazydan-studio/kuaizi-ime/blob/master/tools/pinyin-dict/src/generate/sqlite/ime/index.mjs">kuaizi-ime/tools/pinyin-dict</a>
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

    /** 字典 {@link #open} 的引用计数 */
    private int openedRefs;
    private boolean opened;
    /** 异步线程池 */
    private ThreadPoolExecutor executor;

    private String version;
    private SQLiteDatabase db;

    // <<<<<<<<<<<<< 缓存常量数据
    private PinyinCharsTree pinyinCharsTree;
    // >>>>>>>>>>>>>

    PinyinDict() {
    }

    public static PinyinDict instance() {
        return instance;
    }

    public boolean isOpened() {
        return this.opened;
    }

    public PinyinCharsTree getPinyinCharsTree() {
        return this.pinyinCharsTree;
    }

    // =================== Start: 生命周期 ==================

    /**
     * 在使用前开启字典：由开启方负责 {@link #close 关闭}
     *
     * @param listener
     *         仅用于监听实际的开启过程，若字典已开启，则不会调用该监听
     */
    public synchronized void open(Context context, Listener listener) {
        this.openedRefs += 1;
        if (isOpened()) {
            return;
        }

        listener.beforeOpen(this);

        doUpgrade(context);
        doOpen(context);

        listener.afterOpen(this);

        this.opened = true;
    }

    /** 在资源回收前关闭字典：由 {@link #open 开启} 方负责关闭 */
    public synchronized void close() {
        this.openedRefs -= 1;
        if (this.openedRefs > 0) {
            return;
        }

        if (isOpened()) {
            doClose();
        }
        this.opened = false;
    }

    // =================== End: 生命周期 ==================

    // =================== Start: 数据查询 ==================

    /** 通过字及其读音获取 {@link InputWord} 对象 */
    public InputWord getPinyinWord(String word, String pinyin) {
        SQLiteDatabase db = getDB();

        return PinyinDictDBHelper.getPinyinWord(db, word, pinyin);
    }

    /** 获取指定拼音的候选拼音字列表：已按权重等排序 */
    public Map<Integer, InputWord> getCandidatePinyinWords(CharInput input) {
        Integer pinyinCharsId = getPinyinCharsTree().getCharsId(input);

        SQLiteDatabase db = getDB();

        return getAllPinyinWordsByCharsId(db, pinyinCharsId).stream()
                                                            .collect(Collectors.toMap(InputWord::getId,
                                                                                      Function.identity(),
                                                                                      (a, b) -> a,
                                                                                      // 保持候选字的顺序不变
                                                                                      LinkedHashMap::new));
    }

    /**
     * 获取指定拼音的第一个最佳候选字
     * <p/>
     * 优先选择使用权重最高的，否则，选择候选字列表中的第一个
     */
    public InputWord getFirstBestCandidatePinyinWord(Integer pinyinCharsId) {
        SQLiteDatabase db = getDB();

        return getFirstBestPinyinWord(db, pinyinCharsId, this.userPhraseBaseWeight);
    }

    /** 获取指定拼音的前 <code>top</code> 个高权重的候选拼音字 id */
    public List<Integer> getTopBestCandidatePinyinWordIds(CharInput input, int top) {
        Integer pinyinCharsId = getPinyinCharsTree().getCharsId(input);

        SQLiteDatabase db = getDB();

        return getTopBestPinyinWordIds(db, pinyinCharsId, this.userPhraseBaseWeight, top);
    }

    /**
     * 根据输入的拼音，查找最靠前的 <code>top</code> 个拼音短语
     */
    public List<List<InputWord>> findTopBestMatchedPhrase(List<CharInput> inputs, int top) {
        if (inputs.size() < 2) {
            return List.of();
        }

        Map<Integer, Integer> pinyinCharsPlaceholderMap = new HashMap<>(inputs.size());

        List<Integer> pinyinCharsIdList = new ArrayList<>(inputs.size());
        Map<Integer, Integer> confirmedPhraseWords = new HashMap<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            CharInput input = inputs.get(i);
            // Note: 英文字符也可能组成有效拼音，故而，需仅针对拼音键盘的输入
            if (!input.isPinyin()) {
                continue;
            }

            String chars = input.getJoinedChars();
            Integer charsId = getPinyinCharsTree().getCharsId(chars);
            if (charsId == null) {
                continue;
            }

            int charsIndex = pinyinCharsIdList.size();
            if (input.isWordConfirmed()) {
                confirmedPhraseWords.put(charsIndex, input.getWord().getId());
            }
            pinyinCharsPlaceholderMap.put(i, charsIndex);

            pinyinCharsIdList.add(charsId);
        }

        SQLiteDatabase db = getDB();
        List<Integer[]> phraseWordsList = predictPinyinPhrase(db,
                                                              pinyinCharsIdList,
                                                              confirmedPhraseWords,
                                                              this.userPhraseBaseWeight,
                                                              top);
        if (phraseWordsList.isEmpty()) {
            return List.of();
        }

        Set<Integer> pinyinWordIds = new HashSet<>();
        phraseWordsList.forEach(wordIds -> pinyinWordIds.addAll(List.of(wordIds)));

        Map<Integer, PinyinWord> pinyinWordMap = getPinyinWordsByWordId(db, pinyinWordIds);

        BiFunction<Integer[], Integer, InputWord> getWord = (wordIds, inputIndex) -> {
            Integer pinyinCharsIndex = pinyinCharsPlaceholderMap.get(inputIndex);
            if (pinyinCharsIndex == null) {
                return null;
            }

            Integer wordId = wordIds[pinyinCharsIndex];
            return pinyinWordMap.get(wordId);
        };

        return phraseWordsList.stream().map((wordIds) -> {
            List<InputWord> list = new ArrayList<>(inputs.size());

            // 按拼音所在的位置填充拼音字
            for (int i = 0; i < inputs.size(); i++) {
                InputWord word = getWord.apply(wordIds, i);
                list.add(word);
            }
            return list;
        }).collect(Collectors.toList());
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
    public List<InputWord> findTopBestEmojisMatchedPhrase(List<PinyinWord> phraseWords, int top) {
        if (phraseWords.isEmpty()) {
            return List.of();
        }

        List<Integer> wordGlyphIdList = phraseWords.stream().map(PinyinWord::getGlyphId).collect(Collectors.toList());

        int tries = 4;
        int total = wordGlyphIdList.size();
        List<Integer[]> keywordIdsList = new ArrayList<>(tries);
        for (int i = total - 1; i >= 0 && i >= total - tries; i--) {
            Integer[] keywordIds = wordGlyphIdList.subList(i, total).toArray(new Integer[0]);
            keywordIdsList.add(keywordIds);
        }

        SQLiteDatabase db = getDB();

        return getEmojisByKeyword(db, keywordIdsList, top).stream()
                                                          .map((word) -> (InputWord) word)
                                                          .collect(Collectors.toList());
    }

    /** 查找以指定参数开头的最靠前的 <code>top</code> 个拉丁文 */
    public List<String> findTopBestMatchedLatins(String text, int top) {
        if (text == null || text.length() < 2) {
            return List.of();
        }

        SQLiteDatabase db = getDB();

        return getLatinsByStarts(db, text, top);
    }

    // =================== End: 数据查询 ==================

    // =================== Start: 保存用户输入数据 ==================

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

        this.executor.execute(() -> {
            data.phrases.forEach((phrase) -> doSaveUsedPhrase(phrase, reverse));

            doSaveUsedEmojis(data.emojis, reverse);
            doSaveUsedLatins(data.latins, reverse);
        });
    }

    private void doSaveUsedPhrase(List<PinyinWord> phrase, boolean reverse) {
        SQLiteDatabase db = getDB();

        saveUsedPinyinPhrase(db, phrase, reverse);
    }

    /** 保存表情的使用频率等信息 */
    private void doSaveUsedEmojis(List<InputWord> emojis, boolean reverse) {
        SQLiteDatabase db = getDB();

        saveUsedEmojis(db, emojis.stream().map(InputWord::getId).collect(Collectors.toList()), reverse);
    }

    /** 保存拉丁文的使用频率等信息 */
    private void doSaveUsedLatins(List<String> latins, boolean reverse) {
        SQLiteDatabase db = getDB();

        // 仅针对长单词
        saveUsedLatins(db, latins.stream().filter((latin) -> latin.length() > 3).collect(Collectors.toList()), reverse);
    }

    // =================== End: 保存用户输入数据 ==================

    // =================== Start: 数据库管理 ==================

    public SQLiteDatabase getDB() {
        return isOpened() ? this.db : null;
    }

    private void doOpen(Context context) {
        File userDBFile = getUserDBFile(context);

        this.db = openSQLite(userDBFile, false);
        execSQLite(this.db, /*"pragma cache_size = 200;",*/ "pragma temp_store = memory;");

        // 启用系统支持的可显示的表情
        enableAllPrintableEmojis(this.db);

        if (this.pinyinCharsTree == null) {
            Map<String, Integer> pinyinCharsAndIdMap = new HashMap<>(600);
            querySQLite(this.db, new DBUtils.SQLiteQueryParams<Void>() {{
                this.table = "meta_pinyin_chars";
                this.columns = new String[] { "id_", "value_" };

                this.voidReader = (row) -> {
                    pinyinCharsAndIdMap.put(row.getString("value_"), row.getInt("id_"));
                };
            }});

            this.pinyinCharsTree = PinyinCharsTree.create(pinyinCharsAndIdMap);
        }

        this.executor = new ThreadPoolExecutor(1, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    private void doClose() {
        Async.waitAndShutdown(this.executor, 1500);

        closeSQLite(this.db);

        this.db = null;
        this.pinyinCharsTree = null;
        this.executor = null;
    }

    private File getUserDBFile(Context context) {
        return getDBFile(context, PinyinDictDBType.user);
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

    // =================== End: 数据库管理 ==================

    // =================== Start: 数据版本升级 ==================

    /** 升级数据版本：已成功升级的，将不会重复处理 */
    private void doUpgrade(Context context) {
        String version = getVersion(context);

        if (FIRST_INSTALL_VERSION.equals(version)) {
            From_v0.upgrade(context, this);
        } else if (VERSION_V2.equals(version) && LATEST_VERSION.equals(VERSION_V3)) {
            From_v2_to_v3.upgrade(context, this);
        }

        updateToLatestVersion(context);
    }

    /** 获取应用本地的用户数据的版本 */
    private String getVersion(Context context) {
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

    // =================== End: 数据版本升级 ==================

    public interface Listener {

        default void beforeOpen(PinyinDict dict) {}

        default void afterOpen(PinyinDict dict) {}

        class Noop implements Listener {}
    }
}
