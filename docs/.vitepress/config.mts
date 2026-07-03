import { writeFileSync } from 'node:fs'
import { defineConfig } from 'vitepress'
import { configureDiagramsPlugin, createBuildTimeDiagramsPlugin } from 'vitepress-plugin-diagrams'

const remoteLogoUrl = 'https://raw.githubusercontent.com/crazydan-studio/kuaizi-ime/refs/heads/master/logo.svg'
let logo = remoteLogoUrl
let fetchLogo = async (distDir: string) => {}

const vitePlugins = []
const diagramsPluginOpts = {
  // 必须为 .vitepress 所在根目录下的 public 目录中的子目录，
  // 且该子目录必须在服务启动前已存在，否则，vitepress 将不会加载该目录内的静态文件
  diagramsDir: 'public/diagrams',
  // 必须为相对于 .vitepress 所在根目录下的 public 目录的路径
  publicPath: '/diagrams',
}
let configMarkdown = (md) => {
  configureDiagramsPlugin(md, diagramsPluginOpts)
}

if (process.env.NODE_ENV == 'production') {
  logo = '/logo.svg'
  fetchLogo = async (distDir: string) => {
    const distLogFile = distDir + logo

    await fetch(remoteLogoUrl)
            .then((resp) => resp.arrayBuffer())
            .then((buf) => Buffer.from(buf))
            .then((buf) => writeFileSync(distLogFile, buf))
  }

  // -------------------
  const { configureMarkdown, vitePlugin } = createBuildTimeDiagramsPlugin({
    diagramsDistDir: 'diagrams', ...diagramsPluginOpts
  })

  configMarkdown = configureMarkdown
  vitePlugins.push(vitePlugin())
}

export default defineConfig({
  lang: 'zh-CN',
  title: '筷字输入法',
  description: '筷字输入法（Kuaizi IME）项目文档',
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: logo }]
  ],

  // ---------- Markdown ----------
  markdown: {
    lineNumbers: true,

    theme: {
      light: 'github-light',
      dark: 'one-dark-pro',
    },

    // PlantUML / Mermaid 等图表（由 vitepress-plugin-diagrams 提供）
    config: (md) => configMarkdown(md),
  },

  // ---------- 主题 ----------
  themeConfig: {
    logo,

    nav: [
      { text: '首页', link: '/' },
      { text: 'AI Agent', link: '/ai-agent/' },
      {
        text: 'v3 版本',
        items: [
          { text: '功能总览', link: '/ai-agent/v3/' },
          { text: '核心引擎', link: '/ai-agent/v3/engine/' },
          { text: '键盘类型', link: '/ai-agent/v3/keyboard/' },
          { text: 'UI 视图', link: '/ai-agent/v3/ui/' },
          { text: '字典系统', link: '/ai-agent/v3/dict/' },
          { text: '配置系统', link: '/ai-agent/v3/config/' },
        ],
      },
      {
        text: 'v4 版本',
        items: [
          { text: '功能总览', link: '/ai-agent/v4/' },
          { text: '架构设计', link: '/ai-agent/v4/design/architecture/' },
          { text: ':engine 引擎模块', link: '/ai-agent/v4/design/engine/' },
          { text: ':ui UI 模块', link: '/ai-agent/v4/design/ui/' },
          { text: ':app 应用模块', link: '/ai-agent/v4/design/app/' },
          { text: ':app-codegen 代码生成', link: '/ai-agent/v4/design/app-codegen/' },
          { text: '讨论记录', link: '/ai-agent/v4/discussions/' },
          { text: '开发计划', link: '/ai-agent/v4/plans/' },
          { text: '测试', link: '/ai-agent/v4/tests/' },
        ],
      },
    ],

    sidebar: {
      '/': sidebarRoot(),
      '/ai-agent/v3/': sidebarV3(),
      '/ai-agent/': sidebarAiAgent(),
    },

    search: {
      provider: 'local',
      options: {
        locales: {
          root: {
            translations: {
              button: { buttonText: '搜索', buttonAriaLabel: '搜索' },
              modal: {
                noResultsText: '无法找到相关结果',
                resetButtonTitle: '清除查询条件',
                footer: { selectText: '选择', navigateText: '切换', closeText: '关闭' },
              },
            },
          },
        },
      },
    },

    outline: { level: [2, 4], label: '本页目录' },

    docFooter: { prev: '上一页', next: '下一页' },

    lastUpdated: { text: '最后更新于' },

    editLink: {
      pattern:
        'https://github.com/crazydan-studio/kuaizi-ime-android/edit/refactor-kotlin/docs/:path',
      text: '在 GitHub 上编辑此页',
    },
  },

  // ---------- 构建 ----------
  vite: {
    plugins: vitePlugins,
  },

  cleanUrls: true,

  async buildEnd(siteConfig) {
    await fetchLogo(siteConfig.outDir)
  }
})

/* ====== Sidebar helpers ====== */

function sidebarRoot() {
  return [
    {
      text: '首页',
      items: [{ text: '文档中心', link: '/' }],
    },
    sidebarV3(),
    sidebarAiAgent(),
  ]
}

function sidebarV3() {
  return [
    {
      text: 'v3 版本（Java）',
      items: [{ text: '功能总览', link: '/ai-agent/v3/' }],
    },
    {
      text: '核心引擎',
      collapsed: false,
      items: [
        { text: '输入列表 / 状态机 / 消息体系 / X-Pad', link: '/ai-agent/v3/engine/' },
      ],
    },
    {
      text: '键盘类型',
      collapsed: false,
      items: [
        { text: '8 种键盘与按键表', link: '/ai-agent/v3/keyboard/' },
      ],
    },
    {
      text: 'UI 视图',
      collapsed: false,
      items: [
        { text: '主面板 / 按键 / 输入 / X-Pad / 练习引导', link: '/ai-agent/v3/ui/' },
      ],
    },
    {
      text: '字典系统',
      collapsed: false,
      items: [
        { text: '拼音字典 / HMM / 数据库升级', link: '/ai-agent/v3/dict/' },
      ],
    },
    {
      text: '配置系统',
      collapsed: false,
      items: [
        { text: '运行时配置 / 按键表配置 / 偏好设置', link: '/ai-agent/v3/config/' },
      ],
    },
  ]
}

function sidebarAiAgent() {
  return [
    {
      text: 'AI Agent',
      items: [{ text: '组织结构说明', link: '/ai-agent/' }],
    },
    {
      text: '技能库',
      items: [
        { text: '索引', link: '/ai-agent/skills/' },
        { text: 'Kotlin 最佳实践', link: '/ai-agent/skills/kotlin-best-practices' },
        { text: 'Jetpack Compose 最佳实践', link: '/ai-agent/skills/compose-best-practices' },
        { text: '代码规范', link: '/ai-agent/skills/code-conventions' },
      ],
    },
    {
      text: 'v4 版本',
      items: [{ text: '索引', link: '/ai-agent/v4/' }],
    },
    {
      text: '架构设计',
      collapsed: false,
      items: [
        { text: '010-架构总览', link: '/ai-agent/v4/design/architecture/010-overview' },
        { text: '020-命名规范', link: '/ai-agent/v4/design/architecture/020-naming-conventions' },
        { text: '030-三层模块划分', link: '/ai-agent/v4/design/architecture/030-module-division' },
      ],
    },
    {
      text: ':engine 引擎模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/engine/' },
        { text: '010-引擎架构总览', link: '/ai-agent/v4/design/engine/010-engine-overview' },
        { text: '020-全局状态模型', link: '/ai-agent/v4/design/engine/020-ime-state' },
        { text: '030-键盘状态机', link: '/ai-agent/v4/design/engine/030-keyboard-state-machine' },
        { text: '040-输入列表', link: '/ai-agent/v4/design/engine/040-input-list' },
        { text: '050-候选与字典', link: '/ai-agent/v4/design/engine/050-candidate-and-dict' },
        { text: '060-意图、编辑器操作与桥接', link: '/ai-agent/v4/design/engine/060-intent-editor-action-bridge' },
        { text: '065-ImeEffect 副作用信号', link: '/ai-agent/v4/design/engine/065-ime-effect' },
        { text: '070-剪贴板与收藏', link: '/ai-agent/v4/design/engine/070-clipboard-and-favorites' },
        { text: '080-输入动作程序化', link: '/ai-agent/v4/design/engine/080-input-action' },
        { text: '090-日志系统', link: '/ai-agent/v4/design/engine/090-logging' },
      ],
    },
    {
      text: ':ui UI 模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/ui/' },
        { text: '010-UI 库架构总览', link: '/ai-agent/v4/design/ui/010-ui-library-overview' },
        { text: '020-面板三层分离与屏幕布局', link: '/ai-agent/v4/design/ui/020-panel-separation' },
        { text: '030-键盘视图模型', link: '/ai-agent/v4/design/ui/030-keyboard-view-model' },
        { text: '040-Compose 组件', link: '/ai-agent/v4/design/ui/040-compose-components' },
        { text: '050-输入动作播放', link: '/ai-agent/v4/design/ui/050-input-action-player' },
        { text: '060-配置界面', link: '/ai-agent/v4/design/ui/060-config-ui' },
        { text: '070-交互反馈设计', link: '/ai-agent/v4/design/ui/070-interaction-feedback' },
      ],
    },
    {
      text: ':app 应用模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/app/' },
        { text: '010-配置与设置', link: '/ai-agent/v4/design/app/010-config' },
        { text: '020-日志系统', link: '/ai-agent/v4/design/app/020-logging' },
        { text: '030-UI 测试方案', link: '/ai-agent/v4/design/app/030-ui-testing' },
        { text: '040-用户数据导入导出', link: '/ai-agent/v4/design/app/040-user-data' },
        { text: '050-平台反馈实现', link: '/ai-agent/v4/design/app/050-platform-feedback' },
      ],
    },
    {
      text: ':app-codegen 代码生成模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/app-codegen/' },
        { text: '010-KSP 代码生成', link: '/ai-agent/v4/design/app-codegen/010-codegen' },
      ],
    },
    {
      text: 'v4 讨论记录',
      collapsed: true,
      items: [
        { text: '索引', link: '/ai-agent/v4/discussions/' },
          { text: '设计总览', link: '/ai-agent/v4/design/' },
          { text: '架构索引', link: '/ai-agent/v4/design/architecture/' },
          { text: '设计决策与文档评审', link: '/ai-agent/v4/discussions/001-design-decisions-and-doc-review' },
        { text: '命名规范细化', link: '/ai-agent/v4/discussions/002-naming-convention-refinement' },
      ],
    },
    {
      text: 'v4 开发计划',
      collapsed: true,
      items: [
        { text: '索引', link: '/ai-agent/v4/plans/' },
        { text: '计划编写与执行指南', link: '/ai-agent/v4/plans/000-plan-authoring-and-execution-guide' },
      ],
    },
    {
      text: 'v4 测试',
      collapsed: true,
      items: [
        { text: '索引', link: '/ai-agent/v4/tests/' },
        { text: '测试编写指南', link: '/ai-agent/v4/tests/000-test-writing-guide' },
      ],
    },
    {
      text: 'v4 其他',
      collapsed: true,
      items: [
        { text: '开发日志', link: '/ai-agent/v4/logs/' },
        { text: '缺陷修复', link: '/ai-agent/v4/bugs/' },
      ],
    },
  ]
}
