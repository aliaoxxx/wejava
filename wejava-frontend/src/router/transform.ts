import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'
import type { BackendRoute } from '@/types/global'

/** 所有页面组件，key 形如 ../views/system/user/index.vue */
const viewModules = import.meta.glob('../views/**/*.vue')

export type AppRoute = RouteRecordRaw

function resolveComponent(component: string | null) {
  if (!component) return () => import('@/views/placeholder/index.vue')
  if (component === 'Layout') return Layout
  const key = `../views/${component}.vue`
  // 页面还没实现时落到占位页，保证路由链路可跑通
  return viewModules[key] || (() => import('@/views/placeholder/index.vue'))
}

export function transformRoutes(backendRoutes: BackendRoute[]): AppRoute[] {
  return backendRoutes.map((item) => {
    const route = {
      path: item.path,
      name: item.name,
      component: resolveComponent(item.component),
      meta: {
        title: item.meta?.title ?? '',
        icon: item.meta?.icon ?? '',
        hidden: item.meta?.hidden ?? false,
        cache: item.meta?.cache ?? false
      },
      ...(item.redirect ? { redirect: item.redirect } : {}),
      ...(item.children?.length ? { children: transformRoutes(item.children) } : {})
    }
    return route as AppRoute
  })
}
