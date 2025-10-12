<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import http from '../utils/http'
// @ts-ignore
import UiPageHeader from '../components/ui/PageHeader.vue'
// @ts-ignore
import UiInput from '../components/ui/Input.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

type CardStatus = 'NEW' | 'ACTIVATED' | 'USED' | 'EXPIRED' | 'DISABLED'

type Card = {
  id: number
  appId: number
  cardCode: string
  status: CardStatus
  disabled: boolean
  expireAt?: string
  activatedAt?: string
  createdAt?: string
  maxMachines?: number
  boundMachinesCount?: number
  metadata?: string
  extra?: string
  returnExtra?: boolean
}

const list = ref<Card[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<CardStatus | ''>('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

// 生成卡密对话框
const showGenerateDialog = ref(false)
const generateForm = ref({
  prefix: '',
  suffix: '',
  count: 10,
  expireValue: null as number | null,
  expireUnit: 'DAYS',
  formatPattern: '',
  maxMachines: null as number | null
})
const generating = ref(false)

// 导入对话框
const showImportDialog = ref(false)
const importFile = ref<File | null>(null)
const importing = ref(false)

// 查看机器码对话框
const showMachinesDialog = ref(false)
const currentCardMachines = ref<string[]>([])
const loadingMachines = ref(false)
const currentCard = ref<Card | null>(null)

// 编辑附加信息对话框
const showExtraDialog = ref(false)
const extraForm = ref({
  params: [] as Array<{ id: number; key: string; value: string }>,
  returnExtra: false
})
const savingExtra = ref(false)
let nextParamId = 1

// 操作中的卡密ID集合（防止重复操作）
const operatingCards = ref<Set<number>>(new Set())

// 多选功能
const selectedIds = ref<Set<number>>(new Set())
const isAllSelected = computed(() => {
  return list.value.length > 0 && selectedIds.value.size === list.value.length
})
const isSomeSelected = computed(() => {
  return selectedIds.value.size > 0 && selectedIds.value.size < list.value.length
})

// 确认对话框
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  confirmText: '确定',
  cancelText: '取消',
  type: 'warning' as 'warning' | 'danger',
  onConfirm: () => {}
})

// Toast 提示
const toast = ref({
  show: false,
  message: '',
  type: 'success' as 'success' | 'error' | 'info'
})

// 自定义下拉框状态
const statusDropdownOpen = ref(false)
const expireUnitDropdownOpen = ref(false)

// 当前选中的状态标签
const selectedStatusLabel = computed(() => {
  return statusOptions.find(o => o.value === statusFilter.value)?.label || '全部状态'
})

// 有效期单位选项
const expireUnitOptions = [
  { value: 'MINUTES', label: '分' },
  { value: 'HOURS', label: '时' },
  { value: 'DAYS', label: '天' },
  { value: 'MONTHS', label: '月' },
  { value: 'QUARTERS', label: '季' },
  { value: 'YEARS', label: '年' }
]

const selectedExpireUnitLabel = computed(() => {
  return expireUnitOptions.find(o => o.value === generateForm.value.expireUnit)?.label || '天'
})

function selectStatus(value: any) {
  statusFilter.value = value
  statusDropdownOpen.value = false
  search()
}

function selectExpireUnit(value: string) {
  generateForm.value.expireUnit = value
  expireUnitDropdownOpen.value = false
}

// 状态选项
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'NEW', label: '未使用' },
  { value: 'ACTIVATED', label: '已激活' },
  { value: 'USED', label: '已使用' },
  { value: 'EXPIRED', label: '已过期' },
  { value: 'DISABLED', label: '已禁用' }
]

// 状态徽章颜色
const statusColors = {
  NEW: '#10b981',
  ACTIVATED: '#3b82f6',
  USED: '#8b5cf6',
  EXPIRED: '#6b7280',
  DISABLED: '#ef4444'
}

// 计算分页信息
const totalPages = computed(() => Math.ceil(total.value / size.value))
const hasNextPage = computed(() => page.value < totalPages.value)
const hasPrevPage = computed(() => page.value > 1)

async function fetchCards() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    
    const { data } = await http.get('/admin/cards', { params })
    list.value = data.cards || []
    total.value = data.total || 0
  } catch (e: any) {
    console.error('获取卡密列表失败:', e)
  } finally {
    loading.value = false
  }
}

async function generateCards() {
  generating.value = true
  try {
    const params = new URLSearchParams()
    if (generateForm.value.prefix) params.append('prefix', generateForm.value.prefix)
    if (generateForm.value.suffix) params.append('suffix', generateForm.value.suffix)
    params.append('count', generateForm.value.count.toString())
    if (generateForm.value.expireValue) {
      params.append('expireValue', generateForm.value.expireValue.toString())
      params.append('expireUnit', generateForm.value.expireUnit)
    }
    if (generateForm.value.formatPattern) params.append('formatPattern', generateForm.value.formatPattern)
    if (generateForm.value.maxMachines) params.append('maxMachines', generateForm.value.maxMachines.toString())
    
    await http.post('/admin/cards/generate', params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    
    showGenerateDialog.value = false
    resetGenerateForm()
    await fetchCards()
  } catch (e: any) {
    console.error('生成卡密失败:', e)
    alert(e.response?.data?.message || '生成卡密失败')
  } finally {
    generating.value = false
  }
}

async function handleImport() {
  if (!importFile.value) return
  
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    
    await http.post('/admin/cards/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    showImportDialog.value = false
    importFile.value = null
    await fetchCards()
  } catch (e: any) {
    console.error('导入卡密失败:', e)
    alert(e.response?.data?.message || '导入卡密失败')
  } finally {
    importing.value = false
  }
}

async function handleExport() {
  try {
    const params: any = {}
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    
    const response = await http.get('/admin/cards/export', {
      params,
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `cards-${new Date().getTime()}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    console.error('导出卡密失败:', e)
    alert('导出卡密失败')
  }
}

function showToast(message: string, type: 'success' | 'error' | 'info' = 'success') {
  toast.value.message = message
  toast.value.type = type
  toast.value.show = true
  
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

function showConfirm(title: string, message: string, onConfirm: () => void, type: 'warning' | 'danger' = 'warning') {
  confirmDialog.value = {
    show: true,
    title,
    message,
    confirmText: '确定',
    cancelText: '取消',
    type,
    onConfirm
  }
}

function closeConfirm() {
  confirmDialog.value.show = false
}

async function handleConfirm() {
  confirmDialog.value.onConfirm()
  closeConfirm()
}

async function toggleDisable(card: Card) {
  // 防止重复操作
  if (operatingCards.value.has(card.id)) {
    showToast('操作进行中，请稍候...', 'info')
    return
  }
  
  const action = card.disabled ? '启用' : '禁用'
  showConfirm(
    `${action}卡密`,
    `确定要${action}卡密 ${card.cardCode} 吗？`,
    async () => {
      operatingCards.value.add(card.id)
      try {
        await http.post(`/admin/cards/${card.id}/disable`, null, {
          params: { disabled: !card.disabled }
        })
        // 直接更新本地状态，无需重新请求列表
        card.disabled = !card.disabled
        showToast(`${action}成功`, 'success')
      } catch (e: any) {
        console.error('操作失败:', e)
        showToast(e.response?.data?.message || '操作失败', 'error')
      } finally {
        operatingCards.value.delete(card.id)
      }
    },
    'warning'
  )
}

async function deleteCard(card: Card) {
  // 防止重复操作
  if (operatingCards.value.has(card.id)) {
    showToast('操作进行中，请稍候...', 'info')
    return
  }
  
  showConfirm(
    '删除卡密',
    `确定要删除卡密 ${card.cardCode} 吗？此操作不可恢复！`,
    async () => {
      operatingCards.value.add(card.id)
      try {
        await http.post(`/admin/cards/${card.id}/delete`)
        // 直接从本地列表中移除，无需重新请求
        const index = list.value.findIndex(c => c.id === card.id)
        if (index > -1) {
          list.value.splice(index, 1)
          total.value--
        }
        showToast('删除成功', 'success')
      } catch (e: any) {
        console.error('删除失败:', e)
        showToast(e.response?.data?.message || '删除失败', 'error')
      } finally {
        operatingCards.value.delete(card.id)
      }
    },
    'danger'
  )
}

async function viewMachines(card: Card) {
  currentCard.value = card
  showMachinesDialog.value = true
  loadingMachines.value = true
  currentCardMachines.value = []
  
  try {
    const { data } = await http.get(`/admin/cards/${card.id}/machines`)
    currentCardMachines.value = data || []
  } catch (e: any) {
    console.error('获取机器码失败:', e)
    showToast('获取机器码失败', 'error')
  } finally {
    loadingMachines.value = false
  }
}

// 编辑附加信息
function editExtra(card: Card) {
  currentCard.value = card
  
  // 解析已有的附加信息
  const params = []
  
  // 先尝试从 extra 字段读取（数据库中的正式字段）
  let extraObj: any = null
  if (card.extra) {
    try {
      extraObj = JSON.parse(card.extra)
    } catch (e) {
      console.error('解析 extra 字段失败:', e)
    }
  }
  
  // 如果 extra 字段为空，尝试从 metadata 读取（向后兼容）
  if (!extraObj && card.metadata) {
    try {
      const metaData = JSON.parse(card.metadata)
      if (metaData && typeof metaData === 'object' && 'extra' in metaData) {
        extraObj = metaData.extra
      }
    } catch (e) {
      console.error('解析 metadata 字段失败:', e)
    }
  }
  
  // 填充参数列表
  if (extraObj && typeof extraObj === 'object') {
    for (const [key, value] of Object.entries(extraObj)) {
      params.push({
        id: nextParamId++,
        key,
        value: String(value)
      })
    }
  }
  
  // 设置 returnExtra 标志（从数据库字段读取）
  extraForm.value.returnExtra = card.returnExtra === true
  extraForm.value.params = params.length > 0 ? params : []
  showExtraDialog.value = true
}

// 添加参数
function addParam() {
  extraForm.value.params.push({
    id: nextParamId++,
    key: '',
    value: ''
  })
}

// 删除参数
function removeParam(index: number) {
  extraForm.value.params.splice(index, 1)
}

// 保存附加信息
async function saveExtra() {
  if (!currentCard.value) return
  
  // 构建 JSON
  const extraData: Record<string, any> = {}
  for (const param of extraForm.value.params) {
    if (param.key.trim()) {
      // 尝试推断类型
      const val = param.value.trim()
      if (val === 'true') extraData[param.key] = true
      else if (val === 'false') extraData[param.key] = false
      else if (/^-?\d+$/.test(val)) extraData[param.key] = parseInt(val, 10)
      else if (/^-?\d+\.\d+$/.test(val)) extraData[param.key] = parseFloat(val)
      else extraData[param.key] = val
    }
  }
  
  savingExtra.value = true
  try {
    await http.post(`/admin/cards/${currentCard.value.id}/extra`, {
      extra: JSON.stringify(extraData),
      returnExtra: extraForm.value.returnExtra
    })
    
    // 更新本地卡密的 extra 和 returnExtra 字段
    const card = list.value.find(c => c.id === currentCard.value?.id)
    if (card) {
      card.extra = JSON.stringify(extraData)
      card.returnExtra = extraForm.value.returnExtra
    }
    
    showToast('保存成功', 'success')
    showExtraDialog.value = false
  } catch (e: any) {
    console.error('保存失败:', e)
    showToast(e.response?.data?.message || '保存失败', 'error')
  } finally {
    savingExtra.value = false
  }
}

async function deleteMachine(machineCode: string) {
  if (!currentCard.value) return
  
  showConfirm(
    '删除机器码',
    `确定要删除机器码 "${machineCode}" 吗？`,
    async () => {
      try {
        const params = new URLSearchParams()
        params.append('machineCode', machineCode)
        await http.post(`/admin/cards/${currentCard.value!.id}/unbind`, params, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        })
        
        // 从列表中移除
        const index = currentCardMachines.value.indexOf(machineCode)
        if (index > -1) {
          currentCardMachines.value.splice(index, 1)
        }
        
        // 更新本地绑定数量
        if (currentCard.value && currentCard.value.boundMachinesCount !== undefined) {
          currentCard.value.boundMachinesCount = Math.max(0, currentCard.value.boundMachinesCount - 1)
        }
        
        showToast('删除成功', 'success')
      } catch (e: any) {
        console.error('删除失败:', e)
        showToast(e.response?.data?.message || '删除失败', 'error')
      }
    },
    'danger'
  )
}

function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  importFile.value = target.files?.[0] || null
}

function resetGenerateForm() {
  generateForm.value = {
    prefix: '',
    suffix: '',
    count: 10,
    expireValue: null,
    expireUnit: 'DAYS',
    formatPattern: '',
    maxMachines: null
  }
}

function prevPage() {
  if (hasPrevPage.value) {
    page.value--
    fetchCards()
  }
}

function nextPage() {
  if (hasNextPage.value) {
    page.value++
    fetchCards()
  }
}

function search() {
  page.value = 1
  fetchCards()
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化到期时间（永久卡显示"永久有效"）
function formatExpireDate(card: Card) {
  // 如果卡密未激活且没有到期时间
  if (!card.expireAt && card.status === 'NEW') {
    // 检查 metadata 中是否有有效期设置
    if (card.metadata) {
      try {
        const meta = JSON.parse(card.metadata)
        if (!meta.expireUnit) {
          return '永久有效'
        }
        return '待激活'
      } catch (e) {
        return '待激活'
      }
    }
    return '永久有效'
  }
  
  if (!card.expireAt) return '永久有效'
  
  // 判断是否为永久卡（2099年）
  const expireDate = new Date(card.expireAt)
  
  // 检查日期是否有效
  if (isNaN(expireDate.getTime())) {
    return '永久有效'
  }
  
  if (expireDate.getFullYear() >= 2099) {
    return '永久有效'
  }
  
  return formatDate(card.expireAt)
}

// 获取卡密类型标签
function getCardTypeLabel(card: Card) {
  // 先尝试从 metadata 中读取有效期信息（未激活的卡密）
  if (card.metadata) {
    try {
      const meta = JSON.parse(card.metadata)
      if (meta.expireUnit && meta.expireValue) {
        const unit = meta.expireUnit
        switch(unit) {
          case 'MINUTES': return '分'
          case 'HOURS': return '时'
          case 'DAYS': return '天'
          case 'MONTHS': return '月'
          case 'QUARTERS': return '季'
          case 'YEARS': return '年'
        }
      }
      // 如果 metadata 中没有有效期信息，说明是永久卡
      if (!meta.expireUnit) {
        return '永'
      }
    } catch (e) {
      // JSON 解析失败，继续使用下面的逻辑
    }
  }
  
  // 如果没有到期时间，则为永久卡
  if (!card.expireAt) return '永'
  
  const expireDate = new Date(card.expireAt)
  
  // 检查日期是否有效
  if (isNaN(expireDate.getTime())) {
    return '永'
  }
  
  // 判断是否为永久卡（2099年）
  if (expireDate.getFullYear() >= 2099) {
    return '永'
  }
  
  // 使用创建时间或激活时间作为起始时间
  const startDate = card.activatedAt ? new Date(card.activatedAt) : new Date(card.createdAt || Date.now())
  const diffMs = expireDate.getTime() - startDate.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  const diffMonths = Math.floor(diffDays / 30)
  const diffYears = Math.floor(diffDays / 365)
  
  // 判断类型（根据时长判断）
  if (diffMinutes < 60) return '分'
  if (diffHours < 24) return '时'
  if (diffDays < 30) return '天'
  if (diffMonths < 3) return '月'
  if (diffMonths >= 3 && diffMonths < 12) return '季'
  if (diffYears >= 1) return '年'
  
  return '天'
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    showToast('复制成功！', 'success')
  } catch (e) {
    console.error('复制失败:', e)
    showToast('复制失败', 'error')
  }
}

// 多选功能
function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value.clear()
  } else {
    list.value.forEach(card => selectedIds.value.add(card.id))
  }
}

function toggleSelectCard(cardId: number) {
  if (selectedIds.value.has(cardId)) {
    selectedIds.value.delete(cardId)
  } else {
    selectedIds.value.add(cardId)
  }
}

function clearSelection() {
  selectedIds.value.clear()
}

async function batchEnable() {
  if (selectedIds.value.size === 0) return
  
  showConfirm(
    '批量启用',
    `确定要启用选中的 ${selectedIds.value.size} 个卡密吗？`,
    async () => {
      const ids = Array.from(selectedIds.value)
      let successCount = 0
      
      for (const id of ids) {
        if (operatingCards.value.has(id)) continue
        
        try {
          operatingCards.value.add(id)
          const params = new URLSearchParams()
          params.append('disabled', 'false')
          await http.post(`/admin/cards/${id}/disable`, params, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
          })
          
          // 直接更新本地状态
          const card = list.value.find(c => c.id === id)
          if (card) card.disabled = false
          successCount++
        } catch (e) {
          console.error(`启用卡密 ${id} 失败:`, e)
        } finally {
          operatingCards.value.delete(id)
        }
      }
      
      clearSelection()
      showToast(`成功启用 ${successCount} 个卡密`, 'success')
    }
  )
}

async function batchDisable() {
  if (selectedIds.value.size === 0) return
  
  showConfirm(
    '批量禁用',
    `确定要禁用选中的 ${selectedIds.value.size} 个卡密吗？`,
    async () => {
      const ids = Array.from(selectedIds.value)
      let successCount = 0
      
      for (const id of ids) {
        if (operatingCards.value.has(id)) continue
        
        try {
          operatingCards.value.add(id)
          const params = new URLSearchParams()
          params.append('disabled', 'true')
          await http.post(`/admin/cards/${id}/disable`, params, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
          })
          
          // 直接更新本地状态
          const card = list.value.find(c => c.id === id)
          if (card) card.disabled = true
          successCount++
        } catch (e) {
          console.error(`禁用卡密 ${id} 失败:`, e)
        } finally {
          operatingCards.value.delete(id)
        }
      }
      
      clearSelection()
      showToast(`成功禁用 ${successCount} 个卡密`, 'success')
    }
  )
}

async function batchDelete() {
  if (selectedIds.value.size === 0) return
  
  showConfirm(
    '批量删除',
    `确定要删除选中的 ${selectedIds.value.size} 个卡密吗？此操作不可恢复！`,
    async () => {
      const ids = Array.from(selectedIds.value)
      let successCount = 0
      
      for (const id of ids) {
        if (operatingCards.value.has(id)) continue
        
        try {
          operatingCards.value.add(id)
          await http.post(`/admin/cards/${id}/delete`)
          
          // 直接从列表中移除
          const index = list.value.findIndex(c => c.id === id)
          if (index !== -1) {
            list.value.splice(index, 1)
            total.value--
          }
          successCount++
        } catch (e) {
          console.error(`删除卡密 ${id} 失败:`, e)
        } finally {
          operatingCards.value.delete(id)
        }
      }
      
      clearSelection()
      showToast(`成功删除 ${successCount} 个卡密`, 'success')
    },
    'danger'
  )
}

// 点击外部关闭下拉框
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.custom-select')) {
    statusDropdownOpen.value = false
    expireUnitDropdownOpen.value = false
  }
}

onMounted(() => {
  fetchCards()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="page">
    <UiPageHeader title="卡密管理" />
    
    <!-- 搜索和筛选栏 -->
    <div class="toolbar">
      <div class="search-bar">
        <UiInput 
          v-model="keyword" 
          placeholder="搜索卡密..." 
          @keyup.enter="search"
          class="search-input"
        />
        
        <!-- 自定义下拉框 -->
        <div class="custom-select" :class="{ 'is-open': statusDropdownOpen }">
          <button 
            class="select-trigger" 
            @click="statusDropdownOpen = !statusDropdownOpen"
            type="button"
          >
            <span class="select-label">{{ selectedStatusLabel }}</span>
            <svg class="select-arrow" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
          <transition name="dropdown">
            <div v-if="statusDropdownOpen" class="select-dropdown" @click.stop>
              <div 
                v-for="opt in statusOptions" 
                :key="opt.value" 
                class="select-option"
                :class="{ 'is-selected': statusFilter === opt.value }"
                @click="selectStatus(opt.value)"
              >
                <span>{{ opt.label }}</span>
                <svg v-if="statusFilter === opt.value" class="option-check" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
              </div>
            </div>
          </transition>
        </div>
        
        <UiButton @click="search" variant="soft">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
          </svg>
          搜索
        </UiButton>
      </div>
      
      <div class="action-bar">
        <UiButton @click="showGenerateDialog = true" variant="soft">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          生成卡密
        </UiButton>
        <UiButton @click="showImportDialog = true" variant="ghost">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd" />
          </svg>
          导入
        </UiButton>
        <UiButton @click="handleExport" variant="ghost">
          <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd" />
          </svg>
          导出
        </UiButton>
      </div>
    </div>

    <!-- 批量操作工具栏 -->
    <div v-if="selectedIds.size > 0" class="batch-toolbar">
      <div class="batch-info">
        <span class="batch-count">已选中 {{ selectedIds.size }} 项</span>
        <button class="batch-clear" @click="clearSelection">清空选择</button>
      </div>
      <div class="batch-actions">
        <button class="batch-btn batch-btn-primary" @click="batchEnable">
          <svg class="batch-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          批量启用
        </button>
        <button class="batch-btn batch-btn-warning" @click="batchDisable">
          <svg class="batch-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.368zm1.414-1.414L6.524 5.11a6 6 0 018.367 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clip-rule="evenodd" />
          </svg>
          批量禁用
        </button>
        <button class="batch-btn batch-btn-danger" @click="batchDelete">
          <svg class="batch-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          批量删除
        </button>
      </div>
    </div>

    <!-- 卡密列表 -->
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th class="checkbox-cell">
              <label class="checkbox-label">
                <input 
                  type="checkbox" 
                  class="checkbox-input"
                  :checked="isAllSelected"
                  :indeterminate.prop="isSomeSelected"
                  @change="toggleSelectAll"
                />
                <span class="checkbox-custom"></span>
              </label>
            </th>
            <th>卡密码</th>
            <th>状态</th>
            <th>激活时间</th>
            <th>到期时间</th>
            <th>创建时间</th>
            <th>机器数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <template v-if="loading">
            <tr v-for="i in 5" :key="`loading-${i}`">
              <td colspan="8">
                <div class="skeleton skeleton-rect" style="height: 20px; width: 100%;"></div>
              </td>
            </tr>
      </template>
          
          <template v-else-if="list.length === 0">
            <tr>
              <td colspan="8" class="empty-cell">
                <div class="empty-state">
                  <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                    <path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd" />
                  </svg>
                  <p class="empty-text">暂无卡密</p>
                  <p class="empty-desc muted">点击"生成卡密"按钮创建新的卡密</p>
                </div>
              </td>
        </tr>
      </template>
          
          <template v-else>
            <tr v-for="card in list" :key="card.id" class="data-row" :class="{ 'row-selected': selectedIds.has(card.id) }">
            <td class="checkbox-cell">
              <label class="checkbox-label">
                <input 
                  type="checkbox" 
                  class="checkbox-input"
                  :checked="selectedIds.has(card.id)"
                  @change="toggleSelectCard(card.id)"
                />
                <span class="checkbox-custom"></span>
              </label>
            </td>
            <td class="code-cell">
              <div class="code-wrapper">
                <code>{{ card.cardCode }}</code>
                <span v-if="getCardTypeLabel(card)" class="card-type-badge">
                  {{ getCardTypeLabel(card) }}
                </span>
                <button class="copy-btn" @click="copyToClipboard(card.cardCode)" title="复制">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                    <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                  </svg>
                </button>
    </div>
            </td>
            <td>
              <span class="status-badge" :style="{ backgroundColor: statusColors[card.status] }">
                {{ statusOptions.find(o => o.value === card.status)?.label }}
              </span>
            </td>
            <td>{{ formatDate(card.activatedAt) }}</td>
            <td>{{ formatExpireDate(card) }}</td>
            <td>{{ formatDate(card.createdAt) }}</td>
            <td>
              <span 
                v-if="card.maxMachines" 
                class="machine-count clickable"
                @click="viewMachines(card)"
                :title="card.status !== 'NEW' ? '点击查看绑定的机器码' : '未激活'"
              >
                {{ card.boundMachinesCount || 0 }}/{{ card.maxMachines }}
              </span>
              <span v-else>不限</span>
            </td>
            <td class="action-cell">
              <div class="action-buttons">
                <button 
                  @click="viewMachines(card)" 
                  class="action-btn" 
                  :disabled="operatingCards.has(card.id)"
                  :title="card.status !== 'NEW' ? '查看机器' : '查看机器（未激活）'"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2h-2.22l.123.489.804.804A1 1 0 0113 18H7a1 1 0 01-.707-1.707l.804-.804L7.22 15H5a2 2 0 01-2-2V5zm5.771 7H5V5h10v7H8.771z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button 
                  @click="editExtra(card)" 
                  class="action-btn" 
                  :disabled="operatingCards.has(card.id)"
                  title="附加信息"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button 
                  @click="toggleDisable(card)" 
                  class="action-btn" 
                  :class="{ danger: !card.disabled, 'is-loading': operatingCards.has(card.id) }" 
                  :disabled="operatingCards.has(card.id)"
                  :title="card.disabled ? '启用' : '禁用'"
                >
                  <svg v-if="operatingCards.has(card.id)" class="loading-spinner" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                    <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
                  </svg>
                  <svg v-else-if="card.disabled" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                  <svg v-else viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.368zm1.414-1.414L6.524 5.11a6 6 0 018.367 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button 
                  @click="deleteCard(card)" 
                  class="action-btn danger" 
                  :disabled="operatingCards.has(card.id)"
                  title="删除"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </td>
      </tr>
          </template>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <span class="pagination-info">
        共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页
      </span>
      <div class="pagination-btns">
        <button @click="prevPage" :disabled="!hasPrevPage" class="page-btn">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
          </svg>
          上一页
        </button>
        <button @click="nextPage" :disabled="!hasNextPage" class="page-btn">
          下一页
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 生成卡密对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showGenerateDialog" class="modal-overlay" @click="showGenerateDialog = false">
          <div class="modal-container modal-lg" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">生成卡密</h3>
              <button class="modal-close" @click="showGenerateDialog = false">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="form-row-3">
                <div class="form-group">
                  <label class="form-label">数量</label>
                  <input v-model.number="generateForm.count" type="number" class="form-input" min="1" max="1000" />
                </div>
                <div class="form-group">
                  <label class="form-label">前缀</label>
                  <input v-model="generateForm.prefix" type="text" class="form-input" placeholder="可选" />
                </div>
                <div class="form-group">
                  <label class="form-label">后缀</label>
                  <input v-model="generateForm.suffix" type="text" class="form-input" placeholder="可选" />
                </div>
              </div>
              
              <div class="form-row-2">
                <div class="form-group">
                  <label class="form-label">有效期数值</label>
                  <input v-model.number="generateForm.expireValue" type="number" class="form-input" placeholder="可选，留空则永久有效" />
                </div>
                <div class="form-group">
                  <label class="form-label">单位</label>
                  <div class="custom-select" :class="{ 'is-open': expireUnitDropdownOpen }">
                    <button 
                      class="select-trigger" 
                      @click="expireUnitDropdownOpen = !expireUnitDropdownOpen"
                      type="button"
                    >
                      <span class="select-label">{{ selectedExpireUnitLabel }}</span>
                      <svg class="select-arrow" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                      </svg>
                    </button>
                    <transition name="dropdown">
                      <div v-if="expireUnitDropdownOpen" class="select-dropdown" @click.stop>
                        <div 
                          v-for="opt in expireUnitOptions" 
                          :key="opt.value" 
                          class="select-option"
                          :class="{ 'is-selected': generateForm.expireUnit === opt.value }"
                          @click="selectExpireUnit(opt.value)"
                        >
                          <span>{{ opt.label }}</span>
                          <svg v-if="generateForm.expireUnit === opt.value" class="option-check" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                          </svg>
                        </div>
                      </div>
                    </transition>
                  </div>
                </div>
              </div>
              
              <div class="form-group">
                <label class="form-label">最大机器数</label>
                <input v-model.number="generateForm.maxMachines" type="number" class="form-input" placeholder="可选，留空则不限制" />
              </div>
              
              <div class="form-group">
                <label class="form-label">格式模式</label>
                <input v-model="generateForm.formatPattern" type="text" class="form-input" placeholder="可选，如：XXXX-XXXX-XXXX" />
                <p class="form-hint">留空则自动生成 UUID 格式</p>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showGenerateDialog = false">取消</button>
              <UiButton @click="generateCards" :loading="generating" :disabled="generating">
                生成
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 导入对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showImportDialog" class="modal-overlay" @click="showImportDialog = false">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">导入卡密</h3>
              <button class="modal-close" @click="showImportDialog = false">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">选择 CSV 文件</label>
                <input type="file" accept=".csv" @change="onFileChange" class="file-input" />
                <p class="form-hint">CSV 格式：cardCode,expireAt,maxMachines</p>
              </div>
              <div v-if="importFile" class="file-info">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z" clip-rule="evenodd" />
                </svg>
                <span>{{ importFile.name }}</span>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showImportDialog = false">取消</button>
              <UiButton @click="handleImport" :loading="importing" :disabled="!importFile || importing">
                导入
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 查看机器码对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showMachinesDialog" class="modal-overlay" @click="showMachinesDialog = false">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">绑定的机器码</h3>
              <button class="modal-close" @click="showMachinesDialog = false">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div v-if="currentCard" class="machine-info">
                <div class="info-item">
                  <span class="info-label">卡密码：</span>
                  <code class="info-value">{{ currentCard.cardCode }}</code>
                </div>
                <div class="info-item">
                  <span class="info-label">机器数：</span>
                  <span class="info-value">{{ currentCardMachines.length }}/{{ currentCard.maxMachines || '不限' }}</span>
                </div>
              </div>
              
              <div v-if="loadingMachines" class="machines-loading">
                <div class="loading-spinner-wrapper">
                  <svg class="loading-spinner" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                    <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
                  </svg>
                  <p>加载中...</p>
                </div>
              </div>
              <div v-else-if="currentCardMachines.length === 0" class="machines-empty">
                <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2h-2.22l.123.489.804.804A1 1 0 0113 18H7a1 1 0 01-.707-1.707l.804-.804L7.22 15H5a2 2 0 01-2-2V5zm5.771 7H5V5h10v7H8.771z" clip-rule="evenodd" />
                </svg>
                <p>暂无绑定的机器码</p>
              </div>
              <div v-else class="machines-list">
                <div v-for="(machine, idx) in currentCardMachines" :key="idx" class="machine-item">
                  <div class="machine-code-wrapper">
                    <svg class="machine-icon" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v8a2 2 0 01-2 2h-2.22l.123.489.804.804A1 1 0 0113 18H7a1 1 0 01-.707-1.707l.804-.804L7.22 15H5a2 2 0 01-2-2V5zm5.771 7H5V5h10v7H8.771z" clip-rule="evenodd" />
                    </svg>
                    <code class="machine-code">{{ machine }}</code>
                  </div>
                  <div class="machine-actions">
                    <button class="machine-btn machine-btn-copy" @click="copyToClipboard(machine)" title="复制">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                        <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                      </svg>
                    </button>
                    <button class="machine-btn machine-btn-delete" @click="deleteMachine(machine)" title="删除">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showMachinesDialog = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 编辑附加信息对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showExtraDialog" class="modal-overlay" @click="showExtraDialog = false">
          <div class="modal-container modal-lg" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">编辑附加信息</h3>
              <button class="modal-close" @click="showExtraDialog = false">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <p class="modal-desc">配置卡密验证成功后返回的附加信息（键值对形式）</p>
              
              <!-- 返回开关 -->
              <div class="extra-switch-container">
                <label class="extra-switch-label">
                  <input 
                    type="checkbox" 
                    v-model="extraForm.returnExtra" 
                    class="extra-switch-input"
                  />
                  <span class="extra-switch-slider"></span>
                  <span class="extra-switch-text">验证成功时返回附加信息</span>
                </label>
              </div>
              
              <!-- 参数列表 -->
              <div class="extra-params-header">
                <h4>参数列表</h4>
                <button @click="addParam" class="btn-add-param">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                  </svg>
                  添加参数
                </button>
              </div>
              
              <div v-if="extraForm.params.length === 0" class="extra-empty">
                <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                </svg>
                <p>暂无参数，点击上方"添加参数"按钮开始配置</p>
              </div>
              
              <div v-else class="extra-params-list">
                <transition-group name="param-list">
                  <div v-for="(param, index) in extraForm.params" :key="param.id" class="extra-param-item">
                    <div class="param-index">{{ index + 1 }}</div>
                    <div class="param-fields">
                      <div class="param-field">
                        <label class="field-label">键名（Key）</label>
                        <input 
                          v-model="param.key" 
                          type="text" 
                          class="field-input" 
                          placeholder="例如: vip_level"
                        />
                      </div>
                      <div class="param-field">
                        <label class="field-label">值（Value）</label>
                        <input 
                          v-model="param.value" 
                          type="text" 
                          class="field-input" 
                          placeholder="例如: 3 或 true 或 文本"
                        />
                      </div>
                    </div>
                    <button @click="removeParam(index)" class="param-delete" title="删除">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                      </svg>
                    </button>
                  </div>
                </transition-group>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showExtraDialog = false">取消</button>
              <button class="btn btn-primary" @click="saveExtra" :disabled="savingExtra">
                <svg v-if="savingExtra" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                  <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
                </svg>
                <span v-else>保存</span>
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 确认对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="confirmDialog.show" class="modal-overlay" @click="closeConfirm">
          <div class="confirm-dialog" :class="`confirm-${confirmDialog.type}`" @click.stop>
            <div class="confirm-icon">
              <svg v-if="confirmDialog.type === 'warning'" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
              <svg v-else viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
            </div>
            <h3 class="confirm-title">{{ confirmDialog.title }}</h3>
            <p class="confirm-message">{{ confirmDialog.message }}</p>
            <div class="confirm-actions">
              <button class="btn btn-secondary" @click="closeConfirm">
                {{ confirmDialog.cancelText }}
              </button>
              <button 
                class="btn" 
                :class="confirmDialog.type === 'danger' ? 'btn-danger' : 'btn-primary'"
                @click="handleConfirm"
              >
                {{ confirmDialog.confirmText }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- Toast 提示 -->
    <teleport to="body">
      <transition name="toast-fade">
        <div v-if="toast.show" class="toast-container">
          <div class="toast" :class="`toast-${toast.type}`">
            <div class="toast-icon">
              <svg v-if="toast.type === 'success'" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              <svg v-else-if="toast.type === 'error'" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
              <svg v-else viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
              </svg>
            </div>
            <span class="toast-message">{{ toast.message }}</span>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<style scoped>
/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.search-input {
  flex: 1;
  max-width: 280px;
}

/* 自定义下拉框 */
.custom-select {
  position: relative;
  min-width: 140px;
}

.select-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-1);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-sizing: border-box;
  height: 42px;
}

.select-trigger:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.1);
}

.custom-select.is-open .select-trigger {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.select-label {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.select-arrow {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--text-2);
  transition: all 0.2s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.custom-select.is-open .select-arrow {
  transform: rotate(180deg);
  color: var(--brand);
}

.select-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  z-index: 300;
  overflow: hidden;
  max-height: 200px;
  overflow-y: auto;
  /* 提升动画性能 */
  will-change: transform, opacity;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.select-dropdown::-webkit-scrollbar {
  width: 6px;
}

.select-dropdown::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.select-dropdown::-webkit-scrollbar-thumb {
  background: rgba(79, 70, 229, 0.3);
  border-radius: 3px;
}

.select-dropdown::-webkit-scrollbar-thumb:hover {
  background: rgba(79, 70, 229, 0.5);
}

.select-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 14px;
  color: var(--text-1);
}

.select-option:hover {
  background: rgba(79, 70, 229, 0.05);
}

.select-option.is-selected {
  background: rgba(79, 70, 229, 0.1);
  color: var(--brand);
  font-weight: 600;
}

.option-check {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--brand);
}

/* 下拉动画 */
.dropdown-enter-active {
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.15s ease-out;
}

.dropdown-leave-active {
  transition: transform 0.15s ease-in, opacity 0.12s ease-in;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.97);
}

.action-bar {
  display: flex;
  gap: 8px;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

/* 批量操作工具栏 */
.batch-toolbar {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.08) 0%, rgba(79, 70, 229, 0.08) 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.batch-count {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.batch-clear {
  padding: 4px 12px;
  font-size: 13px;
  color: var(--text-2);
  background: transparent;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.batch-clear:hover {
  background: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.15);
  color: var(--text-1);
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.batch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.batch-icon {
  width: 16px;
  height: 16px;
}

.batch-btn-primary {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.batch-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.batch-btn-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.batch-btn-warning:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.batch-btn-danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.batch-btn-danger:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

/* 复选框样式 */
.checkbox-cell {
  width: 48px;
  padding: 12px !important;
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.checkbox-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.checkbox-custom {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(124, 58, 237, 0.3);
  border-radius: 4px;
  background: white;
  transition: all 0.2s ease;
  position: relative;
}

.checkbox-input:checked + .checkbox-custom {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  border-color: #7c3aed;
}

.checkbox-input:checked + .checkbox-custom::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-input:indeterminate + .checkbox-custom {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  border-color: #7c3aed;
}

.checkbox-input:indeterminate + .checkbox-custom::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 7px;
  width: 8px;
  height: 2px;
  background: white;
}

.checkbox-label:hover .checkbox-custom {
  border-color: #7c3aed;
  transform: scale(1.05);
}

/* 选中行高亮 */
.data-row.row-selected {
  background: rgba(124, 58, 237, 0.06) !important;
}

.data-row.row-selected:hover {
  background: rgba(124, 58, 237, 0.08) !important;
}

/* 表格容器 */
.table-container {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: rgba(79, 70, 229, 0.05);
  border-bottom: 2px solid rgba(79, 70, 229, 0.1);
}

.data-table th {
  padding: 14px 16px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  white-space: nowrap;
}

.data-table tbody tr {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.data-table tbody tr:last-child {
  border-bottom: none;
}

.data-table tbody tr.data-row:hover {
  background: rgba(79, 70, 229, 0.03);
}

.data-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: var(--text-1);
  vertical-align: middle;
  text-align: center;
  white-space: nowrap;
}

.empty-cell {
  padding: 0 !important;
}

.code-cell {
  font-family: 'Monaco', 'Menlo', monospace;
  max-width: 300px;
}

.code-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

.code-wrapper::-webkit-scrollbar {
  height: 4px;
}

.code-wrapper::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 2px;
}

.code-wrapper::-webkit-scrollbar-thumb {
  background: rgba(79, 70, 229, 0.3);
  border-radius: 2px;
}

.code-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(79, 70, 229, 0.5);
}

.code-wrapper code {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  background: rgba(79, 70, 229, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
}

.card-type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: white;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(245, 158, 11, 0.3);
  position: relative;
  top: -1px;
}

.copy-btn {
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  outline: none;
}

.copy-btn:hover {
  color: var(--brand);
}

.copy-btn:focus {
  outline: none;
}

.copy-btn svg {
  width: 100%;
  height: 100%;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  white-space: nowrap;
}

.action-cell {
  width: 120px;
}

.action-buttons {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-1);
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
  color: var(--brand);
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #dc2626;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.is-loading {
  pointer-events: none;
}

.loading-spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
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
  margin: 0;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.pagination-info {
  font-size: 14px;
  color: var(--text-2);
}

.pagination-btns {
  display: flex;
  gap: 8px;
}

.page-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--text-1);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background: rgba(79, 70, 229, 0.1);
  border-color: rgba(79, 70, 229, 0.3);
  color: var(--brand);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn svg {
  width: 16px;
  height: 16px;
}

/* 对话框 */
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
  /* 提升性能 */
  will-change: opacity;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.modal-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
  /* 提升动画性能 */
  will-change: transform, opacity;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  transform: translateZ(0);
}

.modal-lg {
  max-width: 680px;
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
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-1);
}

.modal-close svg {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: 24px 28px;
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: hidden;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.form-row-flex {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  align-items: flex-end;
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

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
  box-sizing: border-box;
  height: 42px;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.8);
  transition: all 0.2s ease;
  box-sizing: border-box;
  min-height: 100px;
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

.file-input {
  width: 100%;
  padding: 10px;
  border: 2px dashed rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-input:hover {
  border-color: var(--brand);
  background: rgba(79, 70, 229, 0.05);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(79, 70, 229, 0.1);
  border-radius: 8px;
  margin-top: 12px;
  color: var(--brand);
  font-size: 14px;
}

.file-info svg {
  width: 20px;
  height: 20px;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #5568d3 0%, #63408b 100%);
}

.btn-danger {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.btn-danger:hover {
  background: linear-gradient(135deg, #e082ea 0%, #e4465b 100%);
}

/* 机器码列表 */
.machines-loading,
.machines-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-2);
}

.machines-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.machine-item {
  padding: 10px 12px;
  background: rgba(79, 70, 229, 0.05);
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 8px;
}

.machine-item code {
  font-size: 13px;
  color: var(--brand);
  font-family: 'Monaco', 'Menlo', monospace;
}

/* 确认对话框 */
.confirm-dialog {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 32px;
  max-width: 420px;
  width: calc(100% - 40px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
  position: relative;
  overflow: hidden;
  /* 提升动画性能 */
  will-change: transform, opacity;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  transform: translateZ(0);
}

.confirm-dialog::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.confirm-dialog.confirm-danger::before {
  background: linear-gradient(90deg, #f093fb 0%, #f5576c 100%);
}

.confirm-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
}

.confirm-dialog.confirm-danger .confirm-icon {
  background: linear-gradient(135deg, rgba(240, 147, 251, 0.15) 0%, rgba(245, 87, 108, 0.15) 100%);
}

.confirm-icon svg {
  width: 36px;
  height: 36px;
  color: #667eea;
}

.confirm-dialog.confirm-danger .confirm-icon svg {
  color: #f5576c;
}

.confirm-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-1);
  margin: 0 0 12px 0;
}

.confirm-message {
  font-size: 15px;
  color: var(--text-2);
  margin: 0 0 28px 0;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-actions .btn {
  flex: 1;
  max-width: 140px;
}

/* Toast 提示 */
.toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 10000;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.08);
  min-width: 280px;
  pointer-events: auto;
}

.toast-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-success .toast-icon {
  color: #10b981;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

.toast-icon svg {
  width: 100%;
  height: 100%;
}

.toast-message {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

/* 对话框动画 */
.modal-fade-enter-active {
  transition: opacity 0.2s ease-out;
}

.modal-fade-leave-active {
  transition: opacity 0.15s ease-in;
}

.modal-fade-enter-active .modal-container,
.modal-fade-enter-active .confirm-dialog {
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease-out;
}

.modal-fade-leave-active .modal-container,
.modal-fade-leave-active .confirm-dialog {
  transition: transform 0.15s ease-in, opacity 0.15s ease-in;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-container,
.modal-fade-enter-from .confirm-dialog {
  opacity: 0;
  transform: scale(0.95) translateY(10px);
}

.modal-fade-leave-to .modal-container,
.modal-fade-leave-to .confirm-dialog {
  opacity: 0;
  transform: scale(0.98) translateY(-5px);
}

/* Toast 动画 */
.toast-fade-enter-active {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.2s ease-out;
}

.toast-fade-leave-active {
  transition: transform 0.2s ease-in, opacity 0.15s ease-in;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(50px) scale(0.9);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(30px) scale(0.95);
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .batch-toolbar {
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.15) 0%, rgba(79, 70, 229, 0.15) 100%);
    border-color: rgba(124, 58, 237, 0.3);
  }
  
  .batch-clear {
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .batch-clear:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.3);
  }
  
  .checkbox-custom {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(124, 58, 237, 0.5);
  }
  
  .data-row.row-selected {
    background: rgba(124, 58, 237, 0.12) !important;
  }
  
  .data-row.row-selected:hover {
    background: rgba(124, 58, 237, 0.15) !important;
  }
  
  .table-container {
    background: rgba(22, 22, 26, 0.6);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .data-table thead {
    background: rgba(124, 58, 237, 0.15);
    border-bottom-color: rgba(124, 58, 237, 0.3);
  }
  
  .data-table tbody tr {
    border-bottom-color: rgba(255, 255, 255, 0.05);
  }
  
  .data-table tbody tr.data-row:hover {
    background: rgba(124, 58, 237, 0.1);
  }
  
  .code-wrapper code {
    background: rgba(124, 58, 237, 0.2);
    color: #c084fc;
  }
  
  .code-wrapper::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .code-wrapper::-webkit-scrollbar-thumb {
    background: rgba(124, 58, 237, 0.4);
  }
  
  .code-wrapper::-webkit-scrollbar-thumb:hover {
    background: rgba(124, 58, 237, 0.6);
  }
  
  .select-dropdown::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .select-dropdown::-webkit-scrollbar-thumb {
    background: rgba(124, 58, 237, 0.4);
  }
  
  .select-dropdown::-webkit-scrollbar-thumb:hover {
    background: rgba(124, 58, 237, 0.6);
  }
  
  .select-trigger {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .select-trigger:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
    box-shadow: 0 2px 8px rgba(124, 58, 237, 0.15);
  }
  
  .custom-select.is-open .select-trigger {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(124, 58, 237, 0.6);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
  }
  
  .select-arrow {
    color: rgba(255, 255, 255, 0.5);
  }
  
  .custom-select.is-open .select-arrow {
    color: #c084fc;
  }
  
  .select-dropdown {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  }
  
  .select-option {
    color: var(--text-1-dark);
  }
  
  .select-option:hover {
    background: rgba(124, 58, 237, 0.15);
  }
  
  .select-option.is-selected {
    background: rgba(124, 58, 237, 0.2);
    color: #c084fc;
  }
  
  .form-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .form-input:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .form-input:focus {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(124, 58, 237, 0.6);
    box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
  }
  
  .action-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .action-btn:hover {
    background: rgba(124, 58, 237, 0.2);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .pagination {
    background: rgba(22, 22, 26, 0.6);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .page-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .page-btn:hover:not(:disabled) {
    background: rgba(124, 58, 237, 0.2);
    border-color: rgba(124, 58, 237, 0.4);
  }
  
  .modal-container {
    background: rgba(22, 22, 26, 0.95);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .modal-header,
  .modal-footer {
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .modal-close:hover {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .file-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.2);
  }
  
  .file-input:hover {
    border-color: rgba(124, 58, 237, 0.5);
    background: rgba(124, 58, 237, 0.1);
  }
  
  .machine-item {
    background: rgba(124, 58, 237, 0.15);
    border-color: rgba(124, 58, 237, 0.3);
  }
  
  .machine-item code {
    color: #c084fc;
  }
  
  .btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .btn-secondary:hover {
    background: rgba(255, 255, 255, 0.15);
  }
  
  .confirm-dialog {
    background: rgba(22, 22, 26, 0.98);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
  
  .toast {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.1);
  }
}

/* 响应式 */
@media (max-width: 1024px) {
  .table-container {
    overflow-x: auto;
  }
  
  .data-table {
    min-width: 900px;
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-bar {
    min-width: auto;
  }
  
  .action-bar {
    justify-content: space-between;
  }
  
  .data-table {
    min-width: 800px;
  }
  
  .data-table th,
  .data-table td {
    padding: 10px 12px;
    font-size: 13px;
  }
  
  .form-row-3 {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .form-row-2 {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .form-row-flex {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .form-row-flex .form-group {
    flex: 1 !important;
    margin-bottom: 0;
  }
  
  .modal-body {
    padding: 20px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 12px;
  }
}

/* 机器数可点击样式 */
.machine-count {
  font-weight: 600;
  color: var(--brand);
  transition: all 0.2s ease;
}

.clickable {
  cursor: pointer;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 3px;
}

.clickable:hover {
  color: #6d28d9;
  text-decoration-style: solid;
}

/* 机器码信息区 */
.machine-info {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(79, 70, 229, 0.05) 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
}

.info-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

/* 机器码列表 */
.machines-loading {
  padding: 40px 0;
  text-align: center;
}

.loading-spinner-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-spinner-wrapper .loading-spinner {
  width: 32px;
  height: 32px;
  color: var(--brand);
  animation: spin 1s linear infinite;
}

.loading-spinner-wrapper p {
  font-size: 14px;
  color: var(--text-2);
  margin: 0;
}

.machines-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  text-align: center;
}

.machines-empty .empty-icon {
  width: 48px;
  height: 48px;
  color: var(--text-2);
  opacity: 0.5;
  margin-bottom: 12px;
}

.machines-empty p {
  font-size: 14px;
  color: var(--text-2);
  margin: 0;
}

.machines-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.machine-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.machine-item:hover {
  background: rgba(124, 58, 237, 0.05);
  border-color: rgba(124, 58, 237, 0.2);
  transform: translateX(4px);
}

.machine-code-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.machine-icon {
  width: 20px;
  height: 20px;
  color: var(--brand);
  flex-shrink: 0;
}

.machine-code {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  background: rgba(124, 58, 237, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.machine-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.machine-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.2s ease;
}

.machine-btn svg {
  width: 16px;
  height: 16px;
}

.machine-btn-copy:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  transform: scale(1.05);
}

.machine-btn-delete:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  transform: scale(1.05);
}

/* 附加信息对话框样式 */
.modal-desc {
  font-size: 14px;
  color: var(--text-2);
  margin-bottom: 20px;
  line-height: 1.6;
}

.extra-switch-container {
  margin-bottom: 24px;
  padding: 16px;
  background: rgba(79, 70, 229, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(79, 70, 229, 0.1);
}

.extra-switch-label {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.extra-switch-input {
  display: none;
}

.extra-switch-slider {
  position: relative;
  width: 48px;
  height: 26px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 13px;
  transition: all 0.3s ease;
}

.extra-switch-slider::before {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.extra-switch-input:checked + .extra-switch-slider {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
}

.extra-switch-input:checked + .extra-switch-slider::before {
  transform: translateX(22px);
}

.extra-switch-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

.extra-params-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.extra-params-header h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0;
}

.btn-add-param {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-param svg {
  width: 16px;
  height: 16px;
}

.btn-add-param:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.extra-empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-2);
}

.extra-empty .empty-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  opacity: 0.3;
}

.extra-empty p {
  font-size: 14px;
  margin: 0;
}

.extra-params-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.extra-param-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.extra-param-item:hover {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(79, 70, 229, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.param-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: white;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  /* 对齐到输入框中心位置（标签13px + gap6px + (输入框42px - 图标32px)/2） */
  margin-top: 29px;
}

.param-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  flex: 1;
}

.param-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
}

.field-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.field-input:focus {
  outline: none;
  border-color: #4f46e5;
  background: white;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.field-input::placeholder {
  color: rgba(0, 0, 0, 0.3);
}

.param-delete {
  width: 32px;
  height: 32px;
  padding: 0;
  background: rgba(239, 68, 68, 0.1);
  border: none;
  border-radius: 8px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 对齐到输入框中心位置（标签13px + gap6px + (输入框42px - 按钮32px)/2） */
  margin-top: 29px;
}

.param-delete svg {
  width: 18px;
  height: 18px;
}

.param-delete:hover {
  background: #ef4444;
  color: white;
  transform: scale(1.05);
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
  transform: translateX(20px);
}

.param-list-move {
  transition: transform 0.3s ease;
}
</style>

