import { defineStore } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'
import { resolveIcon } from '@/utils/icon'

export interface TagView {
  path: string
  fullPath: string
  title: string
  icon?: string
  affix?: boolean
}

export const useTagsViewStore = defineStore('tagsView', () => {
  const visited = ref<TagView[]>([
    { path: '/dashboard', fullPath: '/dashboard', title: '首页', icon: 'HomeFilled', affix: true }
  ])

  const cached = computed(() => visited.value.map((item) => item.path))

  function addView(route: RouteLocationNormalized) {
    if (route.meta?.hidden) return
    if (route.path === '/login' || route.path === '/404' || route.path === '/403') return
    const exists = visited.value.find((item) => item.path === route.path)
    if (exists) {
      exists.fullPath = route.fullPath
      return
    }
    visited.value.push({
      path: route.path,
      fullPath: route.fullPath,
      title: (route.meta?.title as string) || route.name?.toString() || route.path,
      icon: resolveIcon(route.meta?.icon as string | undefined)
    })
  }

  function delView(path: string) {
    visited.value = visited.value.filter((item) => item.affix || item.path !== path)
  }

  function delOthers(path: string) {
    visited.value = visited.value.filter((item) => item.affix || item.path === path)
  }

  function delAll() {
    visited.value = visited.value.filter((item) => item.affix)
  }

  return { visited, cached, addView, delView, delOthers, delAll }
})
