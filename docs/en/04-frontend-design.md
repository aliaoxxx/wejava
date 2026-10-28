[中文](../04-前端设计.md) | English

# 04 · Frontend Design

> Vue 3.5 + TypeScript + Vite + Element Plus + Pinia + Vue Router

---

## 1. Tech Stack

| Dependency | Purpose |
|---|---|
| `vue@3.5` | |
| `vue-router@4` | Dynamic routing |
| `pinia@3` | State management |
| `element-plus@2` | UI component library |
| `@element-plus/icons-vue` | Icons |
| `axios` | HTTP |
| `@vueuse/core` | Composition utilities |
| `nprogress` | Route progress bar |
| `echarts` + `vue-echarts` | Monitoring charts |
| `dayjs` | Date/time handling |
| `unplugin-auto-import` / `unplugin-vue-components` | On-demand imports |
| `sass` | Styles |

**Not included**: `oidc-client-ts` (no OAuth2), full import of `element-plus`.

---

## 2. Directory Structure

```
wejava-frontend/
├── .env.development
├── .env.production
├── vite.config.ts
├── tsconfig.json
├── index.html
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/                        # Split by module, one file per module
    │   ├── auth.ts
    │   ├── user.ts
    │   ├── role.ts
    │   ├── menu.ts
    │   ├── dept.ts
    │   ├── dict.ts
    │   └── types.ts                # Common types
    ├── assets/
    │   ├── icons/
    │   └── styles/
    │       ├── index.scss
    │       ├── variables.scss
    │       └── element-override.scss
    ├── components/                 # Global shared components (auto-registered)
    │   ├── SvgIcon/
    │   ├── Pagination/
    │   ├── DictTag/
    │   └── TreeSelect/
    ├── directives/
    │   ├── index.ts
    │   └── hasPerm.ts
    ├── hooks/
    │   ├── useTable.ts             # Shared list-page logic
    │   └── useDict.ts
    ├── layout/
    │   ├── index.vue
    │   └── components/
    │       ├── Sidebar/
    │       ├── Navbar/
    │       ├── TagsView/
    │       └── AppMain/
    ├── router/
    │   ├── index.ts                # Static routes + dynamic registration
    │   ├── guard.ts                # Global guards
    │   └── routes.ts               # constantRoutes
    ├── store/
    │   ├── index.ts
    │   └── modules/
    │       ├── user.ts
    │       ├── permission.ts       # Dynamic routes
    │       ├── app.ts              # Sidebar collapse
    │       └── tagsView.ts         # Multi-tab
    ├── utils/
    │   ├── request.ts              # ★ axios wrapper + refresh queue
    │   ├── auth.ts                 # Token storage
    │   ├── permission.ts           # hasPerm / hasRole
    │   └── validate.ts
    ├── views/
    │   ├── login/index.vue
    │   ├── error/404.vue
    │   ├── error/403.vue
    │   ├── dashboard/index.vue
    │   ├── system/
    │   │   ├── user/index.vue
    │   │   ├── role/index.vue
    │   │   ├── menu/index.vue
    │   │   ├── dept/index.vue
    │   │   ├── post/index.vue
    │   │   ├── dict/index.vue
    │   │   ├── config/index.vue
    │   │   └── notice/index.vue
    │   └── monitor/
    │       ├── online/index.vue
    │       ├── operlog/index.vue
    │       ├── loginlog/index.vue
    │       └── server/index.vue
    └── types/
        └── global.d.ts
```

---

## 3. Environment Variables

```bash
# .env.development
VITE_APP_TITLE = WeJava Admin
VITE_APP_BASE_API = /api
VITE_APP_ENV = development

# .env.production
VITE_APP_TITLE = WeJava Admin
VITE_APP_BASE_API = /api
VITE_APP_ENV = production
```

```ts
// vite.config.ts
export default defineConfig({
  server: {
    port: 3300,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
        // The backend context-path is already /api, so no rewrite is needed
      }
    }
  }
})
```

> `VITE_APP_BASE_API` must be a relative path. Hard-coding `http://localhost:8000` causes 404s in production.

---

## 4. Routing Design

### 4.1 Static Routes (constantRoutes)

```ts
export const constantRoutes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/login/index.vue'), meta: { hidden: true } },
  { path: '/404', component: () => import('@/views/error/404.vue'), meta: { hidden: true } },
  { path: '/403', component: () => import('@/views/error/403.vue'), meta: { hidden: true } },
  { path: '/redirect/:path(.*)', component: () => import('@/layout/components/Redirect.vue'), meta: { hidden: true } },
  { path: '/', component: Layout, redirect: '/dashboard',
    children: [{ path: 'dashboard', component: () => import('@/views/dashboard/index.vue'),
                 meta: { title: 'Home', icon: 'home', affix: true } }] }
]
```

### 4.2 Dynamic Routes (backend response → frontend transformation)

The `component` returned by the backend `/auth/routers` is a string (e.g. `system/user/index`), and the frontend must map it to a component:

```ts
const modules = import.meta.glob('@/views/**/*.vue')

function resolveComponent(component: string) {
  if (!component || component === 'Layout') return Layout
  if (component === 'InnerLink') return InnerLink
  const path = `/src/views/${component}.vue`
  if (!modules[path]) {
    console.error('[router] Component not found:', path)
    return () => import('@/views/error/404.vue')
  }
  return modules[path]
}
```

### 4.3 Route Guards (critical)

```ts
const whiteList = ['/login', '/404', '/403']

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  const permStore = usePermissionStore()

  if (!userStore.token) {
    // Not logged in
    if (whiteList.includes(to.path)) return next()
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  // Logged in, but user info has not been loaded yet (first visit or refresh)
  if (!userStore.loaded) {
    try {
      await userStore.getInfo()                 // /auth/me
      const routes = await permStore.generateRoutes()   // /auth/routers
      routes.forEach(r => router.addRoute(r))
      // ★ replace: true re-runs navigation so the new routes are registered
      return next({ ...to, replace: true })
    } catch (e) {
      await userStore.resetToken()
      return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
  }

  // Logged in and routes ready, but the path being visited does not exist
  if (to.matched.length === 0) {
    return next('/404')
  }

  next()
})

router.afterEach(() => NProgress.done())
```

**Three things you must watch out for**:

1. Never call `next()` after `next({ ...to, replace: true })`, otherwise navigation runs twice
2. The whitelist must include `/login`, otherwise a 401 redirect to the login page loops forever
3. The 404 fallback route must be **added dynamically** (append `{ path: '/:pathMatch(.*)*', redirect: '/404' }` at the end of `addRoute`); it must not live in `constantRoutes`, otherwise dynamic routes can never be matched

---

## 5. Login and Token

### 5.1 Token Storage

| Item | Location | Description |
|---|---|---|
| `accessToken` | Pinia + localStorage | Survives a page refresh |
| `refreshToken` | Pinia + localStorage | Same as above |

> Storing tokens in cookies is not recommended: cross-origin configuration is complicated, and a JWT in the header is inherently immune to CSRF.
> If security requirements are high, however, keep them **in memory** (never in localStorage); the cost is that a page refresh forces a re-login.

### 5.2 Login Page Flow

```ts
async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const { data } = await loginApi({
      username: form.username,
      password: form.password,
      captchaKey: captcha.key,
      captchaCode: form.captchaCode
    })
    userStore.setToken(data.accessToken, data.refreshToken, data.expiresIn)
    await router.push(redirect.value || '/')
  } catch {
    refreshCaptcha()       // A failed login must refresh the captcha
  } finally {
    loading.value = false
  }
}
```

> After a failed login you **must refresh the captcha** (the backend uses it only once), otherwise the user's second submission is guaranteed to fail.

---

## 6. axios Wrapper (the Easiest Place to Get Wrong in This Project)

### 6.1 Refresh Queue

```ts
// utils/request.ts
import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 15000
})

// ---- Refresh queue state ----
let isRefreshing = false
let waitingQueue: Array<(token: string) => void> = []

function onTokenRefreshed(newToken: string) {
  waitingQueue.forEach(cb => cb(newToken))
  waitingQueue = []
}

// ---- Request interceptor ----
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`
  }
  return config
})

// ---- Response interceptor ----
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res

    // Business error
    ElMessage.error(res.msg || 'Operation failed')
    return Promise.reject(new Error(res.msg))
  },
  async (error) => {
    const { response, config } = error
    const userStore = useUserStore()

    if (!response) {
      ElMessage.error('Network error, please try again later')
      return Promise.reject(error)
    }

    // ★ 401: distinguish "expired" from "invalid"
    if (response.status === 401) {
      const code = response.data?.code

      // 1005 = access expired → silent refresh
      if (code === 1005 && !config._retry && userStore.refreshToken) {
        config._retry = true

        if (isRefreshing) {
          // Refreshing: suspend this request and replay it once the new token arrives
          return new Promise((resolve) => {
            waitingQueue.push((token: string) => {
              config.headers.Authorization = `Bearer ${token}`
              resolve(service(config))
            })
          })
        }

        isRefreshing = true
        try {
          const { data } = await refreshTokenApi(userStore.refreshToken)
          userStore.setToken(data.accessToken, data.refreshToken, data.expiresIn)
          onTokenRefreshed(data.accessToken)
          config.headers.Authorization = `Bearer ${data.accessToken}`
          return service(config)
        } catch (e) {
          waitingQueue = []
          await userStore.resetToken()
          router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
          return Promise.reject(e)
        } finally {
          isRefreshing = false
        }
      }

      // 1006 or refresh failed → log out immediately
      waitingQueue = []
      await userStore.resetToken()
      router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
      return Promise.reject(error)
    }

    // ★ 403 must never redirect to the login page
    if (response.status === 403) {
      ElMessage.error('No access permission')
      return Promise.reject(error)
    }

    ElMessage.error(response.data?.msg || `Request failed (${response.status})`)
    return Promise.reject(error)
  }
)

export default service
```

**Three key points in this code**:

1. **`config._retry`** prevents a request that 401s again after a refresh from recursing forever
2. **`isRefreshing` + `waitingQueue`** ensure that 10 concurrent 401s trigger only one refresh
3. **A 403 does not redirect to the login page** — doing so is an infinite loop for sure

### 6.2 Type Definitions

```ts
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
  traceId: string
}

export interface PageResult<T> {
  total: number
  list: T[]
}
```

```ts
// api/user.ts
export function listUser(params: UserQuery) {
  return request.get<any, R<PageResult<UserVO>>>('/system/user/list', { params })
}
```

---

## 7. Permission Control

### 7.1 Helper Functions

```ts
// utils/permission.ts
import { useUserStore } from '@/store/modules/user'

export function hasPerm(perm: string | string[]): boolean {
  const perms = useUserStore().perms
  if (perms.includes('*:*:*')) return true            // Super admin
  const list = Array.isArray(perm) ? perm : [perm]
  return list.some(p => perms.includes(p))
}

export function hasRole(role: string | string[]): boolean {
  const roles = useUserStore().roles
  if (roles.includes('admin')) return true
  const list = Array.isArray(role) ? role : [role]
  return list.some(r => roles.includes(r))
}
```

### 7.2 Directive

```ts
// directives/hasPerm.ts
import type { Directive, DirectiveBinding } from 'vue'
import { hasPerm } from '@/utils/permission'

export const hasPermDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    if (!hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
```

```vue
<el-button v-hasPerm="'system:user:add'" type="primary" @click="handleAdd">
  Add
</el-button>
```

> **A directive only hides a button; it is not a security boundary.** Every backend endpoint must have `@PreAuthorize`.

### 7.3 Component Approach (for More Complex Cases)

```vue
<Permission :value="'system:user:remove'">
  <el-button type="danger">Delete</el-button>
</Permission>
```

---

## 8. Pinia Store

```ts
// store/modules/user.ts
export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const perms = ref<string[]>([])
  const loaded = ref(false)

  function setToken(access: string, refresh: string, _expiresIn: number) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  async function getInfo() {
    const { data } = await getMe()
    user.value = data.user
    roles.value = data.roles
    perms.value = data.perms
    loaded.value = true
  }

  async function resetToken() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    roles.value = []
    perms.value = []
    loaded.value = false
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    usePermissionStore().reset()
  }

  return { accessToken, refreshToken, user, roles, perms, loaded, setToken, getInfo, resetToken }
})
```

```ts
// store/modules/permission.ts
export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const sidebarRoutes = ref<RouteRecordRaw[]>([])

  async function generateRoutes() {
    const { data } = await getRouters()
    const dynamic = transformRoutes(data)
    routes.value = dynamic
    sidebarRoutes.value = filterHidden(dynamic)
    return [
      ...dynamic,
      { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }  // ★ added last
    ]
  }

  function reset() {
    routes.value = []
    sidebarRoutes.value = []
  }

  return { routes, sidebarRoutes, generateRoutes, reset }
})
```

---

## 9. Layout and Multi-Tab

| Component | Responsibility |
|---|---|
| `layout/index.vue` | Overall skeleton: sidebar + navbar + tags view + content area |
| `Sidebar` | Recursively renders the menu, supports collapsing |
| `Navbar` | Breadcrumb, user dropdown, fullscreen, theme switching |
| `TagsView` | Multi-tab, supports close / close others / close all / refresh |
| `AppMain` | `<router-view v-slot>` + `<keep-alive :include="cachedViews">` |

**Caching rules**: only pages with `meta.cache = true` enter `keep-alive`, and the component `name` must match the route `name`.

```vue
<router-view v-slot="{ Component, route }">
  <keep-alive :include="cachedViews">
    <component :is="Component" :key="route.path" />
  </keep-alive>
</router-view>
```

> With the list page cached, returning from a detail page should preserve the search criteria and pagination — this is a key detail of admin UX.

---

## 10. Shared List-Page Logic (useTable)

```ts
export function useTable<T>(api: (params: any) => Promise<R<PageResult<T>>>, initParams = {}) {
  const loading = ref(false)
  const list = ref<T[]>([])
  const total = ref(0)
  const query = reactive({ pageNum: 1, pageSize: 10, ...initParams })

  async function getList() {
    loading.value = true
    try {
      const { data } = await api(query)
      list.value = data.list
      total.value = data.total
    } finally {
      loading.value = false
    }
  }

  function handleSearch() { query.pageNum = 1; getList() }
  function handleReset() {
    Object.assign(query, { pageNum: 1, pageSize: 10, ...initParams })
    getList()
  }

  onMounted(getList)
  return { loading, list, total, query, getList, handleSearch, handleReset }
}
```

**Convention**: every list page uses the same "search / reset / pagination" trio; after a delete, if the current page is empty, fall back one page.

---

## 11. Build and Deployment

```ts
// key vite.config.ts configuration
build: {
  chunkSizeWarningLimit: 1500,
  rollupOptions: {
    output: {
      manualChunks: {
        vue: ['vue', 'vue-router', 'pinia'],
        element: ['element-plus', '@element-plus/icons-vue'],
        echarts: ['echarts', 'vue-echarts']
      }
    }
  }
}
```

```nginx
server {
    listen 80;
    server_name admin.yourdomain.com;
    root /usr/share/nginx/html;
    index index.html;

    # ★ required for history mode, otherwise a refresh returns 404
    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Long cache for static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|svg|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 12. Frontend Self-Check Checklist

- [ ] Refreshing any child route produces no 404 and no blank screen
- [ ] 10 concurrent 401s trigger only 1 refresh
- [ ] A 403 does not redirect to the login page
- [ ] After logout you land on the login page, and browser Back cannot return to a business page
- [ ] Buttons without permission are invisible, and calling the API directly returns 403
- [ ] Form content is preserved when switching tabs (keep-alive works)
- [ ] Table pagination params are `pageNum` / `pageSize`, consistent with the backend
- [ ] All `id` values are handled as strings; `Number(id)` is never used
- [ ] `beginTime` / `endTime` on list APIs use the `yyyy-MM-dd HH:mm:ss` format
- [ ] The production bundle contains no `console.log` and no sourcemaps (or they are uploaded separately)
- [ ] First-screen size is < 1MB (after gzip)
- [ ] The upload component restricts size and type, matching the backend whitelist
