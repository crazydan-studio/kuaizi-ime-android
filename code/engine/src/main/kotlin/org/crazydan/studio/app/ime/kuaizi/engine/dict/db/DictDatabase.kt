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
