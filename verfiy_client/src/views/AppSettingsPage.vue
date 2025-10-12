<template>
  <div class="settings-page">
    <div class="settings-header">
      <h1 class="settings-title">应用设置</h1>
      <p class="settings-desc">管理你的应用配置、API密钥和安全选项</p>
    </div>

    <div v-if="loading" class="loading-container">
      <svg class="loading-spinner" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
        <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
      </svg>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <button @click="loadApp" class="retry-btn">重试</button>
    </div>

    <div v-else-if="app" class="settings-content">
      <!-- 基本信息 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">基本信息</h2>
          <p class="section-desc">应用的基础配置信息</p>
        </div>
        <div class="section-body">
          <div class="form-group">
            <label class="form-label">应用名称</label>
            <input v-model="form.name" type="text" class="form-input" placeholder="输入应用名称" />
          </div>
          <div class="form-group">
            <label class="form-label">应用描述</label>
            <textarea v-model="form.description" class="form-textarea" rows="3" placeholder="输入应用描述"></textarea>
          </div>
          <div class="form-group">
            <label class="form-label">应用图标</label>
            <input v-model="form.icon" type="text" class="form-input" placeholder="图标URL或base64" />
          </div>
          <div class="form-actions">
            <button @click="saveBasicInfo" :disabled="saving" class="btn btn-primary">
              <svg v-if="saving" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>保存</span>
            </button>
          </div>
        </div>
      </div>

      <!-- API密钥管理 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">API密钥</h2>
          <p class="section-desc">用于API调用的鉴权和签名</p>
        </div>
        <div class="section-body">
          <div class="api-key-item">
            <div class="key-info">
              <label class="key-label">API Key</label>
              <div class="key-display">
                <code class="key-value">{{ app.apiKey }}</code>
                <button @click="copyToClipboard(app.apiKey)" class="key-btn" title="复制">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                    <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div class="api-key-item">
            <div class="key-info">
              <label class="key-label">Secret Key</label>
              <div class="key-display">
                <input 
                  v-model="form.secretKey" 
                  :type="showSecretKey ? 'text' : 'password'" 
                  class="form-input" 
                  placeholder="留空则不启用签名"
                />
                <button @click="showSecretKey = !showSecretKey" class="key-btn" :title="showSecretKey ? '隐藏' : '显示'">
                  <svg v-if="showSecretKey" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clip-rule="evenodd" />
                    <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
                  </svg>
                  <svg v-else viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                    <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button @click="copyToClipboard(form.secretKey || '')" class="key-btn" title="复制" :disabled="!form.secretKey">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                    <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button @click="saveApiKeys" :disabled="saving" class="btn btn-primary">
              <svg v-if="saving" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>保存</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 安全配置 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">安全配置</h2>
          <p class="section-desc">配置加密传输和算法</p>
        </div>
        <div class="section-body">
          <div class="form-group">
            <label class="switch-label">
              <input type="checkbox" v-model="form.secure" class="switch-input" />
              <span class="switch-slider"></span>
              <span class="switch-text">启用加密传输</span>
            </label>
          </div>
          <div v-if="form.secure" class="form-group">
            <label class="form-label">加密算法</label>
            <div class="custom-select" @click="toggleEncryptionDropdown">
              <div class="select-trigger">
                <span>{{ encryptionOptions.find(o => o.value === form.encryptionAlg)?.label || '选择算法' }}</span>
                <svg class="select-arrow" :class="{ 'rotate': encryptionDropdownOpen }" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </div>
              <transition name="dropdown">
                <div v-if="encryptionDropdownOpen" class="select-options">
                  <div 
                    v-for="option in encryptionOptions" 
                    :key="option.value"
                    class="select-option"
                    :class="{ 'selected': form.encryptionAlg === option.value }"
                    @click.stop="selectEncryption(option.value)"
                  >
                    {{ option.label }}
                  </div>
                </div>
              </transition>
            </div>
          </div>
          <div class="form-actions">
            <button @click="saveSecurity" :disabled="saving" class="btn btn-primary">
              <svg v-if="saving" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>保存</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 公告和版本 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">公告和版本</h2>
          <p class="section-desc">配置应用公告和版本信息</p>
        </div>
        <div class="section-body">
          <div class="form-group">
            <label class="form-label">全局公告</label>
            <textarea v-model="form.announcement" class="form-textarea" rows="3" placeholder="输入公告内容"></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">版本号</label>
              <input v-model="form.version" type="text" class="form-input" placeholder="例如: 1.0.0" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">更新日志</label>
            <textarea v-model="form.changelog" class="form-textarea" rows="4" placeholder="输入更新日志"></textarea>
          </div>
          <div class="form-actions">
            <button @click="saveVersion" :disabled="saving" class="btn btn-primary">
              <svg v-if="saving" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>保存</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 危险操作 -->
      <div class="settings-section danger-section">
        <div class="section-header">
          <h2 class="section-title">危险操作</h2>
          <p class="section-desc">删除应用将同时删除所有相关的卡密和日志，此操作不可恢复</p>
        </div>
        <div class="section-body">
          <button @click="confirmDeleteApp" :disabled="deleting" class="btn btn-danger">
            <svg v-if="deleting" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
              <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
            </svg>
            <svg v-else viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <span>删除应用</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 提示 -->
    <teleport to="body">
      <transition name="toast-fade">
        <div v-if="toast.show" class="toast" :class="`toast-${toast.type}`">
          <svg v-if="toast.type === 'success'" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <svg v-else-if="toast.type === 'error'" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <svg v-else viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
          </svg>
          <span>{{ toast.message }}</span>
        </div>
      </transition>
    </teleport>

    <!-- 确认对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="confirmDialog.show" class="modal-overlay" @click="closeConfirm">
          <div class="confirm-dialog" :class="`dialog-${confirmDialog.type}`" @click.stop>
            <div class="dialog-icon">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
            </div>
            <h3 class="dialog-title">{{ confirmDialog.title }}</h3>
            <p class="dialog-message">{{ confirmDialog.message }}</p>
            <div class="dialog-actions">
              <button @click="closeConfirm" class="dialog-btn dialog-btn-cancel">取消</button>
              <button @click="confirmAction" class="dialog-btn dialog-btn-confirm">确定</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'

type Application = {
  id: number
  name: string
  description?: string
  icon?: string
  secure?: boolean
  encryptionAlg?: string
  apiKey: string
  secretKey?: string
  announcement?: string
  version?: string
  changelog?: string
  redeemExtra?: string
  redeemExtraMode?: string
}

const router = useRouter()
const app = ref<Application | null>(null)
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const deleting = ref(false)
const showSecretKey = ref(false)
const jsonError = ref('')

// 下拉框状态
const encryptionDropdownOpen = ref(false)
const redeemModeDropdownOpen = ref(false)

// 表单数据
const form = reactive({
  name: '',
  description: '',
  icon: '',
  secure: false,
  encryptionAlg: 'NONE',
  secretKey: '',
  announcement: '',
  version: '',
  changelog: '',
  redeemExtra: '',
  redeemExtraMode: 'SUCCESS_ONLY'
})

// 加密算法选项
const encryptionOptions = [
  { value: 'NONE', label: '不加密' },
  { value: 'RC4', label: 'RC4' },
  { value: 'AES-128-CBC', label: 'AES-128-CBC' },
  { value: 'AES-256-CBC', label: 'AES-256-CBC' }
]

// 返回模式选项
const redeemModeOptions = [
  { value: 'ALWAYS', label: '总是返回', desc: '无论成功失败都返回自定义参数' },
  { value: 'SUCCESS_ONLY', label: '仅成功时返回', desc: '只在核销成功时返回自定义参数' },
  { value: 'FAILURE_ONLY', label: '仅失败时返回', desc: '只在核销失败时返回自定义参数' }
]

// Toast
const toast = reactive({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
})

// 确认对话框
const confirmDialog = reactive({
  show: false,
  type: 'warning' as 'warning' | 'danger',
  title: '',
  message: '',
  onConfirm: () => {}
})

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => {
    toast.show = false
  }, 3000)
}

function showConfirm(title: string, message: string, onConfirm: () => void, type: 'warning' | 'danger' = 'warning') {
  confirmDialog.title = title
  confirmDialog.message = message
  confirmDialog.onConfirm = onConfirm
  confirmDialog.type = type
  confirmDialog.show = true
}

function closeConfirm() {
  confirmDialog.show = false
}

function confirmAction() {
  confirmDialog.onConfirm()
  closeConfirm()
}

async function loadApp() {
  loading.value = true
  error.value = ''
  try {
    const { data: currentApp } = await http.get('/admin/apps/api/current-app')
    if (!currentApp.id) {
      error.value = '未选择应用'
      return
    }
    
    const { data } = await http.get(`/admin/apps/${currentApp.id}`)
    app.value = data
    
    // 填充表单
    form.name = data.name || ''
    form.description = data.description || ''
    form.icon = data.icon || ''
    form.secure = data.secure || false
    form.encryptionAlg = data.encryptionAlg || 'NONE'
    form.secretKey = data.secretKey || ''
    form.announcement = data.announcement || ''
    form.version = data.version || ''
    form.changelog = data.changelog || ''
    form.redeemExtra = data.redeemExtra || ''
    form.redeemExtraMode = data.redeemExtraMode || 'SUCCESS_ONLY'
  } catch (e: any) {
    console.error('加载应用失败:', e)
    error.value = e.response?.data?.message || '加载应用失败'
  } finally {
    loading.value = false
  }
}

async function saveBasicInfo() {
  if (!app.value) return
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      name: form.name,
      description: form.description,
      icon: form.icon
    })
    if (data.success) {
      app.value = data.app
      showToast('保存成功', 'success')
    } else {
      showToast(data.message || '保存失败', 'error')
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function saveApiKeys() {
  if (!app.value) return
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      secretKey: form.secretKey || null
    })
    if (data.success) {
      app.value = data.app
      showToast('保存成功', 'success')
    } else {
      showToast(data.message || '保存失败', 'error')
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function saveSecurity() {
  if (!app.value) return
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      secure: form.secure,
      encryptionAlg: form.encryptionAlg
    })
    if (data.success) {
      app.value = data.app
      showToast('保存成功', 'success')
    } else {
      showToast(data.message || '保存失败', 'error')
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function saveVersion() {
  if (!app.value) return
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      announcement: form.announcement,
      version: form.version,
      changelog: form.changelog
    })
    if (data.success) {
      app.value = data.app
      showToast('保存成功', 'success')
    } else {
      showToast(data.message || '保存失败', 'error')
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function saveRedeemExtra() {
  if (!app.value) return
  if (jsonError.value) {
    showToast('JSON格式错误', 'error')
    return
  }
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      redeemExtra: form.redeemExtra,
      redeemExtraMode: form.redeemExtraMode
    })
    if (data.success) {
      app.value = data.app
      showToast('保存成功', 'success')
    } else {
      showToast(data.message || '保存失败', 'error')
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function validateJson() {
  if (!form.redeemExtra || form.redeemExtra.trim() === '') {
    jsonError.value = ''
    return
  }
  
  try {
    JSON.parse(form.redeemExtra)
    jsonError.value = ''
  } catch (e) {
    jsonError.value = 'JSON格式错误'
  }
}

function confirmDeleteApp() {
  showConfirm(
    '删除应用',
    `确定要删除应用 "${app.value?.name}" 吗？此操作将同时删除所有相关的卡密和日志，且不可恢复！`,
    deleteApp,
    'danger'
  )
}

async function deleteApp() {
  if (!app.value) return
  deleting.value = true
  try {
    const params = new URLSearchParams()
    params.append('id', app.value.id.toString())
    await http.post('/admin/apps/delete', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    showToast('删除成功', 'success')
    setTimeout(() => {
      router.push('/apps')
    }, 1000)
  } catch (e: any) {
    console.error('删除失败:', e)
    showToast(e.response?.data?.message || '删除失败', 'error')
    deleting.value = false
  }
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    showToast('复制成功', 'success')
  } catch (e) {
    showToast('复制失败', 'error')
  }
}

function toggleEncryptionDropdown() {
  encryptionDropdownOpen.value = !encryptionDropdownOpen.value
  redeemModeDropdownOpen.value = false
}

function toggleRedeemModeDropdown() {
  redeemModeDropdownOpen.value = !redeemModeDropdownOpen.value
  encryptionDropdownOpen.value = false
}

function selectEncryption(value: string) {
  form.encryptionAlg = value
  encryptionDropdownOpen.value = false
}

function selectRedeemMode(value: string) {
  form.redeemExtraMode = value
  redeemModeDropdownOpen.value = false
}

// 点击外部关闭下拉框
function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select')) {
    encryptionDropdownOpen.value = false
    redeemModeDropdownOpen.value = false
  }
}

onMounted(() => {
  loadApp()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.settings-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

.settings-header {
  margin-bottom: 32px;
}

.settings-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.settings-desc {
  font-size: 16px;
  color: var(--text-2);
  margin: 0;
}

/* 加载和错误状态 */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  color: var(--brand);
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  font-size: 16px;
  color: #ef4444;
  margin: 0 0 16px 0;
}

.retry-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 设置区块 */
.settings-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  overflow: visible;
  transition: all 0.3s ease;
}

.settings-section:hover {
  border-color: rgba(124, 58, 237, 0.2);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.1);
}

.danger-section {
  border-color: rgba(239, 68, 68, 0.2);
}

.danger-section:hover {
  border-color: rgba(239, 68, 68, 0.4);
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.1);
}

.section-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.03) 0%, rgba(79, 70, 229, 0.03) 100%);
}

.danger-section .section-header {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.03) 0%, rgba(220, 38, 38, 0.03) 100%);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--text-1);
}

.section-desc {
  font-size: 14px;
  color: var(--text-2);
  margin: 0;
}

.section-body {
  padding: 24px;
}

/* 表单 */
.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-1);
  font-size: 14px;
  font-family: inherit;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.code-textarea {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.form-input:hover,
.form-textarea:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 1);
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
  background: rgba(255, 255, 255, 1);
}

.form-hint {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: var(--text-2);
}

.form-hint.error {
  color: #ef4444;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

/* Switch开关 */
.switch-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.switch-input {
  display: none;
}

.switch-slider {
  position: relative;
  width: 44px;
  height: 24px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.switch-slider::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.switch-input:checked + .switch-slider {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.switch-input:checked + .switch-slider::before {
  transform: translateX(20px);
}

.switch-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

/* 自定义下拉框 */
.custom-select {
  position: relative;
  cursor: pointer;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-1);
  font-size: 14px;
  transition: all 0.2s ease;
}

.select-trigger:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 1);
}

.select-arrow {
  width: 20px;
  height: 20px;
  color: var(--text-2);
  transition: transform 0.2s ease;
}

.select-arrow.rotate {
  transform: rotate(180deg);
}

.select-options {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow-y: auto;
  z-index: 300;
  max-height: 280px;
}

.select-option {
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.select-option:hover {
  background: rgba(124, 58, 237, 0.08);
}

.select-option.selected {
  background: rgba(124, 58, 237, 0.12);
  color: var(--brand);
  font-weight: 600;
}

.option-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

.option-desc {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 2px;
}

/* API密钥 */
.api-key-item {
  margin-bottom: 20px;
}

.api-key-item:last-of-type {
  margin-bottom: 0;
}

.key-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.key-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.key-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.key-value {
  flex: 1;
  padding: 10px 14px;
  background: rgba(124, 58, 237, 0.08);
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  color: var(--brand);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.key-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.key-btn svg {
  width: 18px;
  height: 18px;
}

.key-btn:hover:not(:disabled) {
  background: rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.3);
  color: var(--brand);
  transform: scale(1.05);
}

.key-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.btn-spinner {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

.btn svg:not(.btn-spinner) {
  width: 18px;
  height: 18px;
}

/* Toast */
.toast {
  position: fixed;
  top: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  z-index: 2000;
  min-width: 280px;
  border: 1px solid rgba(0, 0, 0, 0.1);
}

.toast svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.toast-success {
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.3);
}

.toast-error {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}

.toast-info {
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.3);
}

.toast span {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

/* 确认对话框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  backdrop-filter: blur(4px);
}

.confirm-dialog {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 24px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
}

.dialog-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  margin-bottom: 16px;
}

.dialog-warning .dialog-icon {
  background: rgba(251, 191, 36, 0.15);
  color: #f59e0b;
}

.dialog-danger .dialog-icon {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.dialog-icon svg {
  width: 32px;
  height: 32px;
}

.dialog-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: var(--text-1);
}

.dialog-message {
  font-size: 14px;
  color: var(--text-2);
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.dialog-btn {
  flex: 1;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dialog-btn-cancel {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-1);
}

.dialog-btn-cancel:hover {
  background: rgba(0, 0, 0, 0.1);
}

.dialog-btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.dialog-danger .dialog-btn-confirm {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.dialog-btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.dialog-danger .dialog-btn-confirm:hover {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

/* 动画 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .confirm-dialog,
.modal-fade-leave-to .confirm-dialog {
  transform: scale(0.9);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 响应式 */
@media (max-width: 768px) {
  .settings-page {
    padding: 20px 16px;
  }
  
  .settings-title {
    font-size: 24px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .toast {
    left: 16px;
    right: 16px;
    min-width: auto;
  }
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .settings-section {
    background: rgba(22, 22, 26, 0.6);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .settings-section:hover {
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .danger-section {
    border-color: rgba(239, 68, 68, 0.3);
  }
  
  .danger-section:hover {
    border-color: rgba(239, 68, 68, 0.5);
  }
  
  .section-header {
    border-color: rgba(255, 255, 255, 0.1);
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
  }
  
  .danger-section .section-header {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(220, 38, 38, 0.1) 100%);
  }
  
  .form-input,
  .form-textarea {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: var(--text-1-dark);
  }
  
  .form-input:hover,
  .form-textarea:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .form-input:focus,
  .form-textarea:focus {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(124, 58, 237, 0.6);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
  }
  
  .select-trigger {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: var(--text-1-dark);
  }
  
  .select-trigger:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .select-options {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .key-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .key-btn:hover:not(:disabled) {
    background: rgba(124, 58, 237, 0.2);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .toast {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .confirm-dialog {
    background: rgba(22, 22, 26, 0.98);
  }
  
  .dialog-btn-cancel {
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .dialog-btn-cancel:hover {
    background: rgba(255, 255, 255, 0.15);
  }
}
</style>
