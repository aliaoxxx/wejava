/** 后端菜单里的图标名（RuoYi 风格）→ Element Plus 图标组件名 */
export const ICON_MAP: Record<string, string> = {
  setting: 'Setting',
  user: 'User',
  peoples: 'UserFilled',
  'tree-table': 'Grid',
  tree: 'Share',
  post: 'Postcard',
  dict: 'Collection',
  edit: 'Edit',
  message: 'Message',
  monitor: 'Monitor',
  online: 'Connection',
  form: 'Document',
  logininfor: 'Key',
  server: 'Cpu',
  home: 'HomeFilled',
  dashboard: 'HomeFilled'
}

/** 把菜单图标名解析成 Element Plus 组件名，解析不到时用默认图标 */
export function resolveIcon(icon?: string, fallback = 'Document'): string {
  if (!icon) return fallback
  return ICON_MAP[icon] ?? icon
}
