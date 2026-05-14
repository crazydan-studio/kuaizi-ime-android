# 300 — 字典系统重构设计

## 1. 概述

字典系统是筷字输入法的核心数据层，负责拼音字词查询、用户输入历史记录、收藏管理以及数据库版本升级。Java 版本使用手写 SQLiteOpenHelper 和异步单例模式管理字典，v4 版本将其重构为基于 Room 的类型安全数据库访问层，配合协程实现异步操作。

---

## 2. Java 版本字典系统分析

### 2.1 架构

```
IMEditorDict (单例，异步开关)
├── PinyinDict (拼音字典)
│   ├── PinyinCharsTree (拼音字符树)
│   ├── HMM (隐马尔可夫模型)
│   │   └── Viterbi (维特比算法)
│   ├── PinyinDBHelper (拼音字库 DB)
│   └── HmmDBHelper (HMM 数据 DB)
├── UserInputDataDict (用户输入历史)
│   └── UserInputDataDBHelper (用户输入数据 DB)
└── UserInputFavoriteDict (用户收藏)
    └── UserInputFavoriteDBHelper (用户收藏 DB)
```

### 2.2 数据库结构

| 数据库 | 表 | 用途 |
|--------|-----|------|
| pinyin_word | words (spell, text, freq, variant, tone) | 拼音字库 |
| pinyin_phrase | phrases (spells, text, freq) | 拼音词库 |
| user_input_data | user_inputs (text, type, freq, last_used) | 用户输入频率 |
| user_input_favorite | favorites (text, type, usage_count, created_at) | 用户收藏 |
| hmm_data | hmm_* (状态转移概率表) | HMM 词组预测 |

### 2.3 问题分析

1. **手写 SQL 无编译期检查**：SQL 语句以字符串形式嵌入代码，拼写错误只能在运行时发现
2. **单例模式的线程安全**：`IMEditorDict` 的异步打开/关闭使用 `CompletableFuture`，逻辑复杂
3. **数据库升级分散**：`From_v0`、`From_v2_to_v3`、`From_v3_to_v4` 分散在不同类中
4. **无类型安全**：数据库查询结果以 `Cursor` 形式返回，需要手动读取列值
5. **HMM 实现粗糙**：`Hmm` 和 `Viterbi` 是简单的 Java 实现，未优化性能

---

## 3. v4 字典系统设计

### 3.1 整体架构

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

### 3.2 数据库方案

**选择 Room**，理由：
1. 项目仅 Android 平台，Room 是 Android 官方推荐
2. 已有手写 SQL 逻辑，Room 的迁移路径更直接
3. Room 提供 LiveData/Flow 集成，适合响应式架构
4. 编译期 SQL 检查，避免运行时 SQL 错误

### 3.3 Entity 定义

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

### 3.4 DAO 定义

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

### 3.5 Repository 层

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

## 4. 数据库升级策略

### 4.1 Room Migration

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
                // 保留 Java 版本的升级逻辑
            }
        }
    }
}
```

### 4.2 预构建字典

Java 版本将拼音字典（pinyin_word_dict.db、pinyin_phrase_dict.db）打包在 raw 资源中，首次安装时复制到应用数据目录。v4 版本改为以 assets 形式组织，放在 `assets/dict/` 目录中，并使用 Room 的 `createFromAsset()` 简化数据库初始化：

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

> **设计说明**：使用 `createFromAsset()` 替代手动复制 raw 资源的方式，Room 会在首次打开数据库时自动从 assets 中复制字典文件，大幅简化初始化逻辑。

---

## 5. PinyinCharsTree 重构

### 5.1 Java 版本

```java
class PinyinCharsTree {
    Map<Character, PinyinCharsTree> children;
    List<CharKey> keys;
}
```

这是一个用于导航拼音字符的 Trie 结构。

### 5.2 v4 版本

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

## 6. HMM + Viterbi 重构

### 6.1 改进点

Java 版本的 HMM 和 Viterbi 实现是纯 Java 的简单实现。v4 版本的改进：

1. **类型安全**：使用 value class 封装状态和观察值
2. **协程化**：大规模计算在 `Dispatchers.Default` 上执行
3. **不可变数据**：转移矩阵和发射矩阵使用不可变 Map

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
            // ...
        }
}
```

---

## 7. Java 功能完整对照

| Java 字典功能 | v4 对应 | 改进说明 |
|-------------|---------|---------|
| `IMEditorDict` 单例 | `DictRepository` + Room | 移除单例，依赖注入 |
| `IMEditorDict.openAsync()` | Room 自动管理 | Room 处理数据库连接 |
| `IMEditorDict.close()` | Room 自动管理 | 生命周期绑定 |
| `PinyinDict.findWords()` | `PinyinWordDao.lookupBySpell()` | 类型安全，编译期检查 |
| `PinyinDict.findPhrases()` | `PinyinPhraseDao.lookupBySpells()` | 类型安全 |
| `PinyinCharsTree` | 不可变 `PinyinCharsTree` | 线程安全 |
| `Hmm` + `Viterbi` | `HmmModel` + `ViterbiDecoder` | 类型安全，协程化 |
| `UserInputDataDict.record()` | `UserInputDao.incrementFrequency()` | Room DAO |
| `UserInputDataDict.findCompletions()` | `UserInputDao.getCompletions()` | Room DAO |
| `UserInputFavoriteDict` | `FavoriteDao` + Flow | 响应式查询 |
| `From_v0/v2_v3/v3_v4` 升级 | Room Migration | 标准化迁移 |
| `Emojis` 枚举 | `EmojiGroups` data class | 不依赖 DB |
| `SymbolGroup` + `Symbol` | `SymbolGroup` data class | 不依赖 DB |
| `DictDBType` 枚举 | Room Database 类 | 移除，Room 管理 DB 类型 |
| raw 字典复制 | `createFromAsset("dict/...")` | assets/dict 组织，Room 自动复制 |
