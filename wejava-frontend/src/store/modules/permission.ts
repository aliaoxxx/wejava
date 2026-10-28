import { defineStore } from 'pinia'
import { getRouters } from '@/api/auth'
import { transformRoutes, type AppRoute } from '@/router/transform'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<AppRoute[]>([])
  const generated = ref(false)

  async function generateRoutes() {
    const { data } = await getRouters()
    routes.value = transformRoutes(data)
    generated.value = true
    return routes.value
  }

  function reset() {
    routes.value = []
    generated.value = false
  }

  return { routes, generated, generateRoutes, reset }
})
