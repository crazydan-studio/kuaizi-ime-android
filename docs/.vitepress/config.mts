import { defineConfig } from 'vitepress'
import { configureDiagramsPlugin } from 'vitepress-plugin-diagrams'

const logo = 'https://raw.githubusercontent.com/crazydan-studio/kuaizi-ime/refs/heads/master/logo.svg'

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
    config(md) {
      configureDiagramsPlugin(md, {
        // 必须为 .vitepress 所在根目录下的 public 目录中的子目录，
        // 且该子目录必须在服务启动前已存在，否则，vitepress 将不会加载该目录内的静态文件
        diagramsDir: 'public/diagrams',
        // 必须为相对于 .vitepress 所在根目录下的 public 目录的路径
        publicPath: '/diagrams',
      })
    },
  },

  // ---------- 主题 ----------
  themeConfig: {
    logo,

    nav: [
      { text: '首页', link: '/' },
      { text: 'AI Agent', link: '/ai-agent/' },
      {
        text: 'v4 版本',
        items: [
          { text: '架构设计', link: '/ai-agent/v4/design/architecture/' },
          { text: '引擎模块', link: '/ai-agent/v4/design/engine/' },
          { text: 'UI 模块', link: '/ai-agent/v4/design/ui/' },
          { text: '应用模块', link: '/ai-agent/v4/design/app/' },
          { text: 'Java 迁移对照', link: '/ai-agent/v4/design/migration/' },
          { text: '讨论记录', link: '/ai-agent/v4/discussions/' },
          { text: '开发计划', link: '/ai-agent/v4/plans/' },
          { text: '测试', link: '/ai-agent/v4/tests/' },
        ],
      },
    ],

    sidebar: {
      '/': sidebarRoot(),
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
    plugins: [],
  },

  cleanUrls: true,
})

/* ====== Sidebar helpers ====== */

function sidebarRoot() {
  return [
    {
      text: '首页',
      items: [{ text: '文档中心', link: '/' }],
    },
    sidebarAiAgent(),
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
      text: ':ime-engine 引擎模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/engine/' },
        { text: '010-引擎库设计总览', link: '/ai-agent/v4/design/engine/010-engine-overview' },
        { text: '020-键盘状态机', link: '/ai-agent/v4/design/engine/020-state-machine' },
        { text: '030-输入列表', link: '/ai-agent/v4/design/engine/030-input-list' },
        { text: '040-字典系统', link: '/ai-agent/v4/design/engine/040-dict-system' },
        { text: '050-X-Pad 核心', link: '/ai-agent/v4/design/engine/050-xpad-core' },
        { text: '060-输入动作程序化', link: '/ai-agent/v4/design/engine/060-input-action' },
        { text: '070-剪贴板与收藏', link: '/ai-agent/v4/design/engine/070-clipboard-and-favorites' },
        { text: '080-日志系统', link: '/ai-agent/v4/design/engine/080-logging' },
        { text: '090-输出桥接机制', link: '/ai-agent/v4/design/engine/090-output-bridge' },
      ],
    },
    {
      text: ':ime-ui UI 模块',
      collapsed: false,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/ui/' },
        { text: '010-UI 库设计总览', link: '/ai-agent/v4/design/ui/010-ui-library-overview' },
        { text: '020-三层面板分离', link: '/ai-agent/v4/design/ui/020-panel-separation' },
        { text: '030-Compose 迁移', link: '/ai-agent/v4/design/ui/030-compose-migration' },
        { text: '040-输入动作播放器', link: '/ai-agent/v4/design/ui/040-input-action-player' },
        { text: '050-配置 UI 组件', link: '/ai-agent/v4/design/ui/050-config-ui' },
        { text: '060-KeyboardViewModel', link: '/ai-agent/v4/design/ui/060-keyboard-view-model' },
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
      ],
    },
    {
      text: 'Java 迁移对照',
      collapsed: true,
      items: [
        { text: '索引', link: '/ai-agent/v4/design/migration/' },
        { text: '010-引擎模块迁移', link: '/ai-agent/v4/design/migration/010-engine-mapping' },
        { text: '020-UI 模块迁移', link: '/ai-agent/v4/design/migration/020-ui-mapping' },
        { text: '030-应用模块迁移', link: '/ai-agent/v4/design/migration/030-app-mapping' },
      ],
    },
    {
      text: 'v4 讨论记录',
      collapsed: true,
      items: [
        { text: '索引', link: '/ai-agent/v4/discussions/' },
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
