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

package org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.Hmm;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.db.HmmDBHelper.saveHmm;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v0.doWithTransferDB;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.SQLiteQueryParams;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.querySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.vacuumSQLite;

/**
 * 从 v2 版本升级到 v3 版本
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public class From_v2_to_v3 {

    public static void upgrade(Context context, PinyinDict dict) {
        doWithTransferDB(context, dict, (dbFiles) -> {
            try (SQLiteDatabase transferDB = openSQLite(dbFiles.transfer, false)) {
                doUpgrade(transferDB, dbFiles.user, dbFiles.appPhrase);
                vacuumSQLite(transferDB);

                // 清理无用文件
                for (String name : new String[] { "pinyin_app_dict.db", "pinyin_app_dict.db.hash" }) {
                    File file = new File(dbFiles.user.getParentFile(), name);
                    FileUtils.deleteFile(file);
                }
            }
        });
    }

    private static void doUpgrade(SQLiteDatabase targetDB, File userDBFile, File appPhraseDBFile) {
        // v2 版本内建表结构
        /*
        CREATE TABLE
            IF NOT EXISTS used_pinyin_word (
                -- id_, target_chars_id_ 与内置字典中的 pinyin_word 表的 id_ 一致
                id_ INTEGER NOT NULL PRIMARY KEY,
                target_chars_id_ INTEGER NOT NUll,
                weight_ INTEGER DEFAULT 0
            );

        CREATE TABLE
            IF NOT EXISTS used_phrase (
                id_ INTEGER NOT NULL PRIMARY KEY,
                -- 短语内容标识：由 used_phrase_pinyin_word 中的 target_id_ 拼接而成
                value_ TEXT NOT NULL,
                -- 按使用频率等排序的权重
                weight_ INTEGER DEFAULT 0,
                UNIQUE (value_)
            );
        CREATE TABLE
            IF NOT EXISTS used_phrase_pinyin_word (
                id_ INTEGER NOT NULL PRIMARY KEY,
                -- used_phrase 中的 id_
                source_id_ INTEGER NOT NULL,
                -- 与内置字典中的 pinyin_word 表的 id_ 一致
                target_id_ INTEGER NOT NULL,
                -- 拼音字母组合 id
                target_spell_chars_id_ INTEGER NOT NULL,
                -- 字在词中的序号
                target_index_ INTEGER NOT NULL,
                UNIQUE (source_id_, target_id_, target_index_),
                FOREIGN KEY (source_id_) REFERENCES used_phrase (id_)
            );

        DROP VIEW IF EXISTS used_pinyin_phrase;
        CREATE VIEW
            IF NOT EXISTS used_pinyin_phrase (
                id_,
                weight_,
                source_id_,
                target_id_,
                target_index_,
                target_spell_chars_id_
            ) AS
        SELECT
            lnk_.id_,
            phrase_.weight_,
            lnk_.source_id_,
            lnk_.target_id_,
            lnk_.target_index_,
            lnk_.target_spell_chars_id_
        FROM
            used_phrase phrase_
            INNER JOIN used_phrase_pinyin_word lnk_ on lnk_.source_id_ = phrase_.id_
        -- Note: group by 不能对组内元素排序，故，只能在视图内先排序
        ORDER BY
            lnk_.target_index_ asc;

        CREATE TABLE
            IF NOT EXISTS used_emoji (
                id_ INTEGER NOT NULL PRIMARY KEY,
                -- 按使用频率等排序的权重
                weight_ INTEGER DEFAULT 0
            );

        CREATE TABLE
            IF NOT EXISTS used_latin (
                id_ INTEGER NOT NULL PRIMARY KEY,
                -- 拉丁文内容
                value_ TEXT NOT NULL,
                -- 按使用频率等排序的权重
                weight_ INTEGER DEFAULT 0,
                UNIQUE (value_)
            );
        */

        From_v0.doUpgrade(targetDB, appPhraseDBFile);

        // <<<<<<<<<<<<<<<<<<< 迁移现有的用户数据
        String[] clauses = new String[] {
                "attach database '" + userDBFile.getAbsolutePath() + "' as user",
                // <<<<<<<<<<<<<<< 迁移现有数据
                // Note: SQLite 3.33.0 版本才支持 update-from
                // https://www.sqlite.org/lang_update.html#upfrom
//                "update meta_emoji as emoji_"
//                + "   set weight_user_ = user_.weight_"
//                + " from user.used_emoji user_"
//                + " where user_.id_ = emoji_.id_",
                "update meta_emoji"
                + " set weight_user_ = ifnull(("
                + "   select weight_"
                + "   from user.used_emoji user_"
                + "   where user_.id_ = meta_emoji.id_"
                + " ), weight_user_)",
                //
                "insert into meta_latin"
                + "   (id_, value_, weight_user_)"
                + " select"
                + "   user_.id_, user_.value_, user_.weight_"
                + " from user.used_latin user_",
                // >>>>>>>>>>>>>>>
        };
        execSQLite(targetDB, clauses);
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<< 获取用户输入短语出现次数
        // {'<字 id>:<字读音字母组合 id>,...': 10, ...}
        Map<String, Integer> usedPhraseCountMap = new HashMap<>();
        // Note：视图 used_pinyin_phrase 已默认按 target_index_ 升序排序，
        // 组合后的 target_id_ 与短语字的顺序是一致的
        querySQLite(targetDB, new SQLiteQueryParams<Void>() {{
            this.table = "user.used_pinyin_phrase";
            // Note: SQLite 3.44.0 版本才支持 concat 函数，低版本需采用 || 替代
            // https://sqlite.org/releaselog/3_44_0.html
            this.columns = new String[] {
                    "group_concat(" //
                    + "target_id_ || ':' || target_spell_chars_id_" //
                    + ", ',') as target_ids_", //
                    "weight_"
            };
            this.groupBy = "source_id_";
            this.having = "weight_ > 0";

            this.voidReader = (row) -> {
                String phraseWords = row.getString("target_ids_");
                int phraseCount = row.getInt("weight_");

                usedPhraseCountMap.put(phraseWords, phraseCount);
            };
        }});
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<<<<<<<<<<<<<< 更新用户输入短语权重
        Hmm hmm = Hmm.calcTransProb(usedPhraseCountMap);
        saveHmm(targetDB, hmm, false);
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    }
}
