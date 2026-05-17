# UI 视图

筷字输入法 v3 基于 Android View + RecyclerView 实现，采用六边形蜂窝网格布局键盘，自定义 LayoutManager 和手势检测器。所有视图通过 `InputMsg` / `UserKeyMsg` / `UserInputMsg` 消息驱动更新。

---

## 1. 主面板（MainboardView）

顶层 IME 面板容器，组合 `KeyboardView` + `InputboardView`。

| 功能 | 说明 |
|------|------|
| **消息分发** | `onMsg(InputMsg)` 分发消息到子视图 |
| **底部间距适配** | `updateBottomSpacing()` 适配导航栏重叠 |
| **加载状态** | `toggleShowKeyboardWarning()` 显示键盘加载提示 |

---

## 2. 键盘视图（KeyboardView）

渲染所有键盘按键布局，处理用户手势和轨迹绘制。

| 功能 | 说明 |
|------|------|
| **按键布局更新** | 键盘切换/主题/配置变更时更新按键网格 |
| **手势转发** | `onMsg(UserKeyMsg)` 转发按键手势 |
| **手势轨迹** | `ViewGestureTrailer` 绘制滑行轨迹 |
| **按键动画** | `KeyboardViewKeyAnimator` 按键交互动画 |
| **六边形布局** | 继承 `KeyboardViewBase`（RecyclerView），`KeyboardViewLayoutManager` 管理六边形网格 |

### 2.1 KeyboardViewBase

基于 RecyclerView 的键盘基类：

| 功能 | 说明 |
|------|------|
| `update(Key[][], boolean)` | 重新绑定按键数据 |
| `updateGridBottomReservedHeight()` | 更新底部预留高度 |
| `getXPadKeyViewHolder()` | 获取 X-Pad 视图持有者 |
| `findVisibleKeyViewHolderUnderLoose()` | 宽松命中检测（六边形区域） |

### 2.2 KeyboardViewLayoutManager

基于 mixite 库的六边形网格布局管理器：

| 功能 | 说明 |
|------|------|
| **三段布局** | 左段/中段/右段分区排列按键 |
| **X-Pad 模式** | FLAT_TOP 方向六边形排列 |
| **左手模式** | 按键区域镜像翻转 |
| **宽松命中检测** | 六边形区域内宽松查找子视图 |

### 2.3 KeyboardViewGestureListener

将原始手势事件翻译为 `UserKeyMsg` 消息：

| 手势 | 映射消息 |
|------|---------|
| PressStart | `Press_Key_Start` |
| PressEnd | `Press_Key_Stop` |
| SingleTap | `SingleTap_Key` |
| DoubleTap | `DoubleTap_Key` |
| LongPress | `LongPress_Key_Start` + `Tick` |
| Moving | `FingerMoving_Start/Moving/Stop` |
| Flipping | `FingerFlipping` |

---

## 3. 按键视图（ui/view/key/）

### 3.1 按键 ViewHolder 体系

| 类 | 按键类型 | 布局 | 特殊功能 |
|------|---------|------|---------|
| `KeyViewHolder<V>` | 基类 | — | 六边形背景 `HexagonDrawable`、阴影、边框、禁用态、动态字号 |
| `CharKeyViewHolder` | CharKey | `key_char_view` | 按标签长度自动调整字号（1-6 字符） |
| `CtrlKeyViewHolder` | CtrlKey | `key_ctrl_view` | 图标或文字标签 |
| `CtrlKeyPinyinToggleViewHolder` | 拼音切换键 | `key_ctrl_toggle_pinyin_spell_view` | 显示源→目标转换（如"ü,v"） |
| `InputWordKeyViewHolder` | 候选词键 | `key_char_input_word_view` | 词文本、拼音、繁体标记 |
| `SymbolKeyViewHolder` | 符号键 | `key_char_view` | 继承 CharKeyViewHolder |
| `MathOpKeyViewHolder` | 运算键 | `key_char_view` | 继承 CharKeyViewHolder |
| `XPadKeyViewHolder` | X-Pad 键 | `key_xpad_view` | 委托 XPadView 渲染 |
| `NullKeyViewHolder` | 空占位键 | `key_null_view` | 透明占位 |

### 3.2 KeyboardViewAdapter

8 种视图类型：`CHAR_KEY`、`CTRL_KEY`、`NULL_KEY`、`INPUT_WORD_KEY`、`TOGGLE_PINYIN_SPELL_KEY`、`SYMBOL_KEY`、`MATH_OP_KEY`、`XPAD_KEY`

- 支持差异刷新和全量刷新两种更新策略
- `DefaultItemAnimator` 子类最小化自定义动画

---

## 4. X-Pad 视图（ui/view/xpad/）

### 4.1 XPadView

核心 X 型滑行输入面板（灵感来自 8VIM），三区域环形布局：

| 区域 | 形状 | 内容 |
|------|------|------|
| Zone 0（中心） | 正六边形 | 光标定位器（CtrlKey） |
| Zone 1（内环） | 6 个方向扇形块 | 键盘切换器（Latin/Pinyin/Number/Math） |
| Zone 2（外环） | 6 个方向扇形块 × 左右子键 | 字符输入区域（按 Level 分层） |

### 4.2 XPadView 功能

| 功能 | 说明 |
|------|------|
| **手势路由** | 滑行方向自动定位到目标 Zone/Block |
| **区域动画** | 输入开始时区域收缩动画 |
| **左手模式** | Zone 布局镜像翻转 |
| **手势轨迹** | 滑行路径轨迹绘制 |
| **模拟模式** | 练习引导中的自动演示 |
| **状态机** | `Init → InputChars_Input_Waiting → InputChars_Input_Doing → Editor_Edit_Doing` |

### 4.3 绘制体系

| 类 | 说明 |
|------|------|
| `XZone` | 绘制容器，管理路径/图标/文字画笔，支持按下/弹起视觉状态、透明度/缩放变换 |
| `XPainter` | 基础画笔：Paint、对齐、填充/描边、阴影、圆角 |
| `XAlignPainter` | 位置画笔：添加坐标、对齐、旋转、尺寸 |
| `XTextPainter` | 文字画笔：沿路径绘制文字，支持对齐/旋转/加粗 |
| `XDrawablePainter` | 图标画笔：缩放/对齐/旋转/透明度绘制 Drawable |
| `XPathPainter` | 路径画笔：六边形/分隔线/坐标轴，填充/描边/阴影/圆角 |

---

## 5. 输入视图（ui/view/input/）

### 5.1 InputListView

水平滚动的输入列表视图，展示字符、间隙和数学表达式：

| 功能 | 说明 |
|------|------|
| **数据更新** | 选择、配置变更时更新输入数据 |
| **自动滚动** | `scrollToSelectedInput()` 滚动到待确认输入 |
| **命中检测** | `findInputPositionUnder()` 含 Gap 边距的精确命中 |
| **消息上抛** | `fire_UserInputMsg()` 冒泡传递输入消息 |

### 5.2 InputListViewReadonly

只读变体，用于数学表达式内的嵌套输入列表，滚动操作委托给父视图。

### 5.3 InputViewHolder 体系

| 类 | 输入类型 | 布局 | 特殊功能 |
|------|---------|------|---------|
| `InputViewHolder` | 基类 | — | 间隙空格、选中颜色 |
| `CharInputViewHolder` | CharInput | `input_char_view` | 汉字 + 拼音展示 |
| `GapInputViewHolder` | GapInput | `input_gap_view` | 闪烁光标动画 |
| `SpaceInputViewHolder` | 空格 | `input_space_view` | 选中高亮 |
| `MathExprInputViewHolder` | MathExprInput | `input_math_expr_view` | 嵌套 InputListViewReadonly + 边框标记 |

### 5.4 InputListViewAdapter

4 种视图类型：`CHAR_INPUT`、`GAP_INPUT`、`SPACE_INPUT`、`MATH_EXPR_INPUT`，使用 DiffUtil 优化刷新。

---

## 6. 输入面板（InputboardView）

输入面板：InputListView + 工具栏/输入栏按钮。

### 6.1 状态机

```
Init → Input_Freeze_Doing → Input_Doing → Input_Cleaned_Cancel_Waiting → Toolbar_Show_Doing
```

### 6.2 工具栏按钮

| 按钮 | 功能 |
|------|------|
| 设置 | 打开输入法设置 |
| 切换输入法 | 切换到其他输入法 |
| 关闭键盘 | 收起键盘 |
| 复制/粘贴/剪切/全选 | 编辑器操作 |
| 撤销/重做 | 编辑器撤销重做 |
| 清空输入列表 | 清空当前输入 |
| 撤销清空 | 恢复被清空的输入 |
| 展开/收起工具栏 | 工具栏折叠切换 |

---

## 7. 候选弹出视图（CandidatesView）

浮动弹出窗口，三种弹出类型：

| 类型 | 说明 |
|------|------|
| `quick_list` | 输入补全和剪贴板建议 |
| `tooltip` | 按键预览气泡 |
| `snackbar` | 编辑器操作反馈 + 收藏保存提示 |

| 功能 | 说明 |
|------|------|
| **动画** | 进入/退出动画 |
| **定位** | 弹出位置在按键上方 |
| **按键提示** | `showInputKeyTip()` 按键按下气泡 |
| **操作反馈** | `showTooltip()` 操作结果反馈 |
| **收藏提示** | `on_InputClip_CanBe_Favorite_Msg()` 保存收藏提示条 |

### 7.1 InputQuickListView

水平滚动的补全/剪贴板列表：

| 功能 | 说明 |
|------|------|
| **补全点击** | 触发 `SingleTap_InputCompletion` |
| **剪贴板点击** | 触发 `SingleTap_InputClip` |
| **自动重置** | 数据变更时重置滚动位置 |

### 7.2 Quick Input ViewHolder

| 类 | 说明 |
|------|------|
| `InputCompletionViewHolder` | 动态加载子字符输入视图展示补全预览 |
| `InputClipViewHolder` | 剪贴板粘贴建议，类型标签 |
| `InputCompletionCharInputViewHolder` | 补全中的单个字符：词+拼音或拉丁文本 |
| `InputQuickPlaceholderViewHolder` | 透明占位符（滚动内边距） |

---

## 8. 收藏面板（FavoriteboardView）

收藏管理面板，列表 + 删除/清空操作。

| 功能 | 说明 |
|------|------|
| **列表展示** | 垂直列表展示收藏项 |
| **多选删除** | 复选框多选 + 确认对话框删除 |
| **清空所有** | 确认对话框后清空 |
| **标题计数** | 标题显示收藏数量 |
| **空状态警告** | 无收藏时显示提示 |

### 8.1 InputFavoriteViewHolder

展示收藏项：复选框、内容（文本/HTML）、文本类型标签、创建日期、使用次数、粘贴按钮。

---

## 9. 练习引导（ui/guide/）

### 9.1 ExerciseGuide

主 Activity，Drawer 布局，包含导航、沙盒键盘视图、练习列表。

**8 个预置练习**：

| 练习 | 模式 | 说明 |
|------|------|------|
| 自由练习 | `free` | 无交互限制的自由输入 |
| 基本介绍 | `introduce` | 只读介绍说明 |
| 拼音滑行输入 | `normal` | 交互式滑行输入练习 |
| 拼音候选筛选 | `normal` | 候选词筛选练习 |
| 字符替换输入 | `normal` | 按键替换循环练习 |
| 数学输入 | `normal` | 数学表达式输入练习 |
| 编辑器编辑 | `normal` | 文本编辑操作练习 |
| 拼音提交处理 | `normal` | 提交模式练习 |
| X-Pad 输入 | `normal` | X-Pad 环形输入练习 |

### 9.2 Exercise 模型

| 属性 | 说明 |
|------|------|
| `mode` | `free`（自由）/ `normal`（交互）/ `introduce`（只读） |
| `steps` | 练习步骤列表 |
| `newStep()` | 自动将 Key 参数转换为 `<img>` 标签 |
| `gotoNextStep()` | 步骤推进 |

### 9.3 ExerciseStep

| 属性 | 说明 |
|------|------|
| `content` | HTML 内容 |
| `name` | 步骤名称 |
| `action` | `InputMsgListener` 交互动作 |
| `AutoAction` | 自动推进步骤 |
| `Last` 子类 | 最终步骤，提供重新开始/继续回调 |

### 9.4 KeyboardSandboxView

沙盒键盘，渲染与真实键盘一致的按键图片，用于嵌入练习说明。图片缓存提升性能。

---

## 10. 主题系统

### 10.1 BaseThemedView

主题感知视图基类，主题切换时自动重建：

| 功能 | 说明 |
|------|------|
| `doLayout()` | 使用主题 Context 填充布局 |
| `handleInputMsg()` | 主题切换消息触发重建 |
| `updateLayoutDirection()` | 左/右手模式布局方向 |

### 10.2 支持的主题

| 主题 | 说明 |
|------|------|
| `light` | 亮色主题 |
| `night` | 暗色主题 |
| `follow_system` | 跟随系统设置 |

---

## 11. 关于页面（ui/about/）

| 页面 | 说明 |
|------|------|
| `AboutApp` | 应用信息 |
| `AboutChangelog` | 版本更新日志 |
| `AboutDonate` | 捐赠（支付宝/微信二维码） |
| `AboutThanks` | 致谢 |
| `AboutTeam` | 团队信息 |
| `AlphaUserAgreement` | Alpha 测试协议 |
| `SoftwareServiceAgreement` | 软件服务协议 |
| `Copyright` | 版权声明 |

---

## 12. 通用组件（common/widget/）

| 组件 | 说明 |
|------|------|
| `ViewGestureDetector` | 统一手势检测：PressStart/End、LongPress（带 Tick）、SingleTap、DoubleTap、Moving、Flipping |
| `ViewGestureTrailer` | 手势轨迹绘制（渐隐路径） |
| `HexagonDrawable` | 六边形 Drawable，支持圆角、阴影、边框 |
| `AudioPlayer` | 按键音效播放 |
| `Toast` | 自定义 Toast |
| `DialogAlert` / `DialogConfirm` | 对话框构建器 |
| `ShadowDrawable` | 带 Shadow 效果的 Drawable |
| `HtmlTextView` | HTML 渲染 TextView |
| `EditorAction` | 编辑器操作枚举（copy/paste/cut/select_all/undo/redo/favorite） |
| `EditorSelection` | 编辑器选择工具 |

### 12.1 自定义 RecyclerView 体系

| 类 | 说明 |
|------|------|
| `RecyclerView<A,VH>` | 泛型 RecyclerView，含类型化适配器、条目手势检测 |
| `RecyclerViewAdapter` | 基础适配器，DiffUtil + 手动更新策略 |
| `RecyclerViewHolder` | 基础 ViewHolder，`whenViewReady()`、颜色/尺寸工具 |
| `RecyclerViewLayoutManager` | 基础 LayoutManager，宽松/近似子视图查找 |
| `RecyclerViewLinearLayoutManager` | 水平/垂直线性布局 |
| `RecyclerViewGestureDetector` | 条目级手势检测 |
| `RecyclerViewGestureTrailer` | 条目级手势轨迹装饰 |
| `RecyclerPageView` | 分页水平滚动，页激活监听 |
| `RecyclerPageIndicatorView` | 分页圆点指示器 |
