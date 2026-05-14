筷字输入法 Android 客户端
========================

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
  alt="Get it on F-Droid"
  height="80">](https://f-droid.org/packages/org.crazydan.studio.app.ime.kuaizi)

**注意**：本仓库仅作为筷字输入法 Android 客户端的源码仓库，
若有缺陷反馈和改进意见，请移步至
[crazydan-studio/kuaizi-ime](https://github.com/crazydan-studio/kuaizi-ime/issues)
创建 Issue。

## 版本信息

| 属性 | 值 |
|------|-----|
| 版本号 | 4.0.0 |
| versionCode | 400 |
| 开发语言 | Kotlin 2.3.20 |
| UI 框架 | Jetpack Compose (BOM 2026.04.01) |
| 最低 SDK | 25 |
| 目标 SDK | 35 |

## 许可协议

[LGPL 3.0](./LICENSE)

许可协议要点：
- 本产品的源码不能够用于闭源软件，也不能在闭源软件中引入本产品源码，并且对本产品源码的修改部分需开源
  - 若仅仅是将本产品编译构建后的产物作为依赖引入或独立/集成部署，则没有开源要求，可以闭源使用
- 对于新增代码或衍生代码没有开源要求，并可采用其他许可协议发布

## 项目文档

| 文档 | 说明 |
|------|------|
| [文档中心](docs/index.md) | 项目文档组织说明和主要内容索引 |
| [AI Agent 指南](docs/ai-agent/index.md) | 文档和工程组织结构说明 |
| [AI 技能库](docs/ai-agent/skills/) | 最佳实践、代码规范、库使用建议 |
| [v4 版本文档](docs/ai-agent/v4/index.md) | Kotlin 重构版本的开发文档 |

## 项目构建

> 若需要自己生成字词典数据库，则需要从项目
> [kuaizi-ime](https://github.com/crazydan-studio/kuaizi-ime)
> 的子模块 `android` 克隆本项目：`git submodule update --init android`。

### 本地开发

本项目使用 Gradle 构建，需要 JDK 17+。在项目根目录下执行以下命令进行构建：

```bash
# Debug 构建
cd code && ./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

### 文档预览

项目文档使用 [Docsify](https://docsify.js.org/) 提供本地预览服务，无需安装额外依赖即可启动：

```bash
# 在项目根目录下执行
bash tools/preview-docs.sh

# 指定端口（默认 3000）
bash tools/preview-docs.sh 8080
```

启动后浏览器访问 `http://localhost:3000` 即可浏览项目文档。

### 字/词典

客户端默认自带的字典库和词典库由工具
[kuaizi-ime/tools/pinyin-dict](https://github.com/crazydan-studio/kuaizi-ime/blob/master/tools/pinyin-dict/README.md)
生成，请参考该工具的使用说明构建客户端专用的字词典数据库。内置字词典以 assets 形式组织，存放在 `code/app/src/main/assets/dict/` 目录中。

> 注：字典数据来自于[汉典网](https://www.zdic.net)，词典数据来自于[古文之家](https://www.cngwzj.com)。

### 发布包

可直接在本项目的根目录下执行 `bash ./tools/pack-release.sh` 以构建发布包。

> 最终的发布包名为 `Kuaizi_IME-4.0.0.apk`，可在 `code/app/build/outputs/apk/release/` 中找到。

而在构建打包前，需要调整构建脚本中变量 `JAVA_HOME` 的值，默认为
`/usr/lib/jvm/java-17-openjdk`。

然后，在项目根目录下的 `keystore` 目录中准备 APK 证书的配置文件 `keystore/release.properties`：

```bash
cat > keystore/release.properties <<EOF
storeFile = /path/to/android-release-key.jks
storePassword = store-pass
keyPassword = key-pass
keyAlias = android-key-alias
EOF
```

> - 若 `keystore/release.properties` 不存在，则将构建无签名的发布包

## 架构设计

v4 版本采用 MVI（Model-View-Intent）架构，基于 Kotlin 协程 + Flow 和 Jetpack Compose 实现。

### 核心设计

- **Model-View 严格分离**：模型层与视图层通过 StateFlow 实现单向数据流，视图层订阅状态变更自动更新
- **Intent 驱动**：所有用户交互统一表达为 `ImeIntent`，由 ViewModel 的 `reduce` 函数处理
- **不可变状态**：所有 UI 状态为不可变 `data class`，通过 `copy()` 创建新实例
- **组合优于继承**：键盘逻辑通过组合模式实现，避免深层继承链
- **Sealed class 类型安全**：按键类型、输入类型、键盘状态等均使用 `sealed class` 表达

### 分层架构

```
Platform Layer  ← :app 模块     — IMEService 桥接
     ↓
ViewModel Layer ← :app 模块     — MVI 状态管理（使用 :ime-ui 的 KeyboardViewModel）
     ↓
UI Layer        ← :ime-ui 库    — Jetpack Compose 缺省 UI
     ↓
Domain Layer    ← :ime-engine 库 — 键盘逻辑、输入列表、状态机
     ↓
Data Layer      ← :ime-engine 库 — 字典、用户数据、配置
```

### 三层库架构

v4 采用三层库架构，支持其他程序以库的形式引入输入法能力：

- **引擎库 `:ime-engine`**：纯 Kotlin，不依赖 Android 框架，提供核心输入引擎能力
- **UI 库 `:ime-ui`**：基于 Compose 的缺省 UI 实现，对第三方应用开放，可整体或部分替换
- **应用模块 `:app`**：系统 IME 服务壳、设置页面、配置持久化，是库的官方消费者

所有配置通过 `ImeConfig`（含引擎配置 `EngineConfig` 和 UI 配置 `UiConfig` 的明确隔离）统一管理，运行时修改始终优先于持久化配置，直到应用重启时从持久化配置初始化。

### 技术选型

| 技术 | 用途 |
|------|------|
| Kotlin 2.3.20 | 开发语言 |
| Jetpack Compose BOM 2026.04.01 | UI 框架 |
| Kotlin Coroutines + Flow | 异步编程与状态观察 |
| Room | 字典数据库 |
| DataStore | 配置存储 |
| Sealed class | 类型安全的消息和状态定义 |

详细的架构设计文档请参阅 [docs/ai-agent/v4/design/](docs/ai-agent/v4/design/)。

## 开发实践

- **显式优于隐式**：所有公开 API 必须有显式声明，状态变更必须有迹可循
- **任其崩溃（Fail Fast）**：遇到不可恢复的错误立即失败，不静默降级。使用 `require()`、`check()` 和 `!!` 在入口处断言
- **不可变优先**：数据默认不可变，状态变更通过创建新实例
- **任何代码都存在潜在的缺陷**，因此，不要编写非必要代码，不做非必要抽象和封装，不要增加代码复杂度
- 需意识到功能和代码存在相互关联和影响，应制定版本规划，再按功能大小逐步开发，在版本功能完成后再进行统一测试和修复

## 参考资料

- [APK 签名打包](https://developer.android.com/studio/publish/app-signing?hl=zh-cn)
- [VasDolly](https://github.com/Tencent/VasDolly): 快速多渠道打包工具
- [Kotlin 2.3.20 新特性](https://kotlinlang.org/docs/whatsnew2320.html)
- [Jetpack Compose 1.8 新特性](https://juejin.cn/post/7616814234513604651)
- [HMM 和 Viterbi 算法](https://lesley0416.github.io/2019/03/01/HMM_IM/)
- [维特比算法](https://zh.wikipedia.org/wiki/%E7%BB%B4%E7%89%B9%E6%AF%94%E7%AE%97%E6%B3%95)
- [基于 Bigram+HMM 的拼音汉字转换](https://github.com/iseesaw/Pinyin2ChineseChars)
- 设备调试相关
  - [Samsung Remote Test Lab](https://developer.samsung.com/remote-test-lab): 免费的三星远程调试服务

## 友情赞助

**注**：赞助时请添加备注信息 `筷字输入法`。

详细的赞助清单请查看[《友情赞助清单》](https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/index.md)。

| 支付宝 | 微信支付 |
| -- | -- |
| <img src="https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/alipay.jpg?raw=true" width="200px"/> | <img src="https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/wechat.png?raw=true" width="200px"/> |
