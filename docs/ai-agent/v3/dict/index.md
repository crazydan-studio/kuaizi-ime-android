# 字典系统

筷字输入法 v3 的字典系统基于 SQLite 数据库，结合 HMM（隐马尔可夫模型）+ Viterbi 算法实现拼音短语预测，支持用户输入数据积累和词频学习。

---

## 1. 字典核心类

### 1.1 PinyinDict

拼音字典，核心字典服务：

| 方法 | 说明 |
|------|------|
| `getCandidates(CharInput)` | 根据拼音获取候选词列表 |
| `getFirstBestCandidate()` | 获取最高权重候选词 |
| `getTopBestCandidateIds()` | 获取 Top-N 权重候选词 ID |
| `findTopBestMatchedPhrase()` | 基于 Viterbi 的短语预测，含已确认词约束 |
| `findTopBestEmojisMatchedPhrase()` | 从短语关键词中获取 Emoji 建议 |
| `getWord()` | 按词 + 拼音查找 |

### 1.2 PinyinCharsTree

3 层树结构，用于拼音音节分解（声母 → 介母 → 韵母）：

| 方法 | 说明 |
|------|------|
| `create(Map)` | 从数据库数据构建树 |
| `getCharsId()` | 输入映射到数据库 ID |
| `getNextChars()` | 自动补全建议 |
| `isPinyinCharsInput()` | 拼音有效性验证 |
| `getAllPinyinChars()` | 枚举所有拼音组合 |

### 1.3 UserInputDataDict

用户输入统计数据管理：

| 方法 | 说明 |
|------|------|
| `save(UserInputData)` | 异步保存，含 HMM 训练 |
| `revokeSave()` | 撤销保存 |
| `getAllEmojis()` | 分组获取 Emoji |
| `findTopBestMatchedLatins()` | 拉丁词补全 |

### 1.4 UserInputFavoriteDict

收藏 CRUD 操作：

| 方法 | 说明 |
|------|------|
| `save()` | 保存收藏 |
| `updateUsage()` | 更新使用统计 |
| `getAll()` | 获取所有收藏 |
| `remove()` | 删除收藏 |
| `clearAll()` | 清空收藏 |
| `exist()` | 检查是否存在 |

---

## 2. 数据库层

### 2.1 数据库类型（DictDBType）

| 类型 | 数据库文件 | 说明 |
|------|-----------|------|
| `user` | `ime_user_dict.db` | 用户数据（词频、短语、Emoji、拉丁词、收藏） |
| `app_word` | `pinyin_word_dict.app.db` | 应用内建单字字典 |
| `app_phrase` | `pinyin_phrase_dict.app.db` | 应用内建短语字典 |

### 2.2 Database Helper

| 类 | 功能 |
|------|------|
| `PinyinDBHelper` | 拼音单字查询：`getAllPinyinWordsByCharsId()`、`getFirstBestPinyinWord()`、`getTopBestPinyinWordIds()`、`getPinyinWordsByWordId()` |
| `HmmDBHelper` | HMM 数据管理：`predictPinyinPhrase()`（Viterbi 短语预测）、`saveUsedPinyinPhrase()`、`saveHmm()` |
| `UserInputDataDBHelper` | 用户数据查询：`getAllGroupedEmojis()`、`getLatinsByStarts()`、`getEmojisByKeyword()`、`saveUsedEmojis()`、`saveUsedLatins()` |
| `UserInputFavoriteDBHelper` | 收藏 CRUD：`saveInputFavorite()`、`updateInputFavoriteUsage()`、`getAllInputFavorites()`、`removeInputFavorites()`、`clearAllInputFavorites()` |

---

## 3. HMM + Viterbi 算法

### 3.1 Hmm

隐马尔可夫模型，用于拼音短语预测：

| 功能 | 说明 |
|------|------|
| `wordWeight` | 字词权重映射 |
| `transProb` | 字间转移概率嵌套映射 |
| `calcTransProb(Map)` | 批量训练，从短语计数计算转移概率 |
| `calcTransProb(List)` | 单短语训练 |
| BOS/EOS | 句首/句尾标记，用于句子边界建模 |

### 3.2 Viterbi

Viterbi 算法实现，寻找最可能的字符序列：

| 功能 | 说明 |
|------|------|
| `calcViterbi()` | 计算 Viterbi DP 矩阵，对数概率平滑（minProb = -50） |
| `getBestPhraseFromViterbi()` | 回溯矩阵提取 Top-N 最佳短语 |
| 特殊处理 | BOS/EOS/特殊字 ID 处理 |

---

## 4. 符号与 Emoji

### 4.1 SymbolGroup（8 个符号分组）

| 分组 | 说明 |
|------|------|
| `han` | 中文标点（含配对括号/引号） |
| `latin` | 英文标点 |
| `math` | 数学符号 |
| `arrow` | 箭头符号 |
| `index` | 带圈数字/字母 |
| `geometry` | 几何图形符号 |
| `misc` | 杂项符号 |
| `other` | 其他符号 |

### 4.2 Symbol

- `value` — 符号值
- `Pair` 子类 — 左右配对符号（括号、引号等）

### 4.3 Emojis

- `Map<String, List<InputWord>> groups` — 按类别分组的 Emoji
- `GROUP_GENERAL = "常用"` — 常用分组

---

## 5. 数据库升级

### 5.1 升级管线

```
v0 → v2 → v3 → v4
```

### 5.2 各版本升级内容

| 升级类 | 版本 | 主要操作 |
|--------|------|---------|
| `From_v0` | 初始安装 | 创建单字索引、拉丁表、Emoji 列/视图、短语表、转移概率表、收藏表；合并应用短语数据 |
| `From_v2_to_v3` | v2 → v3 | 执行 v0 初始化、迁移 v2 用户数据（Emoji 权重、拉丁数据）、从 v2 短语使用模式生成 HMM、清理旧 DB 文件 |
| `From_v3_to_v4` | v3 → v4 | 仅添加收藏表 |

### 5.3 安全迁移模式

`Upgrader.doWithTransferDB()` 使用应用字典副本作为传输数据库，安全迁移后原子交换，确保升级失败不影响用户数据。

---

## 6. UserInputData

用户输入统计数据容器：

| 字段 | 说明 |
|------|------|
| `phrases` | `List<List<PinyinWord>>` — 用户拼音短语 |
| `emojis` | `List<InputWord>` — 用户 Emoji 使用记录 |
| `latins` | `List<String>` — 用户拉丁词使用记录 |
