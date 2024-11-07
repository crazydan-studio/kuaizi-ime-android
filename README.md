筷字输入法 Android 客户端
===================================

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
