/**
 * 管理端共用常量——选项列表等
 * 抽离到模块顶层，避免随组件重建重复分配
 */

/** 默认主题选项（外观设置） */
export const THEME_OPTIONS = [
  { label: '亮色模式', value: 'light' },
  { label: '暗色模式', value: 'dark' },
  { label: '跟随系统', value: 'system' }
]
