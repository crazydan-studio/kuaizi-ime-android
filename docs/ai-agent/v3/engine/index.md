# 核心引擎

核心引擎负责输入法的数据模型、状态管理、消息传递和输入逻辑。这些类位于 `core/` 包下，构成了输入法的领域核心。

---

## 1. 输入列表（InputList）

`InputList` 是输入法的核心数据结构，管理用户输入的字符序列和游标位置。

### 1.1 数据模型

| 类 | 说明 |
|------|------|
| `Input` | 输入项基类，提供 `isEmpty()`、`confirm()`、`copy()`、`getText(Option)` 等方法 |
| `CharInput` | 字符输入，包含 `Key` 列表和可选的 `InputWord` 候选词绑定 |
| `GapInput` | 间隙占位符，表示游标位置和插入点，始终为空 |
| `MathExprInput` | 数学表达式输入，内嵌独立 `InputList`，内置计算器（调度场算法 → RPN 求值） |
| `InputWord` | 候选词基类，含 `id`、`value`、`weight` |
| `PinyinWord` | 拼音候选词，含 `Spell`（拼音发音）、`Radical`（偏旁部首）、`variant`（繁简变体）、`traditional` 标记 |
| `EmojiWord` | Emoji 候选词 |
| `InputClip` | 剪贴板条目，含 `type`（文本类型）、`code`（唯一标识）、`text`、`html` |
| `InputFavorite` | 用户收藏，含 `id`、`type`、`shortcut`、`text`、`html`、`createdAt`、`usedAt`、`usedCount` |

### 1.2 InputList 核心功能

| 功能 | 说明 |
|------|------|
| **游标系统** | `selected`（当前输入引用）+ `pending`（待确认输入），通过 `GapInput` 间隙实现游标定位 |
| **待确认输入管理** | `confirmPending()`、`dropPending()`、`newCharPending()`、`newMathExprPending()` |
| **退格删除** | `deleteBackward()` — 处理 Gap 删除、拉丁逐字符删除、配对符号删除 |
| **输入选择** | `select()`、`selectLast()`、`selectNextFirstMatched()` — 在输入列表中导航 |
| **输入补全** | `InputCompletions` 支持 `Latin`（拉丁词补全）和 `Phrase_Word`（短语词补全）两种类型 |
| **间隙空格** | `needGapSpace()` — 在数学表达式、拉丁/非拉丁、数学运算符之间自动添加空格 |
| **文本输出** | `getText(Option)` — 带间隙空格插入的文本输出 |
| **拼音短语提取** | `getPinyinPhraseWords()`、`getPinyinPhraseWordsFrom()`、`getPinyinPhraseInputWhichContains()` |
| **配对符号** | `hasOnlyOnePairInputs()`、`clearPairOnSelected()` — 括号/引号成对输入与删除 |
| **撤销提交** | `committed` 和 `cleaned` 阶段支持撤销操作 |

### 1.3 CharInput 拼音切换

| 方法 | 说明 |
|------|------|
| `toggle_Pinyin_SCZ_Starting()` | s ↔ sh、c ↔ ch、z ↔ zh 平翘舌切换 |
| `toggle_Pinyin_NG_Ending()` | in ↔ ing、en ↔ eng、an ↔ ang 前后鼻音切换 |
| `toggle_Pinyin_NL_Starting()` | n ↔ l 鼻音切换 |

### 1.4 MathExprInput 计算器

- 支持 `+`、`-`、`×`、`÷`、`%`、`‰`、`‱`、括号、小数点
- `=` 等号触发计算，使用调度场算法（Shunting-yard）转换为逆波兰表达式（RPN）求值
- 结果格式化为最多 4 位小数

### 1.5 InputTextType 文本类型识别

`Favoriteboard` 中通过正则表达式自动识别剪贴板文本类型：

| 类型 | 正则匹配规则 |
|------|-------------|
| `text` | 默认纯文本 |
| `html` | HTML 内容 |
| `url` | URL 链接 |
| `captcha` | 6-8 位纯数字验证码 |
| `phone` | 手机号码 |
| `email` | 电子邮箱 |
| `id_card` | 身份证号 |
| `credit_card` | 银行卡号 |
| `address` | 地址信息 |

---

## 2. 键盘状态机

### 2.1 状态定义（State.Type）

| 状态 | 说明 |
|------|------|
| `InputChars_Input_Wait_Doing` | 等待输入（初始状态） |
| `InputChars_Slip_Doing` | 滑行输入进行中 |
| `InputChars_Flip_Doing` | 翻转输入进行中（首字母快速滑行） |
| `InputChars_XPad_Input_Doing` | X-Pad 环形输入模式 |
| `InputList_Commit_Option_Choose_Doing` | 选择提交选项 |
| `InputCandidate_Choose_Doing` | 候选词/符号/Emoji 选择 |
| `InputCandidate_Advance_Filter_Doing` | 高级候选筛选 |
| `Editor_Edit_Doing` | 编辑器光标/选择模式 |

### 2.2 状态数据

| 类 | 对应状态 | 关键数据 |
|------|---------|---------|
| `InputCharsSlipStateData` | Slip | Level 0/1/2 按键、level2NextChars |
| `InputCharsFlipStateData` | Flip | startChar（翻转起始字符） |
| `InputListCommitOptionChooseStateData` | Commit Option | Input.Option、hasSpell、hasVariant |
| `PinyinCandidateChooseStateData` | Candidate | candidates、pageSize、Filter、paging |
| `PinyinCandidateFilterStateData` | Filter | PinyinWord.Filter |
| `PinyinCandidateAdvanceFilterStateData` | Advance Filter | candidates、pageSize、Filter |
| `EmojiChooseStateData` | Emoji | Emojis、groups、paging |
| `SymbolChooseStateData` | Symbol | SymbolGroup、onlyPair、paging |
| `EditorEditStateData` | Editor | target（cursor/selection）、from point、motion |
| `PagingStateData<T>` | 分页基类 | pageSize、pageStart、totalSize |

### 2.3 键盘类继承体系

```
Keyboard (接口)
 └─ BaseKeyboard (抽象)
     ├─ EditorEditKeyboard (抽象) — 光标定位与选择编辑
     │   ├─ PinyinKeyboard — 拼音输入（滑行/翻转/X-Pad）
     │   ├─ DirectInputKeyboard (抽象) — 直接输入到编辑器
     │   │   ├─ LatinKeyboard — 拉丁字母输入
     │   │   └─ NumberKeyboard — 纯数字输入
     │   ├─ InputCandidateKeyboard (抽象) — 候选选择与分页
     │   │   ├─ PinyinCandidateKeyboard — 汉字候选选择
     │   │   ├─ SymbolKeyboard — 标点符号选择
     │   │   └─ EmojiKeyboard — Emoji 选择
     │   ├─ MathKeyboard — 数学表达式输入
     │   └─ EditorKeyboard — 文本编辑操作
     └─ InputListCommitOptionKeyboard — 提交模式选项
```

### 2.4 BaseKeyboard 通用功能

| 功能 | 方法 |
|------|------|
| 状态转换 | `change_State_To()`、`change_State_to_Init()`、`change_State_to_Previous()` |
| 输入列表操作 | `commit_InputList()`、`revoke_Committed_InputList()`、`backspace_InputList_or_Editor()`、`delete_InputList_Selected()`、`drop_InputList_Pending()`、`confirm_InputList_Pending()` |
| 单字符输入 | `start_Single_CharKey_Inputting()` 含替换循环 |
| 输入补全 | `do_InputList_Pending_Completion_Creating()` |
| 编辑器操作 | `do_Editor_Editing()`、`do_Editor_Cursor_Moving()`、`do_Editor_Range_Selecting()` |
| 音频反馈 | `play_SingleTick_InputAudio()`、`play_DoubleTick_InputAudio()`、`play_PageFlip_InputAudio()` 等 |
| 弹出提示 | `show_InputChars_Input_Popup()`、`hide_InputChars_Input_Popup()` |

---

## 3. 消息体系

### 3.1 消息流架构

```
用户手势 → UserKeyMsg / UserInputMsg → Keyboard/Inputboard → InputMsg → UI 视图层
```

### 3.2 InputMsgType（37 种）

| 类别 | 消息类型 |
|------|---------|
| **音频** | `InputAudio_Play_Doing` |
| **字符输入** | `InputChars_Input_Doing`、`InputChars_Input_Done`、`InputChars_Input_Popup_Show_Doing`、`InputChars_Input_Popup_Hide_Doing` |
| **候选选择** | `InputCandidate_Choose_Doing`、`InputCandidate_Choose_Done` |
| **输入选择** | `Input_Choose_Doing`、`Input_Choose_Done`、`Input_Pending_Drop_Done`、`Input_Selected_Delete_Done` |
| **输入补全** | `InputCompletion_Create_Done`、`InputCompletion_Apply_Done` |
| **输入列表** | `InputList_Clean_Done`、`InputList_Cleaned_Cancel_Done`、`InputList_Commit_Doing`、`InputList_Committed_Revoke_Doing`、`InputList_PairSymbol_Commit_Doing` |
| **编辑器** | `Editor_Cursor_Move_Doing`、`Editor_Range_Select_Doing`、`Editor_Edit_Doing` |
| **键盘** | `Keyboard_State_Change_Done`、`Keyboard_HandMode_Switch_Doing/Done`、`Keyboard_Theme_Switch_Done`、`Keyboard_Switch_Doing/Done`、`Keyboard_Start_Doing/Done`、`Keyboard_Close_Doing/Done`、`Keyboard_Exit_Done`、`Keyboard_XPad_Simulation_Terminated` |
| **配置** | `Config_Update_Done`、`IME_Switch_Doing` |
| **剪贴板** | `InputClip_Create_Done`、`InputClip_Apply_Done`、`InputClip_Discard_Done`、`InputClip_CanBe_Favorite`、`InputClip_Text_Commit_Doing` |
| **收藏** | `InputFavorite_Query_Doing/Done`、`InputFavorite_Paste_Done`、`InputFavorite_Delete_Done`、`InputFavorite_Clear_All_Done`、`InputFavorite_Save_Done`、`InputFavorite_Text_Commit_Doing` |

### 3.3 UserKeyMsgType（12 种）

| 类型 | 说明 |
|------|------|
| `Press_Key_Start/Stop` | 按键按下/松开 |
| `LongPress_Key_Start/Tick/Stop` | 长按开始/连续触发/结束 |
| `SingleTap_Key` | 单击按键 |
| `DoubleTap_Key` | 双击按键 |
| `FingerMoving_Start/Moving/Stop` | 滑行开始/移动中/结束 |
| `FingerFlipping` | 翻转手势（快速滑行） |

### 3.4 UserInputMsgType（14 种）

| 类型 | 说明 |
|------|------|
| `SingleTap_Input` | 点击输入列表中的项 |
| `SingleTap_InputCompletion` | 点击补全建议 |
| `SingleTap_InputClip` | 点击剪贴板条目 |
| `SingleTap_Btn_Clean_InputList` | 清空输入列表 |
| `SingleTap_Btn_Cancel_Clean_InputList` | 撤销清空 |
| `SingleTap_Btn_Editor_Action` | 编辑器操作按钮 |
| `SingleTap_Btn_Close_Keyboard` | 关闭键盘 |
| `SingleTap_Btn_Open/Close_Favoriteboard` | 打开/关闭收藏面板 |
| `SingleTap_Btn_Select_InputFavorite` | 选择收藏项 |
| `SingleTap_Btn_Paste_InputFavorite` | 粘贴收藏项 |
| `SingleTap_Btn_Delete_Selected_InputFavorite` | 删除选中收藏 |
| `SingleTap_Btn_Clear_All_InputFavorite` | 清空所有收藏 |
| `SingleTap_Btn_Save_As_Favorite` | 保存为收藏 |

---

## 4. 按键模型

### 4.1 按键类型体系

| 类 | 说明 | 核心字段/方法 |
|------|------|-------------|
| `Key` | 按键基类（只读，Builder 模式，可缓存） | `value`、`label`、`icon`、`color`、`disabled`；内部类 `Key.Color`、`Key.Icon`、`Key.Style` |
| `CharKey` | 可输入字符键 | `Type` 枚举：Alphabet/Number/Symbol/Emoji；`Level` 枚举：level_0/1/2/final；替换循环 `replacements`、`nextReplacement()` |
| `CtrlKey` | 控制功能键 | `Type` 枚举（28 种）；`PinyinToggleMode`；`InputWordCommitMode`；泛型 `Option<T>` |
| `SymbolKey` | 标点符号键 | 包装 `dict/Symbol` |
| `MathOpKey` | 数学运算键 | `Type` 枚举：Equal/Plus/Minus/Multiply/Divide/Percent/Permill/Permyriad/Brackets/Dot |
| `InputWordKey` | 候选词键 | 含 `InputWord word` 字段 |
| `XPadKey` | X-Pad 复合键 | 三区域：zone_0_key（光标定位）、zone_1_keys（键盘切换）、zone_2_keys（字符输入） |

### 4.2 CtrlKey.Type（28 种控制键）

| 类别 | 控制键 |
|------|--------|
| **基本** | NoOp、Space、Backspace、Enter、Exit |
| **输入** | Commit_InputList、Commit_InputList_Option、DropInput、ConfirmInput、RevokeInput |
| **拼音** | Toggle_Pinyin_Spell、Filter_PinyinCandidate_advance、Filter_PinyinCandidate_by_Spell、Filter_PinyinCandidate_by_Radical、Confirm_PinyinCandidate_Filter |
| **编辑器** | Editor_Cursor_Locator、Editor_Range_Selector、Edit_Editor |
| **切换** | Switch_HandMode、Switch_Keyboard、Toggle_Emoji_Group、Toggle_Symbol_Group |
| **X-Pad** | XPad_Active_Block、XPad_Char_Key、XPad_Simulation_Terminated |

### 4.3 InputWordCommitMode 提交模式

| 模式 | 说明 |
|------|------|
| `only_pinyin` | 仅提交拼音 |
| `with_pinyin` | 拼音跟随提交 |
| `simple_to_trad` | 简体转繁体提交 |
| `trad_to_simple` | 繁体转简体提交 |

---

## 5. X-Pad 核心模型

### 5.1 XPadKey 三区域结构

| 区域 | 内容 | 说明 |
|------|------|------|
| zone_0 | 光标定位器（1 个 CtrlKey） | 中心六边形，双击切换到 Editor 键盘，长按开始选择编辑 |
| zone_1 | 键盘切换器（6 个 CtrlKey） | 内环：Latin/Pinyin/Number/Math 等键盘切换 |
| zone_2 | 字符输入区（6 × 2 个 CharKey） | 外环：按拼音 Level 分层排列字符 |

### 5.2 X-Pad 手势交互

- 在外环区域滑行触发字符输入，按 Level（level_0 → level_1 → level_2）逐级推进
- 在内环区域点击切换键盘类型
- 在中心区域双击进入编辑器，长按启动选择模式
- 支持左手模式（区域镜像翻转）
- 支持模拟模式（练习引导中自动演示）

---

## 6. Inputboard 与 Favoriteboard

### 6.1 Inputboard（输入面板）

管理 `InputList` 与 `UserInputMsg` 消息的交互：

| 功能 | 说明 |
|------|------|
| **提交** | `commit_InputList()` — 提交输入列表文本到编辑器 |
| **撤销提交** | `revoke_Committed_InputList()` — 撤销最近一次提交 |
| **清空/撤销清空** | `InputList_Clean_Done` / `InputList_Cleaned_Cancel_Done` |
| **输入选择** | 点击输入列表中的 Gap 或 CharInput |
| **补全应用** | 点击输入补全建议 |

### 6.2 Favoriteboard（收藏面板）

剪贴板监控与收藏管理：

| 功能 | 说明 |
|------|------|
| **剪贴板监控** | 注册 `ClipboardManager.OnPrimaryClipChangedListener`，自动检测新剪贴内容 |
| **智能文本识别** | 正则匹配验证码、URL、手机号、邮箱、身份证、银行卡、地址 |
| **收藏 CRUD** | 保存、粘贴、删除、清空 |
| **使用统计** | `usedAt`、`usedCount` 追踪使用频率和时间 |
| **收藏转换** | `InputFavorite.from(InputClip)` — 剪贴板条目转收藏 |
