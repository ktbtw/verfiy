import { createRouter, createWebHistory } from 'vue-router'
import AppsPage from '../views/AppsPage.vue'
import CardsPage from '../views/CardsPage.vue'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'

const routes = [
  { path: '/', component: AppsPage, meta: { requiresAuth: true } },
  { path: '/apps', component: AppsPage, meta: { requiresAuth: true } },
  { path: '/cards', component: CardsPage, meta: { requiresAuth: true } },
  { path: '/app-settings', component: () => import('../views/AppSettingsPage.vue'), meta: { requiresAuth: true } },
  { path: '/callback', component: () => import('../views/CallbackPage.vue'), meta: { requiresAuth: true } },
  { path: '/api-docs', component: () => import('../views/ApiDocsPage.vue'), meta: { requiresAuth: true } },
  { path: '/hook-management', component: () => import('../views/HookManagementPage.vue'), meta: { requiresAuth: true } },
  { path: '/hook-edit', component: () => import('../views/HookEditPage.vue'), meta: { requiresAuth: true } },
  { path: '/hook-edit/:id', component: () => import('../views/HookEditPage.vue'), meta: { requiresAuth: true } },
  { path: '/java-editor', component: () => import('../views/JavaEditorPage.vue'), meta: { requiresAuth: true } },
  { path: '/login', component: LoginPage },
  { path: '/register', component: RegisterPage },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 检查登录状态
async function checkAuth() {
  try {
    const resp = await fetch('/verfiy/api/auth/me', {
      credentials: 'include'
    })
    if (resp.ok) {
      const data = await resp.json()
      return data.authenticated === true
    }
    return false
  } catch (e) {
    return false
  }
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    const isAuthenticated = await checkAuth()
    if (!isAuthenticated) {
      // 未登录，重定向到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      // 已登录，继续访问
      next()
    }
  } else {
    // 公开路由（登录/注册页）
    // 如果已登录且访问登录页，可以选择重定向到主页
    if (to.path === '/login' || to.path === '/register') {
      const isAuthenticated = await checkAuth()
      if (isAuthenticated) {
        // 已登录用户访问登录页，重定向到主页
        next('/apps')
      } else {
        next()
      }
    } else {
      next()
    }
  }
})

export default router


