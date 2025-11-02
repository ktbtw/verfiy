<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
// @ts-ignore
import UiPageHeader from '../components/ui/PageHeader.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

const router = useRouter()

const loading = ref(false)
const appInfo = ref<any>(null)
const hookList = ref<any[]>([])
const showEditDialog = ref(false)
const editForm = ref({
  id: null as number | null,
  packageName: '',
  version: '',
  enabled: true,
  data: '',
  dexData: '',
  zipData: '',
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
      await loadHookList()
    }
  } catch (e: any) {
    console.error('加载应用信息失败:', e)
  }
}

async function loadHookList() {
  if (!appInfo.value?.id) return
  loading.value = true
  try {
    const { data } = await http.get('/admin/hook-info', {
      params: { appId: appInfo.value.id }
    })
    hookList.value = data || []
  } catch (e: any) {
    console.error('加载Hook列表失败:', e)
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  router.push('/hook-edit')
}

function openEditDialog(hook: any) {
  router.push(`/hook-edit/${hook.id}`)
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
      // 移除 data:xxx;base64, 前缀
      resolve(base64.split(',')[1])
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

async function saveHook() {
  if (!editForm.value.packageName.trim()) {
    showToast('请输入包名', 'error')
    return
  }
  
  saving.value = true
  try {
    // 构建 Hook 数据 JSON
    const hookData: any = {}
    hookDataParams.value.forEach(param => {
      if (param.key.trim()) {
        hookData[param.key.trim()] = param.value
      }
    })
    
    const payload: any = {
      ...editForm.value,
      appId: appInfo.value.id,
      data: Object.keys(hookData).length > 0 ? JSON.stringify(hookData) : null
    }
    
    // 处理文件上传
    if (dexFile.value) {
      payload.dexData = await fileToBase64(dexFile.value)
    }
    if (zipFile.value) {
      payload.zipData = await fileToBase64(zipFile.value)
    }
    
    await http.post('/admin/hook-info', payload)
    showToast('保存成功', 'success')
    showEditDialog.value = false
    await loadHookList()
  } catch (e: any) {
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(hook: any) {
  try {
    const newStatus = !hook.enabled
    await http.patch(`/admin/hook-info/${hook.id}/status`, null, {
      params: { enabled: newStatus }
    })
    hook.enabled = newStatus
    showToast(newStatus ? '已启用' : '已禁用', newStatus ? 'success' : 'error')
  } catch (e: any) {
    showToast(e.response?.data?.message || '操作失败', 'error')
  }
}

async function deleteHook(hook: any) {
  if (!confirm(`确定删除 ${hook.packageName} (${hook.version}) 的Hook配置吗？`)) {
    return
  }
  
  try {
    await http.delete(`/admin/hook-info/${hook.id}`)
    showToast('删除成功', 'success')
    await loadHookList()
  } catch (e: any) {
    showToast(e.response?.data?.message || '删除失败', 'error')
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


onMounted(() => {
  loadAppInfo()
})
</script>

<template>
  <div class="page">
    <div class="page-header-with-action">
      <UiPageHeader title="Hook 管理" />
      <UiButton @click="openCreateDialog" variant="soft">
        <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
        </svg>
        新建Hook
      </UiButton>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="hookList.length === 0" class="empty-state">
      <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
      </svg>
      <p class="empty-text">暂无Hook配置</p>
      <button @click="openCreateDialog" class="btn btn-primary">创建第一个Hook</button>
    </div>

    <div v-else class="hook-grid">
      <div
        v-for="hook in hookList"
        :key="hook.id"
        :class="['hook-card', hook.enabled ? 'enabled' : 'disabled']"
      >
        <div class="hook-header">
          <div class="hook-info">
            <h3 class="hook-package">{{ hook.packageName }}</h3>
            <span class="hook-version">
              <template v-if="hook.version === '*'">所有版本启用</template>
              <template v-else>版本 {{ hook.version }}</template>
            </span>
          </div>
          <div class="hook-actions">
            <button 
              class="action-btn toggle-btn" 
              :class="hook.enabled ? 'enabled' : 'disabled'" 
              @click="toggleEnabled(hook)"
              :title="hook.enabled ? '点击禁用' : '点击启用'"
            >
              <svg v-if="hook.enabled" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              <svg v-else viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
            </button>
            <button class="action-btn edit-btn" @click="openEditDialog(hook)" title="编辑">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
              </svg>
            </button>
            <button class="action-btn delete-btn" @click="deleteHook(hook)" title="删除">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </div>
        <div class="hook-meta">
          <span class="meta-item">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd" />
            </svg>
            {{ new Date(hook.updatedAt).toLocaleString() }}
          </span>
          <span v-if="hook.zipVersion" class="meta-item">
            资源版本: {{ hook.zipVersion }}
          </span>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showEditDialog" class="modal-overlay" @click="showEditDialog = false">
          <div class="modal-container hook-edit-modal" @click.stop>
            <div class="modal-header">
              <div class="header-content">
                <div class="modal-icon-wrapper">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
                  </svg>
                </div>
                <div>
                  <h3 class="modal-title">{{ editForm.id ? '编辑 Hook 配置' : '新建 Hook 配置' }}</h3>
                  <p class="modal-subtitle">配置应用的 Hook 行为和数据</p>
                </div>
              </div>
              <button class="modal-close" @click="showEditDialog = false">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <!-- 基本信息 -->
              <div class="section">
                <h4 class="section-title">基本信息</h4>
                <div class="form-row">
                  <div class="form-group">
                    <label class="form-label">包名 *</label>
                    <input v-model="editForm.packageName" type="text" class="form-input" placeholder="com.example.app" />
                  </div>
                  <div class="form-group">
                    <label class="form-label">版本号</label>
                    <input v-model="editForm.version" type="text" class="form-input" placeholder="* 表示所有版本" />
                  </div>
                </div>
                
                <div class="form-group">
                  <label class="switch-label">
                    <input type="checkbox" v-model="editForm.enabled" class="switch-input" />
                    <span class="switch-slider"></span>
                    <span class="switch-text">启用此 Hook 配置</span>
                  </label>
                </div>
              </div>
              
              <!-- Hook 数据配置 -->
              <div class="section">
                <div class="section-header-with-action">
                  <h4 class="section-title">Hook 数据</h4>
                  <button @click="addHookParam" class="btn-add-param">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                    </svg>
                    添加参数
                  </button>
                </div>
                
                <div v-if="hookDataParams.length === 0" class="empty-params">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M3 12v3c0 1.657 3.134 3 7 3s7-1.343 7-3v-3c0 1.657-3.134 3-7 3s-7-1.343-7-3z" />
                    <path d="M3 7v3c0 1.657 3.134 3 7 3s7-1.343 7-3V7c0 1.657-3.134 3-7 3S3 8.657 3 7z" />
                    <path d="M17 5c0 1.657-3.134 3-7 3S3 6.657 3 5s3.134-3 7-3 7 1.343 7 3z" />
                  </svg>
                  <p>暂无 Hook 参数</p>
                </div>
                
                <div v-else class="params-list">
                  <div v-for="param in hookDataParams" :key="param.id" class="param-item">
                    <input v-model="param.key" type="text" class="param-key" placeholder="键名" />
                    <span class="param-separator">:</span>
                    <input v-model="param.value" type="text" class="param-value" placeholder="值" />
                    <button @click="removeHookParam(param.id)" class="btn-remove-param">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- 文件上传 -->
              <div class="section">
                <h4 class="section-title">文件资源</h4>
                
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
                      <span>点击上传 .dex 文件</span>
                    </div>
                    <div v-else class="uploaded-file">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd" />
                      </svg>
                      <span class="file-name">{{ dexFile.name }}</span>
                      <span class="file-size">({{ (dexFile.size / 1024).toFixed(2) }} KB)</span>
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
                      <span>点击上传 .zip 文件</span>
                    </div>
                    <div v-else class="uploaded-file">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M2 6a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V17a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
                      </svg>
                      <span class="file-name">{{ zipFile.name }}</span>
                      <span class="file-size">({{ (zipFile.size / 1024).toFixed(2) }} KB)</span>
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
                  <input v-model.number="editForm.zipVersion" type="number" class="form-input" placeholder="0" min="0" />
                  <p class="form-hint">客户端会根据版本号判断是否需要更新资源</p>
                </div>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showEditDialog = false">取消</button>
              <UiButton @click="saveHook" :loading="saving">
                <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M7.707 10.293a1 1 0 10-1.414 1.414l3 3a1 1 0 001.414 0l3-3a1 1 0 00-1.414-1.414L11 11.586V6h5a2 2 0 012 2v7a2 2 0 01-2 2H4a2 2 0 01-2-2V8a2 2 0 012-2h5v5.586l-1.293-1.293zM9 4a1 1 0 012 0v2H9V4z" />
                </svg>
                保存配置
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

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
  margin-bottom: 24px;
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
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

.empty-icon {
  width: 64px;
  height: 64px;
  color: rgba(0, 0, 0, 0.15);
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: var(--text-2);
  margin-bottom: 20px;
}

.hook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.hook-card {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.hook-card.enabled {
  border-color: rgba(16, 185, 129, 0.3);
  box-shadow: 0 12px 30px rgba(16, 185, 129, 0.12);
}

.hook-card.disabled {
  border-color: rgba(239, 68, 68, 0.2);
  background: rgba(239, 68, 68, 0.04);
  opacity: 0.85;
}

.hook-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.hook-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.hook-info {
  flex: 1;
  min-width: 0;
}

.hook-package {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 4px 0;
  word-break: break-all;
}

.hook-version {
  font-size: 13px;
  color: var(--text-2);
  font-family: 'SF Mono', 'Monaco', monospace;
}

.hook-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.action-btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.action-btn.toggle-btn.enabled {
  background: rgba(16, 185, 129, 0.1) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
  color: #10b981 !important;
}

.action-btn.toggle-btn.enabled:hover {
  background: rgba(16, 185, 129, 0.2) !important;
  border-color: rgba(16, 185, 129, 0.4) !important;
}

.action-btn.toggle-btn.disabled {
  background: rgba(239, 68, 68, 0.1) !important;
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #ef4444 !important;
}

.action-btn.toggle-btn.disabled:hover {
  background: rgba(239, 68, 68, 0.2) !important;
  border-color: rgba(239, 68, 68, 0.4) !important;
}

.action-btn:hover {
  transform: scale(1.1);
}

.edit-btn {
  color: var(--brand);
  background: rgba(79, 70, 229, 0.05);
}

.edit-btn:hover {
  background: rgba(79, 70, 229, 0.15);
  border-color: rgba(79, 70, 229, 0.3);
}

.delete-btn {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.05);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
}

.hook-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-2);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-item svg {
  width: 14px;
  height: 14px;
}

/* 对话框样式 */
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

.modal-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  width: 100%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.hook-edit-modal {
  max-width: 800px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-1);
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-1);
}

.modal-close svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.modal-body {
  padding: 24px;
}

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
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-textarea {
  resize: vertical;
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-secondary {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-1);
}

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: var(--brand);
  color: white;
}

.btn-primary:hover {
  background: #5b4fcf;
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

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-container,
.modal-fade-leave-to .modal-container {
  transform: scale(0.9) translateY(20px);
}
</style>

