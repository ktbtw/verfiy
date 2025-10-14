<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
// @ts-ignore
import UiInput from '../components/ui/Input.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const mounted = ref(false)
const canSubmit = computed(() => !!username.value && !!password.value && !loading.value)

onMounted(() => {
  setTimeout(() => mounted.value = true, 50)
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const resp = await fetch('/verfiy/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username: username.value, password: password.value })
    })
    if (resp.ok) {
      const data = await resp.json()
      if (data.success) {
        // 登录成功，等待一小段时间确保 session 生效
        await new Promise(resolve => setTimeout(resolve, 200))
        
        // 验证认证状态
        const authCheck = await fetch('/verfiy/api/auth/me', { credentials: 'include' })
        if (authCheck.ok) {
          const authData = await authCheck.json()
          if (authData.authenticated) {
            // 登录成功
            // 显式拉取一次 CSRF Token，确保后续 POST 不会 403
            await fetch('/verfiy/api/csrf-token', { credentials: 'include' }).catch(() => {})
            // 确认已认证，跳转到目标页面或主页
            const redirect = (route.query.redirect as string) || '/apps'
            await router.push(redirect)
          } else {
            error.value = '登录状态验证失败，请重试'
          }
        } else {
          error.value = '登录状态验证失败，请重试'
        }
      } else {
        error.value = data.message || '登录失败'
      }
    } else {
      const data = await resp.json().catch(() => ({}))
      error.value = data.message || '登录失败'
    }
  } catch (e) {
    error.value = '网络错误'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <transition name="card-enter">
      <div v-if="mounted" class="auth-card-wrapper">
        <div class="auth-card">
          <!-- Logo & Header -->
          <div class="auth-logo">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="url(#gradient1)" />
                <path d="M2 17L12 22L22 17V12L12 17L2 12V17Z" fill="url(#gradient2)" />
                <defs>
                  <linearGradient id="gradient1" x1="2" y1="2" x2="22" y2="12" gradientUnits="userSpaceOnUse">
                    <stop stop-color="#4F46E5" />
                    <stop offset="1" stop-color="#7C3AED" />
                  </linearGradient>
                  <linearGradient id="gradient2" x1="2" y1="12" x2="22" y2="22" gradientUnits="userSpaceOnUse">
                    <stop stop-color="#7C3AED" />
                    <stop offset="1" stop-color="#4F46E5" />
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>

          <div class="auth-header">
            <h1 class="auth-title">欢迎回来</h1>
            <p class="auth-subtitle">登录您的账户继续使用</p>
          </div>

          <!-- Error Message -->
          <transition name="error-slide">
            <div v-if="error" class="error-message">
              <svg class="error-icon" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
              <span>{{ error }}</span>
      </div>
      </transition>

          <!-- Login Form -->
          <form @submit.prevent="submit" class="auth-form">
            <div class="form-field">
              <label class="field-label">
                <svg class="label-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
                </svg>
                用户名
              </label>
              <div class="input-wrapper">
                <UiInput 
                  v-model="username" 
                  placeholder="请输入用户名" 
                  autocomplete="username"
                  class="auth-input"
                />
              </div>
            </div>

            <div class="form-field">
              <label class="field-label">
                <svg class="label-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
                </svg>
                密码
              </label>
              <div class="input-wrapper password-wrapper">
                <UiInput 
                  v-model="password" 
                  :type="showPassword ? 'text' : 'password'" 
                  placeholder="请输入密码" 
                  autocomplete="current-password"
                  class="auth-input"
                />
                <button 
                  type="button" 
                  class="password-toggle" 
                  @click="showPassword = !showPassword"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                >
                  <svg v-if="!showPassword" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                    <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                  </svg>
                  <svg v-else viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clip-rule="evenodd" />
                    <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="form-actions">
              <UiButton 
                :loading="loading" 
                :disabled="!canSubmit" 
                block 
                type="submit"
                class="submit-button"
              >
                <span v-if="!loading">登录</span>
                <span v-else>登录中...</span>
              </UiButton>
            </div>
          </form>

          <!-- Footer -->
          <div class="auth-footer">
            <p class="footer-text">
              还没有账号？
              <a href="/register" class="footer-link">立即注册</a>
            </p>
          </div>
        </div>
        </div>
    </transition>
  </div>
</template>

<style scoped>
.auth-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 60px 24px 24px;
}

/* 卡片容器 */
.auth-card-wrapper {
  width: 100%;
  max-width: 440px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 32px 36px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* Logo */
.auth-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(124, 58, 237, 0.1));
  border-radius: 14px;
  padding: 10px;
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.2);
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

/* 头部 */
.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px 0;
  background: linear-gradient(135deg, #1f2937 0%, #4f46e5 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--text-2);
  margin: 0;
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
  color: #dc2626;
  font-size: 13px;
  margin-bottom: 20px;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 表单 */
.auth-form {
  margin-bottom: 20px;
}

.form-field {
  margin-bottom: 16px;
}

.form-field:last-child {
  margin-bottom: 0;
}

.field-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 8px;
}

.label-icon {
  width: 16px;
  height: 16px;
  color: var(--text-2);
}

.input-wrapper {
  position: relative;
}

.password-wrapper {
  display: flex;
  align-items: center;
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.password-toggle:hover {
  color: var(--text-1);
}

.password-toggle:focus {
  outline: none;
  box-shadow: none;
}

.password-toggle svg {
  width: 100%;
  height: 100%;
}

/* 统一宽度：为带眼睛按钮的密码输入预留右侧内边距 */
.password-wrapper .ui-input {
  padding-right: 40px;
}

/* 表单动作 */
.form-actions {
  margin-top: 20px;
}

.submit-button {
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.3);
  transition: all 0.3s ease;
}

.submit-button:hover:not(:disabled) {
  box-shadow: 0 6px 24px rgba(79, 70, 229, 0.4);
  transform: translateY(-2px);
}

.submit-button:active:not(:disabled) {
  transform: translateY(0);
}

/* 底部 */
.auth-footer {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.footer-text {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
}

.footer-link {
  color: var(--brand);
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: var(--brand-hover);
  text-decoration: underline;
}

/* 动画 */
.card-enter-enter-active {
  animation: card-enter 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}

@keyframes card-enter {
  0% {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.error-slide-enter-active,
.error-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.error-slide-enter-from,
.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .auth-card {
    background: rgba(22, 22, 26, 0.8);
    box-shadow: 
      0 20px 60px rgba(0, 0, 0, 0.4),
      0 0 0 1px rgba(255, 255, 255, 0.1) inset;
    border-color: rgba(255, 255, 255, 0.1);
  }

  .auth-title {
    background: linear-gradient(135deg, #e5e7eb 0%, #a78bfa 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .error-message {
    background: rgba(239, 68, 68, 0.15);
    border-color: rgba(239, 68, 68, 0.3);
    color: #fca5a5;
  }

  .auth-footer {
    border-top-color: rgba(255, 255, 255, 0.1);
  }
}

/* 响应式 */
@media (max-width: 480px) {
  .auth-container {
    padding: 40px 20px 20px;
  }
  
  .auth-card {
    padding: 24px;
    border-radius: 16px;
  }

  .auth-title {
    font-size: 22px;
  }

  .auth-subtitle {
    font-size: 13px;
  }
  
  .form-field {
    margin-bottom: 14px;
  }
}
</style>


