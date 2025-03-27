/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2025 Crazydan Studio <https://studio.crazydan.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <https://www.gnu.org/licenses/lgpl-3.0.en.html#license-text>.
 */

package org.crazydan.studio.app.ime.kuaizi.dict.upgrade;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.crazydan.studio.app.ime.kuaizi.IMEditorDict;
import org.crazydan.studio.app.ime.kuaizi.dict.DictDBType;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.execSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.openSQLite;
import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.vacuumSQLite;

/**
 * 首次安装版本的初始化
 *
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2024-10-27
 */
public class From_v0 extends Upgrader {

    @Override
    public void upgrade(Context context, IMEditorDict dict) {
        doWithTransferDB(context, dict, (dbFiles) -> {
            try (SQLiteDatabase targetDB = openSQLite(dbFiles.dataTransfer, false)) {
                doUpgrade(targetDB, dbFiles.appPhrase);

                vacuumSQLite(targetDB);
            }
        });
    }

    /** 初始化 v0 版数据库，并{@link #mergePhraseDictData 合并应用的词典数据} */
    protected static void doUpgrade(SQLiteDatabase targetDB, File appPhraseDBFile) {
        initWordDictTables(targetDB);
        initPhraseDictTables(targetDB);

        initFavoriteTables(targetDB);

        mergePhraseDictData(targetDB, appPhraseDBFile);
    }

    /**
     * 初始化与字典相关的数据表
     * <p/>
     * 涉及补充索引、初始化列数据等操作
     */
    protected static void initWordDictTables(SQLiteDatabase targetDB) {
        String[] clauses = new String[] {
                // <<<<<<<<<<<<< 为内置表补充索引
                "create index idx_meta_py_chars_val on meta_pinyin_chars(value_)",
                "create index idx_py_word_word on pinyin_word(word_, word_id_)",
                "create index idx_py_word_spell on pinyin_word(spell_, spell_id_, spell_chars_id_)",
                // >>>>>>>>>>>>>>>>>>>>>>
                //
                // <<<<<<<<<<<<< 补充或调整用户库表
                "create table" //
                + " if not exists meta_latin (" //
                + "   id_ integer not null primary key,"
                // -- 拉丁文内容
                + "   value_ text not null,"
                // -- 使用权重
                + "   weight_user_ integer not null," //
                + "   unique (value_)" //
                + " )",
                // 表情及其关键字
                "alter table meta_emoji"
                // -- 补充用户使用权重列
                + "  add column weight_user_ integer default 0",
                "alter table meta_emoji"
                // -- 补充在系统内是否可用的标记
                + "  add column enabled_ integer default 1",
                "create view"
                + " if not exists emoji ("
                + "   id_, value_, weight_, enabled_,"
                + "   group_, keyword_ids_list_"
                + " ) as"
                + " select"
                + "   emo_.id_, emo_.value_, emo_.weight_user_,"
                + "   emo_.enabled_, grp_.value_, emo_.keyword_ids_list_"
                + " from"
                + "   meta_emoji emo_"
                + "   inner join meta_emoji_group grp_ on grp_.id_ = emo_.group_id_",
                // >>>>>>>>>>>>>>>>>>>>>>>
        };

        execSQLite(targetDB, clauses);
    }

    /** 初始化与词典相关的数据表 */
    protected static void initPhraseDictTables(SQLiteDatabase targetDB) {
        String[] clauses = new String[] {
                // <<<<<<<<<<<<<<<<<<< 创建包含用户和应用权重数据的词典表
                "create table" //
                + " if not exists phrase_word ("
                //  -- 拼音字 id: 其为 pinyin_word 中的 id_
                + "   word_id_ integer not null,"
                //  -- 拼音字母组合 id: 其为 pinyin_word 中的 spell_chars_id_
                + "   spell_chars_id_ integer not null,"
                // -- 应用字典中短语内的字权重：出现次数
                + "   weight_app_ integer not null,"
                // -- 用户字典中短语内的字权重：出现次数
                + "   weight_user_ integer not null,"
                //
                + "   primary key (word_id_)" //
                + " )",
                //
                "create table" //
                + " if not exists phrase_trans_prob ("
                //  -- 当前拼音字 id: EOS 用 -1 代替（句尾字）
                //  -- Note：其为字典库中 pinyin_word 中的 id_
                + "   word_id_ integer not null,"
                // -- 当前拼音字的拼音字母组合 id: 方便直接按拼音字母组合搜索
                // -- Note：其为字典库中 字及其拼音表（pinyin_word）中的 spell_chars_id_
                + "   word_spell_chars_id_ integer not null,"
                //  -- 前序拼音字 id: BOS 用 -1 代替（句首字），TOTAL 用 -2 代替
                //  -- Note：其为字典库中 pinyin_word 中的 id_
                + "   prev_word_id_ integer not null,"
                // -- 前序拼音字的拼音字母组合 id: 方便直接按拼音字母组合搜索
                // -- Note：其为字典库中 字及其拼音表（pinyin_word）中的 spell_chars_id_
                + "   prev_word_spell_chars_id_ integer not null,"
                //  -- 当 word_id_ == -1 且 prev_word_id_ == -2 时，其代表训练数据的句子总数，用于计算句首字出现频率；
                //  -- 当 word_id_ == -1 且 prev_word_id_ != -1 时，其代表末尾字出现次数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ == -1 时，其代表句首字出现次数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ == -2 时，其代表当前拼音字的转移总数；
                //  -- 当 word_id_ != -1 且 prev_word_id_ != -1 时，其代表前序拼音字的出现次数；
                // -- 应用字典中字出现的次数
                + "   value_app_ integer not null,"
                // -- 用户字典中字出现的次数
                + "   value_user_ integer not null,"
                //
                + "   primary key (word_id_, prev_word_id_)" //
                + " )",
                // >>>>>>>>>>>>>>>>>>>>>>
        };

        execSQLite(targetDB, clauses);
    }

    /** 初始化与收藏相关的数据表 */
    protected static void initFavoriteTables(SQLiteDatabase targetDB) {
        String[] clauses = new String[] {
                "create table"
                //
                + " if not exists user_favorite ("
                + "   id_ integer not null,"
                + "   type_ text not null,"
                + "   text_ text not null,"
                + "   html_ text default null,"
                + "   shortcut_ text default null,"
                + "   created_at_ integer not null,"
                + "   used_count_ integer default 0,"
                + "   used_at_ integer default 0,"
                //
                + "   primary key (id_)"
                + " )",
                };

        execSQLite(targetDB, clauses);
    }

    /** 将 {@link DictDBType#app_phrase} 内的数据合并到目标库中 */
    protected static void mergePhraseDictData(SQLiteDatabase targetDB, File appPhraseDBFile) {
        String[] clauses = new String[] {
                // 连接应用库
                "attach database '" + appPhraseDBFile.getAbsolutePath() + "' as app",
                //
                // 通过 SQL 补齐数据
                "insert into phrase_word ("
                + "   word_id_, spell_chars_id_, weight_app_, weight_user_"
                + " )"
                + " select"
                + "   word_id_, -3 as spell_chars_id_,"
                + "   app_.weight_ as weight_app_,"
                + "   0 as weight_user_"
                + " from app.phrase_word app_",
                //
                "insert into phrase_trans_prob ("
                + "   word_id_, prev_word_id_,"
                + "   word_spell_chars_id_, prev_word_spell_chars_id_,"
                + "   value_app_, value_user_"
                + " )"
                + " select"
                + "   word_id_, prev_word_id_,"
                + "   (case"
                + "     when word_id_ < 0 then word_id_"
                + "     else -3"
                + "   end) as word_spell_chars_id_,"
                + "   (case"
                + "     when prev_word_id_ < 0 then prev_word_id_"
                + "     else -3"
                + "   end) as prev_word_spell_chars_id_,"
                + "   app_.value_ as value_app_,"
                + "   0 as value_user_"
                + " from app.phrase_trans_prob app_",
                //
                "update phrase_word"
                + " set spell_chars_id_ = ("
                + "   select spell_chars_id_"
                + "   from pinyin_word"
                + "   where id_ = phrase_word.word_id_"
                + " )"
                + " where spell_chars_id_ = -3",
                "update phrase_trans_prob"
                + " set word_spell_chars_id_ = ("
                + "   select spell_chars_id_"
                + "   from pinyin_word"
                + "   where id_ = phrase_trans_prob.word_id_"
                + " )"
                + " where word_spell_chars_id_ = -3",
                "update phrase_trans_prob"
                + " set prev_word_spell_chars_id_ = ("
                + "   select spell_chars_id_"
                + "   from pinyin_word"
                + "   where id_ = phrase_trans_prob.prev_word_id_"
                + " )"
                + " where prev_word_spell_chars_id_ = -3",
                //
                "create index idx_ph_wrd_spell_chars on phrase_word(spell_chars_id_)",
                "create index idx_ph_trp_spell_chars"
                + " on phrase_trans_prob(word_spell_chars_id_, prev_word_spell_chars_id_);",
                };

        execSQLite(targetDB, clauses);
    }
}
