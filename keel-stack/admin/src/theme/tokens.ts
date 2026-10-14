/**
 * 品牌色板。
 *
 * 控制台所有颜色都从这里取，页面里不写死色值；换配色只改这一处。
 */
export const brand = {
  orange: '#FF6B35',      // 主色
  orangeDeep: '#F4531E',  // 主色加深（hover / 渐变）
  mint: '#33BFB5',        // 辅助色
  ink: '#171725',         // 正文
  paper: '#F7F8FC',       // 浅底
  gold: '#FFBE3F',        // 强调
  success: '#27AE85',
  danger: '#E65353',
  info: '#4E88D8'
} as const

/**
 * 示例模块 item 的状态 / 优先级配色。
 *
 * 只是给 item 页面用的样例；换成自己的业务模块时整块替换（或直接删掉）。
 * 状态文案以后端下发的 statusText 为准，这里只用于筛选下拉等本地场景。
 */
export const itemStatusText: Record<string, string> = {
  TODO: '待处理',
  DOING: '进行中',
  DONE: '已完成'
}

export const itemStatusColor: Record<string, string> = {
  TODO: '#B88700',
  DOING: '#4E88D8',
  DONE: '#27AE85'
}

export const priorityText: Record<string, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高'
}

export const priorityColor: Record<string, string> = {
  LOW: '#8A8A99',
  NORMAL: '#4E88D8',
  HIGH: '#E65353'
}

/**
 * Ant Design Vue 主题 token（v4 Design Token 体系）。
 * 主色即品牌色，圆角、控件高度等也在这里统一，页面里不再单独设置。
 */
export const antdTheme = {
  token: {
    colorPrimary: brand.orange,
    colorSuccess: brand.success,
    colorWarning: brand.gold,
    colorError: brand.danger,
    colorInfo: brand.info,
    colorTextBase: brand.ink,
    colorBgLayout: '#F4F5FA',
    colorBorderSecondary: '#EEEEF3',
    borderRadius: 12,
    fontSize: 14,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Segoe UI', Roboto, sans-serif",
    wireframe: false,
    controlHeight: 38
  },
  components: {
    Layout: {
      headerHeight: 68,
      headerBg: 'transparent',
      headerPadding: '0 24px 0 16px',
      siderBg: '#FFFFFF',
      bodyBg: '#F4F5FA'
    },
    Menu: {
      itemBorderRadius: 10,
      itemHeight: 44,
      itemMarginInline: 10,
      itemSelectedBg: 'rgba(255,107,53,0.10)',
      itemSelectedColor: brand.orange,
      itemHoverBg: 'rgba(255,107,53,0.05)',
      iconSize: 17,
      collapsedIconSize: 18
    },
    Card: {
      borderRadiusLG: 16,
      paddingLG: 20
    },
    Button: {
      borderRadius: 10,
      controlHeight: 38,
      primaryShadow: '0 4px 12px rgba(255,107,53,0.24)'
    },
    Table: {
      borderRadiusLG: 14,
      headerBg: '#FAFAFC',
      headerColor: '#6B6B7B',
      rowHoverBg: 'rgba(255,107,53,0.035)',
      cellPaddingBlock: 14
    },
    Modal: { borderRadiusLG: 18 },
    Drawer: { paddingLG: 24 },
    Tag: { borderRadiusSM: 8 },
    Input: { borderRadius: 10 },
    Select: { borderRadius: 10 },
    Tabs: { itemSelectedColor: brand.orange, inkBarColor: brand.orange },
    Statistic: { titleFontSize: 13 },
    Segmented: { borderRadius: 10, itemSelectedBg: '#FFFFFF' }
  }
}
