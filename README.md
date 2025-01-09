筷字输入法 Android 客户端
===================================

**注意**：本仓库仅作为筷字输入法 Android 客户端的源码仓库，
若有缺陷反馈和改进意见，请移步至
[crazydan-studio/kuaizi-ime](https://github.com/crazydan-studio/kuaizi-ime/issues)
创建 Issue。

## 许可协议

[LGPL 3.0](./LICENSE)

许可协议要点：
- 本产品的源码不能够用于闭源软件，也不能在闭源软件中引入本产品源码，并且对本产品源码的修改部分需开源
  - 若仅仅是将本产品编译构建后的产物作为依赖引入或独立/集成部署，则没有开源要求，可以闭源使用
- 对于新增代码或衍生代码没有开源要求，并可采用其他许可协议发布

## 项目构建

> 若需要自己生成字词典数据库，则需要从项目
> [kuaizi-ime](https://github.com/crazydan-studio/kuaizi-ime?tab=readme-ov-file#%E9%A1%B9%E7%9B%AE%E5%85%8B%E9%9A%86)
> 的子模块 `android` 克隆本项目：`git submodule update --init android`。

### 字/词典

客户端默认自带的[字典库](./app/src/main/res/raw/pinyin_word_dict.db)和[词典库](./app/src/main/res/raw/pinyin_phrase_dict.db)由工具
[kuaizi-ime/tools/pinyin-dict](https://github.com/crazydan-studio/kuaizi-ime/blob/master/tools/pinyin-dict/README.md)
生成，请参考该工具的使用说明构建客户端专用的字词典数据库。

> 注：字典数据来自于[汉典网](https://www.zdic.net)，词典数据来自于[古文之家](https://www.cngwzj.com)。

### 发布包

可直接在本项目的根目录下执行 `bash ./pack-release.sh` 以构建发布包。

> 最终的发布包可在项目根目录下的 `app/build/outputs/apk/release/` 中找到。

而在构建打包前，需要调整构建脚本中变量 `JAVA_HOME` 的值，默认为
`/usr/lib/jvm/java-17-openjdk`。

然后，在本项目的根目录下准备 APK 证书的配置文件 `./keystore/release.properties`：

```bash
cat > keystore/release.properties <<EOF
storeFile = /path/to/android-release-key.jks
storePassword = store-pass
keyPassword = key-pass
keyAlias = android-key-alias
EOF
```

> 在[《参考资料》](#参考资料)章节可阅读签名证书的生成相关的资料。

## 架构设计

<img src="./docs/img/layout-introduce.png" height="350px"/>

### 核心模型

![核心模型](./docs/img/class-diagram.png)

<details><summary>PlantUML 代码</summary>

```plantuml
@startuml
class "InputMethodService" as sys_ime_svc
class "IMEService" as ime_svc #pink
class "IMEConfig" as ime_conf

class "PinyinDict" as dict

class "IMEditor" as ime_editor #pink
class "Keyboard" as keyboard
class "Inputboard" as inputboard
class "KeyboardContext" as keyboard_ctx {
  +inputList: InputList
  +listener: InputMsgListener
}
class "InputboardContext" as inputboard_ctx {
  +inputList: InputList
  +listener: InputMsgListener
}

class "InputList" as input_list #pink
class "Input" as input
class "Key" as key

class "IMEditorView" as ime_editor_view #pink
class "KeyboardView" as keyboard_view
class "InputboardView" as inputboard_view
class "InputListView" as input_list_view

sys_ime_svc <|-down- ime_svc: extends

ime_svc *-right- ime_conf: contains >
ime_svc *-down- ime_editor: contains >
ime_svc *-down- ime_editor_view: contains >

ime_editor_view *-down- inputboard_view: contains >
inputboard_view *-down- input_list_view: contains >
ime_editor_view *-down- keyboard_view: contains >

ime_editor *-up- dict: contains >
ime_editor *-down- inputboard: contains >
ime_editor *-down- keyboard: contains >
ime_editor *-down- input_list: contains >

inputboard *-down- inputboard_ctx: use >
keyboard *-down- keyboard_ctx: use >

inputboard_ctx *-up- input_list: refs >
keyboard_ctx *-up- input_list: refs >

input_list "1" *-down- "1..n" input: contains >
keyboard "1" *-down- "1..n" key: layouts >

@enduml
```

</details>

设计要点：

- 模型与视图分离，二者不做直接关联，模型的变更通过消息（`InputMsg`）机制触发对相关视图的更新
- 输入法主视图 `IMEditorView` 由 `InputboardView`（输入面板）和
  `KeyboardView`（键盘）上下两部分组成，前者实时显示当前正在输入和已输入的内容，
  后者则显示拼音、拉丁文（英文）等类型键盘的输入按键
- 与以上视图相对应的模型则分别为 `IMEditor`、`Inputboard` 和 `Keyboard`，
  其负责根据用户的交互消息（`UserKeyMsg` 和 `UserInputMsg`）生成并管理输入数据 `Input`
- 输入数据 `Input` 将最终通过输入列表 `InputList` 进行维护，并通过视图 `InputListView`
  显示拼音、拉丁文、表情等类型的 `Input`
- 在模型 `Inputboard` 和 `Keyboard` 的内部不持有 `InputList` 及其内部消息的监听器，
  在处理来自视图的用户交互消息时，均通过各自的上下文对象 `InputboardContext`
  和 `KeyboardContext` 调用 `InputList` 的接口构造 `Input`，再通过上下文中指定的
  `listener` 将内部消息向外发送出去
- 拼音输入（`PinyinKeyboard`）、拉丁文输入（`LatinKeyboard`）、
  表情输入（`EmojiKeyboard`）、算术输入（`MathKeyboard`）等键盘均为
  `Keyboard` 的具体实现，并由 `IMEditor` 负责各类键盘的切换

### 消息流转

![消息流转](./docs/img/message-transfer.png)

<details><summary>PlantUML 代码</summary>

```plantuml
@startuml
component [IMEService] as ime_svc #pink

component [IMEditor] as ime_editor #pink
component [Inputboard] as inputboard
component [Keyboard] as keyboard

component [IMEditorView] as ime_editor_view #pink
component [KeyboardView] as keyboard_view
component [InputboardView] as inputboard_view
component [InputListView] as input_list_view

input_list_view ..> inputboard_view: send\n<<UserInputMsg>>
inputboard_view ..> ime_editor_view: transfer\n<<UserInputMsg>>
keyboard_view ..> ime_editor_view: send\n<<UserKeyMsg>>
ime_editor_view ..> ime_svc: transfer\n<<UserKeyMsg>>\nor <<UserInputMsg>>

ime_svc ..> ime_editor: dispatch\n<<UserKeyMsg>>\nor <<UserInputMsg>>
ime_editor ..> keyboard: dispatch\n<<UserKeyMsg>>
ime_editor ..> inputboard: dispatch\n<<UserInputMsg>>


keyboard ..> ime_editor: send\n<<InputMsg>>
inputboard ..> ime_editor: send\n<<InputMsg>>
ime_editor ..> ime_svc: transfer\n<<InputMsg>>

ime_svc ..> ime_editor_view: dispatch\n<<InputMsg>>
ime_editor_view ..> keyboard_view: dispatch\n<<InputMsg>>
ime_editor_view ..> inputboard_view: dispatch\n<<InputMsg>>
inputboard_view ..> input_list_view: dispatch\n<<InputMsg>>

@enduml
```

</details>

设计要点：

- 消息始终保持单向流动，模型层或视图层发送的消息均由上一层进行转发，
  再由最顶层（`IMEService`）将消息向下分别派发至视图层或模型层。
  模型层与视图层之间不直接传递消息，从而消除模型与视图间的耦合性
- 模型层中的 `Keyboard` 和 `Inputboard` 均触发 `InputMsg`（输入消息），
  再由相应的视图根据消息携带的数据 `InputMsgData` 做视图更新
  - `Keyboard` 主要触发与 `Input` 构造相关的消息，如，正在输入、输入已结束、输入已删除等，而
    `Inputboard` 则主要触发与 `InputList` 处理相关的消息，如，输入已选中、输入列表已清空等
- 视图层中的 `KeyboardView` 将触发 `UserKeyMsg`（用户操作按键的消息），
  其最终由模型层中的 `Keyboard` 处理，而 `InputListView` 则触发
  `UserInputMsg`（用户操作输入的消息），并由 `Inputboard` 进行处理
  - `InputboardView` 也会触发 `UserInputMsg` 消息，
    如，点击输入列表清空按钮、选中输入补全等

## 参考资料

- [APK 签名打包](https://developer.android.com/studio/publish/app-signing?hl=zh-cn)
- [深入探索 Android 包体积优化](https://juejin.cn/post/6844904103131234311)
- [VasDolly](https://github.com/Tencent/VasDolly): 是一种快速多渠道打包工具，同时支持基于 V1 签名和 V2、V3 签名进行多渠道打包
- [APK 构建配置](https://developer.android.com/build/gradle-tips): 使用 VasDolly 做多渠道打包，仅需由其注入渠道信息
  - [动态赋值](https://developer.android.com/build/gradle-tips#simplify-app-development)
  - [签名](https://developer.android.com/build/gradle-tips#remove-private-signing-information-from-your-project)
  - [为不同构建类型添加不同资源配置](https://stackoverflow.com/questions/24785270/how-to-change-app-name-per-gradle-build-type#answer-24786371)
- [Android 自定义 View 篇之（三）Canvas 绘制文字](https://www.cnblogs.com/andy-songwei/p/10968358.html):
  涉及详细的文字尺寸、基线等计算方式的说明
- [HMM 和 Viterbi 算法](https://lesley0416.github.io/2019/03/01/HMM_IM/)
- [维特比算法](https://zh.wikipedia.org/wiki/%E7%BB%B4%E7%89%B9%E6%AF%94%E7%AE%97%E6%B3%95)
- [wmhst7/THU_AI_Course_Pinyin](https://github.com/wmhst7/THU_AI_Course_Pinyin)
- [基于 Bigram+HMM 的拼音汉字转换](https://github.com/iseesaw/Pinyin2ChineseChars)

## 友情赞助

**注**：赞助时请添加备注信息 `筷字输入法`。

详细的赞助清单请查看[《友情赞助清单》](https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/index.md)。

| 支付宝 | 微信支付 |
| -- | -- |
| <img src="https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/alipay.jpg?raw=true" width="200px"/> | <img src="https://github.com/crazydan-studio/kuaizi-ime/blob/master/docs/donate/wechat.png?raw=true" width="200px"/> |
