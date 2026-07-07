/*
 * 筷字输入法 - 高效编辑需要又好又快的输入法
 * Copyright (C) 2026 Crazydan Studio <https://studio.crazydan.org>
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

package org.crazydan.studio.app.ime.kuaizi.dict.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Room 字典数据库，管理所有与输入法字典相关的数据表。
 *
 * 包含五张表：
 * - [PinyinWordEntity]：拼音字表
 * - [PinyinPhraseEntity]：拼音词组表
 * - [UserInputEntity]：用户输入记录表
 * - [FavoriteEntity]：用户收藏表
 * - [HmmTransitionEntity]：HMM 转移概率表
 *
 * 数据库文件默认名为 "kuaizi_dict.db"，支持从预置文件创建。
 * 使用单例模式确保全局只有一个数据库实例。
 */
@Database(
    entities = [
        PinyinWordEntity::class,
        PinyinPhraseEntity::class,
        UserInputEntity::class,
        FavoriteEntity::class,
        HmmTransitionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class DictDatabase : RoomDatabase() {
    abstract fun pinyinWordDao(): PinyinWordDao
    abstract fun pinyinPhraseDao(): PinyinPhraseDao
    abstract fun userInputDao(): UserInputDao
    abstract fun hmmDao(): HmmDao

    companion object {
        @Volatile
        private var INSTANCE: DictDatabase? = null

        /**
         * 获取数据库单例实例。
         *
         * @param context Android Context
         * @param dbFile 可选的预置数据库文件，存在时从该文件创建数据库
         */
        fun getInstance(context: Context, dbFile: File? = null): DictDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, dbFile).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, dbFile: File?): DictDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                DictDatabase::class.java,
                dbFile?.name ?: "kuaizi_dict.db",
            )
            if (dbFile != null && dbFile.exists()) {
                builder.createFromFile(dbFile)
            }
            return builder.build()
        }
    }
}
