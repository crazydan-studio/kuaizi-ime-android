import DefaultTheme from 'vitepress/theme'
import type { EnhanceAppContext } from 'vitepress'

import './styles.css'

export default {
  extends: DefaultTheme,

  enhanceApp({ app }: EnhanceAppContext) {
    // 可在此注册全局组件
  },

  setup() {
    // VitePress 内置代码复制按钮，无需额外配置
    // 行号通过 config.mts → markdown.lineNumbers 开启
  },
}
