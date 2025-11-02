<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import http from './utils/http'

const router = useRouter()
const route = useRoute()
const isAuthenticated = ref(false)
const username = ref('')
const currentAppId = ref<number | null>(null)
const currentAppName = ref('')
const currentAppType = ref<string>('NORMAL')

// 判断是否在应用管理相关页面
const isInAppContext = computed(() => {
  return currentAppId.value !== null && 
         (route.path === '/cards' || 
          route.path === '/app-settings' || 
          route.path === '/callback' || 
          route.path === '/api-docs' ||
          route.path === '/hook-management')
})

// 判断是否为 Xposed 应用
const isXposedApp = computed(() => currentAppType.value === 'XPOSED')

async function checkAuth() {
  try {
    const resp = await fetch('/verfiy/api/auth/me', {
      credentials: 'include'
    })
    if (resp.ok) {
      const data = await resp.json()
      isAuthenticated.value = data.authenticated === true
      username.value = data.username || ''
    } else {
      isAuthenticated.value = false
      username.value = ''
    }
  } catch (e) {
    isAuthenticated.value = false
    username.value = ''
  }
}

// 获取当前选中的应用
async function checkCurrentApp() {
  try {
    const resp = await fetch('/verfiy/admin/apps/api/current-app', {
      credentials: 'include'
    })
    if (resp.ok) {
      const data = await resp.json()
      currentAppId.value = data?.id || null
      currentAppName.value = data?.name || ''
      currentAppType.value = data?.appType || 'NORMAL'
    } else {
      currentAppId.value = null
      currentAppName.value = ''
      currentAppType.value = 'NORMAL'
    }
  } catch (e) {
    currentAppId.value = null
    currentAppName.value = ''
    currentAppType.value = 'NORMAL'
  }
}

async function logout() {
  try {
    await http.post('/logout')
    
    // 清空前端状态并跳转
    isAuthenticated.value = false
    username.value = ''
    currentAppId.value = null
    currentAppName.value = ''
    
    // 清理所有可能的 cookie
    document.cookie.split(";").forEach(c => {
      document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/")
    })
    
    await router.push('/login')
    
    // 强制刷新页面确保清理干净
    setTimeout(() => window.location.reload(), 100)
  } catch (e) {
    console.error('退出登录失败', e)
    // 即使失败也跳转到登录页
    isAuthenticated.value = false
    username.value = ''
    currentAppId.value = null
    currentAppName.value = ''
    await router.push('/login')
    setTimeout(() => window.location.reload(), 100)
  }
}

function backToApps() {
  currentAppId.value = null
  currentAppName.value = ''
  currentAppType.value = 'NORMAL'
  router.push('/apps')
}

onMounted(() => {
  checkAuth()
  checkCurrentApp()
  // 主动拉取一次 CSRF Token，确保后续 POST 不会 403
  // 拉一次 Token，并将返回的 token 写入内存缓存，确保首个 POST 可用
  fetch('/verfiy/api/csrf-token', { credentials: 'include' })
    .then(r => r.json())
    .then(d => {
      if (d && d.token) {
        // 将 token 写入一个全局变量（http.ts 会复用）
        // @ts-ignore
        window.__XSRF_TOKEN__ = d.token
      }
    })
    .catch(() => {})
})

// 监听路由变化，更新认证状态和当前应用
watch(() => route.path, () => {
  checkAuth()
  checkCurrentApp()
})
</script>

<template>
  <div>
    <header class="app-header" v-if="route.path !== '/login' && route.path !== '/register'">
      <div class="brand">
        <svg class="brand-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="url(#gradient1)" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
          <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <defs>
            <linearGradient id="gradient1" x1="2" y1="2" x2="22" y2="12" gradientUnits="userSpaceOnUse">
              <stop stop-color="#4F46E5"/>
              <stop offset="1" stop-color="#7C3AED"/>
            </linearGradient>
          </defs>
        </svg>
        <span class="brand-text">xyz验证</span>
        <span v-if="isInAppContext" class="app-name">/ {{ currentAppName }}</span>
      </div>
      <nav class="nav">
        <template v-if="isAuthenticated">
          <!-- 应用管理导航 -->
          <template v-if="isInAppContext">
            <router-link to="/cards" custom v-slot="{ href, navigate, isActive }">
              <a :href="href" @click="navigate" :class="{ active: isActive }">
                <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                  <path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd" />
                </svg>
                卡密管理
              </a>
            </router-link>
            <router-link to="/app-settings" custom v-slot="{ href, navigate, isActive }">
              <a :href="href" @click="navigate" :class="{ active: isActive }">
                <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" />
                </svg>
                应用设置
              </a>
            </router-link>
            <template v-if="!isXposedApp">
              <router-link to="/callback" custom v-slot="{ href, navigate, isActive }">
                <a :href="href" @click="navigate" :class="{ active: isActive }">
                  <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
                  </svg>
                  增加回参
                </a>
              </router-link>
              <router-link to="/api-docs" custom v-slot="{ href, navigate, isActive }">
                <a :href="href" @click="navigate" :class="{ active: isActive }">
                  <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
                  </svg>
                  调用示例
                </a>
              </router-link>
            </template>
            <template v-else>
              <router-link to="/hook-management" custom v-slot="{ href, navigate, isActive }">
                <a :href="href" @click="navigate" :class="{ active: isActive }">
                  <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                  Hook 管理
                </a>
              </router-link>
            </template>
            <a href="#" @click.prevent="backToApps" class="nav-back">
              <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
              </svg>
              返回
            </a>
          </template>
          
          <!-- 默认导航 -->
          <template v-else>
            <router-link to="/apps" custom v-slot="{ href, navigate, isActive }">
              <a :href="href" @click="navigate" :class="{ active: isActive }">
                <svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                </svg>
                应用
              </a>
            </router-link>
            <a href="#" @click.prevent="logout" class="logout-link">退出 ({{ username }})</a>
          </template>
        </template>
        <template v-else>
          <router-link to="/login" custom v-slot="{ href, navigate, isActive }">
            <a :href="href" @click="navigate" :class="{ active: isActive }">登录</a>
          </router-link>
          <router-link to="/register" custom v-slot="{ href, navigate, isActive }">
            <a :href="href" @click="navigate" :class="{ active: isActive }">注册</a>
          </router-link>
        </template>
      </nav>
    </header>
    <main class="page">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.app-name {
  color: var(--text-2);
  font-weight: 400;
  margin-left: 8px;
}

.nav-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
  vertical-align: middle;
}

.nav a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.nav-back {
  color: var(--text-2);
  padding: 6px 10px;
  border-radius: var(--radius-s);
  font-size: var(--font-s);
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.nav-back:hover {
  color: var(--brand);
  background: rgba(79, 70, 229, 0.1);
}

.logout-link {
  color: var(--text-2);
  padding: 6px 10px;
  border-radius: var(--radius-s);
  font-size: var(--font-s);
  cursor: pointer;
  text-decoration: none;
}

.logout-link:hover {
  color: var(--danger);
  background: rgba(239, 68, 68, 0.1);
}

@media (prefers-color-scheme: dark) {
  .nav-back:hover {
    background: rgba(124, 58, 237, 0.15);
  }
}
</style>
