<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
// @ts-ignore
import UiCard from '../components/ui/Card.vue'
// @ts-ignore
import UiPageHeader from '../components/ui/PageHeader.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

type Application = {
  id: number
  name: string
  description?: string
  icon?: string
  apiKey?: string
  secretKey?: string
  secure?: boolean
  encryptionAlg?: string
}

type User = {
  id: number
  username: string
  enabled: boolean
  createdAt: string
  canInvite: boolean
  inviteQuota: number
  usedQuota: number
}

const loading = ref(false)
const apps = ref<Application[]>([])
const error = ref('')
const router = useRouter()
const selectedAppId = ref<number | null>(null)
const showCreateDialog = ref(false)
const newAppName = ref('')
const newAppDescription = ref('')
const creating = ref(false)
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref<'success' | 'error'>('success')
const deletingAppId = ref<number | null>(null)
const showDeleteDialog = ref(false)
const appToDelete = ref<Application | null>(null)
const showSecurityDialog = ref(false)
const securityForm = ref({
  appId: null as number | null,
  encryptionAlg: 'RC4',
  secretKey: ''
})
const savingSecurity = ref(false)
const encAlgDropdownOpen = ref(false)

// 当前用户信息
const currentUser = ref<string | null>(null)
const isAdmin = ref(false)

// 邀请码对话框
const inviteDialog = ref({
  show: false,
  loading: false,
  code: '',
  count: 1,
  canInvite: false,
  inviteQuota: 0
})
const inviteList = ref<any[]>([])

// 用户管理对话框
const userManageDialog = ref({
  show: false,
  loading: false
})
const userList = ref<User[]>([])
const editingUser = ref<User | null>(null)

// 个人信息对话框
const profileDialog = ref({
  show: false,
  loading: false
})
type Profile = {
  username: string
  createdAt: string
  canInvite: boolean
  inviteQuota: number
  usedInviteQuota: number
  remainingQuota: number
  appCount: number
  cardCount: number
}
const profile = ref<Profile | null>(null)

// 修改密码对话框
const changePasswordDialog = ref({
  show: false,
  loading: false,
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  message: '',
  messageType: '' as 'success' | 'error' | ''
})

const encAlgOptions = [
  { value: 'RC4', label: 'RC4' },
  { value: 'AES-128-CBC', label: 'AES-128-CBC' },
  { value: 'AES-256-CBC', label: 'AES-256-CBC' }
]

async function fetchApps() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/admin/apps/api/my-apps')
    apps.value = data || []
  } catch (e: any) {
    console.error('获取应用列表失败:', e)
    if (e.response?.status === 401) {
      // 未登录，跳转到登录页
    router.push('/login')
    } else {
      error.value = e.response?.data?.message || e.message || '加载失败'
    }
  } finally {
    loading.value = false
  }
}

async function selectApp(app: Application) {
  try {
    selectedAppId.value = app.id
    // 调用后端选择应用接口
    await http.post('/admin/apps/select', null, {
      params: { appId: app.id }
    })
    // 跳转到卡密页面
    router.push('/cards')
  } catch (e: any) {
    console.error('选择应用失败:', e)
    selectedAppId.value = null
    error.value = e.response?.data?.message || e.message || '选择应用失败'
  }
}

async function createApp() {
  if (!newAppName.value.trim()) {
    return
  }
  
  creating.value = true
  try {
    const formData = new FormData()
    formData.append('name', newAppName.value.trim())
    if (newAppDescription.value.trim()) {
      formData.append('description', newAppDescription.value.trim())
    }
    
    await http.post('/admin/apps', formData, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    })
    
    // 关闭对话框并重新加载列表
    showCreateDialog.value = false
    newAppName.value = ''
    newAppDescription.value = ''
    await fetchApps()
  } catch (e: any) {
    console.error('创建应用失败:', e)
    error.value = e.response?.data?.message || e.message || '创建应用失败'
  } finally {
    creating.value = false
  }
}

function openCreateDialog() {
  showCreateDialog.value = true
  newAppName.value = ''
  newAppDescription.value = ''
}

function closeCreateDialog() {
  showCreateDialog.value = false
  newAppName.value = ''
  newAppDescription.value = ''
}

function copyToClipboard(text: string, label: string) {
  navigator.clipboard.writeText(text).then(() => {
    toastMessage.value = `${label} 已复制到剪贴板`
    toastType.value = 'success'
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 2000)
  }).catch(err => {
    console.error('复制失败:', err)
    toastMessage.value = '复制失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 2000)
  })
}

function openDeleteDialog(app: Application, event: Event) {
  event.stopPropagation()
  appToDelete.value = app
  showDeleteDialog.value = true
}

function closeDeleteDialog() {
  showDeleteDialog.value = false
  appToDelete.value = null
}

async function confirmDelete() {
  if (!appToDelete.value) return
  
  deletingAppId.value = appToDelete.value.id
  try {
    await http.delete(`/admin/apps/${appToDelete.value.id}`)
    toastMessage.value = '应用已删除'
    toastType.value = 'success'
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 2000)
    
    closeDeleteDialog()
    await fetchApps()
  } catch (e: any) {
    console.error('删除应用失败:', e)
    error.value = e.response?.data?.message || e.message || '删除应用失败'
  } finally {
    deletingAppId.value = null
  }
}

function goToSettings(app: Application, event: Event) {
  event.stopPropagation()
  // 先选择应用
  selectedAppId.value = app.id
  http.post('/admin/apps/select', null, {
    params: { appId: app.id }
  }).then(() => {
    // 跳转到应用设置页面
    router.push('/app-settings')
  }).catch(e => {
    console.error('选择应用失败:', e)
    error.value = e.response?.data?.message || e.message || '选择应用失败'
  })
}

async function openInviteDialog() {
  // 先检查权限
  try {
    const { data } = await http.get('/admin/invites')
    if (data.success) {
      // 有权限，打开对话框
      inviteDialog.value.show = true
      inviteDialog.value.code = ''
      inviteList.value = data.list || []
    } else {
      // 无权限，显示Toast提示
      toastMessage.value = data.message || '无权限'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('检查权限失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '无权限'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  }
}

function closeInviteDialog() {
  inviteDialog.value.show = false
}

async function generateInvite() {
  inviteDialog.value.loading = true
  try {
    const params = new URLSearchParams()
    params.append('count', String(inviteDialog.value.count || 1))
    params.append('canInvite', String(inviteDialog.value.canInvite))
    params.append('inviteQuota', String(inviteDialog.value.inviteQuota))
    
    const { data } = await http.post('/admin/invites/generate-batch?' + params.toString())
    if (data.success) {
      await fetchInviteList()
      toastMessage.value = `成功生成 ${data.count} 个邀请码`
      toastType.value = 'success'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
      // 重置表单
      inviteDialog.value.count = 1
      inviteDialog.value.canInvite = false
      inviteDialog.value.inviteQuota = 0
    } else {
      toastMessage.value = data.message || '生成失败'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('生成邀请码失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '生成失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  } finally {
    inviteDialog.value.loading = false
  }
}

async function fetchInviteList() {
  try {
    const { data } = await http.get('/admin/invites')
    if (data.success) {
      inviteList.value = data.list || []
    } else {
      toastMessage.value = data.message || '加载失败'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('加载邀请码列表失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '加载失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  }
}

async function deleteInvite(code: string) {
  try {
    const { data } = await http.delete(`/admin/invites/${code}`)
    if (data.success) {
      inviteList.value = inviteList.value.filter(i => i.code !== code)
      toastMessage.value = '已删除'
      toastType.value = 'success'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 1500)
    } else {
      toastMessage.value = data.message || '删除失败'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('删除失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '删除失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  }
}

function formatTime(t?: string) {
  if (!t) return '-'
  try {
    const d = new Date(t)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mi = String(d.getMinutes()).padStart(2, '0')
    const ss = String(d.getSeconds()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`
  } catch { return t }
}

function openSecurityDialog(app: Application, event: Event) {
  event.stopPropagation()
  securityForm.value = {
    appId: app.id,
    encryptionAlg: app.encryptionAlg || 'RC4',
    secretKey: app.secretKey || ''
  }
  showSecurityDialog.value = true
}

function closeSecurityDialog() {
  showSecurityDialog.value = false
  encAlgDropdownOpen.value = false
  securityForm.value = {
    appId: null,
    encryptionAlg: 'RC4',
    secretKey: ''
  }
}

function selectEncAlg(value: string) {
  securityForm.value.encryptionAlg = value
  encAlgDropdownOpen.value = false
}

// 全局点击事件处理
function handleGlobalClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.custom-select')) {
    encAlgDropdownOpen.value = false
  }
}

async function saveSecurity() {
  if (!securityForm.value.appId) return
  
  savingSecurity.value = true
  try {
    // 先获取应用的完整信息
    const appResponse = await http.get(`/admin/apps/${securityForm.value.appId}`)
    const app = appResponse.data
    
    // 更新传输安全相关字段
    app.secure = true
    app.encryptionAlg = securityForm.value.encryptionAlg
    if (securityForm.value.secretKey.trim()) {
      app.secretKey = securityForm.value.secretKey.trim()
    }
    
    // 发送更新请求
    const response = await http.post('/admin/apps/update', app)
    
    if (response.data.success) {
      toastMessage.value = '传输安全已开启'
      toastType.value = 'success'
      showToast.value = true
      setTimeout(() => {
        showToast.value = false
      }, 2000)
      
      closeSecurityDialog()
      await fetchApps()
    } else {
      error.value = response.data.message || '保存失败'
    }
  } catch (e: any) {
    console.error('保存失败:', e)
    error.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    savingSecurity.value = false
  }
}

// 获取当前用户信息
async function fetchCurrentUser() {
  try {
    const { data } = await http.get('/admin/apps/api/my-apps')
    // 如果能访问这个接口，说明已登录
    // 通过尝试访问用户管理接口判断是否是 admin
    const checkAdmin = await http.get('/admin/users')
    if (checkAdmin.data.success) {
      isAdmin.value = true
    }
  } catch (e) {
    // 忽略错误
  }
}

// 打开用户管理对话框
async function openUserManageDialog() {
  try {
    const { data } = await http.get('/admin/users')
    if (data.success) {
      userManageDialog.value.show = true
      userList.value = data.users || []
    } else {
      toastMessage.value = data.message || '无权限'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('获取用户列表失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '获取用户列表失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  }
}

// 关闭用户管理对话框
function closeUserManageDialog() {
  userManageDialog.value.show = false
  editingUser.value = null
}

// 开始编辑用户
function startEditUser(user: User) {
  editingUser.value = { ...user }
}

// 取消编辑
function cancelEditUser() {
  editingUser.value = null
}

// 保存用户权限
async function saveUserPermission(user: User) {
  try {
    const { data } = await http.post(
      `/admin/users/${user.username}/invite-permission`,
      null,
      {
        params: {
          canInvite: user.canInvite,
          inviteQuota: user.inviteQuota
        }
      }
    )
    if (data.success) {
      toastMessage.value = '权限已更新'
      toastType.value = 'success'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
      editingUser.value = null
      // 重新加载用户列表
      await openUserManageDialog()
    } else {
      toastMessage.value = data.message || '更新失败'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('更新权限失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '更新失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  }
}

// 打开个人信息对话框
async function openProfileDialog() {
  profileDialog.value.show = true
  profileDialog.value.loading = true
  try {
    const { data } = await http.get('/admin/profile')
    if (data.success) {
      profile.value = data.profile
    } else {
      toastMessage.value = data.message || '获取失败'
      toastType.value = 'error'
      showToast.value = true
      setTimeout(() => (showToast.value = false), 2000)
    }
  } catch (e: any) {
    console.error('获取个人信息失败:', e)
    toastMessage.value = e.response?.data?.message || e.message || '获取失败'
    toastType.value = 'error'
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  } finally {
    profileDialog.value.loading = false
  }
}

// 关闭个人信息对话框
function closeProfileDialog() {
  profileDialog.value.show = false
  profile.value = null
}

// 打开修改密码对话框
function openChangePasswordDialog() {
  changePasswordDialog.value.show = true
  changePasswordDialog.value.oldPassword = ''
  changePasswordDialog.value.newPassword = ''
  changePasswordDialog.value.confirmPassword = ''
  changePasswordDialog.value.message = ''
  changePasswordDialog.value.messageType = ''
}

// 关闭修改密码对话框
function closeChangePasswordDialog() {
  changePasswordDialog.value.show = false
  changePasswordDialog.value.oldPassword = ''
  changePasswordDialog.value.newPassword = ''
  changePasswordDialog.value.confirmPassword = ''
  changePasswordDialog.value.message = ''
  changePasswordDialog.value.messageType = ''
}

// 修改密码
async function changePassword() {
  // 前端验证
  if (!changePasswordDialog.value.oldPassword) {
    changePasswordDialog.value.message = '请输入当前密码'
    changePasswordDialog.value.messageType = 'error'
    return
  }
  
  if (!changePasswordDialog.value.newPassword) {
    changePasswordDialog.value.message = '请输入新密码'
    changePasswordDialog.value.messageType = 'error'
    return
  }
  
  if (changePasswordDialog.value.newPassword.length < 8) {
    changePasswordDialog.value.message = '新密码长度不能少于8位'
    changePasswordDialog.value.messageType = 'error'
    return
  }
  
  if (changePasswordDialog.value.newPassword !== changePasswordDialog.value.confirmPassword) {
    changePasswordDialog.value.message = '两次输入的密码不一致'
    changePasswordDialog.value.messageType = 'error'
    return
  }
  
  changePasswordDialog.value.loading = true
  changePasswordDialog.value.message = ''
  
  try {
    const { data } = await http.post('/admin/user/change-password', {
      oldPassword: changePasswordDialog.value.oldPassword,
      newPassword: changePasswordDialog.value.newPassword
    })
    
    if (data.success) {
      changePasswordDialog.value.message = '密码修改成功，请重新登录'
      changePasswordDialog.value.messageType = 'success'
      
      // 延迟后跳转到登录页
      setTimeout(() => {
        closeChangePasswordDialog()
        window.location.href = '/login'
      }, 2000)
    } else {
      changePasswordDialog.value.message = data.message || '修改失败'
      changePasswordDialog.value.messageType = 'error'
    }
  } catch (e: any) {
    changePasswordDialog.value.message = e.response?.data?.message || e.message || '修改失败'
    changePasswordDialog.value.messageType = 'error'
  } finally {
    changePasswordDialog.value.loading = false
  }
}

onMounted(() => {
  fetchApps()
  fetchCurrentUser()
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<template>
  <div class="page">
    <div class="page-header-with-action">
      <UiPageHeader title="我的应用" />
      <div class="header-actions">
        <button class="header-icon-btn invite-icon-btn" title="邀请码管理" @click="openInviteDialog">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
            <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
          </svg>
        </button>
        <button v-if="isAdmin" class="header-icon-btn user-manage-btn" title="用户管理" @click="openUserManageDialog">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
          </svg>
        </button>
        <button class="header-icon-btn profile-btn" title="个人信息" @click="openProfileDialog">
          <svg viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
          </svg>
        </button>
        <UiButton @click="openCreateDialog" variant="soft">
        <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
        </svg>
        创建应用
        </UiButton>
      </div>
    </div>
    
    <div v-if="error" class="error-banner">
      <svg class="error-icon" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
      </svg>
      <span>{{ error }}</span>
      <button @click="fetchApps" class="retry-btn">重试</button>
    </div>
    
    <div v-if="loading" class="grid">
      <div v-for="i in 6" :key="i" class="skeleton-card elev-hover">
        <div class="skeleton skeleton-rect" style="width: 40%; height: 14px;"></div>
        <div class="skeleton skeleton-rect" style="width: 80%; height: 10px; margin-top: 10px;"></div>
      </div>
    </div>
    
    <transition-group name="fade-list" tag="div" v-else-if="!error" class="grid">
      <UiCard 
        v-for="a in apps" 
        :key="a.id" 
        class="card elev-hover app-card"
        :class="{ 'app-card-selected': selectedAppId === a.id }"
        @click="selectApp(a)"
      >
        <div class="app-card-header">
          <div class="app-icon-name">
            <div class="app-icon-wrapper">
              <img v-if="a.icon" :src="a.icon" alt="" class="app-icon" />
              <div v-else class="app-icon-placeholder">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                </svg>
              </div>
            </div>
            <div class="app-name-desc">
              <div class="app-name">{{ a.name }}</div>
              <div class="app-desc muted">{{ a.description || '暂无描述' }}</div>
            </div>
          </div>
          <div class="app-actions">
            <button class="action-btn settings-btn" @click="goToSettings(a, $event)" title="应用设置">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd" />
              </svg>
            </button>
            <button class="action-btn delete-btn" @click="openDeleteDialog(a, $event)" title="删除应用">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
            </button>
          </div>
        </div>
        
        <div class="app-keys" @click.stop>
          <div class="key-item">
            <label class="key-label">API Key</label>
            <div 
              class="key-value" 
              @click="copyToClipboard(a.apiKey || '', 'API Key')"
              :title="'点击复制: ' + (a.apiKey || '加载中...')"
            >
              {{ a.apiKey || '加载中...' }}
            </div>
          </div>
          
          <div class="key-item">
            <label class="key-label">
              Secret Key
              <span v-if="a.secure" class="security-badge security-enabled">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
                </svg>
                {{ a.encryptionAlg || 'RC4' }}
              </span>
            </label>
            <div 
              v-if="a.secure && a.secretKey"
              class="key-value"
              @click="copyToClipboard(a.secretKey || '', 'Secret Key')"
              :title="'点击复制: ' + (a.secretKey || '加载中...')"
            >
              {{ a.secretKey || '加载中...' }}
            </div>
            <div 
              v-else
              class="key-value security-disabled"
              @click="openSecurityDialog(a, $event)"
              title="点击开启传输安全"
            >
              <svg class="security-icon" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 1.944A11.954 11.954 0 012.166 5C2.056 5.649 2 6.319 2 7c0 5.225 3.34 9.67 8 11.317C14.66 16.67 18 12.225 18 7c0-.682-.057-1.35-.166-2.001A11.954 11.954 0 0110 1.944zM11 14a1 1 0 11-2 0 1 1 0 012 0zm0-7a1 1 0 10-2 0v3a1 1 0 102 0V7z" clip-rule="evenodd" />
              </svg>
              <span>传输安全未开启</span>
            </div>
          </div>
        </div>
      </UiCard>
    </transition-group>
    
    <div v-if="!loading && !error && apps.length === 0" class="empty-state">
      <p class="muted">暂无应用</p>
    </div>
    
    <!-- 创建应用对话框 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCreateDialog" class="modal-overlay" @click="closeCreateDialog">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">创建新应用</h3>
              <button class="modal-close" @click="closeCreateDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">应用名称 *</label>
                <input 
                  v-model="newAppName" 
                  type="text" 
                  class="form-input" 
                  placeholder="请输入应用名称"
                  @keyup.enter="createApp"
                />
              </div>
              
              <div class="form-group">
                <label class="form-label">应用描述</label>
                <textarea 
                  v-model="newAppDescription" 
                  class="form-textarea" 
                  placeholder="请输入应用描述（可选）"
                  rows="3"
                ></textarea>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeCreateDialog">取消</button>
              <UiButton 
                @click="createApp" 
                :disabled="!newAppName.trim() || creating"
                :loading="creating"
              >
                创建
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 生成邀请码对话框 -->
      <transition name="modal-fade">
        <div v-if="inviteDialog.show" class="modal-overlay" @click="closeInviteDialog">
          <div class="modal-container invite-modal" @click.stop>
            <div class="modal-header">
              <div class="modal-header-content">
                <div class="modal-icon-wrapper">
                  <svg class="modal-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 5a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V9zm1 4a1 1 0 000 2h12a1 1 0 100-2H4z" clip-rule="evenodd" />
                  </svg>
                </div>
                <div>
                  <h3 class="modal-title">邀请码管理</h3>
                  <p class="modal-subtitle">生成和管理用户注册邀请码</p>
                </div>
              </div>
              <button class="modal-close" @click="closeInviteDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            <div class="modal-body invite-modal-body">
              <!-- 批量生成配置区域 -->
              <div class="invite-generate-form">
                <div class="generate-controls">
                  <div class="input-group">
                    <label class="input-label">数量</label>
                    <input 
                      type="number" 
                      v-model.number="inviteDialog.count" 
                      min="1" 
                      max="100" 
                      class="styled-input"
                    />
                  </div>
                  
                  <div class="checkbox-group">
                    <label class="styled-checkbox">
                      <input type="checkbox" v-model="inviteDialog.canInvite" />
                      <span class="checkbox-box"></span>
                      <span class="checkbox-text">允许新用户邀请</span>
                    </label>
                  </div>
                  
                  <div class="input-group" :class="{ 'disabled-group': !inviteDialog.canInvite }">
                    <label class="input-label">配额</label>
                    <input 
                      type="number" 
                      v-model.number="inviteDialog.inviteQuota" 
                      min="-1" 
                      class="styled-input"
                      :disabled="!inviteDialog.canInvite"
                    />
                  </div>
                  
                  <UiButton @click="generateInvite" :loading="inviteDialog.loading" class="primary-action-btn">
                    <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
                    </svg>
                    <span>批量生成</span>
                  </UiButton>
                </div>
              </div>
              
              <div class="invite-toolbar">
                <div class="toolbar-info">
                  <svg class="info-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                  </svg>
                  <span>共 {{ inviteList.length }} 条邀请码</span>
                </div>
              </div>
              <div class="invite-table-wrapper">
                <table class="invite-table">
                  <thead>
                    <tr class="invite-header">
                      <th>邀请码</th>
                      <th>创建时间</th>
                      <th>状态</th>
                      <th>邀请权限</th>
                      <th>邀请配额</th>
                      <th>使用者</th>
                      <th>使用时间</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in inviteList" :key="item.code" class="invite-row">
                      <td>
                        <div class="code-cell" @click="copyToClipboard(item.code, '邀请码')" :title="'点击复制: ' + item.code">
                          <code class="invite-code">{{ item.code }}</code>
                          <svg class="copy-icon" viewBox="0 0 20 20" fill="currentColor">
                            <path d="M8 2a1 1 0 000 2h2a1 1 0 100-2H8z" />
                            <path d="M3 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v6h-4.586l1.293-1.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L10.414 13H15v3a2 2 0 01-2 2H5a2 2 0 01-2-2V5z" />
                          </svg>
                        </div>
                      </td>
                      <td class="time-cell">{{ formatTime(item.createdAt) }}</td>
                      <td>
                        <span v-if="item.used" class="status-badge status-used">已使用</span>
                        <span v-else class="status-badge status-available">未使用</span>
                      </td>
                      <td class="permission-cell">
                        <span v-if="item.canInvite" class="permission-badge permission-yes">✓</span>
                        <span v-else class="permission-badge permission-no">✗</span>
                      </td>
                      <td class="quota-cell">{{ item.inviteQuota === -1 ? '无限' : (item.inviteQuota || 0) }}</td>
                      <td class="user-cell">{{ item.usedBy || '-' }}</td>
                      <td class="time-cell">{{ item.usedAt ? formatTime(item.usedAt) : '-' }}</td>
                      <td>
                        <button 
                          class="delete-btn" 
                          :disabled="item.used" 
                          @click="deleteInvite(item.code)"
                          :title="item.used ? '已使用的邀请码无法删除' : '删除邀请码'"
                        >
                          <svg viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                          </svg>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="inviteList.length === 0" class="empty-state-invite">
                  <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z" />
                  </svg>
                  <p class="empty-text">暂无邀请码</p>
                  <p class="empty-hint">点击"生成新邀请码"按钮创建</p>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeInviteDialog">关闭</button>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 删除确认对话框 -->
      <transition name="modal-fade">
        <div v-if="showDeleteDialog" class="modal-overlay" @click="closeDeleteDialog">
          <div class="modal-container modal-small" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">确认删除</h3>
              <button class="modal-close" @click="closeDeleteDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <p class="delete-warning">
                确定要删除应用 <strong>{{ appToDelete?.name }}</strong> 吗？
              </p>
              <p class="delete-warning-sub">
                此操作将同时删除该应用下的所有卡密，且无法撤销。
              </p>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeDeleteDialog">取消</button>
              <UiButton 
                @click="confirmDelete" 
                variant="danger"
                :loading="deletingAppId === appToDelete?.id"
              >
                确认删除
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 传输安全设置对话框 -->
      <transition name="modal-fade">
        <div v-if="showSecurityDialog" class="modal-overlay" @click="closeSecurityDialog">
          <div class="modal-container" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">开启传输安全</h3>
              <button class="modal-close" @click="closeSecurityDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="form-group">
                <label class="form-label">加密算法</label>
                <div class="custom-select" :class="{ 'is-open': encAlgDropdownOpen }">
                  <button 
                    class="select-trigger" 
                    @click="encAlgDropdownOpen = !encAlgDropdownOpen"
                    type="button"
                  >
                    <span class="select-label">{{ securityForm.encryptionAlg }}</span>
                    <svg class="select-arrow" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                    </svg>
                  </button>
                  
                  <div v-if="encAlgDropdownOpen" class="select-dropdown" @click.stop>
                    <div 
                      v-for="opt in encAlgOptions" 
                      :key="opt.value" 
                      class="select-option"
                      :class="{ 'is-selected': securityForm.encryptionAlg === opt.value }"
                      @click="selectEncAlg(opt.value)"
                    >
                      <span>{{ opt.label }}</span>
                      <svg v-if="securityForm.encryptionAlg === opt.value" class="check-icon" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    </div>
                  </div>
                </div>
                <p class="form-hint">选择用于传输加密的算法</p>
              </div>
              
              <div class="form-group">
                <label class="form-label">Secret Key（可选）</label>
                <input 
                  v-model="securityForm.secretKey" 
                  type="text" 
                  class="form-input" 
                  placeholder="留空则使用 API Key 作为密钥"
                />
                <p class="form-hint">用于签名和加密的密钥，留空则使用 API Key</p>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeSecurityDialog">取消</button>
              <UiButton 
                @click="saveSecurity" 
                :loading="savingSecurity"
              >
                开启传输安全
              </UiButton>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 用户管理对话框 -->
      <transition name="modal-fade">
        <div v-if="userManageDialog.show" class="modal-overlay" @click="closeUserManageDialog">
          <div class="modal-container user-manage-modal" @click.stop>
            <div class="modal-header">
              <div class="modal-header-content">
                <div class="modal-icon-wrapper">
                  <svg class="modal-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                  </svg>
                </div>
                <div>
                  <h3 class="modal-title">用户管理</h3>
                  <p class="modal-subtitle">管理用户邀请权限和配额</p>
                </div>
              </div>
              <button class="modal-close" @click="closeUserManageDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            <div class="modal-body user-manage-body">
              <div class="user-table-wrapper">
                <table class="user-table">
                  <thead>
                    <tr class="user-header">
                      <th>用户名</th>
                      <th>注册时间</th>
                      <th>邀请权限</th>
                      <th>邀请配额</th>
                      <th>已使用</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="user in userList" :key="user.id" class="user-row">
                      <td>
                        <div class="username-cell">
                          <span class="username">{{ user.username }}</span>
                          <span v-if="user.username === 'admin'" class="admin-badge">管理员</span>
                        </div>
                      </td>
                      <td class="time-cell">{{ formatTime(user.createdAt) }}</td>
                      <td>
                        <template v-if="editingUser?.id === user.id">
                          <label class="checkbox-wrapper">
                            <input type="checkbox" v-model="editingUser.canInvite" />
                            <span class="checkbox-label">{{ editingUser.canInvite ? '已授权' : '未授权' }}</span>
                          </label>
                        </template>
                        <template v-else>
                          <span v-if="user.canInvite" class="status-badge status-enabled">已授权</span>
                          <span v-else class="status-badge status-disabled">未授权</span>
                        </template>
                      </td>
                      <td>
                        <template v-if="editingUser?.id === user.id">
                          <input 
                            type="number" 
                            v-model.number="editingUser.inviteQuota" 
                            class="quota-input"
                            min="-1"
                            placeholder="-1表示无限"
                          />
                        </template>
                        <template v-else>
                          <span class="quota-text">
                            {{ user.inviteQuota === -1 ? '无限' : user.inviteQuota }}
                          </span>
                        </template>
                      </td>
                      <td class="used-cell">{{ user.usedQuota }}</td>
                      <td>
                        <div class="action-buttons">
                          <template v-if="user.username !== 'admin'">
                            <template v-if="editingUser?.id === user.id">
                              <button class="action-btn save-btn" @click="saveUserPermission(editingUser)" title="保存">
                                <svg viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                                </svg>
                              </button>
                              <button class="action-btn cancel-btn" @click="cancelEditUser" title="取消">
                                <svg viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                                </svg>
                              </button>
                            </template>
                            <template v-else>
                              <button class="action-btn edit-btn" @click="startEditUser(user)" title="编辑">
                                <svg viewBox="0 0 20 20" fill="currentColor">
                                  <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                                </svg>
                              </button>
                            </template>
                          </template>
                          <span v-else class="admin-text">-</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="userList.length === 0" class="empty-state-user">
                  <svg class="empty-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
                  </svg>
                  <p class="empty-text">暂无用户</p>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="closeUserManageDialog">关闭</button>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- 个人信息对话框 -->
      <transition name="modal-fade">
        <div v-if="profileDialog.show" class="modal-overlay" @click="closeProfileDialog">
          <div class="modal-container profile-modal" @click.stop>
            <div class="modal-header">
              <div class="modal-header-content">
                <div class="modal-icon-wrapper profile-icon-wrapper">
                  <svg class="modal-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
                  </svg>
                </div>
                <div>
                  <h3 class="modal-title">个人信息</h3>
                  <p class="modal-subtitle">查看我的账号统计信息</p>
                </div>
              </div>
              <button class="modal-close" @click="closeProfileDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            <div class="modal-body profile-body">
              <div v-if="profileDialog.loading" class="loading-state">
                <div class="loading-spinner"></div>
                <p>加载中...</p>
              </div>
              <div v-else-if="profile" class="profile-content">
                <div class="profile-section">
                  <h4 class="section-title">基本信息</h4>
                  <div class="info-grid">
                    <div class="info-item">
                      <div class="info-label">
                        <svg class="info-icon" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd" />
                        </svg>
                        用户名
                      </div>
                      <div class="info-value">{{ profile.username }}</div>
                    </div>
                    <div class="info-item">
                      <div class="info-label">
                        <svg class="info-icon" viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd" />
                        </svg>
                        注册时间
                      </div>
                      <div class="info-value">{{ formatTime(profile.createdAt) }}</div>
                    </div>
                  </div>
                </div>

                <div class="profile-section">
                  <h4 class="section-title">数据统计</h4>
                  <div class="stats-grid">
                    <div class="stat-card">
                      <div class="stat-icon app-stat">
                        <svg viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                        </svg>
                      </div>
                      <div class="stat-info">
                        <div class="stat-value">{{ profile.appCount }}</div>
                        <div class="stat-label">应用数量</div>
                      </div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-icon card-stat">
                        <svg viewBox="0 0 20 20" fill="currentColor">
                          <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                          <path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd" />
                        </svg>
                      </div>
                      <div class="stat-info">
                        <div class="stat-value">{{ profile.cardCount }}</div>
                        <div class="stat-label">卡密总数</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="profile-section">
                  <h4 class="section-title">邀请权限</h4>
                  <div class="invite-info-grid">
                    <div class="invite-status-card" :class="profile.canInvite ? 'enabled' : 'disabled'">
                      <div class="status-header">
                        <svg class="status-icon" viewBox="0 0 20 20" fill="currentColor">
                          <path v-if="profile.canInvite" fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                          <path v-else fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                        </svg>
                        <span class="status-text">{{ profile.canInvite ? '已授权' : '未授权' }}</span>
                      </div>
                      <div class="status-desc">{{ profile.canInvite ? '你可以生成邀请码' : '请联系管理员开通' }}</div>
                    </div>

                    <div v-if="profile.canInvite" class="quota-stats">
                      <div class="quota-item">
                        <div class="quota-label">邀请配额</div>
                        <div class="quota-value">{{ profile.inviteQuota === -1 ? '无限' : profile.inviteQuota }}</div>
                      </div>
                      <div class="quota-item">
                        <div class="quota-label">已使用</div>
                        <div class="quota-value">{{ profile.usedInviteQuota }}</div>
                      </div>
                      <div class="quota-item">
                        <div class="quota-label">剩余配额</div>
                        <div class="quota-value highlight">{{ profile.remainingQuota === -1 ? '无限' : profile.remainingQuota }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-primary" @click="openChangePasswordDialog">修改密码</button>
              <button class="btn btn-secondary" @click="closeProfileDialog">关闭</button>
            </div>
          </div>
        </div>
      </transition>

      <!-- 修改密码对话框 -->
      <transition name="modal-fade">
        <div v-if="changePasswordDialog.show" class="modal-overlay" @click="closeChangePasswordDialog">
          <div class="modal-container change-password-modal" @click.stop>
            <div class="modal-header">
              <div class="modal-title-section">
                <div class="modal-icon-wrapper password-icon-wrapper">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
                  </svg>
                </div>
                <h3 class="modal-title">修改密码</h3>
              </div>
              <button class="modal-close" @click="closeChangePasswordDialog">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <!-- 消息提示 -->
              <transition name="message-slide">
                <div v-if="changePasswordDialog.message" :class="['message', changePasswordDialog.messageType === 'success' ? 'message-success' : 'message-error']">
                  <svg v-if="changePasswordDialog.messageType === 'success'" class="message-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                  </svg>
                  <svg v-else class="message-icon" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                  </svg>
                  <span>{{ changePasswordDialog.message }}</span>
                </div>
              </transition>

              <form @submit.prevent="changePassword" class="password-form">
                <div class="form-field">
                  <label class="field-label">当前密码</label>
                  <input 
                    type="password" 
                    v-model="changePasswordDialog.oldPassword" 
                    placeholder="请输入当前密码"
                    class="form-input"
                    :disabled="changePasswordDialog.loading"
                  />
                </div>

                <div class="form-field">
                  <label class="field-label">新密码</label>
                  <input 
                    type="password" 
                    v-model="changePasswordDialog.newPassword" 
                    placeholder="请输入新密码（至少8位）"
                    class="form-input"
                    :disabled="changePasswordDialog.loading"
                  />
                </div>

                <div class="form-field">
                  <label class="field-label">确认新密码</label>
                  <input 
                    type="password" 
                    v-model="changePasswordDialog.confirmPassword" 
                    placeholder="请再次输入新密码"
                    class="form-input"
                    :disabled="changePasswordDialog.loading"
                  />
                </div>
              </form>
            </div>
            <div class="modal-footer">
              <button 
                class="btn btn-primary" 
                @click="changePassword"
                :disabled="changePasswordDialog.loading"
              >
                <span v-if="!changePasswordDialog.loading">确认修改</span>
                <span v-else>修改中...</span>
              </button>
              <button class="btn btn-secondary" @click="closeChangePasswordDialog" :disabled="changePasswordDialog.loading">取消</button>
            </div>
          </div>
        </div>
      </transition>
      
      <!-- Toast 通知 -->
      <transition name="toast-fade">
        <div v-if="showToast" class="toast" :class="toastType === 'error' ? 'toast-error' : 'toast-success'">
          <svg v-if="toastType === 'success'" class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <svg v-else class="toast-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
          </svg>
          <span>{{ toastMessage }}</span>
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
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-btn {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: relative;
}

.header-icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(124, 58, 237, 0.15);
  border-color: rgba(124, 58, 237, 0.3);
}

.header-icon-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(124, 58, 237, 0.1);
}

.header-icon-btn svg {
  width: 20px;
  height: 20px;
  color: var(--brand);
  transition: all 0.25s ease;
}

.header-icon-btn:hover svg {
  transform: scale(1.1);
}

/* 邀请码按钮特殊样式 */
.invite-icon-btn {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.05) 0%, rgba(79, 70, 229, 0.05) 100%);
  border-color: rgba(124, 58, 237, 0.15);
}

.invite-icon-btn:hover {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
  border-color: rgba(124, 58, 237, 0.4);
}

.icon-badge {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-dot {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4), 0 0 0 2px white;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.08);
    opacity: 0.9;
  }
}

/* 邀请码对话框样式优化 */
.invite-modal {
  max-width: 900px !important;
  width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.modal-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(124, 58, 237, 0.05) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.modal-icon {
  width: 24px;
  height: 24px;
  color: var(--brand);
}

.modal-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--text-2);
  font-weight: 400;
}

/* 批量生成表单样式 - 精美版本 */
.invite-generate-form {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #d946ef 100%);
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.3), 0 8px 10px -6px rgba(139, 92, 246, 0.2);
}

.generate-controls {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 100px;
  align-self: flex-end;
}

.input-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.styled-input {
  padding: 0 14px;
  height: 40px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  width: 100px;
  text-align: center;
  -moz-appearance: textfield;
}

.styled-input::-webkit-outer-spin-button,
.styled-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.styled-input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.1);
  transform: translateY(-1px);
}

.styled-input:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.disabled-group {
  opacity: 0.4;
}

.checkbox-group {
  display: flex;
  align-items: center;
  padding: 0 8px;
  flex: 1;
  padding-bottom: 8px;
}

.styled-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.styled-checkbox input[type="checkbox"] {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.checkbox-box {
  position: relative;
  width: 22px;
  height: 22px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.15);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.styled-checkbox input[type="checkbox"]:checked + .checkbox-box {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.styled-checkbox input[type="checkbox"]:checked + .checkbox-box::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%) rotate(45deg);
  width: 6px;
  height: 11px;
  border: solid white;
  border-width: 0 2.5px 2.5px 0;
}

.checkbox-text {
  color: white;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.primary-action-btn {
  margin-left: auto;
  padding: 0 28px;
  height: 40px;
  background: white;
  color: #8b5cf6;
  font-weight: 700;
  font-size: 14px;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  align-self: flex-end;
}

.primary-action-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ffffff 0%, #f3f4f6 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.primary-action-btn:active:not(:disabled) {
  transform: translateY(0);
}

.primary-action-btn .btn-icon {
  width: 16px;
  height: 16px;
}

/* 权限和配额列样式 */
.permission-cell {
  text-align: center;
}

.permission-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-weight: bold;
  font-size: 14px;
}

.permission-yes {
  background: #10b981;
  color: white;
}

.permission-no {
  background: #ef4444;
  color: white;
}

.quota-cell {
  text-align: center;
  font-weight: 500;
  color: #6b7280;
}

.invite-modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.invite-modal-body::-webkit-scrollbar {
  width: 8px;
}

.invite-modal-body::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.invite-modal-body::-webkit-scrollbar-thumb {
  background: rgba(124, 58, 237, 0.3);
  border-radius: 4px;
}

.invite-modal-body::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 58, 237, 0.5);
}

.invite-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.03) 0%, rgba(124, 58, 237, 0.01) 100%);
  border-radius: 12px;
  border: 1px solid rgba(124, 58, 237, 0.1);
}

.toolbar-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-2);
  font-weight: 500;
}

.info-icon {
  width: 18px;
  height: 18px;
  color: var(--brand);
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.invite-table-wrapper {
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow-x: auto;
  background: white;
}

.invite-table-wrapper::-webkit-scrollbar {
  height: 8px;
}

.invite-table-wrapper::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.invite-table-wrapper::-webkit-scrollbar-thumb {
  background: rgba(124, 58, 237, 0.3);
  border-radius: 4px;
}

.invite-table-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 58, 237, 0.5);
}

.invite-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  min-width: 800px;
}

.invite-table thead {
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0.04));
}

.invite-header th {
  padding: 14px 16px;
  text-align: center;
  font-weight: 600;
  color: var(--text-1);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.invite-header th:first-child {
  text-align: left;
}

.invite-row {
  transition: background-color 0.15s ease;
}

.invite-row:hover {
  background: rgba(124, 58, 237, 0.02);
}

.invite-row td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: var(--text-1);
  white-space: nowrap;
  text-align: center;
}

.invite-row td:first-child {
  text-align: left;
}

.invite-table tbody tr:last-child td {
  border-bottom: none;
}

.code-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 0;
  transition: all 0.2s ease;
  justify-content: flex-start;
}

.code-cell:hover .invite-code {
  background: rgba(124, 58, 237, 0.08);
  color: var(--brand);
}

.code-cell:hover .copy-icon {
  opacity: 1;
  transform: translateX(0);
}

.invite-code {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  background: rgba(0, 0, 0, 0.04);
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s ease;
  letter-spacing: 0.5px;
}

.copy-icon {
  width: 16px;
  height: 16px;
  color: var(--text-2);
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.time-cell {
  color: var(--text-2);
  font-size: 13px;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  white-space: nowrap;
}

.user-cell {
  font-weight: 500;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.status-available {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.status-used {
  background: linear-gradient(135deg, rgba(107, 114, 128, 0.1) 0%, rgba(75, 85, 99, 0.1) 100%);
  color: #6b7280;
  border: 1px solid rgba(107, 114, 128, 0.2);
}

/* 邀请码表格中的删除按钮 */
.invite-table .delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1.5px solid rgba(239, 68, 68, 0.3);
  background: transparent;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
}

.invite-table .delete-btn svg {
  width: 18px !important;
  height: 18px !important;
  fill: currentColor;
  flex-shrink: 0;
}

.invite-table .delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
  color: #dc2626;
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.2);
}

.invite-table .delete-btn:active:not(:disabled) {
  transform: scale(0.95);
  background: rgba(239, 68, 68, 0.15);
}

.invite-table .delete-btn:disabled {
  color: #d1d5db;
  border-color: rgba(0, 0, 0, 0.1);
  cursor: not-allowed;
  opacity: 0.5;
}

.empty-state-invite {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 64px;
  height: 64px;
  color: rgba(0, 0, 0, 0.15);
  margin-bottom: 16px;
}

.empty-text {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
}

.empty-hint {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
}

.header-icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79,70,229,0.25);
}

.btn-icon {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

/* 应用卡片 */
.app-card {
  position: relative;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  border: 2px solid transparent;
  user-select: none;
  padding: 20px;
}

.app-card:hover {
  border-color: rgba(79, 70, 229, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.app-card:active {
  transform: translateY(-2px);
}

.app-card-selected {
  border-color: rgba(79, 70, 229, 0.6);
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.05), rgba(124, 58, 237, 0.05));
}

/* 应用卡片头部 */
.app-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.app-icon-name {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.app-icon-wrapper {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(124, 58, 237, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-icon-placeholder {
  width: 24px;
  height: 24px;
  color: var(--brand);
}

.app-icon-placeholder svg {
  width: 100%;
  height: 100%;
}

.app-name-desc {
  flex: 1;
  min-width: 0;
}

.app-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-desc {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: all 0.2s ease;
}

.app-card:hover .app-actions {
  opacity: 1;
}

.action-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  padding: 0;
}

.action-btn svg {
  width: 18px;
  height: 18px;
}

.settings-btn {
  color: var(--brand);
}

.settings-btn:hover {
  background: rgba(79, 70, 229, 0.1);
  transform: scale(1.1);
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  transform: scale(1.1);
}

/* 密钥区域 */
.app-keys {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.key-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.key-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-2);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.security-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
}

.security-badge svg {
  width: 12px;
  height: 12px;
}

.security-enabled {
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
}

.key-value {
  padding: 10px 14px;
  background: transparent;
  border-radius: 8px;
  font-size: 13px;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  color: var(--text-1);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: all;
  word-break: break-all;
  position: relative;
}

.key-value::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
  z-index: -1;
}

.key-value:hover::before {
  background: rgba(79, 70, 229, 0.08);
  box-shadow: 0 0 0 1px rgba(79, 70, 229, 0.3) inset;
}

.key-value:active::before {
  background: rgba(79, 70, 229, 0.12);
}

.security-disabled {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #f59e0b;
  font-family: inherit;
  user-select: none;
}

.security-disabled::before {
  background: rgba(245, 158, 11, 0.1);
}

.security-disabled:hover::before {
  background: rgba(245, 158, 11, 0.15);
  box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.3) inset;
}

.security-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
  color: #dc2626;
  margin-bottom: 16px;
}

.error-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.retry-btn {
  margin-left: auto;
  padding: 6px 12px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: #b91c1c;
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
}

.empty-state p {
  font-size: 15px;
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
}

.modal-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
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
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
}

/* 自定义下拉列表样式 */
.custom-select {
  position: relative;
  width: 100%;
}

.select-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-1);
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.2s ease;
}

.select-trigger:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 0.95);
}

.custom-select.is-open .select-trigger {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
  background: white;
}

.select-label {
  flex: 1;
  text-align: left;
}

.select-arrow {
  width: 18px;
  height: 18px;
  color: var(--text-2);
  transition: all 0.2s ease;
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
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
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
  color: var(--text-1);
  font-size: 14px;
}

.select-option:hover {
  background: rgba(79, 70, 229, 0.05);
}

.select-option.is-selected {
  background: rgba(79, 70, 229, 0.1);
  color: var(--brand);
  font-weight: 600;
}

.check-icon {
  width: 16px;
  height: 16px;
  color: var(--brand);
}

.form-hint {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: var(--text-2);
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

.modal-small {
  max-width: 400px;
}

.delete-warning {
  font-size: 15px;
  color: var(--text-1);
  margin: 0 0 12px 0;
}

.delete-warning strong {
  color: #ef4444;
  font-weight: 600;
}

.delete-warning-sub {
  font-size: 13px;
  color: var(--text-2);
  margin: 0;
}

/* Toast 通知 */
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

/* 对话框动画 */
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

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .app-card:hover {
    border-color: rgba(124, 58, 237, 0.5);
  }
  
  .app-card-selected {
    border-color: rgba(124, 58, 237, 0.8);
    background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(124, 58, 237, 0.1));
  }
  
  .key-value {
    color: var(--text-1-dark);
  }
  
  .key-value::before {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .key-value:hover::before {
    background: rgba(124, 58, 237, 0.15);
    box-shadow: 0 0 0 1px rgba(124, 58, 237, 0.4) inset;
  }
  
  .key-value:active::before {
    background: rgba(124, 58, 237, 0.2);
  }
  
  .security-disabled::before {
    background: rgba(245, 158, 11, 0.15);
  }
  
  .security-disabled:hover::before {
    background: rgba(245, 158, 11, 0.2);
    box-shadow: 0 0 0 1px rgba(245, 158, 11, 0.4) inset;
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
  
  .form-input,
  .form-textarea {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .select-trigger {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .select-trigger:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(124, 58, 237, 0.4);
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
  
  .select-dropdown::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
  }
  
  .select-dropdown::-webkit-scrollbar-thumb {
    background: rgba(124, 58, 237, 0.4);
  }
  
  .select-dropdown::-webkit-scrollbar-thumb:hover {
    background: rgba(124, 58, 237, 0.6);
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
  
  .check-icon {
    color: #c084fc;
  }
  
  .btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .btn-secondary:hover {
    background: rgba(255, 255, 255, 0.15);
  }
  
  /* 暗色模式下的邀请码样式 */
  .invite-generate-form {
    background: linear-gradient(135deg, #4338ca 0%, #6b21a8 50%, #a21caf 100%);
    box-shadow: 0 10px 25px -5px rgba(67, 56, 202, 0.4), 0 8px 10px -6px rgba(107, 33, 168, 0.3);
  }

  .styled-input {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.25);
  }

  .styled-input:focus {
    background: rgba(255, 255, 255, 0.2);
    border-color: rgba(255, 255, 255, 0.5);
  }

  .checkbox-box {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.3);
  }

  .primary-action-btn {
    background: rgba(255, 255, 255, 0.95);
    color: #7c3aed;
  }

  .primary-action-btn:hover:not(:disabled) {
    background: white;
  }

  .quota-cell {
    color: #9ca3af;
  }

  .invite-table-wrapper {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .invite-table thead {
    background: linear-gradient(to bottom, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.08));
  }
  
  .invite-header th {
    color: var(--text-1-dark);
    border-bottom-color: rgba(255, 255, 255, 0.15);
  }
  
  .invite-row:hover {
    background: rgba(124, 58, 237, 0.08);
  }
  
  .invite-row td {
    border-bottom-color: rgba(255, 255, 255, 0.08);
    color: var(--text-1-dark);
  }
  
  .invite-code {
    background: rgba(255, 255, 255, 0.08);
    color: var(--text-1-dark);
  }
  
  .code-cell:hover .invite-code {
    background: rgba(124, 58, 237, 0.15);
    color: #c084fc;
  }
  
  .time-cell {
    color: var(--text-2);
  }
  
  .status-available {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.15) 100%);
    color: #34d399;
    border-color: rgba(16, 185, 129, 0.3);
  }
  
  .status-used {
    background: linear-gradient(135deg, rgba(156, 163, 175, 0.15) 0%, rgba(107, 114, 128, 0.15) 100%);
    color: #9ca3af;
    border-color: rgba(156, 163, 175, 0.3);
  }
  
  .invite-table .delete-btn {
    border-color: rgba(239, 68, 68, 0.4);
    background: transparent;
  }
  
  .invite-table .delete-btn:hover:not(:disabled) {
    background: rgba(239, 68, 68, 0.15);
    border-color: #ef4444;
  }
  
  .invite-table .delete-btn:active:not(:disabled) {
    background: rgba(239, 68, 68, 0.2);
  }
  
  .invite-table .delete-btn:disabled {
    border-color: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.3);
  }
  
  .empty-icon {
    color: rgba(255, 255, 255, 0.15);
  }
  
  .toolbar-info {
    color: var(--text-2);
  }
  
  /* 暗色模式下的邀请码按钮 */
  .header-icon-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .header-icon-btn:hover {
    background: rgba(124, 58, 237, 0.15);
    border-color: rgba(124, 58, 237, 0.4);
    box-shadow: 0 8px 16px rgba(124, 58, 237, 0.25);
  }
  
  .invite-icon-btn {
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
    border-color: rgba(124, 58, 237, 0.25);
  }
  
  .invite-icon-btn:hover {
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.2) 0%, rgba(79, 70, 229, 0.2) 100%);
    border-color: rgba(124, 58, 237, 0.5);
  }
  
  .badge-dot {
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.5), 0 0 0 2px rgba(22, 22, 26, 1);
  }
}

/* 用户管理按钮样式 */
.user-manage-btn {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(37, 99, 235, 0.05) 100%);
  border-color: rgba(59, 130, 246, 0.15);
}

.user-manage-btn:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(37, 99, 235, 0.1) 100%);
  border-color: rgba(59, 130, 246, 0.4);
}

/* 用户管理对话框样式 */
.user-manage-modal {
  max-width: 1000px !important;
  width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.user-manage-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.user-table-wrapper {
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  overflow-x: auto;
  background: white;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  min-width: 800px;
}

.user-table thead {
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.02), rgba(0, 0, 0, 0.04));
}

.user-header th {
  padding: 14px 16px;
  text-align: center;
  font-weight: 600;
  color: var(--text-1);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.1);
  white-space: nowrap;
}

.user-header th:first-child {
  text-align: left;
}

.user-row {
  transition: background-color 0.15s ease;
}

.user-row:hover {
  background: rgba(59, 130, 246, 0.02);
}

.user-row td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: var(--text-1);
  white-space: nowrap;
  text-align: center;
}

.user-row td:first-child {
  text-align: left;
}

.username-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-weight: 500;
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(220, 38, 38, 0.1) 100%);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.status-enabled {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.status-disabled {
  background: linear-gradient(135deg, rgba(107, 114, 128, 0.1) 0%, rgba(75, 85, 99, 0.1) 100%);
  color: #6b7280;
  border: 1px solid rgba(107, 114, 128, 0.2);
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-wrapper input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.checkbox-label {
  font-size: 13px;
  font-weight: 500;
}

.quota-input {
  width: 100px;
  padding: 6px 10px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  font-size: 13px;
  text-align: center;
}

.quota-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.1);
}

.quota-text {
  font-weight: 500;
  color: var(--brand);
}

.used-cell {
  font-weight: 500;
  color: var(--text-2);
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

.edit-btn {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.edit-btn:hover {
  background: rgba(59, 130, 246, 0.2);
  transform: scale(1.1);
}

.save-btn {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.save-btn:hover {
  background: rgba(16, 185, 129, 0.2);
  transform: scale(1.1);
}

.cancel-btn {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.cancel-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: scale(1.1);
}

.admin-text {
  color: var(--text-2);
  font-size: 13px;
}

.empty-state-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

/* 深色模式下的用户管理样式 */
@media (prefers-color-scheme: dark) {
  .user-manage-btn {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(37, 99, 235, 0.1) 100%);
    border-color: rgba(59, 130, 246, 0.25);
  }
  
  .user-manage-btn:hover {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(37, 99, 235, 0.2) 100%);
    border-color: rgba(59, 130, 246, 0.5);
  }
  
  .user-table-wrapper {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .user-table thead {
    background: linear-gradient(to bottom, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.08));
  }
  
  .user-header th {
    color: var(--text-1-dark);
    border-bottom-color: rgba(255, 255, 255, 0.15);
  }
  
  .user-row:hover {
    background: rgba(59, 130, 246, 0.08);
  }
  
  .user-row td {
    border-bottom-color: rgba(255, 255, 255, 0.08);
    color: var(--text-1-dark);
  }
  
  .admin-badge {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(220, 38, 38, 0.15) 100%);
    color: #fca5a5;
    border-color: rgba(239, 68, 68, 0.3);
  }
  
  .status-enabled {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(5, 150, 105, 0.15) 100%);
    color: #34d399;
    border-color: rgba(16, 185, 129, 0.3);
  }
  
  .status-disabled {
    background: linear-gradient(135deg, rgba(156, 163, 175, 0.15) 0%, rgba(107, 114, 128, 0.15) 100%);
    color: #9ca3af;
    border-color: rgba(156, 163, 175, 0.3);
  }
  
  .quota-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: var(--text-1-dark);
  }
  
  .quota-input:focus {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(59, 130, 246, 0.5);
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
  }
}

/* 个人信息按钮样式 */
.profile-btn {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05) 0%, rgba(5, 150, 105, 0.05) 100%);
  border-color: rgba(16, 185, 129, 0.15);
}

.profile-btn:hover {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
  border-color: rgba(16, 185, 129, 0.4);
}

/* 个人信息对话框样式 */
.profile-modal {
  max-width: 600px !important;
}

/* 修改密码对话框 */
.change-password-modal {
  max-width: 460px !important;
}

.password-icon-wrapper {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(220, 38, 38, 0.1));
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.password-form .form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.password-form .field-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.password-form .form-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.password-form .form-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.password-form .form-input:disabled {
  background: rgba(0, 0, 0, 0.05);
  cursor: not-allowed;
}

.profile-icon-wrapper {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(16, 185, 129, 0.05) 100%);
}

.profile-body {
  padding: 24px;
  max-height: 500px;
  overflow-y: auto;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(16, 185, 129, 0.2);
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-section {
  background: rgba(0, 0, 0, 0.02);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-2);
  font-weight: 500;
}

.info-icon {
  width: 16px;
  height: 16px;
  color: var(--brand);
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
  color: white;
}

.app-stat {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
}

.card-stat {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-1);
}

.stat-label {
  font-size: 12px;
  color: var(--text-2);
  font-weight: 500;
}

.invite-info-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.invite-status-card {
  padding: 16px;
  border-radius: 10px;
  border: 2px solid;
}

.invite-status-card.enabled {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.05) 0%, rgba(5, 150, 105, 0.05) 100%);
  border-color: rgba(16, 185, 129, 0.3);
}

.invite-status-card.disabled {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.05) 0%, rgba(220, 38, 38, 0.05) 100%);
  border-color: rgba(239, 68, 68, 0.3);
}

.status-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.status-icon {
  width: 20px;
  height: 20px;
}

.invite-status-card.enabled .status-icon {
  color: #10b981;
}

.invite-status-card.disabled .status-icon {
  color: #ef4444;
}

.status-text {
  font-size: 16px;
  font-weight: 600;
}

.invite-status-card.enabled .status-text {
  color: #059669;
}

.invite-status-card.disabled .status-text {
  color: #dc2626;
}

.status-desc {
  font-size: 13px;
  color: var(--text-2);
}

.quota-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quota-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  text-align: center;
}

.quota-label {
  font-size: 12px;
  color: var(--text-2);
  margin-bottom: 6px;
}

.quota-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-1);
}

.quota-value.highlight {
  color: #10b981;
}

/* 深色模式下的个人信息样式 */
@media (prefers-color-scheme: dark) {
  .profile-btn {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
    border-color: rgba(16, 185, 129, 0.25);
  }
  
  .profile-btn:hover {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(5, 150, 105, 0.2) 100%);
    border-color: rgba(16, 185, 129, 0.5);
  }
  
  .profile-section {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .stat-card, .quota-item {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .invite-status-card.enabled {
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%);
    border-color: rgba(16, 185, 129, 0.4);
  }
  
  .invite-status-card.disabled {
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(220, 38, 38, 0.1) 100%);
    border-color: rgba(239, 68, 68, 0.4);
  }
  
  .invite-status-card.enabled .status-text {
    color: #34d399;
  }
  
  .invite-status-card.disabled .status-text {
    color: #fca5a5;
  }
  
  .quota-value.highlight {
    color: #34d399;
  }
}
</style>


