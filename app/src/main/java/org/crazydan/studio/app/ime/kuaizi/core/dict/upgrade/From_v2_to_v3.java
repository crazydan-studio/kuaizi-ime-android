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
import org.crazydan.studio.app.ime.kuaizi.utils.DBUtils;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

import static org.crazydan.studio.app.ime.kuaizi.core.dict.hmm.HmmDBHelper.saveHmm;
import static org.crazydan.studio.app.ime.kuaizi.core.dict.upgrade.From_v0.doWithTransferDB;
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
        doWithTransferDB(context, dict, (transferDBFile, userDBFile, appPhraseDBFile) -> {
            try (SQLiteDatabase transferDB = openSQLite(transferDBFile, false)) {
                doUpgrade(transferDB, userDBFile, appPhraseDBFile);
                vacuumSQLite(transferDB);
            }

            // TODO 待删除 - 测试备份
            File userDBBakFile = new File(userDBFile.getParentFile(), userDBFile.getName() + ".bak");
            if (!userDBBakFile.exists()) {
                FileUtils.moveFile(userDBFile, userDBBakFile);
            }
        });
    }

    private static void doUpgrade(SQLiteDatabase targetDB, File userDBFile, File appPhraseDBFile) {
        From_v0.doUpgrade(targetDB, appPhraseDBFile);

        // <<<<<<<<<<<<<<<<<<< 迁移现有的用户数据
        String[] clauses = new String[] {
                "attach database '" + userDBFile.getAbsolutePath() + "' as user",
                // 添加用户库表所需字段
                "alter table meta_emoji"
                // -- 按使用频率等排序的权重
                + "  add column weight_user_ integer default 0",
                //
                "update meta_emoji as emoji_"
                + "   set emoji_.weight_user_ = user_.weight_"
                + " from user.used_emoji as user_"
                + " where user_.id_ = emoji_.id_",
                //
                "insert into meta_latin"
                + "   (id_, value_, weight_user_)"
                + " select"
                + "   user_.id_, user_.value_, user_.weight_"
                + " from user.used_latin as user_",
                };
        execSQLite(targetDB, clauses);
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<< 获取用户输入短语出现次数
        // {'<字 id>:<字读音字母组合 id>,...': 10, ...}
        Map<String, Integer> usedPhraseCountMap = new HashMap<>();
        // Note：视图 used_pinyin_phrase 已默认按 target_index_ 升序排序，
        // 组合后的 target_id_ 与短语字的顺序是一致的
        querySQLite(targetDB, new DBUtils.SQLiteQueryParams<Void>() {{
            this.table = "user.used_pinyin_phrase";
            this.columns = new String[] {
                    "group_concat("  //
                    + "  concat(target_id_, ':', target_spell_chars_id_)"  //
                    + ", ',') as target_ids_", //
                    "weight_"
            };
            this.where = "weight_ > 0 ";
            this.groupBy = "source_id_";
            this.reader = (row) -> {
                String phraseWords = row.getString("target_ids_");
                int phraseCount = row.getInt("weight_");

                usedPhraseCountMap.put(phraseWords, phraseCount);
                return null;
            };
        }});
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

        // <<<<<<<<<<<<<<<<<<<<<<<<< 更新用户输入短语权重
        Hmm hmm = Hmm.calcTransProb(usedPhraseCountMap);
        saveHmm(targetDB, hmm, false);
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    }
}
