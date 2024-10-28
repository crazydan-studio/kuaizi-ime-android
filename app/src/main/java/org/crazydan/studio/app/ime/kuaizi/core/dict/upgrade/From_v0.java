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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.core.dict.PinyinDictDBType;
import org.crazydan.studio.app.ime.kuaizi.utils.FileUtils;

import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.copySQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.utils.DBUtils.vacuumSQLite;

/**
 * 首次安装版本的初始化
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public class From_v0 {

    public static void upgrade(Context context, PinyinDict dict) {
        doWithTransferDB(context, dict, (transferDBFile, userDBFile, appPhraseDBFile) -> {
            try (SQLiteDatabase transferDB = openSQLite(transferDBFile, false)) {
                doUpgrade(transferDB, appPhraseDBFile);
                vacuumSQLite(transferDB);
            }
        });
    }

    protected static void doUpgrade(SQLiteDatabase targetDB, File appPhraseDBFile) {
        String[] clauses = new String[] {
                // 连接应用库
                "attach database '" + appPhraseDBFile.getAbsolutePath() + "' as app",
                // 创建包含用户和应用权重数据的词典表
                "create table" //
                + " if not exists phrase_word ("
                //  -- 拼音字 id: 其为 link_word_with_pinyin 中的 id_
                + "   word_id_ integer not null,"
                //  -- 拼音字母组合 id: 其为 link_word_with_pinyin 中的 spell_chars_id_
                + "   spell_chars_id_ integer not null,"
                // -- 应用字典中短语内的字权重：出现次数
                + "   weight_app_ integer not null,"
                // -- 用户字典中短语内的字权重：出现次数
                + "   weight_user_ integer not null," //
                + "   primary key (word_id_, spell_chars_id_)" //
                + " )",
                //
                "create table" //
                + " if not exists phrase_trans_prob ("
                //  -- 当前拼音字 id: EOS 用 -1 代替（句尾字）
                //  -- Note：其为字典库中 link_word_with_pinyin 中的 id_
                + "   word_id_ integer not null,"
                //  -- 前序拼音字 id: BOS 用 -1 代替（句首字），TOTAL 用 -2 代替
                //  -- Note：其为字典库中 link_word_with_pinyin 中的 id_
                + "   prev_word_id_ integer not null,"
                //  -- 当 word_id_ == -1 且 prev_word_id_ == -2 时，其代表训练数据的句子总数，用于计算句首字出现频率；
                //  -- 当 word_id_ == -1 且 prev_word_id_ != -1 时，其代表末尾字出现次数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ == -1 时，其代表句首字出现次数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ == -2 时，其代表当前拼音字的转移总数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ != -1 时，其代表前序拼音字的出现次数；
                // -- 应用字典中字出现的次数
                + "   value_app_ integer not null,"
                // -- 用户字典中字出现的次数
                + "   value_user_ integer not null," //
                + "   primary key (word_id_, prev_word_id_)" //
                + " )",
                // 添加用户库表
                "create table" //
                + " if not exists meta_latin (" //
                + "   id_ integer not null primary key,"
                // -- 拉丁文内容
                + "   value_ text not null,"
                // -- 按使用频率等排序的权重
                + "   weight_user_ integer not null," //
                + "   unique (value_)" //
                + " )",
                // 通过 SQL 迁移数据
                "insert into phrase_word"
                + "   (word_id_, spell_chars_id_, weight_app_, weight_user_)"
                + " select"
                + "   word_id_, spell_chars_id_,"
                + "   app_.weight_ as weight_app_,"
                + "   0 as weight_user_"
                + " from app.phrase_word as app_",
                //
                "insert into phrase_trans_prob"
                + "   (word_id_, prev_word_id_, value_app_, value_user_)"
                + " select"
                + "   word_id_, prev_word_id_,"
                + "   app_.value_ as value_app_,"
                + "   0 as value_user_"
                + " from app.phrase_trans_prob as app_",
                };

        execSQLite(targetDB, clauses);
    }

    /** 使用应用的字典库作为迁移库，在该库上做数据升级相关的迁移操作 */
    protected static void doWithTransferDB(Context context, PinyinDict dict, TransferConsumer consumer) {
        File appPhraseDBFile = dict.getDBFile(context, PinyinDictDBType.app_phrase);

        File userDBFile = dict.getDBFile(context, PinyinDictDBType.user);
        // 使用应用的字典库作为迁移库
        File transferDBFile = dict.getDBFile(context, PinyinDictDBType.app_word);

        // Note: SQLite 数据库只有复制到本地才能进行 SQL 操作
        copySQLite(context, appPhraseDBFile, R.raw.pinyin_phrase_dict);
        copySQLite(context, transferDBFile, R.raw.pinyin_word_dict);

        try {
            consumer.transfer(transferDBFile, userDBFile, appPhraseDBFile);

            // 迁移库就地转换为用户库
            FileUtils.moveFile(transferDBFile, userDBFile);
        } catch (Exception e) {
            // 避免库迁移异常造成文件残留
            FileUtils.deleteFile(userDBFile);
        } finally {
            FileUtils.deleteFile(transferDBFile);
            FileUtils.deleteFile(appPhraseDBFile);
        }
    }

    protected interface TransferConsumer {
        void transfer(File transferDBFile, File userDBFile, File appPhraseDBFile);
    }
}
