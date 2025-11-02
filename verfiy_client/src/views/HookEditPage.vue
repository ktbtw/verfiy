<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import http from '../utils/http'
// @ts-ignore
import UiPageHeader from '../components/ui/PageHeader.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const appInfo = ref<any>(null)
const hookId = ref<number | null>(null)

const form = ref({
  packageName: '',
  version: '*',
  enabled: true,
  zipVersion: 0
})

// Hook 数据键值对列表
const hookDataParams = ref<Array<{ id: number; key: string; value: string }>>([])
let nextParamId = 1

// 文件上传相关
const dexFile = ref<File | null>(null)
const zipFile = ref<File | null>(null)
const dexFileInput = ref<HTMLInputElement | null>(null)
const zipFileInput = ref<HTMLInputElement | null>(null)

const saving = ref(false)
const toast = ref({
  show: false,
  message: '',
  type: 'success' as 'success' | 'error'
})

async function loadAppInfo() {
  try {
    const { data } = await http.get('/admin/apps/api/current-app')
    if (data && data.id) {
      appInfo.value = data
    }
  } catch (e: any) {
    console.error('加载应用信息失败:', e)
  }
}

async function loadHookInfo() {
  if (!hookId.value) return
  
  loading.value = true
  try {
    const { data } = await http.get(`/admin/hook-info/${hookId.value}`)
    if (data) {
      form.value.packageName = data.packageName
      form.value.version = data.version
      form.value.enabled = data.enabled
      form.value.zipVersion = data.zipVersion || 0
      
      // 解析 Hook 数据为键值对
      if (data.data) {
        try {
          const parsed = JSON.parse(data.data)
          if (parsed && typeof parsed === 'object') {
            hookDataParams.value = Object.entries(parsed).map(([key, value]) => ({
              id: nextParamId++,
              key: key,
              value: String(value)
            }))
          }
        } catch (e) {
          // 忽略解析错误
        }
      }
    }
  } catch (e: any) {
    showToast('加载失败: ' + (e.response?.data?.message || e.message), 'error')
  } finally {
    loading.value = false
  }
}

function addHookParam() {
  hookDataParams.value.push({
    id: nextParamId++,
    key: '',
    value: ''
  })
}

function removeHookParam(id: number) {
  hookDataParams.value = hookDataParams.value.filter(p => p.id !== id)
}

async function handleDexFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    dexFile.value = target.files[0]
  }
}

async function handleZipFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    zipFile.value = target.files[0]
  }
}

function clearDexFile() {
  dexFile.value = null
  if (dexFileInput.value) {
    dexFileInput.value.value = ''
  }
}

function clearZipFile() {
  zipFile.value = null
  if (zipFileInput.value) {
    zipFileInput.value.value = ''
  }
}

async function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const base64 = reader.result as string
      resolve(base64.split(',')[1])
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

async function saveHook() {
  if (!form.value.packageName.trim()) {
    showToast('请输入包名', 'error')
    return
  }
  
  saving.value = true
  try {
    const hookData: any = {}
    hookDataParams.value.forEach(param => {
      if (param.key.trim()) {
        hookData[param.key.trim()] = param.value
      }
    })
    
    const payload: any = {
      id: hookId.value,
      appId: appInfo.value.id,
      packageName: form.value.packageName.trim(),
      version: form.value.version.trim() || '*',
      enabled: form.value.enabled,
      zipVersion: form.value.zipVersion,
      data: Object.keys(hookData).length > 0 ? JSON.stringify(hookData) : null
    }
    
    if (dexFile.value) {
      payload.dexData = await fileToBase64(dexFile.value)
    }
    if (zipFile.value) {
      payload.zipData = await fileToBase64(zipFile.value)
    }
    
    await http.post('/admin/hook-info', payload)
    showToast('保存成功', 'success')
    setTimeout(() => {
      router.push('/hook-management')
    }, 1000)
  } catch (e: any) {
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function showToast(message: string, type: 'success' | 'error') {
  toast.value.message = message
  toast.value.type = type
  toast.value.show = true
  setTimeout(() => {
    toast.value.show = false
  }, 2000)
}

function cancel() {
  router.push('/hook-management')
}

onMounted(() => {
  loadAppInfo()
  const id = route.params.id
  if (id) {
    hookId.value = Number(id)
    loadHookInfo()
  }
})
</script>

<template>
  <div class="page">
    <div class="page-header-with-action">
      <UiPageHeader :title="hookId ? '编辑 Hook 配置' : '新建 Hook 配置'" />
      <div class="header-actions">
        <button class="btn-back" @click="cancel">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
          </svg>
          返回
        </button>
        <UiButton @click="saveHook" :loading="saving">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path d="M7.707 10.293a1 1 0 10-1.414 1.414l3 3a1 1 0 001.414 0l3-3a1 1 0 00-1.414-1.414L11 11.586V6h5a2 2 0 012 2v7a2 2 0 01-2 2H4a2 2 0 01-2-2V8a2 2 0 012-2h5v5.586l-1.293-1.293zM9 4a1 1 0 012 0v2H9V4z" />
          </svg>
          保存配置
        </UiButton>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="edit-content">
      <!-- 基本信息 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">基本信息</h2>
          <p class="section-desc">配置 Hook 的包名和版本</p>
        </div>
        <div class="section-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">包名 *</label>
              <input 
                v-model="form.packageName" 
                type="text" 
                class="form-input" 
                placeholder="com.example.app"
              />
            </div>
            <div class="form-group">
              <label class="form-label">版本号</label>
              <input 
                v-model="form.version" 
                type="text" 
                class="form-input" 
                placeholder="* 表示所有版本"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label class="switch-label">
              <input type="checkbox" v-model="form.enabled" class="switch-input" />
              <span class="switch-slider"></span>
              <span class="switch-text">启用此 Hook 配置</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Hook 数据配置 -->
      <div class="settings-section">
        <div class="section-header">
          <div>
            <h2 class="section-title">Hook 数据</h2>
            <p class="section-desc">以键值对形式配置 Hook 参数</p>
          </div>
          <button @click="addHookParam" class="btn-add-param">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
            </svg>
            添加参数
          </button>
        </div>
        <div class="section-body">
          <div v-if="hookDataParams.length === 0" class="empty-params">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path d="M3 12v3c0 1.657 3.134 3 7 3s7-1.343 7-3v-3c0 1.657-3.134 3-7 3s-7-1.343-7-3z" />
              <path d="M3 7v3c0 1.657 3.134 3 7 3s7-1.343 7-3V7c0 1.657-3.134 3-7 3S3 8.657 3 7z" />
              <path d="M17 5c0 1.657-3.134 3-7 3S3 6.657 3 5s3.134-3 7-3 7 1.343 7 3z" />
            </svg>
            <p>暂无 Hook 参数</p>
            <p class="empty-hint">点击"添加参数"按钮开始配置</p>
          </div>
          
          <div v-else class="params-list">
            <transition-group name="param-list">
              <div v-for="param in hookDataParams" :key="param.id" class="param-item">
                <input v-model="param.key" type="text" class="param-key" placeholder="键名" />
                <span class="param-separator">:</span>
                <input v-model="param.value" type="text" class="param-value" placeholder="值" />
                <button @click="removeHookParam(param.id)" class="btn-remove-param" title="删除">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </transition-group>
          </div>
        </div>
      </div>

      <!-- 文件上传 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">文件资源</h2>
          <p class="section-desc">上传 Dex 文件和 Zip 资源包</p>
        </div>
        <div class="section-body">
          <div class="form-group">
            <label class="form-label">Dex 文件</label>
            <div class="file-upload-area">
              <input 
                ref="dexFileInput"
                type="file" 
                accept=".dex" 
                @change="handleDexFileChange"
                style="display: none"
              />
              <div v-if="!dexFile" class="upload-placeholder" @click="dexFileInput?.click()">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                </svg>
                <span class="upload-text">点击选择 .dex 文件</span>
                <span class="upload-hint">或拖拽文件到此处</span>
              </div>
              <div v-else class="uploaded-file">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd" />
                </svg>
                <div class="file-info">
                  <span class="file-name">{{ dexFile.name }}</span>
                  <span class="file-size">{{ (dexFile.size / 1024).toFixed(2) }} KB</span>
                </div>
                <button @click="clearDexFile" class="btn-clear-file">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">Zip 资源包</label>
            <div class="file-upload-area">
              <input 
                ref="zipFileInput"
                type="file" 
                accept=".zip" 
                @change="handleZipFileChange"
                style="display: none"
              />
              <div v-if="!zipFile" class="upload-placeholder" @click="zipFileInput?.click()">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                </svg>
                <span class="upload-text">点击选择 .zip 文件</span>
                <span class="upload-hint">或拖拽文件到此处</span>
              </div>
              <div v-else class="uploaded-file">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path d="M2 6a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V17a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
                </svg>
                <div class="file-info">
                  <span class="file-name">{{ zipFile.name }}</span>
                  <span class="file-size">{{ (zipFile.size / 1024).toFixed(2) }} KB</span>
                </div>
                <button @click="clearZipFile" class="btn-clear-file">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">资源版本号</label>
            <input 
              v-model.number="form.zipVersion" 
              type="number" 
              class="form-input" 
              placeholder="0" 
              min="0"
              style="max-width: 200px"
            />
            <p class="form-hint">客户端根据版本号判断是否需要更新资源</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <teleport to="body">
      <transition name="toast-fade">
        <div v-if="toast.show" class="toast" :class="`toast-${toast.type}`">
          <svg v-if="toast.type === 'success'" class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <svg v-else class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <span>{{ toast.message }}</span>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<style scoped>
.page-header-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  background: white;
  color: var(--text-1);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.btn-back:hover {
  background: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.2);
}

.btn-back svg {
  width: 16px;
  height: 16px;
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(79, 70, 229, 0.2);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.edit-content {
  max-width: 900px;
  margin: 0 auto;
}

.settings-section {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 4px 0;
}

.section-desc {
  font-size: 13px;
  color: var(--text-2);
  margin: 0;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-hint {
  font-size: 12px;
  color: var(--text-2);
  margin: 0;
}

/* Switch 开关样式 */
.switch-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.switch-input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.switch-slider {
  position: relative;
  width: 44px;
  height: 24px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.switch-slider::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  left: 2px;
  top: 2px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.switch-input:checked + .switch-slider {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.switch-input:checked + .switch-slider::before {
  transform: translateX(20px);
}

.switch-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

/* Hook 参数列表 */
.btn-add-param {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid rgba(79, 70, 229, 0.3);
  border-radius: 8px;
  background: rgba(79, 70, 229, 0.05);
  color: var(--brand);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  font-weight: 600;
}

.btn-add-param:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.4);
  transform: translateY(-1px);
}

.btn-add-param svg {
  width: 14px;
  height: 14px;
}

.empty-params {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  text-align: center;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
}

.empty-params svg {
  width: 48px;
  height: 48px;
  color: rgba(0, 0, 0, 0.15);
  margin-bottom: 12px;
}

.empty-params p {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
}

.empty-hint {
  font-size: 12px !important;
  color: var(--text-2) !important;
  opacity: 0.7;
  margin-top: 4px !important;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.param-item:hover {
  background: rgba(79, 70, 229, 0.02);
  border-color: rgba(79, 70, 229, 0.15);
}

.param-key,
.param-value {
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  font-size: 13px;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  background: white;
  transition: all 0.2s ease;
}

.param-key {
  flex: 0 0 200px;
}

.param-value {
  flex: 1;
}

.param-key:focus,
.param-value:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
}

.param-separator {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-2);
  flex-shrink: 0;
}

.btn-remove-param {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 6px;
  background: transparent;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.btn-remove-param:hover {
  background: rgba(239, 68, 68, 0.1);
  transform: scale(1.1);
}

.btn-remove-param svg {
  width: 14px;
  height: 14px;
}

/* 文件上传区域 */
.file-upload-area {
  width: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  border: 2px dashed rgba(79, 70, 229, 0.3);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.02), rgba(124, 58, 237, 0.02));
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-placeholder:hover {
  border-color: rgba(79, 70, 229, 0.5);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.05), rgba(124, 58, 237, 0.05));
  transform: translateY(-2px);
}

.upload-placeholder svg {
  width: 32px;
  height: 32px;
  color: var(--brand);
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 4px;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-2);
}

.uploaded-file {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05), rgba(5, 150, 105, 0.05));
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 12px;
}

.uploaded-file svg {
  width: 24px;
  height: 24px;
  color: #10b981;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: var(--text-2);
  font-family: 'SF Mono', 'Monaco', monospace;
}

.btn-clear-file {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 6px;
  background: white;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.btn-clear-file:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
  transform: scale(1.1);
}

.btn-clear-file svg {
  width: 14px;
  height: 14px;
}

/* Toast */
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  backdrop-filter: blur(20px);
  color: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  z-index: 10000;
  font-size: 14px;
  font-weight: 500;
}

.toast-success {
  background: rgba(22, 163, 74, 0.95);
}

.toast-error {
  background: rgba(239, 68, 68, 0.95);
}

.toast-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

/* 参数列表动画 */
.param-list-enter-active,
.param-list-leave-active {
  transition: all 0.3s ease;
}

.param-list-enter-from,
.param-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.param-list-move {
  transition: transform 0.3s ease;
}

@media (prefers-color-scheme: dark) {
  .settings-section {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .btn-back {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .btn-back:hover {
    background: rgba(255, 255, 255, 0.08);
  }
  
  .form-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .param-item {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .param-key,
  .param-value {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .upload-placeholder {
    background: linear-gradient(135deg, rgba(79, 70, 229, 0.08), rgba(124, 58, 237, 0.08));
    border-color: rgba(79, 70, 229, 0.4);
  }
  
  .uploaded-file {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(5, 150, 105, 0.1));
    border-color: rgba(16, 185, 129, 0.3);
  }
}
</style>
