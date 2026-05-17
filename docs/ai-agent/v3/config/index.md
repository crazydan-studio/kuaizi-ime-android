# 配置系统

筷字输入法 v3 的配置系统包含运行时配置、按键表配置和应用偏好设置三个层次。

---

## 1. 运行时配置（Config）

### 1.1 Config 接口

| 实现 | 说明 |
|------|------|
| `Immutable` | 只读包装器，不可修改 |
| `Mutable` | 覆盖层配置，本地覆盖 + 源配置委托；`set(key, value)` 设置覆盖值，`null` 时移除覆盖，回退到源配置 |

### 1.2 ConfigKey（17 + 7 个配置键）

#### 键盘与输入

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `ime_subtype` | `hans` | 输入法子类型（简体/繁体） |
| `orientation` | `portrait` | 屏幕方向 |
| `single_line_input` | — | 单行输入模式 |
| `prev_keyboard_type` | — | 上一次键盘类型 |
| `hand_mode` | `right` | 左/右手模式 |
| `enable_x_input_pad` | — | 启用 X-Pad |
| `enable_latin_use_pinyin_keys_in_x_input_pad` | — | X-Pad 中拉丁键使用拼音键布局 |
| `enable_candidate_variant_first` | — | 候选词优先显示繁体/简体变体 |

#### 禁用开关

| 配置键 | 说明 |
|--------|------|
| `disable_settings_btn` | 禁用设置按钮 |
| `disable_switch_ime_btn` | 禁用切换输入法按钮 |
| `disable_close_keyboard_btn` | 禁用关闭键盘按钮 |
| `disable_user_input_data` | 禁用用户输入数据收集 |
| `disable_key_clicked_audio` | 禁用按键音效 |
| `disable_key_animation` | 禁用按键动画 |
| `disable_input_candidates_paging_audio` | 禁用候选翻页音效 |
| `disable_input_key_popup_tips` | 禁用按键弹出提示 |
| `disable_gesture_slipping_trail` | 禁用滑行轨迹 |
| `disable_input_clip_popup_tips` | 禁用剪贴板弹出提示 |

#### 高级设置

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `adapt_desktop_swipe_up_gesture` | — | 适配桌面上滑手势 |
| `scaled_touch_slop` | `8` | 缩放触摸 slop |
| `input_clip_popup_tips_timeout` | `15s` | 剪贴板提示超时 |
| `used_input_clip_code` | — | 已使用的剪贴板条目标识 |
| `theme` | `follow_system` | 主题模式 |

---

## 2. KeyTableConfig

按键表配置，根据当前运行时状态动态生成：

| 字段 | 说明 |
|------|------|
| `hasInputs` | 输入列表是否有内容 |
| `charInputSelected` | 是否选中了字符输入 |
| `keyboardHandMode` | 左/右手模式 |
| `useSingleLineInputMode` | 单行输入模式 |
| `xInputPadEnabled` | X-Pad 是否启用 |
| `latinUsePinyinKeysInXInputPadEnabled` | X-Pad 中拉丁键使用拼音键布局 |
| `hasRevokableInputsCommit` | 是否有可撤销的提交 |

---

## 3. 应用偏好设置

### 3.1 偏好设置页面

| 页面 | 说明 |
|------|------|
| `Preferences` | 主偏好设置页面（`app_preferences.xml`） |
| `PreferencesTheme` | 主题偏好设置页面（`app_preferences_theme.xml`） |

### 3.2 偏好存储

- 使用 Android `SharedPreferences` 进行持久化
- `PreferencesUtils` 工具类封装读写操作

---

## 4. IME 配置（IMEConfig）

`IMEConfig` 类管理输入法服务的全局配置，在 `IMEService` 中创建和维护。

---

## 5. 配置变更流程

```
用户操作 → Config.Mutable.set(key, value)
  → InputMsg(Config_Update_Done) → UI 视图更新
  → 键盘重建（KeyTableConfig 变化时）
  → 主题切换时 BaseThemedView 自动重建
```

### 5.1 练习中的配置覆盖

`ExerciseGuide` 通过 `Config.Mutable` 覆盖层实现每个练习的独立配置，不修改持久化配置：
- 练习特定的 `xInputPadEnabled`、`handMode` 等设置
- 练习结束时自动移除覆盖，恢复原始配置
