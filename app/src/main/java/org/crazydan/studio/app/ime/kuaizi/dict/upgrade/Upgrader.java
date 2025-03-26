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
import android.util.Log;
import org.crazydan.studio.app.ime.kuaizi.R;
import org.crazydan.studio.app.ime.kuaizi.common.utils.FileUtils;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDict;
import org.crazydan.studio.app.ime.kuaizi.dict.PinyinDictDBType;

import static org.crazydan.studio.app.ime.kuaizi.common.utils.DBUtils.copySQLite;

/**
 * @author <a href="mailto:flytreeleft@crazydan.org">flytreeleft</a>
 * @date 2025-03-26
 */
public abstract class Upgrader {

    public abstract void upgrade(Context context, PinyinDict dict);

    interface DBTransferConsumer {
        void transfer(DBFiles dbFiles);
    }

    static class DBFiles {
        /** {@link PinyinDictDBType#user 用户库}对应的数据库文件 */
        protected File user;

        /**
         * 数据迁移库
         * <p/>
         * 在需要将{@link PinyinDictDBType#user 用户库}和{@link PinyinDictDBType#app_word 字典库}进行合并时，
         * 需通过该迁移库作为中间库来实施数据迁移和合并，从而避免迁移失败而导致用户数据丢失
         * <p/>
         * 该迁移库实际为应用安装包内{@link PinyinDictDBType#app_word 字典库}的副本，可以对其进行反复重试，
         * 在操作全部成功后，需用该文件替换 {@link #user} 库文件，
         * 该替换操作会在函数 {@link #doWithTransferDB} 内自动完成
         */
        protected File dataTransfer;

        /** 应用安装包内{@link PinyinDictDBType#app_phrase 词典库}的副本 */
        protected File appPhrase;
    }

    /**
     * 使用应用的字典库作为迁移库，在该库上做数据升级相关的迁移操作
     * <p/>
     * 注意：该函数会在操作成功后将迁移库替换用户库，
     * 因此，在不涉及数据迁移和合并的升级过程，仅仅是对用户库的更新，则不能在该函数内进行
     */
    static void doWithTransferDB(Context context, PinyinDict dict, DBTransferConsumer consumer) {
        File appPhraseDBFile = dict.getDBFile(context, PinyinDictDBType.app_phrase);

        File userDBFile = dict.getDBFile(context, PinyinDictDBType.user);
        // 使用应用的字典库作为迁移库
        File transferDBFile = dict.getDBFile(context, PinyinDictDBType.app_word);

        // Note: SQLite 数据库只有复制到本地才能进行 SQL 操作
        copySQLite(context, appPhraseDBFile, R.raw.pinyin_phrase_dict);
        copySQLite(context, transferDBFile, R.raw.pinyin_word_dict);

        try {
            consumer.transfer(new DBFiles() {{
                this.user = userDBFile;
                this.dataTransfer = transferDBFile;
                this.appPhrase = appPhraseDBFile;
            }});

            // 迁移库就地转换为用户库
            FileUtils.moveFile(transferDBFile, userDBFile);
        } catch (Exception e) {
            Log.e("DictUpgrade", "Failed to doWithTransferDB", e);
            throw e;
        } finally {
            FileUtils.deleteFile(transferDBFile);
            FileUtils.deleteFile(appPhraseDBFile);
        }
    }
}
