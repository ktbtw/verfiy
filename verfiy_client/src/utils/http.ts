import axios from 'axios'

const http = axios.create({
  baseURL: '/verfiy',
  withCredentials: true,
})

// 请求拦截器：自动添加 CSRF Token
http.interceptors.request.use(
  (config) => {
    // 从 cookie 中获取 CSRF token
    const csrfToken = getCookie('XSRF-TOKEN')
    if (csrfToken && config.method !== 'get') {
      config.headers['X-XSRF-TOKEN'] = csrfToken
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
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










