<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, toRaw, defineComponent, watch, h, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '../utils/http'
import * as monaco from 'monaco-editor'
// @ts-ignore
import UiPageHeader from '../components/ui/PageHeader.vue'
// @ts-ignore
import UiButton from '../components/ui/Button.vue'

const router = useRouter()
const route = router.currentRoute

// ============= 文件树数据结构 =============
interface FileNode {
  name: string
  type: 'file' | 'folder'
  path: string
  content?: string
  children?: FileNode[]
  expanded?: boolean
}

const FileTreeNode = defineComponent({
  name: 'FileTreeNode',
  props: {
    node: {
      type: Object as () => FileNode,
      required: true
    },
    currentPath: {
      type: String,
      default: ''
    },
    depth: {
      type: Number,
      default: 0
    }
  },
  emits: ['toggle', 'select', 'contextmenu'],
  setup(props, { emit }) {
    const emitToggle = (payload: FileNode) => emit('toggle', payload)
    const emitSelect = (payload: FileNode) => emit('select', payload)
    const emitContextMenu = (event: MouseEvent, node: FileNode) => emit('contextmenu', event, node)

    // 使用 computed 计算 active 状态
    const isActive = computed(() => {
      const active = props.node.type === 'file' && props.node.path === props.currentPath
      console.log('[JavaEditor][FileTreeNode] computed isActive', {
        nodePath: props.node.path,
        currentPath: props.currentPath,
        nodeType: props.node.type,
        active
      })
      return active
    })

    // 监听 currentPath 变化
    watch(() => props.currentPath, (newPath) => {
      console.log('[JavaEditor][FileTreeNode] currentPath prop changed', {
        nodePath: props.node.path,
        newCurrentPath: newPath,
        nodeType: props.node.type
      })
    })

    function handleClick() {
      console.log('[JavaEditor][FileTreeNode] handleClick', { path: props.node.path, type: props.node.type })
      if (props.node.type === 'folder') {
        emitToggle(props.node)
      } else {
        emitSelect(props.node)
      }
    }

    function handleContextMenu(event: MouseEvent) {
      event.preventDefault()
      console.log('[JavaEditor][FileTreeNode] handleContextMenu', { path: props.node.path, type: props.node.type })
      emitContextMenu(event, props.node)
    }

    return () => {
      const isFolder = props.node.type === 'folder'

      const iconSvg = isFolder
        ? h('svg', { 
            viewBox: '0 0 20 20', 
            fill: 'currentColor', 
            class: 'icon',
            style: { 
              width: '1rem', 
              height: '1rem', 
              flexShrink: '0',
              color: '#f59e0b'
            }
          }, [
            props.node.expanded
              ? h('path', { d: 'M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z' })
              : h('path', {
                  'fill-rule': 'evenodd',
                  d: 'M2 6a2 2 0 012-2h4l2 2h4a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z',
                  'clip-rule': 'evenodd'
                })
          ])
        : h('svg', { 
            viewBox: '0 0 20 20', 
            fill: 'currentColor', 
            class: 'icon',
            style: { 
              width: '1rem', 
              height: '1rem', 
              flexShrink: '0',
              color: '#9ca3af'
            }
          }, [
            h('path', {
              'fill-rule': 'evenodd',
              d: 'M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z',
              'clip-rule': 'evenodd'
            })
          ])

      const label = h(
        'div',
        {
          class: {
            'node-label': true,
            active: isActive.value,
            folder: props.node.type === 'folder'
          },
          style: { paddingLeft: `${props.depth * 16 + 8}px` },
          onClick: handleClick,
          onContextmenu: handleContextMenu
        },
        [iconSvg, h('span', props.node.name)]
      )

      let childrenNodes: ReturnType<typeof h>[] = []
      if (isFolder && props.node.expanded && props.node.children) {
        childrenNodes = props.node.children.map(child =>
          h(FileTreeNode, {
            key: child.path,
            node: child,
            currentPath: props.currentPath,
            depth: props.depth + 1,
            onToggle: emitToggle,
            onSelect: emitSelect,
            onContextmenu: emitContextMenu
          })
        )
      }

      return h('div', { class: 'tree-node' }, [label, ...childrenNodes])
    }
  }
})

// ============= 状态管理 =============
const fileTree = ref<FileNode>({
  name: 'src',
  type: 'folder',
  path: 'src',
  expanded: true,
  children: [
    {
      name: 'com',
      type: 'folder',
      path: 'src/com',
      expanded: true,
      children: [
        {
          name: 'example',
          type: 'folder',
          path: 'src/com/example',
          expanded: true,
          children: [
            {
              name: 'MyHook.java',
              type: 'file',
              path: 'src/com/example/MyHook.java',
              content: `package com.example;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class MyHook {
    
    /**
     * Hook 方法示例
     * @param context 上下文对象
     * @param classLoader 类加载器
     */
    public static void hookMethod(Context context, ClassLoader classLoader) {
        // 在这里编写你的 Hook 代码
        
    }
}
`
            }
          ]
        }
      ]
    }
  ]
})

watch(
  fileTree,
  newTree => {
    console.log('[JavaEditor] watch fileTree change', JSON.parse(JSON.stringify(newTree)))
  },
  { deep: true }
)

const currentFilePath = ref<string>('src/com/example/MyHook.java')
const fileContents = ref<Map<string, string>>(new Map())
const compiling = ref(false)
const downloadDisabled = ref(false)
const compileTaskId = ref<string | null>(null)
const compileResult = ref<{
  success: boolean
  message: string
  taskId?: string
} | null>(null)

const editorContainer = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null
let completionProvider: monaco.IDisposable | null = null

// 左侧面板宽度
const panelWidth = ref(280)
const isResizing = ref(false)
const resizeStartX = ref(0)
const resizeStartWidth = ref(0)
const MIN_PANEL_WIDTH = 150
const MAX_PANEL_WIDTH = 600

// 右键菜单状态
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  node: null as FileNode | null
})

// 新建文件/文件夹对话框
const newItemDialog = ref({
  visible: false,
  type: 'file' as 'file' | 'folder',
  name: '',
  parentNode: null as FileNode | null
})

// 重命名对话框
const renameDialog = ref({
  visible: false,
  name: '',
  node: null as FileNode | null
})

// 删除确认对话框
const deleteDialog = ref({
  visible: false,
  node: null as FileNode | null
})

// 下载确认对话框
const downloadConfirmDialog = ref({
  visible: false,
  dontShowAgain: false
})

// 下载提示的 localStorage key
const DOWNLOAD_CONFIRM_KEY = 'download_confirm_disabled'

// 用户编译配额
const userQuota = ref({
  isAdmin: false,
  remaining: 5,
  limit: 5,
  windowHours: 1
})

// 任务列表对话框
const taskListDialog = ref({
  visible: false,
  loading: false,
  tasks: [] as any[]
})

// 删除任务确认对话框
const deleteTaskDialog = ref({
  visible: false,
  task: null as any
})

// 全部删除确认对话框
const deleteAllTasksDialog = ref({
  visible: false
})

// 长按相关
const longPressTimer = ref<number | null>(null)
const isLongPress = ref(false)

const toast = ref({
  show: false,
  message: '',
  type: 'success' as 'success' | 'error'
})

// 自动保存相关
const lastSaveTime = ref<string>('')
const autoSaveTimer = ref<number | null>(null)
const AUTOSAVE_INTERVAL = 2000 // 2秒
const currentUserId = ref<string>('')
const currentHookId = ref<string>('')

// 动态生成存储 key
const getStorageKey = () => {
  const userId = currentUserId.value || 'anonymous'
  const hookId = currentHookId.value || route.value.query.hookId as string || 'default'
  return `java-editor-cache-${userId}-${hookId}`
}

// ============= 工具函数 =============
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// ============= 本地缓存 =============
// 保存到 localStorage
function saveToLocalStorage() {
  try {
    const storageKey = getStorageKey()
    console.log('[JavaEditor] 保存缓存使用 key:', storageKey)
    
    // 保存当前编辑器内容到 fileContents
    if (currentFilePath.value && editor) {
      fileContents.value.set(currentFilePath.value, editor.getValue())
    }
    
    // 准备要保存的数据
    const dataToSave = {
      fileTree: toRaw(fileTree.value),
      fileContents: Array.from(fileContents.value.entries()),
      currentFilePath: currentFilePath.value,
      panelWidth: panelWidth.value,
      compileTaskId: compileTaskId.value, // 保存编译任务ID
      userId: currentUserId.value,
      hookId: currentHookId.value,
      timestamp: new Date().toISOString()
    }
    
    localStorage.setItem(storageKey, JSON.stringify(dataToSave))
    
    const now = new Date()
    lastSaveTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
    
    console.log('[JavaEditor] 已自动保存到本地缓存', lastSaveTime.value)
  } catch (error) {
    console.error('[JavaEditor] 保存到 localStorage 失败:', error)
  }
}

// 从 localStorage 加载
function loadFromLocalStorage() {
  try {
    const storageKey = getStorageKey()
    console.log('[JavaEditor] 加载缓存使用 key:', storageKey)
    
    const savedData = localStorage.getItem(storageKey)
    if (!savedData) {
      console.log('[JavaEditor] 没有找到本地缓存')
      return false
    }
    
    const data = JSON.parse(savedData)
    console.log('[JavaEditor] 找到本地缓存，时间:', data.timestamp)
    
    // 验证缓存是否属于当前用户和 hook
    if (data.userId && data.userId !== currentUserId.value) {
      console.log('[JavaEditor] 缓存用户不匹配，忽略')
      return false
    }
    if (data.hookId && data.hookId !== currentHookId.value) {
      console.log('[JavaEditor] 缓存 Hook 不匹配，忽略')
      return false
    }
    
    // 恢复文件树
    if (data.fileTree) {
      fileTree.value = data.fileTree
    }
    
    // 恢复文件内容
    if (data.fileContents) {
      fileContents.value = new Map(data.fileContents)
    }
    
    // 恢复当前文件路径
    if (data.currentFilePath) {
      currentFilePath.value = data.currentFilePath
    }
    
    // 恢复面板宽度
    if (data.panelWidth) {
      panelWidth.value = data.panelWidth
    }
    
    // 恢复编译任务ID
    if (data.compileTaskId) {
      compileTaskId.value = data.compileTaskId
      console.log('[JavaEditor] 恢复编译任务ID:', data.compileTaskId)
      // 异步检查任务状态
      checkCompileTaskStatus(data.compileTaskId)
    }
    
    const saveDate = new Date(data.timestamp)
    lastSaveTime.value = `${saveDate.getHours().toString().padStart(2, '0')}:${saveDate.getMinutes().toString().padStart(2, '0')}:${saveDate.getSeconds().toString().padStart(2, '0')}`
    
    showToast(`已恢复本地缓存 (${lastSaveTime.value})`, 'success')
    return true
  } catch (error) {
    console.error('[JavaEditor] 从 localStorage 加载失败:', error)
    return false
  }
}

// 清除本地缓存
function clearLocalStorage() {
  try {
    const storageKey = getStorageKey()
    localStorage.removeItem(storageKey)
    lastSaveTime.value = ''
    console.log('[JavaEditor] 已清除本地缓存:', storageKey)
  } catch (error) {
    console.error('[JavaEditor] 清除 localStorage 失败:', error)
  }
}

// 获取当前用户信息
async function fetchCurrentUser() {
  try {
    const response = await http.get('/api/auth/me')
    if (response.data && response.data.username) {
      currentUserId.value = response.data.username
      console.log('[JavaEditor] 当前用户:', currentUserId.value)
    }
  } catch (error) {
    console.error('[JavaEditor] 获取用户信息失败:', error)
  }
}

// 初始化缓存标识
function initializeCacheIdentifiers() {
  // 从 query 参数获取 hookId
  if (route.value.query.hookId) {
    currentHookId.value = route.value.query.hookId as string
    console.log('[JavaEditor] 当前 Hook ID:', currentHookId.value)
  }
}

// 触发自动保存（防抖）
function triggerAutoSave() {
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  autoSaveTimer.value = window.setTimeout(() => {
    saveToLocalStorage()
  }, AUTOSAVE_INTERVAL)
}

// ============= 面板调节大小 =============
function startResize(event: MouseEvent) {
  console.log('[JavaEditor] startResize', {
    clientX: event.clientX,
    currentWidth: panelWidth.value
  })
  isResizing.value = true
  resizeStartX.value = event.clientX
  resizeStartWidth.value = panelWidth.value
  event.preventDefault()
  event.stopPropagation()
}

function handleMouseMove(event: MouseEvent) {
  if (!isResizing.value) return
  
  event.preventDefault()
  
  // 计算鼠标移动的距离
  const deltaX = event.clientX - resizeStartX.value
  const newWidth = resizeStartWidth.value + deltaX
  
  // 限制宽度范围
  if (newWidth >= MIN_PANEL_WIDTH && newWidth <= MAX_PANEL_WIDTH) {
    panelWidth.value = newWidth
  } else if (newWidth < MIN_PANEL_WIDTH) {
    panelWidth.value = MIN_PANEL_WIDTH
  } else if (newWidth > MAX_PANEL_WIDTH) {
    panelWidth.value = MAX_PANEL_WIDTH
  }
}

function stopResize() {
  if (isResizing.value) {
    console.log('[JavaEditor] stopResize', {
      finalWidth: panelWidth.value
    })
    isResizing.value = false
  }
}

// 递归查找节点
function findNode(tree: FileNode, path: string): FileNode | null {
  if (tree.path === path) return tree
  if (tree.children) {
    for (const child of tree.children) {
      const found = findNode(child, path)
      if (found) return found
    }
  }
  return null
}

// 递归收集所有文件
function collectAllFiles(node: FileNode, files: Map<string, string> = new Map()): Map<string, string> {
  if (node.type === 'file') {
    // 去掉 src/ 前缀，只保留包路径
    const relativePath = node.path.replace(/^src\//, '')
    const content = fileContents.value.get(node.path) || node.content || ''
    files.set(relativePath, content)
  } else if (node.children) {
    node.children.forEach(child => collectAllFiles(child, files))
  }
  return files
}

function refreshFileTree() {
  const cloned = JSON.parse(JSON.stringify(toRaw(fileTree.value))) as FileNode
  fileTree.value = cloned
}

// ============= 文件树操作 =============
function toggleFolder(node: FileNode) {
  if (node.type === 'folder') {
    node.expanded = !node.expanded
  }
}

function selectFile(node: FileNode) {
  if (node.type === 'file') {
    console.log('[JavaEditor] selectFile CALLED', {
      nodePath: node.path,
      nodeType: node.type,
      currentFilePath: currentFilePath.value
    })
    
    // 保存当前文件内容
    if (currentFilePath.value && editor) {
      console.log('[JavaEditor] selectFile -> storing content for', currentFilePath.value)
      fileContents.value.set(currentFilePath.value, editor.getValue())
    }
    
    // 切换到新文件
    currentFilePath.value = node.path
    console.log('[JavaEditor] selectFile -> UPDATED currentFilePath to', currentFilePath.value)
    
    const content = fileContents.value.get(node.path) || node.content || ''
    if (editor) {
      console.log('[JavaEditor] selectFile -> setting editor content length', content.length)
      editor.setValue(content)
    }
  }
}

// Watch currentFilePath 变化
watch(currentFilePath, (newPath, oldPath) => {
  console.log('[JavaEditor] currentFilePath changed', {
    from: oldPath,
    to: newPath
  })
})

function showContextMenu(event: MouseEvent, node: FileNode) {
  event.preventDefault()
  console.log('[JavaEditor] showContextMenu', { path: node.path, type: node.type })
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node
  }
}

function hideContextMenu() {
  contextMenu.value.visible = false
}

// 新建包
function showNewFolderDialog(parentNode: FileNode) {
  console.log('[JavaEditor] showNewFolderDialog', { path: parentNode.path })
  newItemDialog.value = {
    visible: true,
    type: 'folder',
    name: '',
    parentNode
  }
  hideContextMenu()
}

// 新建类
function showNewFileDialog(parentNode: FileNode) {
  console.log('[JavaEditor] showNewFileDialog', { path: parentNode.path })
  newItemDialog.value = {
    visible: true,
    type: 'file',
    name: '',
    parentNode
  }
  hideContextMenu()
}

// 确认新建
function confirmNewItem() {
  const { type, name, parentNode } = newItemDialog.value
  console.log('[JavaEditor] confirmNewItem', { type, name, parentPath: parentNode?.path })
  if (!name || !parentNode) return
  
  // 检查是否以 .java 结尾，确定最终是创建文件还是文件夹
  const endsWithJava = name.endsWith('.java')
  const isCreatingFile = type === 'file' || endsWithJava
  
  // 如果以 .java 结尾，先去掉后缀再分割，避免把 .java 当成路径的一部分
  const nameWithoutJava = endsWithJava ? name.slice(0, -5) : name
  const parts = nameWithoutJava.split('.')
  
  console.log('[JavaEditor] confirmNewItem -> parsed', { 
    isCreatingFile, 
    nameWithoutJava, 
    parts 
  })
  
  let currentParent = parentNode
  let currentPath = parentNode.path
  
  // 如果是创建文件且有多级路径，或者是创建包且有多级
  const shouldCreateNestedStructure = parts.length > 1
  
  if (shouldCreateNestedStructure) {
    // 处理中间的包路径部分
    const pathParts = isCreatingFile ? parts.slice(0, -1) : parts
    
    // 逐层创建文件夹
    for (const part of pathParts) {
      if (!part) continue
      
      // 查找或创建子文件夹
      if (!currentParent.children) {
        currentParent.children = []
      }
      
      let childFolder = currentParent.children.find(c => c.name === part && c.type === 'folder')
      
      if (!childFolder) {
        // 创建新文件夹
        const folderPath = `${currentPath}/${part}`
        childFolder = {
          name: part,
          type: 'folder',
          path: folderPath,
          expanded: true,
          children: []
        }
        currentParent.children.push(childFolder)
        console.log('[JavaEditor] confirmNewItem -> created folder', { name: part, path: folderPath })
      }
      
      currentParent.expanded = true
      currentParent = childFolder
      currentPath = childFolder.path
    }
  }
  
  // 如果是创建文件，在最后一层创建文件
  if (isCreatingFile) {
    const lastPart = parts[parts.length - 1]
    if (!lastPart) {
      showToast('文件名不能为空', 'error')
    return
  }
  
    // 统一添加 .java 后缀（前面已经去掉了，所以这里必然不会重复）
    const fileName = `${lastPart}.java`
    const filePath = `${currentPath}/${fileName}`
    
    // 检查文件是否已存在
    if (currentParent.children?.some(c => c.name === fileName && c.type === 'file')) {
      showToast('文件已存在', 'error')
      refreshFileTree()
      return
    }
    
    const newFileNode: FileNode = {
    name: fileName,
      type: 'file',
      path: filePath,
      content: generateClassTemplate(filePath, fileName)
    }
    
    if (!currentParent.children) {
      currentParent.children = []
    }
    currentParent.children.push(newFileNode)
    console.log('[JavaEditor] confirmNewItem -> created file', { name: fileName, path: filePath })
  
  // 强制触发响应式更新
  refreshFileTree()
    
    // 打开新创建的文件
    fileContents.value.set(filePath, newFileNode.content || '')
    selectFile(newFileNode)
    
    showToast('类创建成功')
  } else {
    // 如果是单层创建文件夹
    if (!shouldCreateNestedStructure) {
      const folderName = name
      const folderPath = `${currentPath}/${folderName}`
      
      // 检查是否已存在
      if (currentParent.children?.some(c => c.name === folderName && c.type === 'folder')) {
        showToast('包已存在', 'error')
        return
      }
      
      const newFolderNode: FileNode = {
        name: folderName,
        type: 'folder',
        path: folderPath,
        expanded: true,
        children: []
      }
      
      if (!currentParent.children) {
        currentParent.children = []
      }
      currentParent.children.push(newFolderNode)
      console.log('[JavaEditor] confirmNewItem -> created folder', { name: folderName, path: folderPath })
    }
    
    // 强制触发响应式更新
    refreshFileTree()
    showToast('包创建成功')
  }
  
  newItemDialog.value.visible = false
}

// 生成类模板
function generateClassTemplate(filePath: string, fileName: string): string {
  // 从路径提取包名: src/com/example/MyClass.java -> com.example
  const pathParts = filePath.split('/')
  const packageParts = pathParts.slice(1, -1) // 去掉 src 和文件名
  const packageName = packageParts.join('.')
  const className = fileName.replace('.java', '')
  
  return `package ${packageName};

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ${className} {
    
    // 在这里编写你的代码
    
}
`
}

// 显示重命名对话框
function showRenameDialog(node: FileNode) {
  renameDialog.value = {
    visible: true,
    name: node.name,
    node
  }
  hideContextMenu()
}

// 确认重命名
function confirmRename() {
  const { name, node } = renameDialog.value
  console.log('[JavaEditor] confirmRename', { originalName: node?.name, newName: name, path: node?.path })
  if (!name || !node) return
  
  const newName = node.type === 'file' && !name.endsWith('.java') ? `${name}.java` : name
  const oldPath = node.path
  const newPath = node.path.substring(0, node.path.lastIndexOf('/') + 1) + newName
  
  // 提取旧类名和新类名
  const oldClassName = node.name.replace('.java', '')
  const newClassName = newName.replace('.java', '')
  
  node.name = newName
  node.path = newPath
  
  // 更新文件内容缓存
  if (node.type === 'file' && fileContents.value.has(oldPath)) {
    let content = fileContents.value.get(oldPath)!
    
    // 如果是 .java 文件，自动更新类名
    if (newName.endsWith('.java') && oldClassName !== newClassName) {
      console.log('[JavaEditor] 自动更新类名', { 
        oldClassName, 
        newClassName 
      })
      
      // 替换 public class 旧类名 为 public class 新类名
      const classRegex = new RegExp(`(public\\s+class\\s+)${oldClassName}(\\s*\\{)`, 'g')
      const updatedContent = content.replace(classRegex, `$1${newClassName}$2`)
      
      // 如果替换成功，显示提示
      if (updatedContent !== content) {
        content = updatedContent
        showToast(`已自动更新类名: ${oldClassName} → ${newClassName}`)
        console.log('[JavaEditor] 类名已更新')
      }
    }
    
    fileContents.value.delete(oldPath)
    fileContents.value.set(newPath, content)
    
    // 如果当前正在编辑这个文件，同步更新编辑器
    if (currentFilePath.value === oldPath) {
      currentFilePath.value = newPath
      if (editor) {
        editor.setValue(content)
        console.log('[JavaEditor] 编辑器内容已更新')
      }
    }
  }
  
  // 强制触发响应式更新
  refreshFileTree()
  
  renameDialog.value.visible = false
  showToast('重命名成功')
}

// 显示删除确认对话框
function showDeleteDialog(node: FileNode) {
  deleteDialog.value = {
    visible: true,
    node
  }
  hideContextMenu()
}

// 确认删除
function confirmDelete() {
  const node = deleteDialog.value.node
  if (!node) return
  
  // 找到父节点并删除
  function removeFromParent(tree: FileNode, target: FileNode): boolean {
    if (tree.children) {
      const index = tree.children.findIndex(c => c.path === target.path)
      if (index !== -1) {
        tree.children.splice(index, 1)
        return true
      }
      for (const child of tree.children) {
        if (removeFromParent(child, target)) return true
      }
    }
    return false
  }
  
  removeFromParent(fileTree.value, node)
  
  // 删除文件内容缓存
  if (node.type === 'file') {
    fileContents.value.delete(node.path)
    if (currentFilePath.value === node.path) {
      currentFilePath.value = ''
      if (editor) editor.setValue('')
    }
  }
  
  // 强制触发响应式更新
  refreshFileTree()
  
  deleteDialog.value.visible = false
  showToast('删除成功')
}

// ============= 编译相关 =============
async function handleCompile() {
  // 保存当前编辑器内容
  if (currentFilePath.value && editor) {
    fileContents.value.set(currentFilePath.value, editor.getValue())
  }
  
  // 收集所有文件
  const files = collectAllFiles(fileTree.value)
  
  if (files.size === 0) {
    showToast('没有可编译的文件', 'error')
    return
  }
  
  compiling.value = true
  compileResult.value = null
  
  try {
    // 转换为对象格式
    const filesObj: Record<string, string> = {}
    files.forEach((content, path) => {
      filesObj[path] = content
    })
    
    const response = await http.post('/admin/dex-compile/compile', {
      files: filesObj
    })
    
    if (response.data && response.data.success) {
      compileTaskId.value = response.data.taskId
      compileResult.value = {
        success: true,
        message: '编译成功！',
        taskId: response.data.taskId
      }
      showToast('编译成功！')
      // 立即保存编译任务ID到缓存
      saveToLocalStorage()
      // 刷新配额
      fetchUserQuota()
    } else {
      compileResult.value = {
        success: false,
        message: response.data?.compileLog || response.data?.message || '编译失败'
      }
      showToast('编译失败', 'error')
    }
  } catch (error: any) {
    console.error('编译出错:', error)
    
    // 检查是否是频率限制错误
    if (error.response?.status === 429) {
      const errorData = error.response?.data
      compileResult.value = {
        success: false,
        message: errorData?.message || '访问频率超限！请稍后再试'
      }
      showToast(`访问频率超限！普通用户每小时最多编译 ${userQuota.value.limit} 次`, 'error')
      // 刷新配额显示
      fetchUserQuota()
    } else {
    compileResult.value = {
      success: false,
      message: error.response?.data?.compileLog || error.response?.data?.message || error.message || '编译请求失败'
    }
    showToast('编译请求失败', 'error')
    }
  } finally {
    compiling.value = false
  }
}

function handleDownload() {
  if (!compileTaskId.value) {
    showToast('没有可下载的文件', 'error')
    return
  }
  
  // 检查用户是否选择了不再提示
  const dontShow = localStorage.getItem(DOWNLOAD_CONFIRM_KEY) === 'true'
  if (dontShow) {
    // 直接下载
    confirmDownload()
  } else {
    // 显示下载确认对话框
    downloadConfirmDialog.value.dontShowAgain = false
    downloadConfirmDialog.value.visible = true
  }
}

async function confirmDownload() {
  if (!compileTaskId.value) {
    return
  }
  
  // 保存用户的"不再提示"选择
  if (downloadConfirmDialog.value.dontShowAgain) {
    localStorage.setItem(DOWNLOAD_CONFIRM_KEY, 'true')
  }
  
  downloadConfirmDialog.value.visible = false
  
  try {
    downloadDisabled.value = true
    const response = await http.get(`/admin/dex-compile/download/${compileTaskId.value}`, {
      responseType: 'blob'
    })
    
    const blob = new Blob([response.data], { type: 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'hook.dex'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    showToast('下载成功！此 Dex 文件已从服务器清除')
    compileTaskId.value = null
    compileResult.value = null
    // 立即保存状态到缓存（清除 taskId）
    saveToLocalStorage()
  } catch (error: any) {
    console.error('下载出错:', error)
    if (error.response?.status === 404) {
      showToast('文件不存在或已被下载', 'error')
      compileTaskId.value = null
    } else {
      showToast('下载失败', 'error')
    }
  } finally {
    downloadDisabled.value = false
  }
}

// 检查编译任务状态
async function checkCompileTaskStatus(taskId: string) {
  try {
    console.log('[JavaEditor] 检查编译任务状态:', taskId)
    const response = await http.get(`/admin/dex-compile/task/${taskId}`)
    
    if (response.data && response.data.success) {
      const task = response.data
      
      if (task.downloaded) {
        // 任务已被下载，清除 taskId
        console.log('[JavaEditor] 任务已被下载，清除 taskId')
        compileTaskId.value = null
        compileResult.value = null
      } else if (task.compileSuccess) {
        // 任务编译成功且未下载，恢复编译结果
        console.log('[JavaEditor] 发现未下载的编译任务')
        compileResult.value = {
          success: true,
          message: '编译成功！（已恢复）',
          taskId: taskId
        }
        showToast('发现未下载的编译任务，可以继续下载', 'success')
      } else {
        // 任务编译失败
        console.log('[JavaEditor] 任务编译失败')
        compileResult.value = {
          success: false,
          message: task.log || '编译失败'
        }
        compileTaskId.value = null
      }
    } else {
      // 任务不存在，清除 taskId
      console.log('[JavaEditor] 任务不存在，清除 taskId')
      compileTaskId.value = null
      compileResult.value = null
    }
  } catch (error: any) {
    console.error('[JavaEditor] 检查任务状态失败:', error)
    // 如果是 404，说明任务已被删除
    if (error.response?.status === 404) {
      compileTaskId.value = null
      compileResult.value = null
    }
  }
}

// 查询服务器上未下载的编译任务
async function checkUndownloadedTasks() {
  try {
    console.log('[JavaEditor] 查询未下载的编译任务')
    const response = await http.get('/admin/dex-compile/undownloaded')
    
    if (response.data && response.data.success) {
      const tasks = response.data.tasks
      console.log('[JavaEditor] 找到未下载任务:', tasks.length, '个')
      
      if (tasks.length > 0) {
        // 取最新的一个任务
        const latestTask = tasks[0]
        compileTaskId.value = latestTask.taskId
        compileResult.value = {
          success: true,
          message: `编译成功！（${new Date(latestTask.createTime).toLocaleString()}）`,
          taskId: latestTask.taskId
        }
        
        // 如果有多个未下载任务，给出提示
        if (tasks.length > 1) {
          showToast(`发现 ${tasks.length} 个未下载的编译任务，显示最新的一个`, 'success')
        } else {
          showToast('发现1个未下载的编译任务，可以继续下载', 'success')
        }
        
        // 立即保存到缓存
        saveToLocalStorage()
      }
    }
  } catch (error: any) {
    console.error('[JavaEditor] 查询未下载任务失败:', error)
    // 静默失败，不影响用户体验
  }
}

// 查询用户编译配额
async function fetchUserQuota() {
  try {
    const response = await http.get('/admin/dex-compile/quota')
    
    if (response.data && response.data.success) {
      userQuota.value = {
        isAdmin: response.data.isAdmin,
        remaining: response.data.remaining,
        limit: response.data.limit,
        windowHours: response.data.windowHours
      }
      console.log('[JavaEditor] 用户配额:', userQuota.value)
    }
  } catch (error: any) {
    console.error('[JavaEditor] 查询配额失败:', error)
  }
}

// 显示任务列表
async function showTaskList() {
  taskListDialog.value.visible = true
  taskListDialog.value.loading = true
  
  try {
    const response = await http.get('/admin/dex-compile/tasks')
    
    if (response.data && response.data.success) {
      taskListDialog.value.tasks = response.data.tasks
      console.log('[JavaEditor] 任务列表:', response.data.tasks)
    } else {
      showToast('加载任务列表失败', 'error')
    }
  } catch (error: any) {
    console.error('[JavaEditor] 查询任务列表失败:', error)
    showToast('加载任务列表失败', 'error')
  } finally {
    taskListDialog.value.loading = false
  }
}

// 从任务列表中下载任务
function loadTaskFromList(task: any) {
  // 如果是长按，不执行下载
  if (isLongPress.value) {
    return
  }
  
  if (task.success && !task.downloaded) {
    // 设置当前任务ID
    compileTaskId.value = task.taskId
    compileResult.value = {
      success: true,
      message: `编译成功！（${new Date(task.createTime).toLocaleString()}）`,
      taskId: task.taskId
    }
    // 关闭任务列表
    taskListDialog.value.visible = false
    // 直接触发下载
    handleDownload()
  } else if (task.downloaded) {
    showToast('该任务已被下载', 'error')
  } else {
    showToast('该任务编译失败', 'error')
  }
}

// 长按开始
function handleTaskPress(task: any) {
  isLongPress.value = false
  longPressTimer.value = window.setTimeout(() => {
    isLongPress.value = true
    // 显示删除确认对话框
    deleteTaskDialog.value.task = task
    deleteTaskDialog.value.visible = true
  }, 500) // 500ms 判定为长按
}

// 长按结束/取消
function handleTaskPressEnd() {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
  // 延迟重置长按状态，避免影响点击事件
  setTimeout(() => {
    isLongPress.value = false
  }, 100)
}

// 确认删除任务
async function confirmDeleteTask() {
  const task = deleteTaskDialog.value.task
  if (!task) return
  
  deleteTaskDialog.value.visible = false
  
  try {
    const response = await http.delete(`/admin/dex-compile/task/${task.taskId}`)
    
    if (response.data && response.data.success) {
      showToast('任务已删除', 'success')
      
      // 如果删除的是当前任务，清除状态
      if (compileTaskId.value === task.taskId) {
        compileTaskId.value = null
        compileResult.value = null
      }
      
      // 刷新任务列表
      showTaskList()
    } else {
      showToast(response.data?.message || '删除失败', 'error')
    }
  } catch (error: any) {
    console.error('[JavaEditor] 删除任务失败:', error)
    showToast(error.response?.data?.message || '删除失败', 'error')
  }
}

// 显示全部删除确认对话框
function showDeleteAllTasksDialog() {
  if (taskListDialog.value.tasks.length === 0) {
    showToast('没有可删除的任务', 'error')
    return
  }
  deleteAllTasksDialog.value.visible = true
}

// 确认全部删除
async function confirmDeleteAllTasks() {
  deleteAllTasksDialog.value.visible = false
  
  try {
    const response = await http.delete('/admin/dex-compile/tasks/all')
    
    if (response.data && response.data.success) {
      showToast(`已删除 ${response.data.deletedCount} 个任务`, 'success')
      
      // 清除当前任务状态
      compileTaskId.value = null
      compileResult.value = null
      
      // 刷新任务列表
      showTaskList()
    } else {
      showToast(response.data?.message || '删除失败', 'error')
    }
  } catch (error: any) {
    console.error('[JavaEditor] 批量删除任务失败:', error)
    showToast(error.response?.data?.message || '删除失败', 'error')
  }
}

// 格式化任务状态
function getTaskStatusText(task: any) {
  if (task.downloaded) {
    return '已下载'
  } else if (task.success) {
    return '可下载'
  } else {
    return '编译失败'
  }
}

// 获取任务状态样式
function getTaskStatusClass(task: any) {
  if (task.downloaded) {
    return 'status-downloaded'
  } else if (task.success) {
    return 'status-available'
  } else {
    return 'status-failed'
  }
}

function goBack() {
  router.back()
}

// ============= Monaco Editor 初始化 =============
onMounted(async () => {
  // 初始化缓存标识（用户ID和Hook ID）
  initializeCacheIdentifiers()
  await fetchCurrentUser()
  
  // 尝试从本地缓存加载
  const loadedFromCache = loadFromLocalStorage()
  
  // 如果没有缓存，加载初始文件
  if (!loadedFromCache) {
  const initialFile = findNode(fileTree.value, currentFilePath.value)
  if (initialFile && initialFile.type === 'file') {
    fileContents.value.set(initialFile.path, initialFile.content || '')
    }
  }
  
  // 查询服务器上未下载的编译任务（即使本地有缓存也要查询，以防用户在其他设备编译）
  checkUndownloadedTasks()
  
  // 查询用户编译配额
  fetchUserQuota()
  
  // 初始化 Monaco Editor
  if (editorContainer.value) {
    const initialContent = fileContents.value.get(currentFilePath.value) || ''
    editor = monaco.editor.create(editorContainer.value, {
      value: initialContent,
      language: 'java',
      theme: 'vs',
      fontSize: 14,
      minimap: { enabled: false },
      automaticLayout: true,
      scrollBeyondLastLine: false,
      lineNumbers: 'on',
      roundedSelection: false,
      readOnly: false,
      cursorStyle: 'line',
      wordWrap: 'on',
      tabSize: 4,
      suggestOnTriggerCharacters: true,
      quickSuggestions: {
        other: true,
        comments: false,
        strings: false
      }
    })

    // 添加 Android 和 Xposed 的代码补全提示
    // 先清理之前的补全提供器（如果存在）
    if (completionProvider) {
      completionProvider.dispose()
    }
    
    completionProvider = monaco.languages.registerCompletionItemProvider('java', {
      provideCompletionItems: () => {
        const suggestions = [
          // === Xposed 核心类 ===
          {
            label: 'XC_MethodHook',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'XC_MethodHook',
            documentation: 'Xposed method hook class',
            range: null as any
          },
          {
            label: 'XC_MethodReplacement',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'XC_MethodReplacement',
            documentation: 'Replace method implementation completely',
            range: null as any
          },
          {
            label: 'XposedHelpers',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'XposedHelpers',
            documentation: 'Xposed helper methods',
            range: null as any
          },
          {
            label: 'XposedBridge',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'XposedBridge',
            documentation: 'Xposed bridge class',
            range: null as any
          },

          // === Xposed 方法模板 ===
          {
            label: 'beforeHookedMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'protected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n\t${1}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Called before the hooked method',
            range: null as any
          },
          {
            label: 'afterHookedMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'protected void afterHookedMethod(MethodHookParam param) throws Throwable {\n\t${1}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Called after the hooked method',
            range: null as any
          },
          {
            label: 'replaceHookedMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {\n\t${1:return null;}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Replace the hooked method completely',
            range: null as any
          },

          // === XposedHelpers 常用方法 ===
          {
            label: 'XposedHelpers.findAndHookMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.findAndHookMethod("${1:className}", classLoader, "${2:methodName}", ${3:paramTypes}, new XC_MethodHook() {\n\t@Override\n\tprotected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n\t\t${4}\n\t}\n});',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Find and hook a method',
            range: null as any
          },
          {
            label: 'XposedHelpers.findClass',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.findClass("${1:className}", ${2:classLoader})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Find a class by name',
            range: null as any
          },
          {
            label: 'XposedHelpers.callMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.callMethod(${1:obj}, "${2:methodName}"${3:, args})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Call an object method',
            range: null as any
          },
          {
            label: 'XposedHelpers.callStaticMethod',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.callStaticMethod(${1:clazz}, "${2:methodName}"${3:, args})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Call a static method',
            range: null as any
          },
          {
            label: 'XposedHelpers.getObjectField',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.getObjectField(${1:obj}, "${2:fieldName}")',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Get object field value',
            range: null as any
          },
          {
            label: 'XposedHelpers.setObjectField',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.setObjectField(${1:obj}, "${2:fieldName}", ${3:value})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Set object field value',
            range: null as any
          },
          {
            label: 'XposedHelpers.getStaticObjectField',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.getStaticObjectField(${1:clazz}, "${2:fieldName}")',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Get static field value',
            range: null as any
          },
          {
            label: 'XposedHelpers.setStaticObjectField',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.setStaticObjectField(${1:clazz}, "${2:fieldName}", ${3:value})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Set static field value',
            range: null as any
          },
          {
            label: 'XposedHelpers.newInstance',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedHelpers.newInstance(${1:clazz}${2:, args})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Create a new instance',
            range: null as any
          },

          // === XposedBridge 方法 ===
          {
            label: 'XposedBridge.log',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedBridge.log("${1:message}")',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Log message to Xposed log',
            range: null as any
          },
          {
            label: 'XposedBridge.hookAllMethods',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedBridge.hookAllMethods(${1:clazz}, "${2:methodName}", new XC_MethodHook() {\n\t@Override\n\tprotected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n\t\t${3}\n\t}\n});',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Hook all methods with same name',
            range: null as any
          },
          {
            label: 'XposedBridge.hookAllConstructors',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'XposedBridge.hookAllConstructors(${1:clazz}, new XC_MethodHook() {\n\t@Override\n\tprotected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n\t\t${2}\n\t}\n});',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Hook all constructors',
            range: null as any
          },

          // === Android 核心类 ===
          {
            label: 'Context',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Context',
            documentation: 'Android Context class',
            range: null as any
          },
          {
            label: 'Activity',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Activity',
            documentation: 'Android Activity class',
            range: null as any
          },
          {
            label: 'Intent',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Intent',
            documentation: 'Android Intent class',
            range: null as any
          },
          {
            label: 'Bundle',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Bundle',
            documentation: 'Android Bundle class',
            range: null as any
          },
          {
            label: 'View',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'View',
            documentation: 'Android View class',
            range: null as any
          },
          {
            label: 'TextView',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'TextView',
            documentation: 'Android TextView class',
            range: null as any
          },
          {
            label: 'Toast',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Toast',
            documentation: 'Android Toast class',
            range: null as any
          },
          {
            label: 'Log',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Log',
            documentation: 'Android Log class',
            range: null as any
          },

          // === Android 常用方法 ===
          {
            label: 'Toast.makeText',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'Toast.makeText(${1:context}, "${2:text}", Toast.LENGTH_SHORT).show()',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Show a toast message',
            range: null as any
          },
          {
            label: 'Log.d',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'Log.d("${1:TAG}", "${2:message}")',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Debug log',
            range: null as any
          },
          {
            label: 'Log.e',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'Log.e("${1:TAG}", "${2:message}"${3:, throwable})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Error log',
            range: null as any
          },

          // === Java 基础类 ===
          {
            label: 'String',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'String',
            documentation: 'Java String class',
            range: null as any
          },
          {
            label: 'ClassLoader',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'ClassLoader',
            documentation: 'Java ClassLoader',
            range: null as any
          },
          {
            label: 'Class',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Class',
            documentation: 'Java Class',
            range: null as any
          },
          {
            label: 'Object',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'Object',
            documentation: 'Java Object class',
            range: null as any
          },
          {
            label: 'List',
            kind: monaco.languages.CompletionItemKind.Interface,
            insertText: 'List',
            documentation: 'Java List interface',
            range: null as any
          },
          {
            label: 'Map',
            kind: monaco.languages.CompletionItemKind.Interface,
            insertText: 'Map',
            documentation: 'Java Map interface',
            range: null as any
          },
          {
            label: 'ArrayList',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'ArrayList',
            documentation: 'Java ArrayList class',
            range: null as any
          },
          {
            label: 'HashMap',
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: 'HashMap',
            documentation: 'Java HashMap class',
            range: null as any
          },

          // === 常用代码片段 ===
          {
            label: 'try-catch',
            kind: monaco.languages.CompletionItemKind.Snippet,
            insertText: 'try {\n\t${1}\n} catch (${2:Exception} e) {\n\t${3:e.printStackTrace();}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Try-catch block',
            range: null as any
          },
          {
            label: 'if-null',
            kind: monaco.languages.CompletionItemKind.Snippet,
            insertText: 'if (${1:obj} == null) {\n\t${2}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'If null check',
            range: null as any
          },
          {
            label: 'if-not-null',
            kind: monaco.languages.CompletionItemKind.Snippet,
            insertText: 'if (${1:obj} != null) {\n\t${2}\n}',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'If not null check',
            range: null as any
          },
          {
            label: 'param.setResult',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'param.setResult(${1:value})',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Set method return value',
            range: null as any
          },
          {
            label: 'param.args',
            kind: monaco.languages.CompletionItemKind.Property,
            insertText: 'param.args[${1:0}]',
            insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
            documentation: 'Get method argument',
            range: null as any
          },
          {
            label: 'param.getResult',
            kind: monaco.languages.CompletionItemKind.Method,
            insertText: 'param.getResult()',
            documentation: 'Get method return value',
            range: null as any
          },
          {
            label: 'param.thisObject',
            kind: monaco.languages.CompletionItemKind.Property,
            insertText: 'param.thisObject',
            documentation: 'Get the this object',
            range: null as any
          }
        ]
        return { suggestions }
      }
    })
    
    // 监听编辑器内容变化，触发自动保存
    editor.onDidChangeModelContent(() => {
      triggerAutoSave()
    })
  }
  
  // 点击其他地方关闭右键菜单
  document.addEventListener('click', hideContextMenu)
  
  // 添加拖动事件监听
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', stopResize)
})

onBeforeUnmount(() => {
  // 保存一次再卸载
  saveToLocalStorage()
  
  // 清理定时器
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  if (editor) {
    editor.dispose()
  }
  
  // 清理代码补全提供器
  if (completionProvider) {
    completionProvider.dispose()
    completionProvider = null
  }
  
  document.removeEventListener('click', hideContextMenu)
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', stopResize)
})
</script>

<template>
  <div class="java-editor-page" :class="{ resizing: isResizing }" @click="hideContextMenu">
    <!-- 头部 -->
    <div class="header">
      <button @click="goBack" class="btn-back">
        <svg viewBox="0 0 20 20" fill="currentColor" style="width: 1rem; height: 1rem;">
          <path fill-rule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
        </svg>
        返回
      </button>
      <h1 class="title">Java Hook 编辑器</h1>
      <div class="auto-save-status" v-if="lastSaveTime">
        <svg viewBox="0 0 20 20" fill="currentColor" class="save-icon">
          <path d="M7.707 10.293a1 1 0 10-1.414 1.414l3 3a1 1 0 001.414 0l3-3a1 1 0 00-1.414-1.414L11 11.586V6h5a2 2 0 012 2v7a2 2 0 01-2 2H4a2 2 0 01-2-2V8a2 2 0 012-2h5v5.586l-1.293-1.293zM9 4a1 1 0 012 0v2H9V4z" />
        </svg>
        <span>已保存 {{ lastSaveTime }}</span>
        <button @click="clearLocalStorage(); showToast('已清除缓存')" class="btn-clear-cache" title="清除当前缓存">
          <svg viewBox="0 0 20 20" fill="currentColor" style="width: 0.875rem; height: 0.875rem;">
            <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>
      <div class="actions">
        <button @click="handleCompile" :disabled="compiling" class="btn-compile">
          {{ compiling ? '编译中...' : '编译' }}
        </button>
        <button 
          v-if="compileTaskId" 
          @click="handleDownload" 
          :disabled="downloadDisabled"
          class="btn-download"
        >
          下载 Dex
        </button>
        <button @click="showTaskList" class="btn-tasks">
          任务
        </button>
        <span class="quota-info" :title="userQuota.isAdmin ? '管理员无编译次数限制' : `每${userQuota.windowHours}小时最多编译${userQuota.limit}次`">
          剩余: {{ userQuota.isAdmin ? '∞' : userQuota.remaining }}/{{ userQuota.isAdmin ? '∞' : userQuota.limit }}
        </span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 文件树 -->
      <div class="file-tree-panel" :style="{ width: panelWidth + 'px' }">
        <div class="file-tree-header">
          <span>项目文件</span>
          <button @click="showNewFolderDialog(fileTree)" class="btn-new" title="新建包">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z" />
            </svg>
          </button>
          <button @click="showNewFileDialog(fileTree)" class="btn-new" title="新建类">
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
        <div class="file-tree">
          <FileTreeNode 
            :node="fileTree" 
            @toggle="toggleFolder"
            @select="selectFile"
            @contextmenu="showContextMenu"
            :currentPath="currentFilePath"
          />
        </div>
      </div>

      <!-- 拖动分隔条 -->
      <div 
        class="resize-handle" 
        @mousedown="startResize"
        :class="{ resizing: isResizing }"
      ></div>

      <!-- 编辑器 -->
      <div class="editor-panel">
        <div class="editor-header">
          <span v-if="currentFilePath">{{ currentFilePath }}</span>
          <span v-else class="placeholder">请选择一个文件</span>
        </div>
        <div ref="editorContainer" class="monaco-editor-container"></div>
      </div>
    </div>

    <!-- 编译结果 -->
    <div v-if="compileResult" class="compile-result" :class="compileResult.success ? 'success' : 'error'">
      <div class="result-header">
        <span>{{ compileResult.success ? '✓ 编译成功' : '✗ 编译失败' }}</span>
        <button @click="compileResult = null" class="btn-close">×</button>
      </div>
      <pre class="result-message">{{ compileResult.message }}</pre>
    </div>

    <!-- 右键菜单 -->
    <div 
      v-if="contextMenu.visible && contextMenu.node" 
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div v-if="contextMenu.node.type === 'folder'" class="menu-item" @click="showNewFolderDialog(contextMenu.node)">
        新建包
      </div>
      <div v-if="contextMenu.node.type === 'folder'" class="menu-item" @click="showNewFileDialog(contextMenu.node)">
        新建类
      </div>
      <div class="menu-item" @click="showRenameDialog(contextMenu.node)">
        重命名
      </div>
      <div v-if="contextMenu.node.path !== 'src'" class="menu-item danger" @click="showDeleteDialog(contextMenu.node)">
        删除
      </div>
    </div>

    <!-- 新建对话框 -->
    <div v-if="newItemDialog.visible" class="dialog-overlay" @click="newItemDialog.visible = false">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ newItemDialog.type === 'file' ? '新建类' : '新建包' }}</h3>
          <button @click="newItemDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <input 
            v-model="newItemDialog.name" 
            :placeholder="newItemDialog.type === 'file' ? '输入类名或路径（如：Test 或 com.xy.Test.java）' : '输入包名（如：utils 或 com.xy.utils）'"
            @keyup.enter="confirmNewItem"
            autofocus
          />
          <div class="dialog-hint">
            {{ newItemDialog.type === 'file' ? '支持多级创建，如：com.xy.example.Test 会自动创建包结构' : '支持多级创建，如：com.xy.utils 会创建嵌套包' }}
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="newItemDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmNewItem" class="btn-confirm">确定</button>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div v-if="renameDialog.visible" class="dialog-overlay" @click="renameDialog.visible = false">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>重命名</h3>
          <button @click="renameDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <input 
            v-model="renameDialog.name" 
            @keyup.enter="confirmRename"
            autofocus
          />
        </div>
        <div class="dialog-footer">
          <button @click="renameDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmRename" class="btn-confirm">确定</button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="deleteDialog.visible" class="dialog-overlay" @click="deleteDialog.visible = false">
      <div class="dialog dialog-confirm" @click.stop>
        <div class="dialog-header">
          <h3>确认删除</h3>
          <button @click="deleteDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <div class="confirm-message">
            <svg viewBox="0 0 20 20" fill="currentColor" class="warning-icon">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div>
              <p class="confirm-title">确定要删除 <strong>{{ deleteDialog.node?.name }}</strong> 吗？</p>
              <p class="confirm-subtitle" v-if="deleteDialog.node?.type === 'folder'">此操作将删除文件夹及其所有内容</p>
              <p class="confirm-subtitle" v-else>此操作不可撤销</p>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="deleteDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmDelete" class="btn-danger">删除</button>
        </div>
      </div>
    </div>

    <!-- 下载确认对话框 -->
    <div v-if="downloadConfirmDialog.visible" class="dialog-overlay" @click="downloadConfirmDialog.visible = false">
      <div class="dialog dialog-confirm" @click.stop>
        <div class="dialog-header">
          <h3>确认下载</h3>
          <button @click="downloadConfirmDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <div class="confirm-message">
            <svg viewBox="0 0 20 20" fill="currentColor" class="warning-icon">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div>
              <p class="confirm-title"><strong>注意：每个编译任务只能下载一次！</strong></p>
              <p class="confirm-subtitle">下载后服务器将立即清除此文件</p>
              <p class="confirm-subtitle">无法再次下载，请妥善保存</p>
              <p class="confirm-subtitle" style="margin-top: 0.75rem; color: #111827;">文件名：<strong>hook.dex</strong></p>
            </div>
          </div>
          <div class="checkbox-container">
            <label class="checkbox-label">
              <input type="checkbox" v-model="downloadConfirmDialog.dontShowAgain" />
              <span>不再提示</span>
            </label>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="downloadConfirmDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmDownload" class="btn-confirm">确认下载</button>
        </div>
      </div>
    </div>

    <!-- 任务列表对话框 -->
    <div v-if="taskListDialog.visible" class="dialog-overlay" @click="taskListDialog.visible = false">
      <div class="dialog dialog-tasks" @click.stop>
        <div class="dialog-header">
          <h3>编译任务列表</h3>
          <button @click="taskListDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="task-hint">💡 点击下载，长按删除</div>
        <div class="dialog-body">
          <div v-if="taskListDialog.loading" class="task-loading">
            加载中...
          </div>
          <div v-else-if="taskListDialog.tasks.length === 0" class="task-empty">
            暂无编译任务
          </div>
          <div v-else class="task-list">
            <div 
              v-for="task in taskListDialog.tasks" 
              :key="task.taskId" 
              class="task-item"
              :class="{ 
                'task-item-clickable': task.success && !task.downloaded,
                'task-item-current': task.taskId === compileTaskId
              }"
              @click="loadTaskFromList(task)"
              @mousedown="handleTaskPress(task)"
              @mouseup="handleTaskPressEnd"
              @mouseleave="handleTaskPressEnd"
              @touchstart="handleTaskPress(task)"
              @touchend="handleTaskPressEnd"
              @touchcancel="handleTaskPressEnd"
            >
              <div class="task-info">
                <div class="task-time">
                  {{ new Date(task.createTime).toLocaleString() }}
                  <span v-if="task.taskId === compileTaskId" class="current-badge">当前</span>
                </div>
                <div class="task-id">ID: {{ task.taskId.substring(0, 8) }}...</div>
              </div>
              <div class="task-status" :class="getTaskStatusClass(task)">
                {{ getTaskStatusText(task) }}
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="showDeleteAllTasksDialog" class="btn-danger" :disabled="taskListDialog.tasks.length === 0">
            全部删除
          </button>
          <button @click="taskListDialog.visible = false" class="btn-cancel">关闭</button>
        </div>
      </div>
    </div>

    <!-- 删除任务确认对话框 -->
    <div v-if="deleteTaskDialog.visible" class="dialog-overlay" @click="deleteTaskDialog.visible = false">
      <div class="dialog dialog-confirm" @click.stop>
        <div class="dialog-header">
          <h3>确认删除任务</h3>
          <button @click="deleteTaskDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <div class="confirm-message">
            <svg viewBox="0 0 20 20" fill="currentColor" class="warning-icon">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div>
              <p class="confirm-title"><strong>确定要删除这个编译任务吗？</strong></p>
              <p class="confirm-subtitle" v-if="deleteTaskDialog.task && !deleteTaskDialog.task.downloaded">
                此任务尚未下载，删除后对应的 DEX 文件也将被删除
              </p>
              <p class="confirm-subtitle" v-else>
                此任务已被下载，删除只会清除记录
              </p>
              <p class="confirm-subtitle" style="margin-top: 0.5rem;">
                任务时间：{{ deleteTaskDialog.task ? new Date(deleteTaskDialog.task.createTime).toLocaleString() : '' }}
              </p>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="deleteTaskDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmDeleteTask" class="btn-danger">删除</button>
        </div>
      </div>
    </div>

    <!-- 全部删除确认对话框 -->
    <div v-if="deleteAllTasksDialog.visible" class="dialog-overlay" @click="deleteAllTasksDialog.visible = false">
      <div class="dialog dialog-confirm" @click.stop>
        <div class="dialog-header">
          <h3>确认全部删除</h3>
          <button @click="deleteAllTasksDialog.visible = false" class="btn-close">×</button>
        </div>
        <div class="dialog-body">
          <div class="confirm-message">
            <svg viewBox="0 0 20 20" fill="currentColor" class="warning-icon">
              <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
            </svg>
            <div>
              <p class="confirm-title"><strong>确定要删除所有编译任务吗？</strong></p>
              <p class="confirm-subtitle">
                此操作将删除您的 <strong>{{ taskListDialog.tasks.length }}</strong> 个编译任务
              </p>
              <p class="confirm-subtitle">
                未下载的任务对应的 DEX 文件也将被删除
              </p>
              <p class="confirm-subtitle" style="color: #dc2626; font-weight: 600; margin-top: 0.75rem;">
                ⚠️ 此操作不可恢复！
              </p>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="deleteAllTasksDialog.visible = false" class="btn-cancel">取消</button>
          <button @click="confirmDeleteAllTasks" class="btn-danger">全部删除</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="toast.show" class="toast" :class="toast.type">
      {{ toast.message }}
    </div>
  </div>
</template>

<style scoped>
.java-editor-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.java-editor-page.resizing {
  cursor: col-resize !important;
}

.java-editor-page.resizing * {
  cursor: col-resize !important;
  user-select: none !important;
  pointer-events: none;
}

.java-editor-page.resizing .resize-handle {
  pointer-events: auto;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 2rem;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #374151;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.auto-save-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.75rem;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 6px;
  font-size: 0.75rem;
  color: #16a34a;
  margin-left: 1rem;
  flex: 1;
}

.auto-save-status .save-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.btn-clear-cache {
  margin-left: auto;
  padding: 0.25rem;
  border: none;
  background: transparent;
  color: #16a34a;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.btn-clear-cache:hover {
  background: rgba(22, 163, 74, 0.1);
  color: #15803d;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.btn-compile, .btn-download, .btn-tasks {
  padding: 0.5rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-compile {
  background: #111827;
  color: white;
}

.btn-compile:hover:not(:disabled) {
  background: #1f2937;
}

.btn-compile:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-download {
  background: #10b981;
  color: white;
}

.btn-download:hover:not(:disabled) {
  background: #059669;
}

.btn-download:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-tasks {
  background: #3b82f6;
  color: white;
}

.btn-tasks:hover {
  background: #2563eb;
}

.quota-info {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 500;
  cursor: help;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 1px;
  background: #e5e7eb;
  min-height: 0;
}

.file-tree-panel {
  min-width: 150px;
  max-width: 600px;
  background: white;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.file-tree-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
}

.file-tree-header span {
  flex: 1;
}

.btn-new {
  padding: 0.25rem;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.btn-new:hover {
  background: #f3f4f6;
  color: #111827;
}

.btn-new svg {
  width: 1rem;
  height: 1rem;
}

.file-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 0;
}

.tree-node {
  user-select: none;
}

:deep(.node-label) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: #374151;
  transition: all 0.15s;
  position: relative;
}

:deep(.node-label:hover) {
  background: #f3f4f6;
}

:deep(.node-label:active) {
  background: #e5e7eb;
}

:deep(.node-label.active) {
  background: #dbeafe;
  color: #1e40af;
  font-weight: 600;
  border-left: 3px solid #3b82f6;
  padding-left: calc(0.5rem - 3px);
}

:deep(.node-label.active .icon) {
  color: #3b82f6 !important;
}

:deep(.node-label .icon) {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  color: #9ca3af;
}

:deep(.node-label.folder .icon) {
  color: #f59e0b;
}

.resize-handle {
  width: 4px;
  background: #e5e7eb;
  cursor: col-resize;
  position: relative;
  flex-shrink: 0;
  transition: background 0.2s;
}

.resize-handle:hover,
.resize-handle.resizing {
  background: #3b82f6;
}

.resize-handle::before {
  content: '';
  position: absolute;
  top: 0;
  left: -2px;
  right: -2px;
  bottom: 0;
}

.editor-panel {
  flex: 1;
  background: white;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  font-size: 0.875rem;
  color: #374151;
}

.editor-header .placeholder {
  color: #9ca3af;
}

.monaco-editor-container {
  flex: 1;
  min-height: 0;
}

.compile-result {
  margin: 1rem 2rem;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid;
}

.compile-result.success {
  background: #f0fdf4;
  border-color: #86efac;
}

.compile-result.error {
  background: #fef2f2;
  border-color: #fca5a5;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  font-weight: 600;
}

.compile-result.success .result-header {
  color: #16a34a;
  background: #dcfce7;
}

.compile-result.error .result-header {
  color: #dc2626;
  background: #fee2e2;
}

.btn-close {
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  color: inherit;
  padding: 0;
  width: 1.5rem;
  height: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-message {
  padding: 1rem;
  margin: 0;
  font-size: 0.875rem;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Consolas', 'Monaco', monospace;
}

.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 150px;
  overflow: hidden;
}

.menu-item {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: #374151;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #f3f4f6;
}

.menu-item.danger {
  color: #dc2626;
}

.menu-item.danger:hover {
  background: #fee2e2;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog {
  background: white;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  min-width: 400px;
  max-width: 500px;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.dialog-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
}

.dialog-body {
  padding: 1.5rem;
}

.dialog-body input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.dialog-body input:focus {
  border-color: #3b82f6;
}

.dialog-hint {
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: #6b7280;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  gap: 0.5rem;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.dialog-footer .btn-danger:disabled {
  background: #d1d5db;
  color: #6b7280;
  cursor: not-allowed;
  opacity: 0.6;
}

.dialog-footer .btn-danger:disabled:hover {
  background: #d1d5db;
  transform: none;
}

.btn-cancel,
.btn-confirm {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: white;
  border: 1px solid #d1d5db;
  color: #374151;
}

.btn-cancel:hover {
  background: #f9fafb;
}

.btn-confirm {
  background: #3b82f6;
  color: white;
}

.btn-confirm:hover {
  background: #2563eb;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

.dialog-confirm .confirm-message {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.dialog-confirm .warning-icon {
  width: 3rem;
  height: 3rem;
  color: #f59e0b;
  flex-shrink: 0;
}

.dialog-confirm .confirm-title {
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
  color: #111827;
  font-weight: 500;
}

.dialog-confirm .confirm-title strong {
  color: #ef4444;
  font-weight: 600;
}

.dialog-confirm .confirm-subtitle {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.checkbox-container {
  margin-top: 1.25rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: #374151;
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
  accent-color: #3b82f6;
}

.checkbox-label:hover {
  color: #111827;
}

/* 任务列表对话框 */
.dialog-tasks {
  width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.task-hint {
  padding: 0.75rem 1.5rem;
  background: #fffbeb;
  border-bottom: 1px solid #fef3c7;
  color: #92400e;
  font-size: 0.875rem;
  text-align: center;
}

.dialog-tasks .dialog-body {
  flex: 1;
  overflow-y: auto;
  min-height: 300px;
  max-height: 500px;
}

.task-loading, .task-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #9ca3af;
  font-size: 0.875rem;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  user-select: none;
  transition: all 0.2s;
}

.task-item-clickable {
  cursor: pointer;
}

.task-item-clickable:hover {
  background: #f3f4f6;
  border-color: #3b82f6;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.task-item-current {
  background: #eff6ff;
  border: 2px solid #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.task-info {
  flex: 1;
}

.task-time {
  font-size: 0.875rem;
  font-weight: 500;
  color: #111827;
  margin-bottom: 0.25rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.current-badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  background: #3b82f6;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 9999px;
}

.task-id {
  font-size: 0.75rem;
  color: #6b7280;
  font-family: monospace;
}

.task-status {
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-align: center;
  min-width: 70px;
}

.status-available {
  background: #d1fae5;
  color: #065f46;
}

.status-downloaded {
  background: #e5e7eb;
  color: #6b7280;
}

.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  font-size: 0.875rem;
  font-weight: 500;
  z-index: 3000;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast.success {
  background: #10b981;
  color: white;
}

.toast.error {
  background: #ef4444;
  color: white;
}
</style>
