<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
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
  zipVersion: 0,
  requireCardVerification: false
})

// Hook 配置列表
type HookItem = {
  id: number
  className: string
  methodName: string
  methodParamTypes: string[]
  setResultClass: string
  setResultData: string
  setParams: Array<{ paramClass: string; paramData: string }>
  isIntercept: boolean
}

const hookItems = ref<HookItem[]>([])
let nextHookId = 1

// Dex 配置
const dexConfig = ref({
  dexClassName: '',
  dexMethodName: '',
  dexMethodParamTypes: [] as string[],
  useLocalCache: false
})

const MAX_DEX_SIZE = 6 * 1024 * 1024
const MAX_ZIP_SIZE = 10 * 1024 * 1024

// 文件上传相关
const dexFile = ref<File | null>(null)
const zipFile = ref<File | null>(null)
const dexFileInput = ref<HTMLInputElement | null>(null)
const zipFileInput = ref<HTMLInputElement | null>(null)

// Java 基本类型列表
const javaBasicTypes = [
  { label: 'int', value: 'int' },
  { label: 'long', value: 'long' },
  { label: 'float', value: 'float' },
  { label: 'double', value: 'double' },
  { label: 'boolean', value: 'boolean' },
  { label: 'byte', value: 'byte' },
  { label: 'char', value: 'char' },
  { label: 'short', value: 'short' },
  { label: 'String', value: 'String' },
  { label: 'void', value: 'void' }
]

// 类型助手状态
const typeHelperOpen = ref<{ hookId: number; field: 'result' | 'param'; paramIndex?: number } | null>(null)

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
      form.value.requireCardVerification = Boolean(data.requireCardVerification)
      
      // 解析 Hook 数据
      if (data.data) {
        try {
          const parsed = JSON.parse(data.data)
          if (parsed && parsed.data && Array.isArray(parsed.data)) {
            hookItems.value = parsed.data.map((item: any) => ({
              id: nextHookId++,
              className: item.ClassName || '',
              methodName: item.MethodName || '',
              methodParamTypes: item.MethodParamType || [],
              setResultClass: item.SetResult?.SetClass || '',
              setResultData: item.SetResult?.SetData || '',
              setParams: item.SetParam || [],
              isIntercept: item.IsIntercept || false
            }))
          }
          
          // 解析 Dex 配置
          if (parsed.dexData) {
            dexConfig.value.dexClassName = parsed.dexData.DexClassName || ''
            dexConfig.value.dexMethodName = parsed.dexData.DexMethodName || ''
            dexConfig.value.dexMethodParamTypes = parsed.dexData.DexMethodParamTypes || []
            dexConfig.value.useLocalCache = parsed.dexData.UseLocalCache || false
          }
        } catch (e) {
          console.error('解析 Hook 数据失败:', e)
        }
      }
    }
  } catch (e: any) {
    showToast('加载失败: ' + (e.response?.data?.message || e.message), 'error')
  } finally {
    loading.value = false
  }
}

function addHookItem() {
  hookItems.value.push({
    id: nextHookId++,
    className: '',
    methodName: '',
    methodParamTypes: [],
    setResultClass: '',
    setResultData: '',
    setParams: [],
    isIntercept: false
  })
}

function removeHookItem(id: number) {
  hookItems.value = hookItems.value.filter(h => h.id !== id)
}

function addMethodParam(hookItem: HookItem) {
  if (!hookItem.methodParamTypes) {
    hookItem.methodParamTypes = []
  }
  hookItem.methodParamTypes.push('')
}

function removeMethodParam(hookItem: HookItem, index: number) {
  hookItem.methodParamTypes.splice(index, 1)
}

function addSetParam(hookItem: HookItem) {
  if (!hookItem.setParams) {
    hookItem.setParams = []
  }
  hookItem.setParams.push({ paramClass: '', paramData: '' })
}

function removeSetParam(hookItem: HookItem, index: number) {
  hookItem.setParams.splice(index, 1)
}

function toggleTypeHelper(hookId: number, field: 'result' | 'param', paramIndex?: number) {
  if (typeHelperOpen.value?.hookId === hookId && typeHelperOpen.value?.field === field && typeHelperOpen.value?.paramIndex === paramIndex) {
    typeHelperOpen.value = null
  } else {
    typeHelperOpen.value = { hookId, field, paramIndex }
  }
}

function selectType(type: string, hookItem: HookItem, field: 'result' | 'param', paramIndex?: number) {
  if (field === 'result') {
    hookItem.setResultClass = type
  } else if (field === 'param' && paramIndex !== undefined) {
    hookItem.methodParamTypes[paramIndex] = type
  }
  typeHelperOpen.value = null
}

// 验证返回值数据是否符合类型
function validateReturnValue(type: string, value: string): string | null {
  if (!type || !value) return null
  
  const trimmedValue = value.trim()
  
  switch (type.toLowerCase()) {
    case 'int':
    case 'long':
    case 'short':
    case 'byte':
      if (!/^-?\d+$/.test(trimmedValue)) {
        return `${type} 类型应为整数`
      }
      break
    case 'float':
    case 'double':
      if (!/^-?\d+\.?\d*$/.test(trimmedValue) && !/^-?\d*\.\d+$/.test(trimmedValue)) {
        return `${type} 类型应为数字`
      }
      break
    case 'boolean':
      if (trimmedValue !== 'true' && trimmedValue !== 'false') {
        return 'boolean 类型应为 true 或 false'
      }
      break
    case 'char':
      if (trimmedValue.length !== 1) {
        return 'char 类型应为单个字符'
      }
      break
    case 'void':
      if (trimmedValue !== '') {
        return 'void 类型不应有返回值'
      }
      break
  }
  
  return null
}

function closeTypeHelper(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.type-helper-wrapper')) {
    typeHelperOpen.value = null
  }
}

function toggleDexCache() {
  dexConfig.value.useLocalCache = !dexConfig.value.useLocalCache
  showToast(
    dexConfig.value.useLocalCache ? 'Dex 本地缓存已启用' : 'Dex 本地缓存已禁用',
    dexConfig.value.useLocalCache ? 'success' : 'error'
  )
}

function goToJavaEditor() {
  const currentHookId = hookId.value
  if (currentHookId) {
    router.push(`/java-editor?hookId=${currentHookId}`)
  } else {
    router.push('/java-editor')
  }
}

async function handleDexFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    if (file.size > MAX_DEX_SIZE) {
      showToast('Dex 文件超过 6MB 限制', 'error')
      target.value = ''
      dexFile.value = null
      return
    }
    dexFile.value = file
  }
}

async function handleZipFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    if (file.size > MAX_ZIP_SIZE) {
      showToast('Zip 文件超过 10MB 限制', 'error')
      target.value = ''
      zipFile.value = null
      return
    }
    zipFile.value = file
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
      if (typeof reader.result !== 'string') {
        reject(new Error('文件读取结果无效'))
        return
      }

      const parts = reader.result.split(',')
      const base64 = parts.length > 1 ? parts[1] : parts[0]

      if (!base64) {
        reject(new Error('文件转换 Base64 失败'))
        return
      }

      resolve(base64)
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
    // 构建符合客户端格式的 Hook 数据
    const hookData: any = {
      data: hookItems.value.map(item => ({
        ClassName: item.className,
        MethodName: item.methodName,
        MethodParamType: item.methodParamTypes,
        SetResult: {
          SetClass: item.setResultClass,
          SetData: item.setResultData
        },
        SetParam: item.setParams.map(p => ({
          SetClass: p.paramClass,
          SetData: p.paramData
        })),
        IsIntercept: item.isIntercept
      }))
    }
    
    // 添加 Dex 配置
    if (dexConfig.value.dexClassName || dexConfig.value.dexMethodName) {
      hookData.dexData = {
        DexClassName: dexConfig.value.dexClassName,
        DexMethodName: dexConfig.value.dexMethodName,
        DexMethodParamTypes: [], // Dex 方法不需要参数
        UseLocalCache: dexConfig.value.useLocalCache
      }
      
      // 如果上传了 Dex 文件，添加 dexBytes
      if (dexFile.value) {
        const dexBase64 = await fileToBase64(dexFile.value)
        hookData.dexData.dexBytes = dexBase64
      }
    }
    
    // 添加 Zip 配置
    if (zipFile.value) {
      const zipBase64 = await fileToBase64(zipFile.value)
      hookData.zipData = {
        zipBytes: zipBase64,
        zipVersion: form.value.zipVersion
      }
    }
    
    const payload: any = {
      id: hookId.value,
      appId: appInfo.value.id,
      packageName: form.value.packageName.trim(),
      version: form.value.version.trim() || '*',
      enabled: form.value.enabled,
      requireCardVerification: form.value.requireCardVerification,
      zipVersion: form.value.zipVersion,
      data: JSON.stringify(hookData)
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
  document.addEventListener('click', closeTypeHelper)
})

onUnmounted(() => {
  document.removeEventListener('click', closeTypeHelper)
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
          <div class="form-group">
            <label class="switch-label">
              <input type="checkbox" v-model="form.requireCardVerification" class="switch-input" />
              <span class="switch-slider"></span>
              <span class="switch-text">启用卡密验证</span>
            </label>
            <p class="form-hint">开启后，对应应用会出现卡密验证</p>
          </div>
        </div>
      </div>

      <!-- Hook 配置列表 -->
      <div class="settings-section">
        <div class="section-header">
          <div>
            <h2 class="section-title">Hook 配置</h2>
            <p class="section-desc">配置要 Hook 的方法及返回值</p>
          </div>
          <button @click="addHookItem" class="btn-add-param">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
            </svg>
            添加 Hook
          </button>
        </div>
        <div class="section-body">
          <div v-if="hookItems.length === 0" class="empty-params">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path d="M3 12v3c0 1.657 3.134 3 7 3s7-1.343 7-3v-3c0 1.657-3.134 3-7 3s-7-1.343-7-3z" />
              <path d="M3 7v3c0 1.657 3.134 3 7 3s7-1.343 7-3V7c0 1.657-3.134 3-7 3S3 8.657 3 7z" />
              <path d="M17 5c0 1.657-3.134 3-7 3S3 6.657 3 5s3.134-3 7-3 7 1.343 7 3z" />
            </svg>
            <p>暂无 Hook 配置</p>
            <p class="empty-hint">点击"添加 Hook"按钮开始配置方法拦截</p>
          </div>
          
          <div v-else class="hook-items-list">
            <div v-for="(item, idx) in hookItems" :key="item.id" class="hook-item-card">
              <div class="hook-item-header">
                <span class="hook-item-number">#{{ idx + 1 }}</span>
                <label class="intercept-switch">
                  <input type="checkbox" v-model="item.isIntercept" />
                  <span>拦截模式</span>
                </label>
                <button @click="removeHookItem(item.id)" class="btn-remove-hook" title="删除此Hook">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
              
              <div class="hook-item-body">
                <div class="form-row">
                  <div class="form-group">
                    <label class="form-label">类名 *</label>
                    <input v-model="item.className" type="text" class="form-input" placeholder="com.example.MyClass" />
                  </div>
                  <div class="form-group">
                    <label class="form-label">方法名 *</label>
                    <input v-model="item.methodName" type="text" class="form-input" placeholder="myMethod" />
                  </div>
                </div>
                
                <div class="form-group">
                  <div class="label-with-action">
                    <label class="form-label">方法参数类型</label>
                    <button @click="addMethodParam(item)" class="btn-add-small">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                      </svg>
                      添加参数
                    </button>
                  </div>
                  <div class="param-types-list">
                    <div v-if="item.methodParamTypes.length === 0" class="empty-hint-small">
                      无参数方法
                    </div>
                    <div v-else class="type-items">
                      <div v-for="(paramType, pIdx) in item.methodParamTypes" :key="pIdx" class="type-item">
                        <span class="type-index">{{ pIdx + 1 }}.</span>
                        <input v-model="item.methodParamTypes[pIdx]" type="text" class="type-input" placeholder="String / int / com.example.MyClass" />
                        <div class="type-helper-wrapper">
                          <button @click.stop="toggleTypeHelper(item.id, 'param', pIdx)" class="btn-type-helper-small" title="选择类型">
                            <svg viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                            </svg>
                          </button>
                          <div v-if="typeHelperOpen?.hookId === item.id && typeHelperOpen?.field === 'param' && typeHelperOpen?.paramIndex === pIdx" class="type-helper-dropdown" @click.stop>
                            <div class="type-helper-title">Java 基本类型</div>
                            <div class="type-list">
                              <button 
                                v-for="type in javaBasicTypes" 
                                :key="type.value"
                                @click="selectType(type.value, item, 'param', pIdx)"
                                class="type-option"
                              >
                                <code>{{ type.label }}</code>
                              </button>
                            </div>
                          </div>
                        </div>
                        <button @click="removeMethodParam(item, pIdx)" class="btn-remove-small">
                          <svg viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div class="form-row" v-if="!item.isIntercept">
                  <div class="form-group">
                    <label class="form-label">返回值类型</label>
                    <div class="input-with-helper">
                      <input v-model="item.setResultClass" type="text" class="form-input" placeholder="String / int / void" />
                      <div class="type-helper-wrapper">
                        <button @click.stop="toggleTypeHelper(item.id, 'result')" class="btn-type-helper-inline" title="选择类型">
                          <svg viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                          </svg>
                        </button>
                        <div v-if="typeHelperOpen?.hookId === item.id && typeHelperOpen?.field === 'result'" class="type-helper-dropdown" @click.stop>
                          <div class="type-helper-title">Java 基本类型</div>
                          <div class="type-list">
                            <button 
                              v-for="type in javaBasicTypes" 
                              :key="type.value"
                              @click="selectType(type.value, item, 'result')"
                              class="type-option"
                            >
                              <code>{{ type.label }}</code>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="form-group">
                    <label class="form-label">返回值数据</label>
                    <input v-model="item.setResultData" type="text" class="form-input" placeholder="要返回的值" />
                    <div v-if="validateReturnValue(item.setResultClass, item.setResultData)" class="validation-error">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                      </svg>
                      {{ validateReturnValue(item.setResultClass, item.setResultData) }}
                    </div>
                  </div>
                </div>
                
                <div class="intercept-notice" v-if="item.isIntercept">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                  </svg>
                  拦截模式：方法将被完全拦截，返回 null
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 文件上传 -->
      <div class="settings-section">
        <div class="section-header">
          <div>
            <h2 class="section-title">Dex 配置</h2>
            <p class="section-desc">上传 Dex 文件并配置调用信息</p>
          </div>
          <div style="display: flex; gap: 0.5rem;">
            <button 
              @click="toggleDexCache" 
              class="btn-cache-toggle-large" 
              :class="{ 'active': dexConfig.useLocalCache }"
              :title="dexConfig.useLocalCache ? '已启用本地缓存' : '点击启用本地缓存'"
            >
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path d="M3 12v3c0 1.657 3.134 3 7 3s7-1.343 7-3v-3c0 1.657-3.134 3-7 3s-7-1.343-7-3z" />
                <path d="M3 7v3c0 1.657 3.134 3 7 3s7-1.343 7-3V7c0 1.657-3.134 3-7 3S3 8.657 3 7z" />
                <path d="M17 5c0 1.657-3.134 3-7 3S3 6.657 3 5s3.134-3 7-3 7 1.343 7 3z" />
              </svg>
              <span>本地缓存</span>
            </button>
            <button 
              @click="goToJavaEditor" 
              class="btn-cache-toggle-large" 
              title="在线编写 Hook 代码"
            >
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M12.316 3.051a1 1 0 01.633 1.265l-4 12a1 1 0 11-1.898-.632l4-12a1 1 0 011.265-.633zM5.707 6.293a1 1 0 010 1.414L3.414 10l2.293 2.293a1 1 0 11-1.414 1.414l-3-3a1 1 0 010-1.414l3-3a1 1 0 011.414 0zm8.586 0a1 1 0 011.414 0l3 3a1 1 0 010 1.414l-3 3a1 1 0 11-1.414-1.414L16.586 10l-2.293-2.293a1 1 0 010-1.414z" clip-rule="evenodd" />
              </svg>
              <span>在线写 Hook</span>
            </button>
          </div>
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
            <p class="form-hint">支持 .dex，最大 6MB</p>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Dex 类名</label>
              <input v-model="dexConfig.dexClassName" type="text" class="form-input" placeholder="com.example.MyHook" />
              <p class="form-hint">Dex 文件中要调用的类的完整路径</p>
            </div>
            <div class="form-group">
              <label class="form-label">Dex 方法名</label>
              <input v-model="dexConfig.dexMethodName" type="text" class="form-input" placeholder="hookMethod" />
              <p class="form-hint">要调用的静态方法名（无参数）</p>
            </div>
          </div>
          
          <div class="form-group">
            <div class="dex-param-notice">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
              </svg>
              Dex 方法必须是无参数的静态方法
            </div>
          </div>
        </div>
      </div>
      
      <!-- Zip 资源包 -->
      <div class="settings-section">
        <div class="section-header">
          <h2 class="section-title">Zip 资源包</h2>
          <p class="section-desc">上传资源包文件</p>
        </div>
        <div class="section-body">
          <div class="form-group">
            <label class="form-label">资源文件</label>
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
            <p class="form-hint">支持 .zip，最大 10MB</p>
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

/* Hook 配置列表样式 */
.hook-items-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hook-item-card {
  background: white;
  border: 2px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s ease;
}

.hook-item-card:hover {
  border-color: rgba(79, 70, 229, 0.2);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08);
}

.hook-item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.hook-item-number {
  font-size: 14px;
  font-weight: 700;
  color: var(--brand);
  background: rgba(79, 70, 229, 0.1);
  padding: 4px 10px;
  border-radius: 6px;
}

.intercept-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-2);
  flex: 1;
}

.intercept-switch input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.btn-cache-toggle-large {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  font-weight: 600;
}

.btn-cache-toggle-large:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.3);
  color: #3b82f6;
}

.btn-cache-toggle-large.active {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.4);
  color: #3b82f6;
}

.btn-cache-toggle-large svg {
  width: 16px;
  height: 16px;
}


.btn-remove-hook {
  width: 32px;
  height: 32px;
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
}

.btn-remove-hook:hover {
  background: rgba(239, 68, 68, 0.1);
  transform: scale(1.05);
}

.btn-remove-hook svg {
  width: 16px;
  height: 16px;
}

.hook-item-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.label-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.btn-add-small {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 6px;
  background: rgba(79, 70, 229, 0.05);
  color: var(--brand);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-small:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
}

.btn-add-small svg {
  width: 14px;
  height: 14px;
}

.param-types-list {
  margin-top: 8px;
}

.empty-hint-small {
  font-size: 13px;
  color: var(--text-2);
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 6px;
  text-align: center;
}

.type-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-index {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  min-width: 24px;
}

.type-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  font-size: 13px;
  font-family: 'SF Mono', 'Monaco', monospace;
}

.type-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.btn-remove-small {
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

.btn-remove-small:hover {
  background: rgba(239, 68, 68, 0.1);
}

.btn-remove-small svg {
  width: 14px;
  height: 14px;
}

.intercept-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 8px;
  color: #d97706;
  font-size: 13px;
}

.intercept-notice svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.dex-param-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 500;
}

.dex-param-notice svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* 类型助手样式 */
.type-helper-wrapper {
  position: relative;
  display: inline-flex;
}

.btn-type-helper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 6px;
  background: rgba(79, 70, 229, 0.05);
  color: var(--brand);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.btn-type-helper:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
}

.btn-type-helper svg {
  width: 16px;
  height: 16px;
}

.btn-type-helper-small {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 6px;
  background: rgba(79, 70, 229, 0.05);
  color: var(--brand);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.btn-type-helper-small:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
}

.btn-type-helper-small svg {
  width: 14px;
  height: 14px;
}

.btn-type-helper-inline {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--brand);
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.btn-type-helper-inline:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
}

.btn-type-helper-inline svg {
  width: 14px;
  height: 14px;
}

.input-with-helper {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
}

.input-with-helper .form-input {
  padding-right: 44px;
  flex: 1;
}

.type-helper-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  padding: 8px;
  z-index: 1000;
  min-width: 150px;
}

.type-helper-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  padding: 6px 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 4px;
}

.type-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.type-option {
  padding: 6px 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
}

.type-option:hover {
  background: rgba(79, 70, 229, 0.05);
  border-color: rgba(79, 70, 229, 0.3);
}

.type-option code {
  font-size: 12px;
  font-family: 'SF Mono', 'Monaco', monospace;
  color: var(--brand);
  font-weight: 600;
}

/* 验证错误提示 */
.validation-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 6px;
  color: #dc2626;
  font-size: 12px;
  margin-top: 4px;
}

.validation-error svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

@media (prefers-color-scheme: dark) {
  .validation-error {
    background: rgba(239, 68, 68, 0.15);
    border-color: rgba(239, 68, 68, 0.4);
    color: #fca5a5;
  }
}

@media (prefers-color-scheme: dark) {
  .hook-item-card {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .hook-item-card:hover {
    border-color: rgba(124, 58, 237, 0.3);
    box-shadow: 0 4px 12px rgba(124, 58, 237, 0.15);
  }
  
  .hook-item-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }
  
  .hook-item-number {
    background: rgba(124, 58, 237, 0.15);
    color: #c084fc;
  }
  
  .empty-hint-small {
    background: rgba(255, 255, 255, 0.03);
  }
  
  .type-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .intercept-notice {
    background: rgba(245, 158, 11, 0.15);
    border-color: rgba(245, 158, 11, 0.4);
    color: #fbbf24;
  }
  
  .btn-type-helper-inline {
    background: rgba(22, 22, 26, 0.95);
  }
  
  .type-helper-dropdown {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
  }
  
  .type-helper-title {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }
  
  .type-option {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .type-option:hover {
    background: rgba(124, 58, 237, 0.15);
    border-color: rgba(124, 58, 237, 0.3);
  }
  
  .type-option code {
    color: #c084fc;
  }
  
  .dex-param-notice {
    background: rgba(59, 130, 246, 0.15);
    border-color: rgba(59, 130, 246, 0.4);
    color: #60a5fa;
  }
}
</style>
