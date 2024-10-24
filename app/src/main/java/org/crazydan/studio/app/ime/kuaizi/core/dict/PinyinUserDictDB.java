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

package org.crazydan.studio.app.ime.kuaizi.core.dict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.dict.predict.HMM;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.copySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-20
 */
public class PinyinUserDictDB {
    /** 最新版本号 */
    public static final String LATEST_VERSION = "v3";

    public static final String db_version_file = "pinyin_user_dict.version";

    private final Context context;

    private String version;
    private SQLiteDatabase db;

    private enum DictDBType {
        /** 用户库 */
        user("pinyin_user_dict.db"),
        /** 应用字典库 */
        app_word("pinyin_word_dict.app.db"),
        /** 应用词典库 */
        app_phrase("pinyin_phrase_dict.app.db");

        private final String fileName;

        DictDBType(String fileName) {
            this.fileName = fileName;
        }
    }

    public PinyinUserDictDB(Context context) {
        this.context = context;
    }

    /** 更新 HMM 数据 */
    public void updateHMM(SQLiteDatabase db, HMM hmm) {
        // 采用 SQLite 的 UPSET 机制插入或更新数据：https://www.sqlite.org/lang_upsert.html

        // =============================================================================
        Function<String, Integer[]> extractWordIds = (s) -> {
            String[] splits = s.split(":");

            return new Integer[] { Integer.parseInt(splits[0]), Integer.parseInt(splits[1]) };
        };

        List<Object[]> phraseWordData = hmm.wordWeight.keySet().stream().map((key) -> {
            Integer[] wordIds = extractWordIds.apply(key);
            Integer val = hmm.wordWeight.get(key);

            return new Object[] { wordIds[0], wordIds[1], val, val };
        }).collect(Collectors.toList());

        execSQLite(db,
                   "insert into"
                   + "  phrase_word(word_id_, spell_chars_id_, weight_app_, weight_user_)"
                   + "    values(?, ?, 0, ?)"
                   + "  on conflict(word_id_, spell_chars_id_)"
                   + "  do update set "
                   + "    weight_user_ = weight_user_ + ?",
                   phraseWordData);

        // ==============================================================================
        Function<String, Integer> getWordId = (s) -> {
            // EOS 用 -1 代替（句尾字）
            // BOS 用 -1 代替（句首字）
            // TOTAL 用 -2 代替（句子总数）
            if (HMM.EOS.equals(s) || HMM.BOS.equals(s)) {
                return -1;
            } else if (HMM.TOTAL.equals(s)) {
                return -2;
            }

            Integer[] wordIds = extractWordIds.apply(s);
            return wordIds[0];
        };

        List<Object[]> phraseTransProbData = new ArrayList<>();
        hmm.transProb.forEach((curr, prob) -> {
            Integer currId = getWordId.apply(curr);

            prob.forEach((prev, val) -> {
                Integer prevId = getWordId.apply(prev);

                phraseTransProbData.add(new Object[] { currId, prevId, val, val });
            });
        });

        execSQLite(db,
                   "insert into"
                   + "  phrase_trans_prob(word_id_, prev_word_id_, value_app_, value_user_)"
                   + "    values(?, ?, 0, ?)"
                   + "  on conflict(word_id_, prev_word_id_)"
                   + "  do update set "
                   + "    value_user_ = value_user_ + ?",
                   phraseTransProbData);
    }

    /** 升级数据库 */
    public void upgrade() {
        String version = getVersion();

        if ("v2".equals(version) && LATEST_VERSION.equals("v3")) {
            upgrade_v2_to_v3();
            updateToLatestVersion();
        }
    }

    /** 获取应用本地的用户数据库的版本 */
    public String getVersion() {
        if (this.version == null) {
            File userDBFile = getDBFile(this.context, DictDBType.user);
            File versionFile = getVersionFile();

            if (!versionFile.exists()) {
                if (userDBFile.exists()) {
                    this.version = "v2";
                } else {
                    this.version = LATEST_VERSION;
                }
            } else {
                this.version = FileUtils.read(versionFile, true);
            }
        }

        return this.version;
    }

    private void updateToLatestVersion() {
        File file = getVersionFile();

        try {
            FileUtils.write(file, LATEST_VERSION);
            this.version = LATEST_VERSION;
        } catch (IOException ignore) {
        }
    }

    private File getVersionFile() {
        return new File(this.context.getFilesDir(), db_version_file);
    }

    private File getDBFile(Context context, DictDBType dbType) {
        return new File(context.getFilesDir(), dbType.fileName);
    }

    // ============================= Start: 数据库升级 ==============================

    /** 数据库从 v2 版本升级到 v3 版本 */
    private void upgrade_v2_to_v3() {
        File appPhraseDBFile = getDBFile(this.context, DictDBType.app_phrase);

        File userDBFile = getDBFile(this.context, DictDBType.user);
        // 使用应用的字典库作为迁移库
        File transferDBFile = getDBFile(this.context, DictDBType.app_word);

        // Note: SQLite 数据库只有复制到本地才能进行 SQL 操作
        copySQLite(this.context, appPhraseDBFile, R.raw.pinyin_phrase_dict);
        copySQLite(this.context, transferDBFile, R.raw.pinyin_word_dict);

        try (
                SQLiteDatabase transferDB = openSQLite(transferDBFile, false);
        ) {
            // <<<<<<<<<<<<<<<< 初始化
            String[] clauses = new String[] {
                    // 连接应用和用户库
                    "attach database '" + appPhraseDBFile.getAbsolutePath() + "' as app;",
                    "attach database '" + userDBFile.getAbsolutePath() + "' as user;",
                    // 创建包含用户和应用权重数据的词典表
                    "create table" + "  if not exists phrase_word ("
                    //  -- 拼音字 id: 其为 link_word_with_pinyin 中的 id_
                    + "    word_id_ integer not null,"
                    //  -- 拼音字母组合 id: 其为 link_word_with_pinyin 中的 spell_chars_id_
                    + "    spell_chars_id_ integer not null,"
                    // -- 应用字典中短语内的字权重：出现次数
                    + "    weight_app_ integer not null,"
                    // -- 用户字典中短语内的字权重：出现次数
                    + "    weight_user_ integer not null," //
                    + "    primary key (word_id_, spell_chars_id_)" + "  );",
                    //
                    "create table" + "  if not exists phrase_trans_prob ("
                    //  -- 当前拼音字 id: 其为 link_word_with_pinyin 中的 id_
                    + "    word_id_ integer not null,"
                    //  -- 前序拼音字 id: 其为 link_word_with_pinyin 中的 id_
                    + "    prev_word_id_ integer not null,"
                    // -- 应用字典中字出现的次数
                    + "    value_app_ integer not null,"
                    // -- 用户字典中字出现的次数
                    + "    value_user_ integer not null," //
                    + "    primary key (word_id_, prev_word_id_)" + "  );",
                    // 添加用户库表所需字段
                    "alter table meta_emoji"
                    // -- 按使用频率等排序的权重
                    + "  add column weight_user_ integer default 0",
                    //
                    "create table" //
                    + "  if not exists meta_latin (" //
                    + "    id_ integer not null primary key,"
                    // -- 拉丁文内容
                    + "    value_ text not null,"
                    // -- 按使用频率等排序的权重
                    + "    weight_user_ integer not null," //
                    + "    unique (value_)" //
                    + "  );",
                    // 通过 SQL 迁移数据
                    "insert into phrase_word"
                    + "  (word_id_, spell_chars_id_, weight_app_, weight_user_)"
                    + "select"
                    + "  word_id_, spell_chars_id_,"
                    + "  app_.weight_ as weight_app_,"
                    + "  0 as weight_user_"
                    + "from app.phrase_word as app_;",
                    //
                    "insert into phrase_trans_prob"
                    + "  (word_id_, prev_word_id_, value_app_, value_user_)"
                    + "select"
                    + "  word_id_, prev_word_id_,"
                    + "  app_.value_ as value_app_,"
                    + "  0 as value_user_"
                    + "from app.phrase_trans_prob as app_;",
                    //
                    "update meta_emoji as emoji_"
                    + "  set emoji_.weight_user_ = user_.weight_"
                    + "  from user.used_emoji as user_"
                    + "    where user_.id_ = emoji_.id_",
                    //
                    "insert into meta_latin"
                    + "  (id_, value_, weight_user_)"
                    + "select"
                    + "  user_.id_, user_.value_, user_.weight_"
                    + "from user.used_latin as user_;",
                    };
            execSQLite(transferDB, clauses);
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            // <<<<<<<<<<<<< 获取用户输入短语出现次数
            // {'<字 id>:<字读音字母组合 id>,...': 10, ...}
            Map<String, Integer> usedPhraseCountMap = new HashMap<>();
            // Note：视图 used_pinyin_phrase 已默认按 target_index_ 升序排序，
            // 组合后的 target_id_ 与短语字的顺序是一致的
            querySQLite(transferDB, new SQLiteQueryParams<Void>() {{
                this.table = "user.used_pinyin_phrase";
                this.columns = new String[] {
                        "group_concat("  //
                        + "  concat(target_id_, ':', target_spell_chars_id_)"  //
                        + ", ',') as target_ids_", //
                        "weight_"
                };
                this.where = "weight_ > 0 ";
                this.groupBy = "source_id_";
                this.creator = (cursor) -> {
                    String phraseWords = cursor.getString(0);
                    int phraseCount = cursor.getInt(1);

                    usedPhraseCountMap.put(phraseWords, phraseCount);
                    return null;
                };
            }});
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            // <<<<<<<<<<<<<<<<<<<<<<<<< 更新用户输入短语权重
            HMM hmm = HMM.calcTransProb(usedPhraseCountMap);
            updateHMM(transferDB, hmm);
            // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

            execSQLite(transferDB, new String[] {
                    // 回收无用空间
                    "vacuum;",
                    });
        }

        // 迁移库就地转换为用户库
        FileUtils.moveFile(transferDBFile, userDBFile);
    }
    // ============================= End: 数据库升级 ==============================
}
