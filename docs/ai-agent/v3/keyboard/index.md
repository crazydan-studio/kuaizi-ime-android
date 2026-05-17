# 键盘类型

筷字输入法 v3 实现了 8 种键盘类型，每种键盘有独立的按键表（KeyTable）定义布局。所有键盘继承自 `BaseKeyboard`，通过状态机管理输入流程。

---

## 1. 拼音键盘（PinyinKeyboard）

主键盘（`isMaster() = true`），支持三种输入模式。

### 1.1 滑行输入（Slip）

| 特性 | 说明 |
|------|------|
| **触发** | 手指在按键上滑行，经过多个按键形成拼音 |
| **按键分层** | `CharKey.Level`：level_0（初始层）→ level_1（一级后续）→ level_2（二级后续）→ level_final（完整拼音） |
| **实时候选** | `determine_NotConfirmed_InputWord()` 滑行过程中实时确定候选 |
| **有效字符判定** | `PinyinCharsTree.getNextChars()` 判定滑行路径上的有效拼音组合 |
| **状态** | `InputChars_Slip_Doing`，数据 `InputCharsSlipStateData` |

### 1.2 翻转输入（Flip）

| 特性 | 说明 |
|------|------|
| **触发** | 在首字母按键上快速滑行（翻转手势） |
| **功能** | 展示以该字母开头的所有拼音，供用户选择 |
| **状态** | `InputChars_Flip_Doing`，数据 `InputCharsFlipStateData` |

### 1.3 X-Pad 输入

| 特性 | 说明 |
|------|------|
| **触发** | 在 X-Pad 区域的 zone_2 滑行 |
| **按键排列** | 按拼音 Level 分层，6 个方向 × 左右子键 |
| **状态** | `InputChars_XPad_Input_Doing` |

### 1.4 短语预测

- `predict_NotConfirmed_Phrase_InputWords()` 基于 HMM + Viterbi 算法预测未确认拼音的短语候选
- `InputCompletions` 类型 `Phrase_Word` 提供短语补全建议

### 1.5 拉丁词补全

- `getTopBestMatchedLatins()` 通过 `UserInputDataDict` 获取拉丁词补全

### 1.6 按键表（PinyinKeyTable / PinyinKeyTableV2）

| 方法 | 说明 |
|------|------|
| `createGrid()` | 主键盘布局（QWERTY 排列） |
| `createNextCharKeys()` | Slip 模式下一级字符键 |
| `createFullCharKeys()` | Flip 模式全部字符键 |
| `createXPadNextCharKeys()` | X-Pad 模式字符键 |

---

## 2. 拼音候选键盘（PinyinCandidateKeyboard）

拼音输入后的候选词选择界面。

### 2.1 候选展示

| 特性 | 说明 |
|------|------|
| **最佳候选** | 顶部展示权重最高的候选词 |
| **Emoji 建议** | 与候选词关联的 Emoji 建议 |
| **分页** | 手指翻转手势翻页，`PagingStateData` 管理分页状态 |
| **自动继续** | 确认候选后自动选择下一个未确认拼音输入 |

### 2.2 候选筛选

| 功能 | 控制键 | 说明 |
|------|--------|------|
| **拼音切换** | `Toggle_Pinyin_Spell` | s ↔ sh、c ↔ ch、z ↔ zh、n ↔ l、in ↔ ing 等修正 |
| **单选拼音筛选** | `Filter_PinyinCandidate_by_Spell` | 单选拼音过滤 |
| **高级筛选** | `Filter_PinyinCandidate_advance` | 多选拼音 + 偏旁部首组合筛选 |
| **确认筛选** | `Confirm_PinyinCandidate_Filter` | 应用筛选结果 |

### 2.3 按键表（PinyinCandidateKeyTable）

- 候选词网格，按 Level 着色区分
- 拼音筛选键行、偏旁筛选键
- 最佳候选 + Emoji 布局、分页支持

---

## 3. 拉丁键盘（LatinKeyboard）

直接输入拉丁字母的主键盘。

| 特性 | 说明 |
|------|------|
| **布局** | QWERTY 键盘布局（`LatinKeyTable`） |
| **直接输入** | 按键字符直接输入到编辑器 |
| **词补全** | 通过 `UserInputDataDict.findTopBestMatchedLatins()` 提供拉丁词补全 |
| **大小写** | `CharKey.replacements` 支持大写/小写循环切换 |

---

## 4. 数字键盘（NumberKeyboard）

纯数字输入键盘。

| 特性 | 说明 |
|------|------|
| **布局** | 数字网格 + 基本符号（+、-、#、*）（`NumberKeyTable`） |
| **直接输入** | 按键数字直接输入到编辑器 |
| **X-Pad 退出** | 从 X-Pad 切换时显示退出按钮 |

---

## 5. 数学键盘（MathKeyboard）

数学表达式输入键盘。

| 特性 | 说明 |
|------|------|
| **布局** | 数字 + 运算符 + 括号 + 小数点 + 等号（`MathKeyTable`） |
| **运算符** | `+`、`-`、`×`、`÷`、`%`、`‰`、`‱` |
| **括号配对** | `prepare_for_PairKey_Inputting()` 自动创建左右配对括号 |
| **自动计算** | 按 `=` 键触发 `MathExprInput.getText()` 计算结果 |
| **嵌套 InputList** | 操作 `MathExprInput` 的内部 InputList |

---

## 6. 符号键盘（SymbolKeyboard）

标点符号选择键盘。

| 特性 | 说明 |
|------|------|
| **符号分组** | 8 个分组：`han`（中文标点）、`latin`（英文标点）、`math`、`arrow`、`index`、`geometry`、`misc`、`other` |
| **分组切换** | `Toggle_Symbol_Group` 在分组间切换 |
| **配对符号** | `Symbol.Pair` 创建左右配对括号/引号，光标定位在两者之间 |
| **连续输入** | 长按启用连续符号输入 |
| **直接/替换输入** | 空输入列表 → 直接提交；非空 → 替换当前待确认输入 |
| **分页** | 手指翻转手势翻页 |
| **按键表** | `SymbolEmojiKeyTable.createSymbolGrid()` |

---

## 7. Emoji 键盘（EmojiKeyboard）

Emoji 选择键盘。

| 特性 | 说明 |
|------|------|
| **分组** | 按类别分组，含"常用"分组（`GROUP_GENERAL`） |
| **常用 Emoji** | 从 `UserInputDataDict.getAllEmojis()` 获取用户高频使用 |
| **分组切换** | `Toggle_Emoji_Group` 在分组间切换 |
| **连续输入** | 长按启用连续 Emoji 输入 |
| **分页** | 手指翻转手势翻页 |
| **按键表** | `SymbolEmojiKeyTable.createEmojiGrid()` |

---

## 8. 编辑器键盘（EditorKeyboard）

文本编辑操作键盘。

| 特性 | 说明 |
|------|------|
| **编辑操作** | 剪切、复制、粘贴、全选、撤销、重做、收藏（通过 `EditorAction` 枚举） |
| **光标定位** | 手指在光标定位器上移动启动光标移动模式 |
| **返回前键盘** | `switch_Keyboard_to_Previous()` 切换回切换前的键盘类型 |
| **按键表** | `EditorKeyTable.createGrid(CtrlKey.Type)` |

---

## 9. 提交选项键盘（InputListCommitOptionKeyboard）

输入列表提交模式选择。

| 模式 | 说明 |
|------|------|
| `only_pinyin` | 仅提交拼音 |
| `with_pinyin` | 拼音跟随汉字提交 |
| `simple_to_trad` | 简体转繁体提交 |
| `trad_to_simple` | 繁体转简体提交 |

按键表：`InputListCommitOptionKeyTable`

---

## 10. 按键表（KeyTable）通用设计

### 10.1 颜色系统

预定义颜色方案区分不同按键类型：

| 类别 | 说明 |
|------|------|
| 元音键 | 拼音韵母按键颜色 |
| 辅音键 | 拼音声母按键颜色 |
| 数字键 | 数字按键颜色 |
| 符号键 | 符号按键颜色 |
| Level 0-5 | 拼音后续层级颜色渐变 |

### 10.2 控制键样式

每种 `CtrlKey.Type` 有预定义的图标 + 颜色映射。

### 10.3 网格工具

| 方法 | 说明 |
|------|------|
| `fillGridKeyByCoord()` | 按坐标填充按键 |
| `fillGridLevelKeysByCoord()` | 按 Level 填充层级按键 |
| `createXPadGrid()` | 创建 X-Pad 三区域网格 |

### 10.4 KeyTableConfig

| 字段 | 说明 |
|------|------|
| `hasInputs` | 是否有输入内容 |
| `charInputSelected` | 是否选中字符输入 |
| `keyboardHandMode` | 左/右手模式 |
| `useSingleLineInputMode` | 是否使用单行输入模式 |
| `xInputPadEnabled` | 是否启用 X-Pad |
| `latinUsePinyinKeysInXInputPadEnabled` | X-Pad 中拉丁键是否使用拼音键布局 |
| `hasRevokableInputsCommit` | 是否有可撤销的提交 |
