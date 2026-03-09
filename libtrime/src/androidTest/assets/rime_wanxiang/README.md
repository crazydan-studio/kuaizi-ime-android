本目录为 [Rime 万象拼音](https://github.com/amzxyz/rime_wanxiang)的测试资源目录，
在运行单元测试前需自行准备以下文件：

- `rime-wanxiang-base.zip`：标准版输入方案。从发布页面
  https://github.com/amzxyz/rime_wanxiang/releases 直接下载最新版本即可。
- `wanxiang-lts-zh-hans.gram`：语法模型（非必需）。从发布页面
  https://github.com/amzxyz/rime_wanxiang/releases 直接下载最新版本即可。

注意，由于单元测试每次运行结束后均会卸载，从而导致万象的已构建文件被清空，
这将使得每次运行单元测试均会耗时过长（需要重新构建万象的字典）。
因此，为便于后续测试提速，首次运行单元测试时，需要在 `Rime.exitRime()`
调用位置加上断点以暂停测试，并在其执行后通过 `Device Explorer`
将测试应用数据目录中的 `rime-user` 下的文件下载到当前 `installed` 目录中，
后续运行单元测试时将会自动将该 `installed` 内的已构建文件复制到测试应用的数据目录中，
从而避免反复构建万象字典数据。
