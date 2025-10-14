import axios from 'axios'

const http = axios.create({
  baseURL: '/verfiy',
  withCredentials: true,
})

// 简单的内存缓存，避免每次请求都打 /api/csrf-token
let csrfTokenCache: string | null = null

async function ensureCsrfToken(): Promise<string | null> {
  // 1) 先从 Cookie 里取
  const fromCookie = getCookie('XSRF-TOKEN')
  if (fromCookie) {
    csrfTokenCache = fromCookie
    return fromCookie
  }
  // 先看全局（App.vue 可能已经写入）
  // @ts-ignore
  if (window.__XSRF_TOKEN__) {
    // @ts-ignore
    csrfTokenCache = window.__XSRF_TOKEN__
    return csrfTokenCache
  }
  // 2) 再看内存缓存
  if (csrfTokenCache) return csrfTokenCache
  // 3) 最后显式获取一次
  try {
    const resp = await fetch('/verfiy/api/csrf-token', { credentials: 'include' })
    if (resp.ok) {
      const data = await resp.json().catch(() => ({}))
      if (data && typeof data.token === 'string' && data.token.length > 0) {
        csrfTokenCache = data.token
        return csrfTokenCache
      }
    }
  } catch {}
  return null
}

// 请求拦截器：自动添加 CSRF Token（支持异步）
http.interceptors.request.use(
  async (config) => {
    // 仅对非 GET 请求添加
    if (config.method && config.method.toLowerCase() !== 'get') {
      const token = getCookie('XSRF-TOKEN') || csrfTokenCache || (await ensureCsrfToken())
      if (token) {
        config.headers = config.headers || {}
        // 同时设置两种常见头，兼容后端读取策略
        config.headers['X-XSRF-TOKEN'] = token
        config.headers['X-CSRF-TOKEN'] = token
      } else {
        // 若无 token，仅静默继续（后端将返回403，前端正常提示）
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    return Promise.reject(err)
  }
)

// 从 cookie 中获取值的辅助函数
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null
  }
  return null
}

export default http










