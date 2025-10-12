<template>
  <div class="callback-page">
    <div class="callback-header">
      <h1 class="callback-title">增加回参</h1>
      <p class="callback-desc">配置核销成功后返回的自定义参数（键值对形式）</p>
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

    <div v-else class="callback-content">
      <!-- 自定义参数 -->
      <div class="callback-section">
        <div class="section-header">
          <div class="header-content">
            <div>
              <h2 class="section-title">自定义参数</h2>
              <p class="section-desc">添加需要返回的键值对，支持占位符动态替换</p>
            </div>
            <button @click="addParam" class="btn btn-add">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
              </svg>
              添加参数
            </button>
          </div>
        </div>
        <div class="section-body">
          <div v-if="params.length === 0" class="params-empty">
            <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
              <path d="M3 12v3c0 1.657 3.134 3 7 3s7-1.343 7-3v-3c0 1.657-3.134 3-7 3s-7-1.343-7-3z" />
              <path d="M3 7v3c0 1.657 3.134 3 7 3s7-1.343 7-3V7c0 1.657-3.134 3-7 3S3 8.657 3 7z" />
              <path d="M17 5c0 1.657-3.134 3-7 3S3 6.657 3 5s3.134-3 7-3 7 1.343 7 3z" />
            </svg>
            <p class="empty-text">暂无自定义参数</p>
            <p class="empty-desc">点击"添加参数"按钮开始配置</p>
          </div>
          
          <div v-else class="params-list">
            <transition-group name="param-list">
              <div v-for="(param, index) in params" :key="param.id" class="param-item">
                <div class="param-index">{{ index + 1 }}</div>
                <div class="param-fields">
                  <div class="param-field">
                    <label class="field-label">键名（Key）</label>
                    <input 
                      v-model="param.key" 
                      type="text" 
                      class="field-input" 
                      placeholder="例如: vip"
                      @input="validateParam(param)"
                    />
                    <p v-if="param.keyError" class="field-error">{{ param.keyError }}</p>
                  </div>
                  <div class="param-field param-value-field">
                    <label class="field-label">值（Value）</label>
                    <input 
                      v-model="param.value" 
                      type="text" 
                      class="field-input" 
                      placeholder="例如: true 或 5 或 文本"
                    />
                  </div>
                  <div class="param-field mode-field">
                    <label class="field-label">返回时机</label>
                    <div class="custom-select" @click="toggleModeDropdown(index)">
                      <div class="select-trigger">
                        <span>{{ getModeLabel(param.mode) }}</span>
                        <svg class="select-arrow" :class="{ 'rotate': activeModeDropdown === index }" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                        </svg>
                      </div>
                      <transition name="dropdown">
                        <div v-if="activeModeDropdown === index" class="select-options">
                          <div 
                            v-for="option in modeOptions" 
                            :key="option.value"
                            class="select-option"
                            :class="{ 'selected': param.mode === option.value }"
                            @click.stop="selectMode(index, option.value)"
                          >
                            {{ option.label }}
                          </div>
                        </div>
                      </transition>
                    </div>
                  </div>
                </div>
                <div class="param-actions">
                  <div class="placeholder-btn-wrapper">
                    <button 
                      @click="showPlaceholderMenu(index)" 
                      class="param-action-btn placeholder-btn" 
                      title="插入占位符"
                    >
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z" clip-rule="evenodd" />
                        <path d="M2 13.692V16a2 2 0 002 2h12a2 2 0 002-2v-2.308A24.974 24.974 0 0110 15c-2.796 0-5.487-.46-8-1.308z" />
                      </svg>
                    </button>
                    <!-- 占位符菜单 -->
                    <transition name="dropdown">
                      <div v-if="activePlaceholderMenu === index" class="placeholder-menu">
                        <div 
                          v-for="ph in placeholders" 
                          :key="ph.value"
                          class="placeholder-option"
                          @click="insertPlaceholder(index, ph.value)"
                        >
                          <div class="placeholder-icon">
                            <svg v-if="ph.icon === 'clock'" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd" />
                            </svg>
                            <svg v-else-if="ph.icon === 'calendar'" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd" />
                            </svg>
                            <svg v-else-if="ph.icon === 'key'" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M18 8a6 6 0 01-7.743 5.743L10 14l-1 1-1 1H6v2H2v-4l4.257-4.257A6 6 0 1118 8zm-6-4a1 1 0 100 2 2 2 0 012 2 1 1 0 102 0 4 4 0 00-4-4z" clip-rule="evenodd" />
                            </svg>
                          </div>
                          <div class="placeholder-info">
                            <div class="placeholder-label">{{ ph.label }}</div>
                            <div class="placeholder-value">{{ ph.value }}</div>
                            <div class="placeholder-desc">{{ ph.desc }}</div>
                          </div>
                        </div>
                      </div>
                    </transition>
                  </div>
                  <button @click="removeParam(index)" class="param-action-btn param-delete" title="删除">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                    </svg>
                  </button>
                </div>
              </div>
            </transition-group>
          </div>
        </div>
      </div>

      <!-- JSON预览 -->
      <div class="callback-section" v-if="params.length > 0">
        <div class="section-header">
          <h2 class="section-title">配置预览</h2>
          <p class="section-desc">实际保存到后端的配置格式</p>
        </div>
        <div class="section-body">
          <div class="json-preview">
            <pre class="json-code">{{ jsonPreview }}</pre>
            <button @click="copyJson" class="json-copy" title="复制JSON">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 保存按钮 -->
      <div class="callback-actions">
        <button @click="saveParams" :disabled="saving || hasErrors" class="btn btn-primary btn-large">
          <svg v-if="saving" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
            <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
            <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
          </svg>
          <span v-else>保存配置</span>
        </button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import http from '../utils/http'

type Application = {
  id: number
  name: string
  redeemExtra?: string
}

type Param = {
  id: number
  key: string
  value: string
  mode: 'ALWAYS' | 'SUCCESS_ONLY' | 'FAILURE_ONLY'
  keyError?: string
}

const app = ref<Application | null>(null)
const loading = ref(true)
const error = ref('')
const saving = ref(false)

// 参数列表
const params = ref<Param[]>([])
let nextParamId = 1

// 下拉菜单状态
const activeModeDropdown = ref<number | null>(null)
const activePlaceholderMenu = ref<number | null>(null)

// 返回时机选项
const modeOptions = [
  { value: 'SUCCESS_ONLY', label: '仅成功' },
  { value: 'ALWAYS', label: '总是' },
  { value: 'FAILURE_ONLY', label: '仅失败' }
]

// 占位符选项（基于后端支持的所有占位符）
const placeholders = [
  { 
    value: '${timestamp}', 
    label: '时间戳（秒）', 
    desc: '当前时间戳（秒）',
    icon: 'clock'
  },
  { 
    value: '${millis}', 
    label: '时间戳（毫秒）', 
    desc: '当前时间戳（毫秒）',
    icon: 'clock'
  },
  { 
    value: '${date}', 
    label: '日期', 
    desc: '当前日期',
    icon: 'calendar'
  },
  { 
    value: '${datetime}', 
    label: '日期时间', 
    desc: '当前日期时间',
    icon: 'calendar'
  },
  { 
    value: '${iso8601}', 
    label: 'ISO8601时间', 
    desc: 'ISO8601格式时间',
    icon: 'calendar'
  },
  { 
    value: '${uuid}', 
    label: 'UUID', 
    desc: '随机UUID',
    icon: 'key'
  },
  { 
    value: '${nonce}', 
    label: '随机数', 
    desc: '随机数字',
    icon: 'key'
  },
  { 
    value: '${expireTs}', 
    label: '到期时间戳', 
    desc: '卡密到期时间戳',
    icon: 'calendar'
  }
]

// Toast
const toast = reactive({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
})

// 获取模式标签
function getModeLabel(mode: string): string {
  return modeOptions.find(o => o.value === mode)?.label || '仅成功'
}

// 自动推断类型
function inferType(value: string): 'string' | 'number' | 'boolean' {
  if (value === 'true' || value === 'false') {
    return 'boolean'
  }
  if (!isNaN(Number(value)) && value.trim() !== '') {
    return 'number'
  }
  return 'string'
}

// JSON预览
const jsonPreview = computed(() => {
  const config: Record<string, any> = {}
  
  // 按模式分组
  const grouped: Record<string, any> = {
    always: {},
    success: {},
    failure: {}
  }
  
  params.value.forEach(param => {
    if (param.key.trim()) {
      let value: any = param.value
      const type = inferType(param.value)
      
      if (type === 'number') {
        value = parseFloat(param.value) || 0
      } else if (type === 'boolean') {
        value = param.value.toLowerCase() === 'true'
      }
      
      if (param.mode === 'ALWAYS') {
        grouped.always[param.key.trim()] = value
      } else if (param.mode === 'SUCCESS_ONLY') {
        grouped.success[param.key.trim()] = value
      } else if (param.mode === 'FAILURE_ONLY') {
        grouped.failure[param.key.trim()] = value
      }
    }
  })
  
  // 组装最终配置
  if (Object.keys(grouped.always).length > 0) {
    config.always = grouped.always
  }
  if (Object.keys(grouped.success).length > 0) {
    config.success = grouped.success
  }
  if (Object.keys(grouped.failure).length > 0) {
    config.failure = grouped.failure
  }
  
  return JSON.stringify(config, null, 2)
})

// 是否有错误
const hasErrors = computed(() => {
  return params.value.some(p => p.keyError || !p.key.trim())
})

function showToast(message: string, type: 'success' | 'error' | 'info' = 'info') {
  toast.message = message
  toast.type = type
  toast.show = true
  setTimeout(() => {
    toast.show = false
  }, 3000)
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
    
    // 解析JSON为键值对
    if (data.redeemExtra) {
      try {
        const config = JSON.parse(data.redeemExtra)
        const newParams: Param[] = []
        
        // 解析各个模式的参数
        if (config.always) {
          Object.entries(config.always).forEach(([key, value]) => {
            newParams.push({
              id: nextParamId++,
              key,
              value: String(value),
              mode: 'ALWAYS'
            })
          })
        }
        if (config.success) {
          Object.entries(config.success).forEach(([key, value]) => {
            newParams.push({
              id: nextParamId++,
              key,
              value: String(value),
              mode: 'SUCCESS_ONLY'
            })
          })
        }
        if (config.failure) {
          Object.entries(config.failure).forEach(([key, value]) => {
            newParams.push({
              id: nextParamId++,
              key,
              value: String(value),
              mode: 'FAILURE_ONLY'
            })
          })
        }
        
        params.value = newParams
      } catch (e) {
        console.error('解析JSON失败:', e)
      }
    }
  } catch (e: any) {
    console.error('加载应用失败:', e)
    error.value = e.response?.data?.message || '加载应用失败'
  } finally {
    loading.value = false
  }
}

function addParam() {
  params.value.push({
    id: nextParamId++,
    key: '',
    value: '',
    mode: 'SUCCESS_ONLY'
  })
}

function removeParam(index: number) {
  params.value.splice(index, 1)
}

function validateParam(param: Param) {
  // 检查键名是否重复
  const duplicateKey = params.value.filter(p => p.key.trim() && p.key.trim() === param.key.trim()).length > 1
  if (duplicateKey) {
    param.keyError = '键名重复'
  } else if (!param.key.trim()) {
    param.keyError = ''
  } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(param.key.trim())) {
    param.keyError = '键名只能包含字母、数字和下划线，且不能以数字开头'
  } else {
    param.keyError = ''
  }
}

function toggleModeDropdown(index: number) {
  if (activeModeDropdown.value === index) {
    activeModeDropdown.value = null
  } else {
    activeModeDropdown.value = index
    activePlaceholderMenu.value = null
  }
}

function selectMode(index: number, mode: string) {
  const param = params.value[index]
  if (param) {
    param.mode = mode as any
  }
  activeModeDropdown.value = null
}

function showPlaceholderMenu(index: number) {
  if (activePlaceholderMenu.value === index) {
    activePlaceholderMenu.value = null
  } else {
    activePlaceholderMenu.value = index
    activeModeDropdown.value = null
  }
}

function insertPlaceholder(index: number, placeholder: string) {
  const param = params.value[index]
  if (!param) return
  
  if (param.value) {
    param.value += ' ' + placeholder
  } else {
    param.value = placeholder
  }
  activePlaceholderMenu.value = null
}

async function saveParams() {
  if (!app.value) return
  if (hasErrors.value) {
    showToast('请修正错误后再保存', 'error')
    return
  }
  
  saving.value = true
  try {
    const { data } = await http.post('/admin/apps/update', {
      id: app.value.id,
      // 发送空字符串表示清空，而不是 null（null 会导致不更新）
      redeemExtra: params.value.length > 0 ? jsonPreview.value.replace(/\n\s*/g, '') : ''
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

async function copyJson() {
  try {
    await navigator.clipboard.writeText(jsonPreview.value)
    showToast('复制成功', 'success')
  } catch (e) {
    showToast('复制失败', 'error')
  }
}

// 点击外部关闭下拉框
function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select') && !target.closest('.placeholder-btn')) {
    activeModeDropdown.value = null
    activePlaceholderMenu.value = null
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
.callback-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}

.callback-header {
  margin-bottom: 32px;
}

.callback-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.callback-desc {
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

/* 区块 */
.callback-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.callback-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  overflow: visible;
  transition: all 0.3s ease;
}

.callback-section:hover {
  border-color: rgba(124, 58, 237, 0.2);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.1);
}

.section-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.03) 0%, rgba(79, 70, 229, 0.03) 100%);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  overflow: visible;
}

/* 参数列表 */
.params-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: var(--text-2);
  opacity: 0.5;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-2);
  margin: 0;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.param-item {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.param-item:hover {
  border-color: rgba(124, 58, 237, 0.2);
  background: rgba(124, 58, 237, 0.02);
}

.param-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  margin-bottom: 2px;
}

.param-fields {
  display: grid;
  grid-template-columns: 1fr 2fr auto;
  gap: 12px;
  flex: 1;
  align-items: end;
}

.param-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: flex-end;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
}

.field-input {
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-1);
  font-size: 14px;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;
}

.field-input:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 1);
}

.field-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
  background: rgba(255, 255, 255, 1);
}

.field-error {
  font-size: 12px;
  color: #ef4444;
  margin: 0;
}

.placeholder-btn-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.placeholder-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  min-width: 260px;
  max-height: 280px;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 300;
}

.placeholder-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.placeholder-option:last-child {
  border-bottom: none;
}

.placeholder-option:hover {
  background: rgba(124, 58, 237, 0.08);
}

.placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
  color: var(--brand);
  flex-shrink: 0;
}

.placeholder-icon svg {
  width: 18px;
  height: 18px;
}

.placeholder-info {
  flex: 1;
}

.placeholder-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 2px;
}

.placeholder-value {
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', monospace;
  color: var(--brand);
  margin-bottom: 4px;
}

.placeholder-desc {
  font-size: 11px;
  color: var(--text-2);
  line-height: 1.4;
}

.mode-field {
  min-width: 140px;
}

.custom-select {
  position: relative;
  cursor: pointer;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
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
  width: 16px;
  height: 16px;
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
  overflow: hidden;
  z-index: 300;
}

.select-option {
  padding: 8px 12px;
  font-size: 13px;
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

/* 参数操作按钮区域 */
.param-actions {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

.param-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.param-action-btn svg {
  width: 18px;
  height: 18px;
}

.param-action-btn.placeholder-btn:hover {
  background: rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.3);
  color: var(--brand);
  transform: scale(1.05);
}

.param-action-btn.param-delete:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
  transform: scale(1.05);
}

/* JSON预览 */
.json-preview {
  position: relative;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  padding: 16px;
  overflow-x: auto;
}

.json-code {
  margin: 0;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.6;
}

.json-copy {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.json-copy svg {
  width: 16px;
  height: 16px;
}

.json-copy:hover {
  background: rgba(124, 58, 237, 0.1);
  color: var(--brand);
  transform: scale(1.05);
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
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-add {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-add:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-add svg {
  width: 16px;
  height: 16px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-large {
  padding: 12px 32px;
  font-size: 16px;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
}

.callback-actions {
  display: flex;
  justify-content: center;
  padding: 24px 0;
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

.param-list-enter-active,
.param-list-leave-active {
  transition: all 0.3s ease;
}

.param-list-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.param-list-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.param-list-move {
  transition: transform 0.3s ease;
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
  .callback-page {
    padding: 20px 16px;
  }
  
  .callback-title {
    font-size: 24px;
  }
  
  .param-fields {
    grid-template-columns: 1fr;
  }
  
  .mode-field {
    min-width: auto;
  }
  
  .toast {
    left: 16px;
    right: 16px;
    min-width: auto;
  }
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .callback-section {
    background: rgba(22, 22, 26, 0.6);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .callback-section:hover {
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .section-header {
    border-color: rgba(255, 255, 255, 0.1);
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
  }
  
  .param-item {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .param-item:hover {
    background: rgba(124, 58, 237, 0.1);
    border-color: rgba(124, 58, 237, 0.3);
  }
  
  .field-input,
  .select-trigger,
  .param-action-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
    color: var(--text-1-dark);
  }
  
  .field-input:hover,
  .select-trigger:hover,
  .param-action-btn:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .field-input:focus {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(124, 58, 237, 0.6);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
  }
  
  .placeholder-menu,
  .select-options {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .json-preview {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .json-copy {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .json-copy:hover {
    background: rgba(124, 58, 237, 0.2);
  }
  
  .toast {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.2);
  }
}
</style>
