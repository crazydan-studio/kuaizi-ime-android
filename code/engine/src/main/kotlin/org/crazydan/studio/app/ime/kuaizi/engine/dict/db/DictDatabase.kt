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

package org.crazydan.studio.app.ime.kuaizi.engine.dict.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

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
        @Volatile private var INSTANCE: DictDatabase? = null

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
