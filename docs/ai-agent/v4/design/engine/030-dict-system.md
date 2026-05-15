# 字典系统

```plantuml
@file:../diagrams/engine-dict-system.puml
```

## 1. 概述

字典系统是筷字输入法的核心数据层，负责拼音字词查询、用户输入历史记录、收藏管理以及数据库版本升级。基于 Room 的类型安全数据库访问层，配合协程实现异步操作。

---

## 2. 整体架构

```
DictRepository (仓库，协程化)
├── PinyinDictSource (拼音字典数据源)
│   ├── WordDao (字库 Room DAO)
│   ├── PhraseDao (词库 Room DAO)
│   └── HmmDao (HMM 数据 Room DAO)
├── UserInputDataSource (用户输入数据源)
│   └── UserInputDao (Room DAO)
└── FavoriteDataSource (收藏数据源)
    └── FavoriteDao (Room DAO)
```

---

## 3. ImeDatabase 与 Entity

### 3.1 数据库定义

```kotlin
@Database(
    entities = [
        PinyinWordEntity::class,
        PinyinPhraseEntity::class,
        UserInputEntity::class,
        FavoriteEntity::class,
        HmmTransitionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ImeDatabase : RoomDatabase() {
    abstract fun pinyinWordDao(): PinyinWordDao
    abstract fun pinyinPhraseDao(): PinyinPhraseDao
    abstract fun userInputDao(): UserInputDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun hmmDao(): HmmDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v3 → v4 的升级逻辑
            }
        }
    }
}
```

### 3.2 Entity 定义

```kotlin
// 拼音字
@Entity(tableName = "pinyin_word", indices = [Index(value = ["spell"])])
data class PinyinWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "spell") val spell: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "freq") val freq: Int,
    @ColumnInfo(name = "variant") val variant: String? = null,
    @ColumnInfo(name = "tone") val tone: Int? = null,
)

// 拼音词组
@Entity(tableName = "pinyin_phrase", indices = [Index(value = ["spells"])])
data class PinyinPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "spells") val spells: String,  // 逗号分隔的拼音
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "freq") val freq: Int,
)

// 用户输入记录
@Entity(tableName = "user_input_data", indices = [Index(value = ["text", "type"], unique = true)])
data class UserInputEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "freq") val freq: Int,
    @ColumnInfo(name = "last_used") val lastUsed: Long,
)

// 用户收藏
@Entity(tableName = "user_input_favorite", indices = [Index(value = ["text"], unique = true)])
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "usage_count") val usageCount: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

// HMM 状态转移
@Entity(tableName = "hmm_transition")
data class HmmTransitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_state") val fromState: String,
    @ColumnInfo(name = "to_state") val toState: String,
    @ColumnInfo(name = "probability") val probability: Double,
)
```

---

## 4. DAO 接口

```kotlin
@Dao
interface PinyinWordDao {
    @Query("SELECT * FROM pinyin_word WHERE spell = :spell ORDER BY freq DESC")
    suspend fun lookupBySpell(spell: String): List<PinyinWordEntity>

    @Query("SELECT * FROM pinyin_word WHERE spell LIKE :prefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PinyinWordEntity>

    @Query("SELECT DISTINCT spell FROM pinyin_word WHERE spell LIKE :prefix || '%' ORDER BY spell")
    suspend fun lookupSpellsByPrefix(prefix: String): List<String>

    @Query("SELECT * FROM pinyin_word WHERE spell = :spell AND variant IS NOT NULL ORDER BY freq DESC")
    suspend fun lookupVariants(spell: String): List<PinyinWordEntity>
}

@Dao
interface PinyinPhraseDao {
    @Query("SELECT * FROM pinyin_phrase WHERE spells LIKE :spellPrefix || '%' ORDER BY freq DESC LIMIT :limit")
    suspend fun lookupBySpellPrefix(spellPrefix: String, limit: Int = 20): List<PinyinPhraseEntity>

    @Query("SELECT * FROM pinyin_phrase WHERE spells = :spells ORDER BY freq DESC")
    suspend fun lookupBySpells(spells: String): List<PinyinPhraseEntity>
}

@Dao
interface UserInputDao {
    @Query("SELECT * FROM user_input_data WHERE type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getTopByType(type: String, limit: Int = 100): List<UserInputEntity>

    @Query("SELECT * FROM user_input_data WHERE text LIKE :prefix || '%' AND type = :type ORDER BY freq DESC LIMIT :limit")
    suspend fun getCompletions(prefix: String, type: String, limit: Int = 20): List<UserInputEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserInputEntity)

    @Query("UPDATE user_input_data SET freq = freq + 1, last_used = :timestamp WHERE text = :text AND type = :type")
    suspend fun incrementFrequency(text: String, type: String, timestamp: Long)

    @Query("DELETE FROM user_input_data")
    suspend fun clearAll()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM user_input_favorite ORDER BY usage_count DESC, created_at DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM user_input_favorite ORDER BY usage_count DESC, created_at DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM user_input_favorite WHERE text = :text")
    suspend fun delete(text: String)

    @Query("DELETE FROM user_input_favorite")
    suspend fun clearAll()
}
```

---

## 5. DictRepository

> **注意**：`DictRepository` 是引擎库内部基于 Room 的字典访问实现，属于 `:ime-engine` 模块的内部组件。引擎库对外暴露的公共接口是 `ImeDictProvider`（详见文档 160 第 6 节），`DictRepository` 是 `ImeDictProvider` 的内部实现细节——`ImeSqliteDictProvider` 内部委托 `DictRepository` 完成实际的数据库操作。第三方应用可通过实现 `ImeDictProvider` 接口替换整个字典层，而无需了解 `DictRepository` 的存在。

```kotlin
class DictRepository(
    private val wordDao: PinyinWordDao,
    private val phraseDao: PinyinPhraseDao,
    private val userInputDao: UserInputDao,
    private val favoriteDao: FavoriteDao,
    private val hmmDao: HmmDao,
) {
    /** 查询拼音字的候选项 */
    suspend fun lookupPinyinWords(spell: String): List<InputWord.Pinyin> {
        return wordDao.lookupBySpell(spell).map { it.toDomain() }
    }

    /** 查询拼音词组的候选项 */
    suspend fun lookupPinyinPhrases(spells: String): List<InputWord.PinyinPhrase> {
        return phraseDao.lookupBySpells(spells).map { it.toDomain() }
    }

    /** 使用 HMM+Viterbi 预测词组 */
    suspend fun predictPhrase(currentSpell: String, context: List<String>): List<InputWord.PinyinPhrase> {
        val candidates = hmmDao.predictNextStates(currentSpell)
        return candidates.flatMap { phraseDao.lookupBySpells(it).map { e -> e.toDomain() } }
    }

    /** 获取拉丁词补全 */
    suspend fun getLatinCompletions(prefix: String): List<InputCompletion.LatinWord> {
        return userInputDao.getCompletions(prefix, "latin").map { it.toCompletion() }
    }

    /** 记录用户输入 */
    suspend fun recordUserInput(text: String, type: String) {
        userInputDao.incrementFrequency(text, type, System.currentTimeMillis())
    }

    /** 获取所有收藏（Flow，响应式） */
    fun observeFavorites(): Flow<List<InputFavorite>> {
        return favoriteDao.getAllFlow().map { entities -> entities.map { it.toDomain() } }
    }
}
```

---

## 6. ImeDictProvider / ImeSqliteDictProvider

```kotlin
interface ImeDictProvider {
    fun query(pinyin: String): List<Candidate>
    fun queryPrefix(prefix: String): List<Candidate>
    fun recordInput(pinyin: String, word: String)
}

class ImeSqliteDictProvider(
    private val dictRepository: DictRepository,
    private val pinyinCharsTree: PinyinCharsTree,
    private val hmmModel: HmmModel,
) : ImeDictProvider {
    override fun query(pinyin: String): List<Candidate> { ... }
    override fun queryPrefix(prefix: String): List<Candidate> { ... }
    override fun recordInput(pinyin: String, word: String) { ... }
}
```

**查询流程**：
1. PinyinCharsTree 前缀匹配获取候选
2. HmmModel 计算转移概率排序
3. ViterbiDecoder 解码最优路径
4. 合并用户词频和收藏权重

---

## 7. 预构建字典

以 assets 形式组织，放在 `assets/dict/` 目录中，使用 Room 的 `createFromAsset()` 简化数据库初始化：

**assets 目录结构**：

```
code/app/src/main/assets/dict/
├── pinyin_word_dict.db      ← 拼音字库
├── pinyin_phrase_dict.db    ← 拼音词库
└── hmm_data.db              ← HMM 数据
```

**数据库初始化**：

```kotlin
fun createDatabase(context: Context): ImeDatabase {
    return Room.databaseBuilder(context, ImeDatabase::class.java, "kuaizi_dict.db")
        .createFromAsset("dict/pinyin_word_dict.db")
        .addMigrations(MIGRATION_3_4)
        .build()
}
```

---

## 8. PinyinCharsTree 前缀树

```kotlin
data class PinyinCharsTree(
    val children: Map<Char, PinyinCharsTree> = emptyMap(),
    val keys: List<InputKey.Char> = emptyList(),
) {
    /** 查找指定前缀的子树 */
    fun find(prefix: String): PinyinCharsTree? {
        if (prefix.isEmpty()) return this
        val first = prefix.first()
        return children[first]?.find(prefix.drop(1))
    }

    /** 获取所有可达字符 */
    fun allReachableChars(): Set<Char> = children.keys

    /** 构建器 */
    class Builder {
        private val nodes = mutableMapOf<String, MutableMap<Char, PinyinCharsTree>>()

        fun addPath(path: String, key: InputKey.Char): Builder {
            // 构建 Trie 路径
            return this
        }

        fun build(): PinyinCharsTree {
            // 构建不可变 Trie
            return PinyinCharsTree()
        }
    }
}
```

---

## 9. HmmModel + ViterbiDecoder

```kotlin
@JvmInline
value class HmmState(val value: String)

@JvmInline
value class HmmObservation(val value: String)

data class HmmModel(
    val states: Set<HmmState>,
    val observations: Set<HmmObservation>,
    val initialProb: Map<HmmState, Double>,
    val transitionProb: Map<HmmState, Map<HmmState, Double>>,
    val emissionProb: Map<HmmState, Map<HmmObservation, Double>>,
)

class ViterbiDecoder(private val model: HmmModel) {
    suspend fun decode(observations: List<HmmObservation>): List<HmmState> =
        withContext(Dispatchers.Default) {
            // Viterbi 算法实现
        }
}
```

**设计要点**：
- 类型安全：使用 value class 封装状态和观察值
- 协程化：大规模计算在 `Dispatchers.Default` 上执行
- 不可变数据：转移矩阵和发射矩阵使用不可变 Map
