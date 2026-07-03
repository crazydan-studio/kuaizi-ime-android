# ImeEffect 副作用信号

## 1. 设计边界

Engine 的 `ImeEffect` 密封类仅承载领域事件型的 `PopupTip`——引擎在业务逻辑处理过程中（如意图处理、副作用执行）根据内部状态判断是否需要通知 UI 层。UI 层无法自行推导这些事件，因为它们依赖引擎内部的业务逻辑（如收藏写入结果、剪贴板检测结果、字典查询错误）。

交互反馈（音效、触觉、按键弹出提示）归 UI 层在 `gestureToIntent()` 中直接处理，详见 `:ui` 模块的[交互反馈设计](../ui/070-interaction-feedback.md)。

```kotlin
sealed class ImeEffect {
    sealed class PopupTip : ImeEffect() {
        data class Message(
            val message: String,
            val timeoutMs: Long = 3000L,
        ) : PopupTip()

        data class Action(
            val message: String,
            val actionLabel: String,
            val action: ImeIntent,
            val persistent: Boolean = false,
            val timeoutMs: Long = 5000L,
        ) : PopupTip()
    }
}
```

## 2. 发射时机

| 场景 | 信号类型 | 说明 |
|------|---------|------|
| 收藏保存成功 | `PopupTip.Message("已收藏")` | `processSideEffects` 中处理 `SaveFavorite` 后 |
| 收藏删除成功 | `PopupTip.Message("已删除收藏")` | `processSideEffects` 中处理删除后 |
| 可粘贴内容检测 | `PopupTip.Action(..., actionLabel="粘贴")` | `start()` 中剪贴板检测后 |
| 配置导入完成 | `PopupTip.Message("配置已导入")` | `UserDataService.importReplace()` 完成后 |
| 导入失败 | `PopupTip.Message("导入失败", timeoutMs=5000)` | 导入异常时 |
| 字典查询无结果 | `PopupTip.Message("无匹配候选")` | `SetCandidates(candidates=[])` 时 |
