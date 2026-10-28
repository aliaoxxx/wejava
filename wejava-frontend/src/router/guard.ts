import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import router from '@/router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { useTagsViewStore } from '@/store/modules/tagsView'

const WHITE_LIST = ['/login', '/404', '/403']

router.beforeEach(async (to, _from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (!userStore.isLoggedIn()) {
    if (WHITE_LIST.includes(to.path)) return next()
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  if (to.path === '/login') {
    return next('/')
  }

  if (!permissionStore.generated) {
    try {
      await userStore.fetchInfo()
      const routes = await permissionStore.generateRoutes()
      routes.forEach((route: RouteRecordRaw) => router.addRoute(route))
      // 动态路由加完后重新导航一次，否则当前地址匹配不到
      return next({ ...to, replace: true })
    } catch (error) {
      // 这里静默踢回登录页最难排查，务必留下现场
      console.error('[router] 初始化用户或注册动态路由失败', error)
      await userStore.reset()
      return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
  }

  if (to.matched.length === 0) {
    return next('/404')
  }

  useTagsViewStore().addView(to)
  next()
})

router.afterEach(() => {
  NProgress.done()
})
