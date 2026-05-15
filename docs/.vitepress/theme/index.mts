import DefaultTheme from 'vitepress/theme'
import type { EnhanceAppContext } from 'vitepress'

import ImageViewerP from '@miletorix/vitepress-image-viewer'
import '@miletorix/vitepress-image-viewer/style.css'

import './styles.css'

export default {
  extends: DefaultTheme,

  enhanceApp({ app }: EnhanceAppContext) {
    ImageViewerP(app)
  },

  setup() {
    // VitePress 内置代码复制按钮，无需额外配置
    // 行号通过 config.mts → markdown.lineNumbers 开启
  },
}
