<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import http from '../utils/http'

type ApiType = 'redeem' | 'notice'

const loading = ref(false)
const appInfo = ref<any>(null)
const activeTab = ref<ApiType>('redeem')
const codeTab = ref<'java' | 'js' | 'python'>('java')

// 调用表单
const redeemForm = ref({
  code: '',
  machine: ''
})

// 调用结果
const redeemResult = ref<any>(null)
const noticeResult = ref<any>(null)
const decryptedRedeemResult = ref<any>(null)
const decryptedNoticeResult = ref<any>(null)
// 调用结果状态
const redeemSuccess = ref<boolean>(false)
const noticeSuccess = ref<boolean>(false)

// Toast
const toast = ref({
  show: false,
  message: '',
  type: 'success' as 'success' | 'error' | 'info'
})

function showToast(message: string, type: 'success' | 'error' | 'info' = 'success') {
  toast.value = { show: true, message, type }
  setTimeout(() => { toast.value.show = false }, 3000)
}

// 加载应用信息
async function loadApp() {
  try {
    const resp = await http.get('/admin/apps/api/current-app')
    if (resp.data && resp.data.id) {
      appInfo.value = resp.data
      console.log('✅ 已加载应用信息:', resp.data)
    } else {
      console.warn('⚠️ 未选择应用，请先在"应用"页面点击一个应用')
      appInfo.value = null
    }
    
    // MD5 测试 - 验证实现是否正确
    const testMd5 = md5('hello')
    console.log('MD5("hello") =', testMd5)
    console.log('期望值: 5d41402abc4b2a76b9719d911017c592')
    console.log('MD5 实现', testMd5 === '5d41402abc4b2a76b9719d911017c592' ? '✅ 正确' : '❌ 错误')
  } catch (e) {
    console.error('加载应用信息失败:', e)
  }
}

// MD5 哈希实现（经过验证的正确实现）
// @ts-nocheck
function md5(str: string): string {
  function rotateLeft(n: number, s: number): number {
    return (n << s) | (n >>> (32 - s))
  }
  
  function addUnsigned(x: number, y: number): number {
    const x8 = x & 0x80000000
    const y8 = y & 0x80000000
    const x4 = x & 0x40000000
    const y4 = y & 0x40000000
    const result = (x & 0x3FFFFFFF) + (y & 0x3FFFFFFF)
    if (x4 & y4) return result ^ 0x80000000 ^ x8 ^ y8
    if (x4 | y4) {
      if (result & 0x40000000) return result ^ 0xC0000000 ^ x8 ^ y8
      else return result ^ 0x40000000 ^ x8 ^ y8
    } else {
      return result ^ x8 ^ y8
    }
  }
  
  function F(x: number, y: number, z: number): number { return (x & y) | ((~x) & z) }
  function G(x: number, y: number, z: number): number { return (x & z) | (y & (~z)) }
  function H(x: number, y: number, z: number): number { return x ^ y ^ z }
  function I(x: number, y: number, z: number): number { return y ^ (x | (~z)) }
  
  function FF(a: number, b: number, c: number, d: number, x: number, s: number, ac: number): number {
    a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  
  function GG(a: number, b: number, c: number, d: number, x: number, s: number, ac: number): number {
    a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  
  function HH(a: number, b: number, c: number, d: number, x: number, s: number, ac: number): number {
    a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  
  function II(a: number, b: number, c: number, d: number, x: number, s: number, ac: number): number {
    a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac))
    return addUnsigned(rotateLeft(a, s), b)
  }
  
  function convertToWordArray(str: string): number[] {
    let lWordCount: number
    const lMessageLength = str.length
    const lNumberOfWords_temp1 = lMessageLength + 8
    const lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64
    const lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16
    const lWordArray: number[] = Array(lNumberOfWords - 1).fill(0)
    let lBytePosition = 0
    let lByteCount = 0
    
    while (lByteCount < lMessageLength) {
      lWordCount = (lByteCount - (lByteCount % 4)) / 4
      lBytePosition = (lByteCount % 4) * 8
      lWordArray[lWordCount] = lWordArray[lWordCount] | (str.charCodeAt(lByteCount) << lBytePosition)
      lByteCount++
    }
    
    lWordCount = (lByteCount - (lByteCount % 4)) / 4
    lBytePosition = (lByteCount % 4) * 8
    lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition)
    lWordArray[lNumberOfWords - 2] = lMessageLength << 3
    lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29
    return lWordArray
  }
  
  function wordToHex(lValue: number): string {
    let WordToHexValue = ''
    let WordToHexValue_temp = ''
    let lByte: number, lCount: number
    for (lCount = 0; lCount <= 3; lCount++) {
      lByte = (lValue >>> (lCount * 8)) & 255
      WordToHexValue_temp = '0' + lByte.toString(16)
      WordToHexValue = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length - 2, 2)
    }
    return WordToHexValue
  }
  
  const x = convertToWordArray(str)
  let a = 0x67452301
  let b = 0xEFCDAB89
  let c = 0x98BADCFE
  let d = 0x10325476
  
  const S11 = 7, S12 = 12, S13 = 17, S14 = 22
  const S21 = 5, S22 = 9, S23 = 14, S24 = 20
  const S31 = 4, S32 = 11, S33 = 16, S34 = 23
  const S41 = 6, S42 = 10, S43 = 15, S44 = 21
  
  for (let k = 0; k < x.length; k += 16) {
    const AA = a, BB = b, CC = c, DD = d
    
    a = FF(a, b, c, d, x[k + 0], S11, 0xD76AA478)
    d = FF(d, a, b, c, x[k + 1], S12, 0xE8C7B756)
    c = FF(c, d, a, b, x[k + 2], S13, 0x242070DB)
    b = FF(b, c, d, a, x[k + 3], S14, 0xC1BDCEEE)
    a = FF(a, b, c, d, x[k + 4], S11, 0xF57C0FAF)
    d = FF(d, a, b, c, x[k + 5], S12, 0x4787C62A)
    c = FF(c, d, a, b, x[k + 6], S13, 0xA8304613)
    b = FF(b, c, d, a, x[k + 7], S14, 0xFD469501)
    a = FF(a, b, c, d, x[k + 8], S11, 0x698098D8)
    d = FF(d, a, b, c, x[k + 9], S12, 0x8B44F7AF)
    c = FF(c, d, a, b, x[k + 10], S13, 0xFFFF5BB1)
    b = FF(b, c, d, a, x[k + 11], S14, 0x895CD7BE)
    a = FF(a, b, c, d, x[k + 12], S11, 0x6B901122)
    d = FF(d, a, b, c, x[k + 13], S12, 0xFD987193)
    c = FF(c, d, a, b, x[k + 14], S13, 0xA679438E)
    b = FF(b, c, d, a, x[k + 15], S14, 0x49B40821)
    
    a = GG(a, b, c, d, x[k + 1], S21, 0xF61E2562)
    d = GG(d, a, b, c, x[k + 6], S22, 0xC040B340)
    c = GG(c, d, a, b, x[k + 11], S23, 0x265E5A51)
    b = GG(b, c, d, a, x[k + 0], S24, 0xE9B6C7AA)
    a = GG(a, b, c, d, x[k + 5], S21, 0xD62F105D)
    d = GG(d, a, b, c, x[k + 10], S22, 0x02441453)
    c = GG(c, d, a, b, x[k + 15], S23, 0xD8A1E681)
    b = GG(b, c, d, a, x[k + 4], S24, 0xE7D3FBC8)
    a = GG(a, b, c, d, x[k + 9], S21, 0x21E1CDE6)
    d = GG(d, a, b, c, x[k + 14], S22, 0xC33707D6)
    c = GG(c, d, a, b, x[k + 3], S23, 0xF4D50D87)
    b = GG(b, c, d, a, x[k + 8], S24, 0x455A14ED)
    a = GG(a, b, c, d, x[k + 13], S21, 0xA9E3E905)
    d = GG(d, a, b, c, x[k + 2], S22, 0xFCEFA3F8)
    c = GG(c, d, a, b, x[k + 7], S23, 0x676F02D9)
    b = GG(b, c, d, a, x[k + 12], S24, 0x8D2A4C8A)
    
    a = HH(a, b, c, d, x[k + 5], S31, 0xFFFA3942)
    d = HH(d, a, b, c, x[k + 8], S32, 0x8771F681)
    c = HH(c, d, a, b, x[k + 11], S33, 0x6D9D6122)
    b = HH(b, c, d, a, x[k + 14], S34, 0xFDE5380C)
    a = HH(a, b, c, d, x[k + 1], S31, 0xA4BEEA44)
    d = HH(d, a, b, c, x[k + 4], S32, 0x4BDECFA9)
    c = HH(c, d, a, b, x[k + 7], S33, 0xF6BB4B60)
    b = HH(b, c, d, a, x[k + 10], S34, 0xBEBFBC70)
    a = HH(a, b, c, d, x[k + 13], S31, 0x289B7EC6)
    d = HH(d, a, b, c, x[k + 0], S32, 0xEAA127FA)
    c = HH(c, d, a, b, x[k + 3], S33, 0xD4EF3085)
    b = HH(b, c, d, a, x[k + 6], S34, 0x04881D05)
    a = HH(a, b, c, d, x[k + 9], S31, 0xD9D4D039)
    d = HH(d, a, b, c, x[k + 12], S32, 0xE6DB99E5)
    c = HH(c, d, a, b, x[k + 15], S33, 0x1FA27CF8)
    b = HH(b, c, d, a, x[k + 2], S34, 0xC4AC5665)
    
    a = II(a, b, c, d, x[k + 0], S41, 0xF4292244)
    d = II(d, a, b, c, x[k + 7], S42, 0x432AFF97)
    c = II(c, d, a, b, x[k + 14], S43, 0xAB9423A7)
    b = II(b, c, d, a, x[k + 5], S44, 0xFC93A039)
    a = II(a, b, c, d, x[k + 12], S41, 0x655B59C3)
    d = II(d, a, b, c, x[k + 3], S42, 0x8F0CCC92)
    c = II(c, d, a, b, x[k + 10], S43, 0xFFEFF47D)
    b = II(b, c, d, a, x[k + 1], S44, 0x85845DD1)
    a = II(a, b, c, d, x[k + 8], S41, 0x6FA87E4F)
    d = II(d, a, b, c, x[k + 15], S42, 0xFE2CE6E0)
    c = II(c, d, a, b, x[k + 6], S43, 0xA3014314)
    b = II(b, c, d, a, x[k + 13], S44, 0x4E0811A1)
    a = II(a, b, c, d, x[k + 4], S41, 0xF7537E82)
    d = II(d, a, b, c, x[k + 11], S42, 0xBD3AF235)
    c = II(c, d, a, b, x[k + 2], S43, 0x2AD7D2BB)
    b = II(b, c, d, a, x[k + 9], S44, 0xEB86D391)
    
    a = addUnsigned(a, AA)
    b = addUnsigned(b, BB)
    c = addUnsigned(c, CC)
    d = addUnsigned(d, DD)
  }
  
  return (wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d)).toLowerCase()
}

// RC4 加密/解密
// @ts-ignore - 加密算法的数组访问是安全的
function rc4(text: string, key: string): string {
  // 先将文本转换为 UTF-8 字节数组
  const encoder = new TextEncoder()
  const textBytes = encoder.encode(text)
  
  const S: number[] = []
  const keyLen = key.length
  let j = 0
  
  for (let i = 0; i < 256; i++) S[i] = i
  for (let i = 0; i < 256; i++) {
    j = (j + S[i] + key.charCodeAt(i % keyLen)) % 256
    ;[S[i], S[j]] = [S[j], S[i]]
  }
  
  let i = 0
  j = 0
  const result: number[] = []
  for (let k = 0; k < textBytes.length; k++) {
    i = (i + 1) % 256
    j = (j + S[i]) % 256
    ;[S[i], S[j]] = [S[j], S[i]]
    result.push(textBytes[k] ^ S[(S[i] + S[j]) % 256])
  }
  
  return btoa(String.fromCharCode(...result))
}

// @ts-ignore - 加密算法的数组访问是安全的
function rc4Decrypt(base64Text: string, key: string): string {
  const encrypted = atob(base64Text)
  const S: number[] = []
  const keyLen = key.length
  let j = 0
  
  for (let i = 0; i < 256; i++) S[i] = i
  for (let i = 0; i < 256; i++) {
    j = (j + S[i] + key.charCodeAt(i % keyLen)) % 256
    ;[S[i], S[j]] = [S[j], S[i]]
  }
  
  let i = 0
  j = 0
  const result: number[] = []
  for (let k = 0; k < encrypted.length; k++) {
    i = (i + 1) % 256
    j = (j + S[i]) % 256
    ;[S[i], S[j]] = [S[j], S[i]]
    result.push(encrypted.charCodeAt(k) ^ S[(S[i] + S[j]) % 256])
  }
  
  // 正确处理 UTF-8 编码的字节序列
  const uint8Array = new Uint8Array(result)
  const decoder = new TextDecoder('utf-8')
  return decoder.decode(uint8Array)
}

// AES-CBC 加密（使用 Web Crypto API）
async function aesEncrypt(text: string, key: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(text)
  
  // 将密钥标准化为 16 字节（AES-128）
  const keyBytes = encoder.encode(key)
  const normalizedKey = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0
  }
  
  // 使用全零 IV（与后端保持一致）
  const iv = new Uint8Array(16)
  
  // 导入密钥
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    normalizedKey,
    { name: 'AES-CBC' },
    false,
    ['encrypt']
  )
  
  // 加密
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-CBC', iv },
    cryptoKey,
    data
  )
  
  // 转换为 Base64
  const encryptedArray = new Uint8Array(encrypted)
  return btoa(String.fromCharCode(...encryptedArray))
}

// AES-CBC 解密（使用 Web Crypto API）
async function aesDecrypt(base64Text: string, key: string): Promise<string> {
  const encoder = new TextEncoder()
  
  // 将密钥标准化为 16 字节（AES-128）
  const keyBytes = encoder.encode(key)
  const normalizedKey = new Uint8Array(16)
  for (let i = 0; i < 16; i++) {
    normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0
  }
  
  // 使用全零 IV（与后端保持一致）
  const iv = new Uint8Array(16)
  
  // Base64 解码
  const encryptedText = atob(base64Text)
  const encryptedArray = new Uint8Array(encryptedText.length)
  for (let i = 0; i < encryptedText.length; i++) {
    encryptedArray[i] = encryptedText.charCodeAt(i)
  }
  
  // 导入密钥
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    normalizedKey,
    { name: 'AES-CBC' },
    false,
    ['decrypt']
  )
  
  // 解密
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-CBC', iv },
    cryptoKey,
    encryptedArray
  )
  
  // 转换为字符串
  const decoder = new TextDecoder('utf-8')
  return decoder.decode(decrypted)
}

// 调用卡密验证接口
async function callRedeemApi() {
  if (!redeemForm.value.code) {
    showToast('请输入卡密码', 'error')
    return
  }
  
  if (!appInfo.value) {
    showToast('应用信息未加载', 'error')
    return
  }
  
  loading.value = true
  redeemResult.value = null
  decryptedRedeemResult.value = null
  redeemSuccess.value = false
  
  try {
    const { apiKey, secretKey, secure, encryptionAlg } = appInfo.value
    // 使用 secretKey 进行签名，如果没有配置则使用 apiKey
    const secret = secretKey || apiKey
    const timestamp = Math.floor(Date.now() / 1000).toString()
    
    let payload = ''
    let code = redeemForm.value.code
    let machine = redeemForm.value.machine
    
    if (secure) {
      // 加密整个请求对象（更安全）
      const plainData = JSON.stringify({
        code: redeemForm.value.code,
        machine: redeemForm.value.machine
      })
      
      if (encryptionAlg?.startsWith('AES')) {
        // AES 加密
        payload = await aesEncrypt(plainData, secret)
      } else {
        // RC4 加密
        payload = rc4(plainData, secret)
      }
      code = ''
      machine = ''
    }
    
    const toSign = secure ? payload : code
    const sign = md5(secret + timestamp + toSign)
    
    // 调试日志
    console.log('===== 前端签名调试信息 =====')
    console.log('apiKey:', apiKey)
    console.log('secretKey:', secretKey)
    console.log('最终 secret:', secret)
    console.log('timestamp:', timestamp)
    console.log('code:', code)
    console.log('payload:', payload)
    console.log('toSign:', toSign)
    console.log('签名字符串:', secret + timestamp + toSign)
    console.log('计算出的签名:', sign)
    console.log('============================')
    
    // 构建请求体
    const requestBody: any = {}
    if (payload) {
      // 安全模式：只发送 payload
      requestBody.payload = payload
    } else {
      // 普通模式：发送明文字段
      if (code) requestBody.code = code
      if (machine) requestBody.machine = machine
    }
    
    const response = await fetch('/verfiy/api/redeem', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': apiKey,
        'X-Timestamp': timestamp,
        'X-Sign': sign
      },
      body: JSON.stringify(requestBody)
    })
    
    const result = await response.json()
    redeemResult.value = result
    
    console.log('===== 卡密验证返回结果 =====')
    console.log('HTTP 状态码:', response.status)
    console.log('返回数据:', result)
    console.log('secure 模式:', secure)
    console.log('============================')
    
    // 判断是否成功，需要根据是否加密来处理
    let isSuccess = false
    let message = ''
    
    console.log('🔍 判断条件检查:')
    console.log('  - result.payload:', !!result.payload)
    console.log('  - result.sign:', !!result.sign)
    console.log('  - secure:', secure, '(类型:', typeof secure, ')')
    console.log('  - 判断结果:', !!(result.payload && result.sign && secure))
    
    if (result.payload && result.sign && secure) {
      // 加密模式：success 在解密后的数据里
      console.log('✅ 进入加密模式处理')
      try {
        let decrypted: string
        if (encryptionAlg?.startsWith('AES')) {
          // AES 解密
          console.log('  使用 AES 解密...')
          decrypted = await aesDecrypt(result.payload, secret)
        } else {
          // RC4 解密
          console.log('  使用 RC4 解密...')
          decrypted = rc4Decrypt(result.payload, secret)
        }
        decryptedRedeemResult.value = JSON.parse(decrypted)
        isSuccess = decryptedRedeemResult.value.success === true // 严格判断 true，null/undefined 视为 false
        message = decryptedRedeemResult.value.message || '核销成功'
        console.log('  解密后的数据:', decryptedRedeemResult.value)
        console.log('  isSuccess:', isSuccess)
      } catch (e) {
        console.error('❌ 解密失败:', e)
        isSuccess = false
        message = '解密失败'
      }
    } else {
      // 非加密模式：success 直接在返回数据里
      console.log('⚠️ 进入非加密模式处理')
      isSuccess = result.success === true // 严格判断 true，null/undefined 视为 false
      message = result.message || (isSuccess ? '核销成功' : '核销失败')
      console.log('  result.success:', result.success)
      console.log('  isSuccess:', isSuccess)
    }
    
    // 保存成功状态
    redeemSuccess.value = isSuccess
    // 显示详细的提示信息
    showToast(isSuccess ? '✅ ' + message : '❌ ' + message, isSuccess ? 'success' : 'error')
  } catch (e: any) {
    console.error('调用失败:', e)
    showToast('调用失败: ' + (e.message || '未知错误'), 'error')
  } finally {
    loading.value = false
  }
}

// 调用通知接口
async function callNoticeApi() {
  if (!appInfo.value) {
    showToast('应用信息未加载', 'error')
    return
  }
  
  loading.value = true
  noticeResult.value = null
  decryptedNoticeResult.value = null
  noticeSuccess.value = false
  
  try {
    const { apiKey, secretKey, secure, encryptionAlg } = appInfo.value
    // 使用 secretKey 进行签名，如果没有配置则使用 apiKey
    const secret = secretKey || apiKey
    const timestamp = Math.floor(Date.now() / 1000).toString()
    
    const sign = md5(secret + timestamp)
    
    const response = await fetch('/verfiy/api/notice', {
      method: 'GET',
      headers: {
        'X-API-Key': apiKey,
        'X-Timestamp': timestamp,
        'X-Sign': sign
      }
    })
    
    const result = await response.json()
    noticeResult.value = result
    
    console.log('===== 通知接口返回结果 =====')
    console.log('HTTP 状态码:', response.status)
    console.log('返回数据:', result)
    console.log('secure 模式:', secure)
    console.log('==========================')
    
    // 判断是否成功，需要根据是否加密来处理
    let isSuccess = false
    let message = ''
    
    if (result.payload && result.sign && secure) {
      // 加密模式：success 在解密后的数据里
      try {
        let decrypted: string
        if (encryptionAlg?.startsWith('AES')) {
          // AES 解密
          decrypted = await aesDecrypt(result.payload, secret)
        } else {
          // RC4 解密
          decrypted = rc4Decrypt(result.payload, secret)
        }
        decryptedNoticeResult.value = JSON.parse(decrypted)
        isSuccess = decryptedNoticeResult.value.success === true // 严格判断 true，null/undefined 视为 false
        message = decryptedNoticeResult.value.message || '获取通知成功'
        console.log('解密后的数据:', decryptedNoticeResult.value)
      } catch (e) {
        console.error('解密失败:', e)
        isSuccess = false
        message = '解密失败'
      }
    } else {
      // 非加密模式：success 直接在返回数据里
      isSuccess = result.success
      message = result.message || (isSuccess ? '获取通知成功' : '获取通知失败')
    }
    
    // 保存成功状态
    noticeSuccess.value = isSuccess
    showToast(isSuccess ? '✅ ' + message : '❌ ' + message, isSuccess ? 'success' : 'error')
  } catch (e: any) {
    console.error('调用失败:', e)
    showToast('调用失败: ' + (e.message || '未知错误'), 'error')
  } finally {
    loading.value = false
  }
}

// 复制到剪贴板
async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    showToast('已复制到剪贴板', 'success')
  } catch (e) {
    showToast('复制失败', 'error')
  }
}

// 代码示例（使用函数返回避免模板字符串问题）
const redeemCodeExamples = computed(() => {
  const app = appInfo.value
  if (!app) return { java: '// 加载中...', js: '// 加载中...', python: '# 加载中...' }
  
  const secret = app.secretKey || app.apiKey
  const isSecure = app.secure === true
  const encAlg = app.encryptionAlg || 'RC4'
  
  // ===== Java 代码 =====
  let javaDecryptMethods = ''
  let javaDecryptLogic = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      // AES 解密方法
      javaDecryptMethods = `
    // AES-CBC 解密方法
    public static String decryptAes(String base64Cipher, String key) {
        try {
            byte[] cipher = java.util.Base64.getDecoder().decode(base64Cipher);
            byte[] keyBytes = normalizeKey(key);
            byte[] iv = new byte[16]; // 全零 IV
            
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            c.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] plain = c.doFinal(cipher);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }
    
    // 规范化密钥长度（AES 需要 16/24/32 字节）
    private static byte[] normalizeKey(String key) {
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        if (kb.length == 16 || kb.length == 24 || kb.length == 32) return kb;
        byte[] out = new byte[16];
        for (int i = 0; i < out.length; i++) {
            out[i] = (i < kb.length) ? kb[i] : 0;
        }
        return out;
    }`
      
      javaDecryptLogic = `
        
        // 解析响应并解密
        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
        if (jsonResponse.has("payload")) {
            // 加密响应，需要解密
            String payload = jsonResponse.getString("payload");
            String decrypted = decryptAes(payload, secretKey);
            System.out.println("\\n解密后的数据: " + decrypted);
            
            // 解析解密后的 JSON
            org.json.JSONObject result = new org.json.JSONObject(decrypted);
            if (result.getBoolean("success")) {
                System.out.println("✅ 核销成功！");
                System.out.println("   到期时间: " + result.optString("expireAtReadable", "N/A"));
                if (result.has("extra")) {
                    System.out.println("   附加信息: " + result.get("extra"));
                }
            } else {
                System.out.println("❌ 核销失败: " + result.getString("message"));
            }
        } else {
            System.out.println("❌ 响应格式错误");
        }`
    } else {
      // RC4 解密方法
      javaDecryptMethods = `
    // RC4 解密方法
    public static String decryptRc4(String base64Cipher, String key) {
        byte[] cipherBytes = java.util.Base64.getDecoder().decode(base64Cipher);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] plain = rc4(cipherBytes, keyBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }
    
    private static byte[] rc4(byte[] data, byte[] key) {
        byte[] s = new byte[256];
        for (int i = 0; i < 256; i++) s[i] = (byte) i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + (s[i] & 0xFF) + (key[i % key.length] & 0xFF)) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
        }
        byte[] out = new byte[data.length];
        int i = 0; j = 0;
        for (int k = 0; k < data.length; k++) {
            i = (i + 1) & 0xFF;
            j = (j + (s[i] & 0xFF)) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
            int t = ((s[i] & 0xFF) + (s[j] & 0xFF)) & 0xFF;
            out[k] = (byte) (data[k] ^ s[t]);
        }
        return out;
    }`
      
      javaDecryptLogic = `
        
        // 解析响应并解密
        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
        if (jsonResponse.has("payload")) {
            // 加密响应，需要解密
            String payload = jsonResponse.getString("payload");
            String decrypted = decryptRc4(payload, secretKey);
            System.out.println("\\n解密后的数据: " + decrypted);
            
            // 解析解密后的 JSON
            org.json.JSONObject result = new org.json.JSONObject(decrypted);
            if (result.getBoolean("success")) {
                System.out.println("✅ 核销成功！");
                System.out.println("   到期时间: " + result.optString("expireAtReadable", "N/A"));
                if (result.has("extra")) {
                    System.out.println("   附加信息: " + result.get("extra"));
                }
            } else {
                System.out.println("❌ 核销失败: " + result.getString("message"));
            }
        } else {
            System.out.println("❌ 响应格式错误");
        }`
    }
  } else {
    // 非加密模式
    javaDecryptLogic = `
        
        // 解析响应 JSON
        org.json.JSONObject result = new org.json.JSONObject(response.toString());
        if (result.getBoolean("success")) {
            System.out.println("✅ 核销成功！");
            System.out.println("   状态码: " + result.getInt("code"));
            System.out.println("   到期时间: " + result.optString("expireAtReadable", "N/A"));
            if (result.has("extra")) {
                System.out.println("   附加信息: " + result.get("extra"));
            }
        } else {
            System.out.println("❌ 核销失败: " + result.getString("message"));
        }`
  }
  
  // Java 加密方法（安全模式下需要）
  let javaEncryptMethods = ''
  let javaRequestLogic = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      // AES 加密方法
      javaEncryptMethods = `
    // AES-CBC 加密方法
    public static String encryptAes(String plaintext, String key) {
        try {
            byte[] keyBytes = normalizeKey(key);
            byte[] iv = new byte[16]; // 全零 IV
            
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            byte[] encrypted = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败", e);
        }
    }
`
      javaRequestLogic = `
        // 安全模式：构建请求数据并加密
        org.json.JSONObject requestData = new org.json.JSONObject();
        requestData.put("code", cardCode);
        requestData.put("machine", machine);
        
        // 加密整个请求对象
        String plainJson = requestData.toString();
        String payload = encryptAes(plainJson, secretKey);
        
        // 使用 payload 计算签名
        String sign = md5(secretKey + timestamp + payload);
        
        // 构建请求体
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("payload", payload);
        
        // 发送 POST 请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        conn.setRequestProperty("X-Sign", sign);
        
        // 写入请求体
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }`
    } else {
      // RC4 加密方法
      javaEncryptMethods = `
    // RC4 加密方法
    public static String encryptRc4(String plaintext, String key) {
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = rc4(plainBytes, keyBytes);
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }
`
      javaRequestLogic = `
        // 安全模式：构建请求数据并加密
        org.json.JSONObject requestData = new org.json.JSONObject();
        requestData.put("code", cardCode);
        requestData.put("machine", machine);
        
        // 加密整个请求对象
        String plainJson = requestData.toString();
        String payload = encryptRc4(plainJson, secretKey);
        
        // 使用 payload 计算签名
        String sign = md5(secretKey + timestamp + payload);
        
        // 构建请求体
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("payload", payload);
        
        // 发送 POST 请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        conn.setRequestProperty("X-Sign", sign);
        
        // 写入请求体
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }`
    }
  } else {
    // 非安全模式
    javaRequestLogic = `
        // 普通模式：使用 code 计算签名
        String sign = md5(secretKey + timestamp + cardCode);
        
        // 构建请求体
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("code", cardCode);
        requestBody.put("machine", machine);
        
        // 发送 POST 请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        conn.setRequestProperty("X-Sign", sign);
        
        // 写入请求体
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }`
  }
  
  const javaCode = `import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 卡密验证接口调用示例
 * 传输安全: ${isSecure ? '已开启 (' + encAlg + ')' : '未开启'}
 * 
 * 注意：需要添加 org.json 依赖
 * Maven: <dependency><groupId>org.json</groupId><artifactId>json</artifactId><version>20231013</version></dependency>
 */
public class CardRedeemExample {
    
    // MD5 加密方法
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }${javaEncryptMethods}${javaDecryptMethods}
    
    public static void main(String[] args) throws Exception {
        // 配置信息
        String apiUrl = "https://www.xyapi.top/verify/api/redeem";
        String apiKey = "${app.apiKey}";  // 用于身份识别（X-API-Key）
        String secretKey = "${secret}";   // 用于签名和加密
        String cardCode = "your-card-code";
        String machine = "your-machine-code";
        
        // 计算时间戳
        long timestamp = System.currentTimeMillis() / 1000;
        ${javaRequestLogic}
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        // 打印原始响应
        System.out.println("Response Code: " + responseCode);
        System.out.println("Response Body: " + response.toString());${javaDecryptLogic}
    }
}`
  
  // ===== JavaScript 代码 =====
  let jsDecryptFunctions = ''
  let jsDecryptLogic = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      // AES 解密函数
      jsDecryptFunctions = `
// AES-CBC 解密函数（使用 Web Crypto API）
async function decryptAes(base64Cipher, key) {
  // Base64 解码
  const cipherBytes = Uint8Array.from(atob(base64Cipher), c => c.charCodeAt(0));
  
  // 规范化密钥长度为 16 字节
  const keyBytes = new TextEncoder().encode(key);
  const normalizedKey = new Uint8Array(16);
  for (let i = 0; i < normalizedKey.length; i++) {
    normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0;
  }
  
  // 导入密钥
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    normalizedKey,
    { name: 'AES-CBC' },
    false,
    ['decrypt']
  );
  
  // 全零 IV
  const iv = new Uint8Array(16);
  
  // 解密
  const plainBytes = await crypto.subtle.decrypt(
    { name: 'AES-CBC', iv },
    cryptoKey,
    cipherBytes
  );
  
  return new TextDecoder('utf-8').decode(plainBytes);
}
`
      jsDecryptLogic = `
  console.log('原始响应:', data);
  
  if (data.payload) {
    // 加密响应，需要解密
    decryptAes(data.payload, secretKey).then(decrypted => {
      console.log('解密后的数据:', decrypted);
      const result = JSON.parse(decrypted);
      
      if (result.success) {
        console.log('✅ 核销成功！');
        console.log('   到期时间:', result.expireAtReadable || 'N/A');
        if (result.extra) {
          console.log('   附加信息:', result.extra);
        }
      } else {
        console.log('❌ 核销失败:', result.message);
      }
    }).catch(err => console.error('解密失败:', err));
  } else {
    console.log('❌ 响应格式错误');
  }`
    } else {
      // RC4 解密函数
      jsDecryptFunctions = `
// RC4 解密函数
function decryptRc4(base64Cipher, key) {
  // Base64 解码
  const cipherBytes = Uint8Array.from(atob(base64Cipher), c => c.charCodeAt(0));
  const keyBytes = new TextEncoder().encode(key);
  
  // RC4 算法
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i++) s[i] = i;
  let j = 0;
  for (let i = 0; i < 256; i++) {
    j = (j + s[i] + keyBytes[i % keyBytes.length]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
  }
  
  const out = new Uint8Array(cipherBytes.length);
  let i = 0; j = 0;
  for (let k = 0; k < cipherBytes.length; k++) {
    i = (i + 1) & 0xFF;
    j = (j + s[i]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
    const t = (s[i] + s[j]) & 0xFF;
    out[k] = cipherBytes[k] ^ s[t];
  }
  
  return new TextDecoder('utf-8').decode(out);
}
`
      jsDecryptLogic = `
  console.log('原始响应:', data);
  
  if (data.payload) {
    // 加密响应，需要解密
    try {
      const decrypted = decryptRc4(data.payload, secretKey);
      console.log('解密后的数据:', decrypted);
      const result = JSON.parse(decrypted);
      
      if (result.success) {
        console.log('✅ 核销成功！');
        console.log('   到期时间:', result.expireAtReadable || 'N/A');
        if (result.extra) {
          console.log('   附加信息:', result.extra);
        }
      } else {
        console.log('❌ 核销失败:', result.message);
      }
    } catch (err) {
      console.error('解密失败:', err);
    }
  } else {
    console.log('❌ 响应格式错误');
  }`
    }
  } else {
    // 非加密模式
    jsDecryptLogic = `
  console.log('响应数据:', data);
  
  if (data.success) {
    console.log('✅ 核销成功！');
    console.log('   状态码:', data.code);
    console.log('   到期时间:', data.expireAtReadable || 'N/A');
    if (data.extra) {
      console.log('   附加信息:', data.extra);
    }
  } else {
    console.log('❌ 核销失败！');
    console.log('   状态码:', data.code);
    console.log('   失败原因:', data.message);
  }`
  }
  
  // JavaScript 加密函数（安全模式下需要）
  let jsEncryptFunctions = ''
  let jsRequestLogic = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      // AES 加密函数
      jsEncryptFunctions = `
// AES-CBC 加密函数（使用 Web Crypto API）
async function encryptAes(plaintext, key) {
  const encoder = new TextEncoder();
  const data = encoder.encode(plaintext);
  
  // 规范化密钥长度为 16 字节
  const keyBytes = encoder.encode(key);
  const normalizedKey = new Uint8Array(16);
  for (let i = 0; i < normalizedKey.length; i++) {
    normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0;
  }
  
  // 导入密钥
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    normalizedKey,
    { name: 'AES-CBC' },
    false,
    ['encrypt']
  );
  
  // 全零 IV
  const iv = new Uint8Array(16);
  
  // 加密
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-CBC', iv },
    cryptoKey,
    data
  );
  
  // 转换为 Base64
  const encryptedArray = new Uint8Array(encrypted);
  return btoa(String.fromCharCode(...encryptedArray));
}
`
      jsRequestLogic = `
// 安全模式：构建请求数据并加密
const requestData = {
  code: cardCode,
  machine: machine
};

// 加密整个请求对象
const plainJson = JSON.stringify(requestData);
const payload = await encryptAes(plainJson, secretKey);

// 使用 payload 计算签名
const sign = md5(secretKey + timestamp + payload);

// 发送请求
fetch(apiUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  },
  body: JSON.stringify({ payload })
})
.then(res => res.json())
.then(data => {${jsDecryptLogic}
})
.catch(err => console.error('请求失败:', err));`
    } else {
      // RC4 加密函数
      jsEncryptFunctions = `
// RC4 加密函数
function encryptRc4(plaintext, key) {
  const encoder = new TextEncoder();
  const plainBytes = encoder.encode(plaintext);
  const keyBytes = encoder.encode(key);
  
  // RC4 算法
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i++) s[i] = i;
  let j = 0;
  for (let i = 0; i < 256; i++) {
    j = (j + s[i] + keyBytes[i % keyBytes.length]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
  }
  
  const out = new Uint8Array(plainBytes.length);
  let i = 0; j = 0;
  for (let k = 0; k < plainBytes.length; k++) {
    i = (i + 1) & 0xFF;
    j = (j + s[i]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
    const t = (s[i] + s[j]) & 0xFF;
    out[k] = plainBytes[k] ^ s[t];
  }
  
  return btoa(String.fromCharCode(...out));
}
`
      jsRequestLogic = `
// 安全模式：构建请求数据并加密
const requestData = {
  code: cardCode,
  machine: machine
};

// 加密整个请求对象
const plainJson = JSON.stringify(requestData);
const payload = encryptRc4(plainJson, secretKey);

// 使用 payload 计算签名
const sign = md5(secretKey + timestamp + payload);

// 发送请求
fetch(apiUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  },
  body: JSON.stringify({ payload })
})
.then(res => res.json())
.then(data => {${jsDecryptLogic}
})
.catch(err => console.error('请求失败:', err));`
    }
  } else {
    // 非安全模式
    jsRequestLogic = `
// 普通模式：使用 code 计算签名
const sign = md5(secretKey + timestamp + cardCode);

// 发送请求
fetch(apiUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  },
  body: JSON.stringify({
    code: cardCode,
    machine: machine
  })
})
.then(res => res.json())
.then(data => {${jsDecryptLogic}
})
.catch(err => console.error('请求失败:', err));`
  }
  
  const jsCode = `/**
 * 卡密验证接口调用示例（JavaScript）
 * 传输安全: ${isSecure ? '已开启 (' + encAlg + ')' : '未开启'}
 * 
 * 可以在 Node.js 或浏览器中运行
 */

// MD5 加密函数
function md5(str) {
  function rotateLeft(n, s) { return (n << s) | (n >>> (32 - s)); }
  function addUnsigned(x, y) {
    const x8 = x & 0x80000000, y8 = y & 0x80000000;
    const x4 = x & 0x40000000, y4 = y & 0x40000000;
    const result = (x & 0x3FFFFFFF) + (y & 0x3FFFFFFF);
    if (x4 & y4) return result ^ 0x80000000 ^ x8 ^ y8;
    if (x4 | y4) {
      if (result & 0x40000000) return result ^ 0xC0000000 ^ x8 ^ y8;
      else return result ^ 0x40000000 ^ x8 ^ y8;
    } else return result ^ x8 ^ y8;
  }
  function F(x, y, z) { return (x & y) | ((~x) & z); }
  function G(x, y, z) { return (x & z) | (y & (~z)); }
  function H(x, y, z) { return x ^ y ^ z; }
  function I(x, y, z) { return y ^ (x | (~z)); }
  function FF(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(F(b, c, d), x), ac));
    return addUnsigned(rotateLeft(a, s), b);
  }
  function GG(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(G(b, c, d), x), ac));
    return addUnsigned(rotateLeft(a, s), b);
  }
  function HH(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(H(b, c, d), x), ac));
    return addUnsigned(rotateLeft(a, s), b);
  }
  function II(a, b, c, d, x, s, ac) {
    a = addUnsigned(a, addUnsigned(addUnsigned(I(b, c, d), x), ac));
    return addUnsigned(rotateLeft(a, s), b);
  }
  function convertToWordArray(str) {
    let lWordCount;
    const lMessageLength = str.length;
    const lNumberOfWords_temp1 = lMessageLength + 8;
    const lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64;
    const lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16;
    const lWordArray = Array(lNumberOfWords - 1).fill(0);
    let lBytePosition = 0, lByteCount = 0;
    while (lByteCount < lMessageLength) {
      lWordCount = (lByteCount - (lByteCount % 4)) / 4;
      lBytePosition = (lByteCount % 4) * 8;
      lWordArray[lWordCount] = lWordArray[lWordCount] | (str.charCodeAt(lByteCount) << lBytePosition);
      lByteCount++;
    }
    lWordCount = (lByteCount - (lByteCount % 4)) / 4;
    lBytePosition = (lByteCount % 4) * 8;
    lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition);
    lWordArray[lNumberOfWords - 2] = lMessageLength << 3;
    lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29;
    return lWordArray;
  }
  function wordToHex(lValue) {
    let WordToHexValue = '', WordToHexValue_temp = '', lByte, lCount;
    for (lCount = 0; lCount <= 3; lCount++) {
      lByte = (lValue >>> (lCount * 8)) & 255;
      WordToHexValue_temp = '0' + lByte.toString(16);
      WordToHexValue = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length - 2, 2);
    }
    return WordToHexValue;
  }
  const x = convertToWordArray(str);
  let a = 0x67452301, b = 0xEFCDAB89, c = 0x98BADCFE, d = 0x10325476;
  const S11=7, S12=12, S13=17, S14=22, S21=5, S22=9, S23=14, S24=20;
  const S31=4, S32=11, S33=16, S34=23, S41=6, S42=10, S43=15, S44=21;
  for (let k = 0; k < x.length; k += 16) {
    const AA = a, BB = b, CC = c, DD = d;
    a=FF(a,b,c,d,x[k+0],S11,0xD76AA478); d=FF(d,a,b,c,x[k+1],S12,0xE8C7B756);
    c=FF(c,d,a,b,x[k+2],S13,0x242070DB); b=FF(b,c,d,a,x[k+3],S14,0xC1BDCEEE);
    a=FF(a,b,c,d,x[k+4],S11,0xF57C0FAF); d=FF(d,a,b,c,x[k+5],S12,0x4787C62A);
    c=FF(c,d,a,b,x[k+6],S13,0xA8304613); b=FF(b,c,d,a,x[k+7],S14,0xFD469501);
    a=FF(a,b,c,d,x[k+8],S11,0x698098D8); d=FF(d,a,b,c,x[k+9],S12,0x8B44F7AF);
    c=FF(c,d,a,b,x[k+10],S13,0xFFFF5BB1); b=FF(b,c,d,a,x[k+11],S14,0x895CD7BE);
    a=FF(a,b,c,d,x[k+12],S11,0x6B901122); d=FF(d,a,b,c,x[k+13],S12,0xFD987193);
    c=FF(c,d,a,b,x[k+14],S13,0xA679438E); b=FF(b,c,d,a,x[k+15],S14,0x49B40821);
    a=GG(a,b,c,d,x[k+1],S21,0xF61E2562); d=GG(d,a,b,c,x[k+6],S22,0xC040B340);
    c=GG(c,d,a,b,x[k+11],S23,0x265E5A51); b=GG(b,c,d,a,x[k+0],S24,0xE9B6C7AA);
    a=GG(a,b,c,d,x[k+5],S21,0xD62F105D); d=GG(d,a,b,c,x[k+10],S22,0x02441453);
    c=GG(c,d,a,b,x[k+15],S23,0xD8A1E681); b=GG(b,c,d,a,x[k+4],S24,0xE7D3FBC8);
    a=GG(a,b,c,d,x[k+9],S21,0x21E1CDE6); d=GG(d,a,b,c,x[k+14],S22,0xC33707D6);
    c=GG(c,d,a,b,x[k+3],S23,0xF4D50D87); b=GG(b,c,d,a,x[k+8],S24,0x455A14ED);
    a=GG(a,b,c,d,x[k+13],S21,0xA9E3E905); d=GG(d,a,b,c,x[k+2],S22,0xFCEFA3F8);
    c=GG(c,d,a,b,x[k+7],S23,0x676F02D9); b=GG(b,c,d,a,x[k+12],S24,0x8D2A4C8A);
    a=HH(a,b,c,d,x[k+5],S31,0xFFFA3942); d=HH(d,a,b,c,x[k+8],S32,0x8771F681);
    c=HH(c,d,a,b,x[k+11],S33,0x6D9D6122); b=HH(b,c,d,a,x[k+14],S34,0xFDE5380C);
    a=HH(a,b,c,d,x[k+1],S31,0xA4BEEA44); d=HH(d,a,b,c,x[k+4],S32,0x4BDECFA9);
    c=HH(c,d,a,b,x[k+7],S33,0xF6BB4B60); b=HH(b,c,d,a,x[k+10],S34,0xBEBFBC70);
    a=HH(a,b,c,d,x[k+13],S31,0x289B7EC6); d=HH(d,a,b,c,x[k+0],S32,0xEAA127FA);
    c=HH(c,d,a,b,x[k+3],S33,0xD4EF3085); b=HH(b,c,d,a,x[k+6],S34,0x04881D05);
    a=HH(a,b,c,d,x[k+9],S31,0xD9D4D039); d=HH(d,a,b,c,x[k+12],S32,0xE6DB99E5);
    c=HH(c,d,a,b,x[k+15],S33,0x1FA27CF8); b=HH(b,c,d,a,x[k+2],S34,0xC4AC5665);
    a=II(a,b,c,d,x[k+0],S41,0xF4292244); d=II(d,a,b,c,x[k+7],S42,0x432AFF97);
    c=II(c,d,a,b,x[k+14],S43,0xAB9423A7); b=II(b,c,d,a,x[k+5],S44,0xFC93A039);
    a=II(a,b,c,d,x[k+12],S41,0x655B59C3); d=II(d,a,b,c,x[k+3],S42,0x8F0CCC92);
    c=II(c,d,a,b,x[k+10],S43,0xFFEFF47D); b=II(b,c,d,a,x[k+1],S44,0x85845DD1);
    a=II(a,b,c,d,x[k+8],S41,0x6FA87E4F); d=II(d,a,b,c,x[k+15],S42,0xFE2CE6E0);
    c=II(c,d,a,b,x[k+6],S43,0xA3014314); b=II(b,c,d,a,x[k+13],S44,0x4E0811A1);
    a=II(a,b,c,d,x[k+4],S41,0xF7537E82); d=II(d,a,b,c,x[k+11],S42,0xBD3AF235);
    c=II(c,d,a,b,x[k+2],S43,0x2AD7D2BB); b=II(b,c,d,a,x[k+9],S44,0xEB86D391);
    a=addUnsigned(a,AA); b=addUnsigned(b,BB); c=addUnsigned(c,CC); d=addUnsigned(d,DD);
  }
  return (wordToHex(a)+wordToHex(b)+wordToHex(c)+wordToHex(d)).toLowerCase();
}
${jsEncryptFunctions}${jsDecryptFunctions}
// 配置信息
const apiUrl = 'https://www.xyapi.top/verify/api/redeem';
const apiKey = '${app.apiKey}';      // 用于身份识别（X-API-Key）
const secretKey = '${secret}';       // 用于签名和加密
const cardCode = 'your-card-code';
const machine = 'your-machine-code';

// 计算时间戳
const timestamp = Math.floor(Date.now() / 1000).toString();

${jsRequestLogic}`
  
  // ===== Python 代码 =====
  let pythonCode = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      pythonCode = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
卡密验证 API 调用示例（启用 AES-CBC 加密）
使用前请先安装依赖库: pip install requests pycryptodome
"""
import hashlib
import time
import requests
import json
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def normalize_key(key_str):
    """规范化密钥长度为 16 字节"""
    key_bytes = key_str.encode('utf-8')
    if len(key_bytes) in (16, 24, 32):
        return key_bytes
    # Pad or trim to 16 bytes
    normalized = bytearray(16)
    for i in range(16):
        normalized[i] = key_bytes[i] if i < len(key_bytes) else 0
    return bytes(normalized)

def encrypt_aes(plaintext, key):
    """使用 AES-CBC 加密"""
    try:
        key_bytes = normalize_key(key)
        iv = bytes(16)  # 全零 IV
        cipher = AES.new(key_bytes, AES.MODE_CBC, iv)
        
        # PKCS7 padding
        from Crypto.Util.Padding import pad
        plain_bytes = plaintext.encode('utf-8')
        padded = pad(plain_bytes, AES.block_size)
        
        encrypted = cipher.encrypt(padded)
        return base64.b64encode(encrypted).decode('utf-8')
    except Exception as e:
        raise RuntimeError(f'AES 加密失败: {e}')

def decrypt_aes(base64_cipher, key):
    """使用 AES-CBC 解密"""
    try:
        cipher_bytes = base64.b64decode(base64_cipher)
        key_bytes = normalize_key(key)
        iv = bytes(16)  # 全零 IV
        
        cipher = AES.new(key_bytes, AES.MODE_CBC, iv)
        plain_bytes = unpad(cipher.decrypt(cipher_bytes), AES.block_size)
        return plain_bytes.decode('utf-8')
    except Exception as e:
        raise RuntimeError(f'AES 解密失败: {e}')

def verify_card(api_url, api_key, secret_key, card_code, machine_code=None):
    """
    调用卡密验证接口（安全模式：加密整个请求对象）
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
        card_code: 卡密码
        machine_code: 机器码
    
    Returns:
        dict: API 响应数据
    """
    # 构建请求数据对象
    request_data = {'code': card_code}
    if machine_code:
        request_data['machine'] = machine_code
    
    # 将请求数据转为 JSON 并加密
    plain_json = json.dumps(request_data)
    encrypted_payload = encrypt_aes(plain_json, secret_key)
    
    # 计算时间戳和签名（使用加密后的 payload）
    timestamp = str(int(time.time()))
    sign_str = secret_key + timestamp + encrypted_payload
    sign = md5(sign_str)
    
    # 构建请求体（只包含加密的 payload）
    payload = {'payload': encrypted_payload}
    
    # 设置请求头
    headers = {
        'Content-Type': 'application/json',
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        # 发送 POST 请求
        response = requests.post(
            api_url,
            json=payload,
            headers=headers,
            timeout=10
        )
        response.raise_for_status()
        
        # 解析 JSON 响应
        data = response.json()
        
        # 如果响应包含 payload，说明是加密响应
        if 'payload' in data:
            print('检测到加密响应，正在解密...')
            decrypted = decrypt_aes(data['payload'], secret_key)
            print(f'解密后的数据: {decrypted}')
            return json.loads(decrypted)
        
        return data
        
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None
    except Exception as e:
        print(f'解密失败: {e}')
        return None

# ========== 主程序 ==========
if __name__ == '__main__':
    # 配置信息
    API_URL = 'https://www.xyapi.top/verify/api/redeem'
    API_KEY = '\${app.apiKey}'     # 用于身份识别（X-API-Key）
    SECRET_KEY = '\${secret}'      # 用于签名和加密
    CARD_CODE = 'your-card-code'
    MACHINE_CODE = 'your-machine-code'
    
    print('========== 卡密验证 API 调用示例（AES 加密）==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print(f'卡密码: {CARD_CODE}')
    print(f'机器码: {MACHINE_CODE}')
    print('=' * 56)
    print()
    
    # 调用接口
    result = verify_card(API_URL, API_KEY, SECRET_KEY, CARD_CODE, MACHINE_CODE)
    
    if result:
        print('\\n最终响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        # 判断结果
        if result.get('success'):
            print('✅ 核销成功！')
            print(f'   到期时间: {result.get("expireAtReadable", "N/A")}')
            if result.get('extra'):
                print(f'   附加信息: {result.get("extra")}')
        else:
            print('❌ 核销失败！')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
    } else {
      // RC4 加密
      pythonCode = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
卡密验证 API 调用示例（启用 RC4 加密）
使用前请先安装 requests 库: pip install requests
"""
import hashlib
import time
import requests
import json
import base64

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def rc4(data, key):
    """RC4 加密/解密算法"""
    key_bytes = key.encode('utf-8')
    s = list(range(256))
    j = 0
    
    # KSA
    for i in range(256):
        j = (j + s[i] + key_bytes[i % len(key_bytes)]) & 0xFF
        s[i], s[j] = s[j], s[i]
    
    # PRGA
    out = bytearray()
    i = j = 0
    for byte in data:
        i = (i + 1) & 0xFF
        j = (j + s[i]) & 0xFF
        s[i], s[j] = s[j], s[i]
        t = (s[i] + s[j]) & 0xFF
        out.append(byte ^ s[t])
    
    return bytes(out)

def encrypt_rc4(plaintext, key):
    """使用 RC4 加密"""
    plain_bytes = plaintext.encode('utf-8')
    encrypted_bytes = rc4(plain_bytes, key)
    return base64.b64encode(encrypted_bytes).decode('utf-8')

def decrypt_rc4(base64_cipher, key):
    """使用 RC4 解密"""
    cipher_bytes = base64.b64decode(base64_cipher)
    plain_bytes = rc4(cipher_bytes, key)
    return plain_bytes.decode('utf-8')

def verify_card(api_url, api_key, secret_key, card_code, machine_code=None):
    """
    调用卡密验证接口（安全模式：加密整个请求对象）
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
        card_code: 卡密码
        machine_code: 机器码
    
    Returns:
        dict: API 响应数据
    """
    # 构建请求数据对象
    request_data = {'code': card_code}
    if machine_code:
        request_data['machine'] = machine_code
    
    # 将请求数据转为 JSON 并加密
    plain_json = json.dumps(request_data)
    encrypted_payload = encrypt_rc4(plain_json, secret_key)
    
    # 计算时间戳和签名（使用加密后的 payload）
    timestamp = str(int(time.time()))
    sign_str = secret_key + timestamp + encrypted_payload
    sign = md5(sign_str)
    
    # 构建请求体（只包含加密的 payload）
    payload = {'payload': encrypted_payload}
    
    # 设置请求头
    headers = {
        'Content-Type': 'application/json',
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        # 发送 POST 请求
        response = requests.post(
            api_url,
            json=payload,
            headers=headers,
            timeout=10
        )
        response.raise_for_status()
        
        # 解析 JSON 响应
        data = response.json()
        
        # 如果响应包含 payload，说明是加密响应
        if 'payload' in data:
            print('检测到加密响应，正在解密...')
            decrypted = decrypt_rc4(data['payload'], secret_key)
            print(f'解密后的数据: {decrypted}')
            return json.loads(decrypted)
        
        return data
        
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None
    except Exception as e:
        print(f'解密失败: {e}')
        return None

# ========== 主程序 ==========
if __name__ == '__main__':
    # 配置信息
    API_URL = 'https://www.xyapi.top/verify/api/redeem'
    API_KEY = '\${app.apiKey}'     # 用于身份识别（X-API-Key）
    SECRET_KEY = '\${secret}'      # 用于签名和加密
    CARD_CODE = 'your-card-code'
    MACHINE_CODE = 'your-machine-code'
    
    print('========== 卡密验证 API 调用示例（RC4 加密）==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print(f'卡密码: {CARD_CODE}')
    print(f'机器码: {MACHINE_CODE}')
    print('=' * 54)
    print()
    
    # 调用接口
    result = verify_card(API_URL, API_KEY, SECRET_KEY, CARD_CODE, MACHINE_CODE)
    
    if result:
        print('\\n最终响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        # 判断结果
        if result.get('success'):
            print('✅ 核销成功！')
            print(f'   到期时间: {result.get("expireAtReadable", "N/A")}')
            if result.get('extra'):
                print(f'   附加信息: {result.get("extra")}')
        else:
            print('❌ 核销失败！')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
    }
  } else {
    // 非加密模式
    pythonCode = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
卡密验证 API 调用示例
使用前请先安装 requests 库: pip install requests
"""
import hashlib
import time
import requests
import json

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def verify_card(api_url, api_key, secret_key, card_code, machine_code=None):
    """
    调用卡密验证接口
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
        card_code: 卡密码
        machine_code: 机器码
    
    Returns:
        dict: API 响应数据
    """
    # 计算时间戳和签名
    timestamp = str(int(time.time()))
    sign_str = secret_key + timestamp + card_code
    sign = md5(sign_str)
    
    # 构建请求体（JSON格式）
    payload = {'code': card_code}
    if machine_code:
        payload['machine'] = machine_code
    
    # 设置请求头
    headers = {
        'Content-Type': 'application/json',
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        # 发送 POST 请求（使用 json 参数）
        response = requests.post(
            api_url,
            json=payload,
            headers=headers,
            timeout=10
        )
        response.raise_for_status()  # 检查 HTTP 状态码
        
        # 解析 JSON 响应
        data = response.json()
        return data
        
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None

# ========== 主程序 ==========
if __name__ == '__main__':
    # 配置信息
    API_URL = 'https://www.xyapi.top/verify/api/redeem'
    API_KEY = '\${app.apiKey}'     # 用于身份识别（X-API-Key）
    SECRET_KEY = '\${secret}'      # 用于签名和加密
    CARD_CODE = 'your-card-code'
    MACHINE_CODE = 'your-machine-code'
    
    print('========== 卡密验证 API 调用示例 ==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print(f'卡密码: {CARD_CODE}')
    print(f'机器码: {MACHINE_CODE}')
    print('=' * 46)
    print()
    
    # 调用接口
    result = verify_card(API_URL, API_KEY, SECRET_KEY, CARD_CODE, MACHINE_CODE)
    
    if result:
        print('响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        # 判断结果
        if result.get('success'):
            print('✅ 核销成功！')
            print(f'   状态码: {result.get("code")}')
            print(f'   到期时间: {result.get("expireAtReadable", "N/A")}')
            if result.get('extra'):
                print(f'   附加信息: {result.get("extra")}')
        else:
            print('❌ 核销失败！')
            print(f'   状态码: {result.get("code")}')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
  }
  
  return {
    java: javaCode,
    js: jsCode,
    python: pythonCode
  }
})

const noticeCodeExamples = computed(() => {
  const app = appInfo.value
  if (!app) return { java: '// 加载中...', js: '// 加载中...', python: '# 加载中...' }
  
  const secret = app.secretKey || app.apiKey
  const isSecure = app.secure === true
  const encAlg = app.encryptionAlg || 'RC4'
  
  // ===== Java 代码 =====
  let javaDecryptMethods = ''
  let javaDecryptLogic = ''
  
  if (isSecure) {
    // 重用 redeemCodeExamples 中的解密方法
    if (encAlg.startsWith('AES')) {
      javaDecryptMethods = `
    // AES-CBC 解密方法
    public static String decryptAes(String base64Cipher, String key) {
        try {
            byte[] cipher = java.util.Base64.getDecoder().decode(base64Cipher);
            byte[] keyBytes = normalizeKey(key);
            byte[] iv = new byte[16]; // 全零 IV
            
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.spec.IvParameterSpec ivSpec = new javax.crypto.spec.IvParameterSpec(iv);
            c.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] plain = c.doFinal(cipher);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败", e);
        }
    }
    
    private static byte[] normalizeKey(String key) {
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        if (kb.length == 16 || kb.length == 24 || kb.length == 32) return kb;
        byte[] out = new byte[16];
        for (int i = 0; i < out.length; i++) {
            out[i] = (i < kb.length) ? kb[i] : 0;
        }
        return out;
    }`
      javaDecryptLogic = `
        
        // 解析响应并解密
        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
        if (jsonResponse.has("payload")) {
            String payload = jsonResponse.getString("payload");
            String decrypted = decryptAes(payload, secretKey);
            System.out.println("\\n解密后的数据: " + decrypted);
            
            org.json.JSONObject result = new org.json.JSONObject(decrypted);
            if (result.getBoolean("success")) {
                System.out.println("✅ 获取成功！");
                System.out.println("   公告: " + result.optString("announcement", "无"));
                System.out.println("   版本: " + result.optString("version", "无"));
                System.out.println("   更新日志: " + result.optString("changelog", "无"));
            } else {
                System.out.println("❌ 获取失败: " + result.getString("message"));
            }
        } else {
            System.out.println("❌ 响应格式错误");
        }`
    } else {
      javaDecryptMethods = `
    // RC4 解密方法
    public static String decryptRc4(String base64Cipher, String key) {
        byte[] cipherBytes = java.util.Base64.getDecoder().decode(base64Cipher);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] plain = rc4(cipherBytes, keyBytes);
        return new String(plain, StandardCharsets.UTF_8);
    }
    
    private static byte[] rc4(byte[] data, byte[] key) {
        byte[] s = new byte[256];
        for (int i = 0; i < 256; i++) s[i] = (byte) i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + (s[i] & 0xFF) + (key[i % key.length] & 0xFF)) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
        }
        byte[] out = new byte[data.length];
        int i = 0; j = 0;
        for (int k = 0; k < data.length; k++) {
            i = (i + 1) & 0xFF;
            j = (j + (s[i] & 0xFF)) & 0xFF;
            byte tmp = s[i]; s[i] = s[j]; s[j] = tmp;
            int t = ((s[i] & 0xFF) + (s[j] & 0xFF)) & 0xFF;
            out[k] = (byte) (data[k] ^ s[t]);
        }
        return out;
    }`
      javaDecryptLogic = `
        
        // 解析响应并解密
        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
        if (jsonResponse.has("payload")) {
            String payload = jsonResponse.getString("payload");
            String decrypted = decryptRc4(payload, secretKey);
            System.out.println("\\n解密后的数据: " + decrypted);
            
            org.json.JSONObject result = new org.json.JSONObject(decrypted);
            if (result.getBoolean("success")) {
                System.out.println("✅ 获取成功！");
                System.out.println("   公告: " + result.optString("announcement", "无"));
                System.out.println("   版本: " + result.optString("version", "无"));
                System.out.println("   更新日志: " + result.optString("changelog", "无"));
            } else {
                System.out.println("❌ 获取失败: " + result.getString("message"));
            }
        } else {
            System.out.println("❌ 响应格式错误");
        }`
    }
  } else {
    javaDecryptLogic = `
        
        // 解析响应 JSON
        org.json.JSONObject result = new org.json.JSONObject(response.toString());
        if (result.getBoolean("success")) {
            System.out.println("✅ 获取成功！");
            System.out.println("   公告: " + result.optString("announcement", "无"));
            System.out.println("   版本: " + result.optString("version", "无"));
            System.out.println("   更新日志: " + result.optString("changelog", "无"));
        } else {
            System.out.println("❌ 获取失败: " + result.getString("message"));
        }`
  }
  
  const javaCode = `import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 通知接口调用示例
 * 传输安全: ${isSecure ? '已开启 (' + encAlg + ')' : '未开启'}
 * 
 * 注意：需要添加 org.json 依赖
 * Maven: <dependency><groupId>org.json</groupId><artifactId>json</artifactId><version>20231013</version></dependency>
 */
public class NoticeApiExample {
    
    // MD5 加密方法
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }${javaDecryptMethods}
    
    public static void main(String[] args) throws Exception {
        // 配置信息
        String apiUrl = "https://www.xyapi.top/verfiy/api/notice";
        String apiKey = "${app.apiKey}";  // 用于身份识别（X-API-Key）
        String secretKey = "${secret}";   // 用于签名和加密
        
        // 计算时间戳和签名
        long timestamp = System.currentTimeMillis() / 1000;
        String sign = md5(secretKey + timestamp);
        
        // 构建请求
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法和请求头
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-API-Key", apiKey);
        conn.setRequestProperty("X-Timestamp", String.valueOf(timestamp));
        conn.setRequestProperty("X-Sign", sign);
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        // 打印原始响应
        System.out.println("Response Code: " + responseCode);
        System.out.println("Response Body: " + response.toString());${javaDecryptLogic}
    }
}`
  
  // ===== JavaScript 代码 =====
  let jsCodeNotice = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      jsCodeNotice = `/**
 * 通知接口调用示例（JavaScript）
 * 传输安全: 已开启 (${encAlg})
 * 注意：MD5 实现请参考"卡密验证接口"示例
 */

// 这里省略了完整的 MD5 实现，请从"卡密验证接口"代码示例中复制
// 或使用第三方库： const md5 = require('crypto-js/md5');
function md5(str) {
  // 请复制完整的 MD5 函数实现
  console.warn('请实现 md5 函数');
  return 'replace-with-actual-md5-implementation';
}

// AES-CBC 解密函数（使用 Web Crypto API）
async function decryptAes(base64Cipher, key) {
  const cipherBytes = Uint8Array.from(atob(base64Cipher), c => c.charCodeAt(0));
  const keyBytes = new TextEncoder().encode(key);
  const normalizedKey = new Uint8Array(16);
  for (let i = 0; i < normalizedKey.length; i++) {
    normalizedKey[i] = i < keyBytes.length ? keyBytes[i] : 0;
  }
  const cryptoKey = await crypto.subtle.importKey('raw', normalizedKey, { name: 'AES-CBC' }, false, ['decrypt']);
  const iv = new Uint8Array(16);
  const plainBytes = await crypto.subtle.decrypt({ name: 'AES-CBC', iv }, cryptoKey, cipherBytes);
  return new TextDecoder('utf-8').decode(plainBytes);
}

// 配置信息
const apiUrl = 'https://www.xyapi.top/verfiy/api/notice';
const apiKey = '\${app.apiKey}';
const secretKey = '\${secret}';

// 计算时间戳和签名
const timestamp = Math.floor(Date.now() / 1000).toString();
const sign = md5(secretKey + timestamp);

// 发送请求
fetch(apiUrl, {
  method: 'GET',
  headers: {
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  }
})
.then(res => res.json())
.then(data => {
  console.log('原始响应:', data);
  if (data.payload) {
    decryptAes(data.payload, secretKey).then(decrypted => {
      console.log('解密后的数据:', decrypted);
      const result = JSON.parse(decrypted);
      if (result.success) {
        console.log('✅ 获取成功！');
        console.log('   公告:', result.announcement || '无');
        console.log('   版本:', result.version || '无');
        console.log('   更新日志:', result.changelog || '无');
      } else {
        console.log('❌ 获取失败:', result.message);
      }
    }).catch(err => console.error('解密失败:', err));
  } else {
    console.log('❌ 响应格式错误');
  }
})
.catch(err => console.error('请求失败:', err));`
    } else {
      // RC4 加密
      jsCodeNotice = `/**
 * 通知接口调用示例（JavaScript）
 * 传输安全: 已开启 (RC4)
 * 注意：MD5 实现请参考"卡密验证接口"示例
 */

// 这里省略了完整的 MD5 实现，请从"卡密验证接口"代码示例中复制
function md5(str) {
  console.warn('请实现 md5 函数');
  return 'replace-with-actual-md5-implementation';
}

// RC4 解密函数
function decryptRc4(base64Cipher, key) {
  const cipherBytes = Uint8Array.from(atob(base64Cipher), c => c.charCodeAt(0));
  const keyBytes = new TextEncoder().encode(key);
  const s = new Uint8Array(256);
  for (let i = 0; i < 256; i++) s[i] = i;
  let j = 0;
  for (let i = 0; i < 256; i++) {
    j = (j + s[i] + keyBytes[i % keyBytes.length]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
  }
  const out = new Uint8Array(cipherBytes.length);
  let i = 0; j = 0;
  for (let k = 0; k < cipherBytes.length; k++) {
    i = (i + 1) & 0xFF;
    j = (j + s[i]) & 0xFF;
    [s[i], s[j]] = [s[j], s[i]];
    const t = (s[i] + s[j]) & 0xFF;
    out[k] = cipherBytes[k] ^ s[t];
  }
  return new TextDecoder('utf-8').decode(out);
}

// 配置信息
const apiUrl = 'https://www.xyapi.top/verfiy/api/notice';
const apiKey = '\${app.apiKey}';
const secretKey = '\${secret}';

// 计算时间戳和签名
const timestamp = Math.floor(Date.now() / 1000).toString();
const sign = md5(secretKey + timestamp);

// 发送请求
fetch(apiUrl, {
  method: 'GET',
  headers: {
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  }
})
.then(res => res.json())
.then(data => {
  console.log('原始响应:', data);
  if (data.payload) {
    try {
      const decrypted = decryptRc4(data.payload, secretKey);
      console.log('解密后的数据:', decrypted);
      const result = JSON.parse(decrypted);
      if (result.success) {
        console.log('✅ 获取成功！');
        console.log('   公告:', result.announcement || '无');
        console.log('   版本:', result.version || '无');
        console.log('   更新日志:', result.changelog || '无');
      } else {
        console.log('❌ 获取失败:', result.message);
      }
    } catch (err) {
      console.error('解密失败:', err);
    }
  } else {
    console.log('❌ 响应格式错误');
  }
})
.catch(err => console.error('请求失败:', err));`
    }
  } else {
    // 非加密模式
    jsCodeNotice = `/**
 * 通知接口调用示例（JavaScript）
 * 传输安全: 未开启
 * 注意：MD5 实现请参考"卡密验证接口"示例
 */

// 这里省略了完整的 MD5 实现，请从"卡密验证接口"代码示例中复制
function md5(str) {
  console.warn('请实现 md5 函数');
  return 'replace-with-actual-md5-implementation';
}

// 配置信息
const apiUrl = 'https://www.xyapi.top/verfiy/api/notice';
const apiKey = '\${app.apiKey}';
const secretKey = '\${secret}';

// 计算时间戳和签名
const timestamp = Math.floor(Date.now() / 1000).toString();
const sign = md5(secretKey + timestamp);

// 发送请求
fetch(apiUrl, {
  method: 'GET',
  headers: {
    'X-API-Key': apiKey,
    'X-Timestamp': timestamp,
    'X-Sign': sign
  }
})
.then(res => res.json())
.then(data => {
  console.log('响应数据:', data);
  if (data.success) {
    console.log('✅ 获取成功！');
    console.log('   公告:', data.announcement || '无');
    console.log('   版本:', data.version || '无');
    console.log('   更新日志:', data.changelog || '无');
  } else {
    console.log('❌ 获取失败:', data.message);
  }
})
.catch(err => console.error('请求失败:', err));`
    }
  
  // ===== Python 代码 =====
  let pythonCodeNotice = ''
  
  if (isSecure) {
    if (encAlg.startsWith('AES')) {
      pythonCodeNotice = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
通知接口 API 调用示例（启用 AES-CBC 加密）
使用前请先安装依赖库: pip install requests pycryptodome
"""
import hashlib
import time
import requests
import json
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def normalize_key(key_str):
    """规范化密钥长度为 16 字节"""
    key_bytes = key_str.encode('utf-8')
    if len(key_bytes) in (16, 24, 32):
        return key_bytes
    normalized = bytearray(16)
    for i in range(16):
        normalized[i] = key_bytes[i] if i < len(key_bytes) else 0
    return bytes(normalized)

def decrypt_aes(base64_cipher, key):
    """使用 AES-CBC 解密"""
    try:
        cipher_bytes = base64.b64decode(base64_cipher)
        key_bytes = normalize_key(key)
        iv = bytes(16)
        cipher = AES.new(key_bytes, AES.MODE_CBC, iv)
        plain_bytes = unpad(cipher.decrypt(cipher_bytes), AES.block_size)
        return plain_bytes.decode('utf-8')
    except Exception as e:
        raise RuntimeError(f'AES 解密失败: {e}')

def get_notice(api_url, api_key, secret_key):
    """
    调用通知接口
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
    
    Returns:
        dict: API 响应数据
    """
    timestamp = str(int(time.time()))
    sign = md5(secret_key + timestamp)
    
    headers = {
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        response = requests.get(api_url, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()
        
        if 'payload' in data:
            print('检测到加密响应，正在解密...')
            decrypted = decrypt_aes(data['payload'], secret_key)
            print(f'解密后的数据: {decrypted}')
            return json.loads(decrypted)
        
        return data
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None
    except Exception as e:
        print(f'解密失败: {e}')
        return None

if __name__ == '__main__':
    API_URL = 'https://www.xyapi.top/verfiy/api/notice'
    API_KEY = '\${app.apiKey}'
    SECRET_KEY = '\${secret}'
    
    print('========== 通知接口 API 调用示例（AES 加密）==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print('=' * 54)
    print()
    
    result = get_notice(API_URL, API_KEY, SECRET_KEY)
    
    if result:
        print('\\n最终响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        if result.get('success'):
            print('✅ 获取成功！')
            print(f'   公告: {result.get("announcement", "无")}')
            print(f'   版本: {result.get("version", "无")}')
            print(f'   更新日志: {result.get("changelog", "无")}')
        else:
            print('❌ 获取失败！')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
    } else {
      // RC4 加密
      pythonCodeNotice = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
通知接口 API 调用示例（启用 RC4 加密）
使用前请先安装 requests 库: pip install requests
"""
import hashlib
import time
import requests
import json
import base64

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def rc4(data, key):
    """RC4 加密/解密算法"""
    key_bytes = key.encode('utf-8')
    s = list(range(256))
    j = 0
    for i in range(256):
        j = (j + s[i] + key_bytes[i % len(key_bytes)]) & 0xFF
        s[i], s[j] = s[j], s[i]
    out = bytearray()
    i = j = 0
    for byte in data:
        i = (i + 1) & 0xFF
        j = (j + s[i]) & 0xFF
        s[i], s[j] = s[j], s[i]
        t = (s[i] + s[j]) & 0xFF
        out.append(byte ^ s[t])
    return bytes(out)

def decrypt_rc4(base64_cipher, key):
    """使用 RC4 解密"""
    cipher_bytes = base64.b64decode(base64_cipher)
    plain_bytes = rc4(cipher_bytes, key)
    return plain_bytes.decode('utf-8')

def get_notice(api_url, api_key, secret_key):
    """
    调用通知接口
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
    
    Returns:
        dict: API 响应数据
    """
    timestamp = str(int(time.time()))
    sign = md5(secret_key + timestamp)
    
    headers = {
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        response = requests.get(api_url, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()
        
        if 'payload' in data:
            print('检测到加密响应，正在解密...')
            decrypted = decrypt_rc4(data['payload'], secret_key)
            print(f'解密后的数据: {decrypted}')
            return json.loads(decrypted)
        
        return data
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None
    except Exception as e:
        print(f'解密失败: {e}')
        return None

if __name__ == '__main__':
    API_URL = 'https://www.xyapi.top/verfiy/api/notice'
    API_KEY = '\${app.apiKey}'
    SECRET_KEY = '\${secret}'
    
    print('========== 通知接口 API 调用示例（RC4 加密）==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print('=' * 52)
    print()
    
    result = get_notice(API_URL, API_KEY, SECRET_KEY)
    
    if result:
        print('\\n最终响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        if result.get('success'):
            print('✅ 获取成功！')
            print(f'   公告: {result.get("announcement", "无")}')
            print(f'   版本: {result.get("version", "无")}')
            print(f'   更新日志: {result.get("changelog", "无")}')
        else:
            print('❌ 获取失败！')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
    }
  } else {
    // 非加密模式
    pythonCodeNotice = `#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
通知接口 API 调用示例
使用前请先安装 requests 库: pip install requests
"""
import hashlib
import time
import requests
import json

def md5(text):
    """计算 MD5 哈希值"""
    return hashlib.md5(text.encode('utf-8')).hexdigest()

def get_notice(api_url, api_key, secret_key):
    """
    调用通知接口
    
    Args:
        api_url: API 地址
        api_key: API Key（用于身份识别）
        secret_key: 密钥（用于签名和加密）
    
    Returns:
        dict: API 响应数据
    """
    timestamp = str(int(time.time()))
    sign = md5(secret_key + timestamp)
    
    headers = {
        'X-API-Key': api_key,
        'X-Timestamp': timestamp,
        'X-Sign': sign
    }
    
    try:
        response = requests.get(api_url, headers=headers, timeout=10)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f'请求失败: {e}')
        return None

if __name__ == '__main__':
    API_URL = 'https://www.xyapi.top/verfiy/api/notice'
    API_KEY = '\${app.apiKey}'
    SECRET_KEY = '\${secret}'
    
    print('========== 通知接口 API 调用示例 ==========')
    print(f'API URL: {API_URL}')
    print(f'API Key: {API_KEY}')
    print('=' * 46)
    print()
    
    result = get_notice(API_URL, API_KEY, SECRET_KEY)
    
    if result:
        print('响应数据:')
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()
        
        if result.get('success'):
            print('✅ 获取成功！')
            print(f'   公告: {result.get("announcement", "无")}')
            print(f'   版本: {result.get("version", "无")}')
            print(f'   更新日志: {result.get("changelog", "无")}')
        else:
            print('❌ 获取失败！')
            print(f'   失败原因: {result.get("message")}')
    else:
        print('❌ 请求失败，请检查网络连接或 API 配置')`
  }
  
  return {
    java: javaCode,
    js: jsCodeNotice,
    python: pythonCodeNotice
  }
})

// 示例数据
const redeemExamples = computed(() => {
  const normalizeMode = (mode?: string | null) => {
    if (!mode) return 'SUCCESS_ONLY'
    const raw = mode.trim()
    if (!raw) return 'SUCCESS_ONLY'
    const upper = raw.toUpperCase()
    if (upper === 'ALWAYS') return 'ALWAYS'
    if (upper === 'SUCCESS_ONLY' || upper === 'SUCCESS') return 'SUCCESS_ONLY'
    if (upper === 'FAILURE_ONLY' || upper === 'FAILURE') return 'FAILURE_ONLY'
    if (raw === '总是' || raw === '总是返回') return 'ALWAYS'
    if (raw === '成功' || raw === '仅成功' || raw === '仅成功时返回') return 'SUCCESS_ONLY'
    if (raw === '失败' || raw === '仅失败' || raw === '仅失败时返回') return 'FAILURE_ONLY'
    return upper
  }

  const assignFields = (target: Record<string, any>, source: any) => {
    if (!source || typeof source !== 'object') return
    Object.entries(source).forEach(([k, v]) => {
      if (!k) return
      target[k] = v
    })
  }

  const customParams = appInfo.value?.redeemExtra
  const globalMode = normalizeMode(appInfo.value?.redeemExtraMode || 'SUCCESS_ONLY')

  const alwaysFields: Record<string, any> = {}
  const successFields: Record<string, any> = {}
  const failureFields: Record<string, any> = {}

  if (customParams) {
    try {
      const parsed = JSON.parse(customParams)
      if (parsed && typeof parsed === 'object') {
        Object.entries(parsed).forEach(([key, val]) => {
          const mode = normalizeMode(key)
          if (mode === 'ALWAYS' || mode === 'SUCCESS_ONLY' || mode === 'FAILURE_ONLY') {
            if (mode === 'ALWAYS') {
              assignFields(alwaysFields, val)
            } else if (mode === 'SUCCESS_ONLY') {
              assignFields(successFields, val)
            } else {
              assignFields(failureFields, val)
            }
            return
          }

          if (val && typeof val === 'object' && ('value' in val || 'mode' in val)) {
            const valueMode = normalizeMode((val as any).mode || globalMode)
            const target = valueMode === 'ALWAYS' ? alwaysFields : valueMode === 'FAILURE_ONLY' ? failureFields : successFields
            target[key] = (val as any).value ?? ''
          } else {
            const target = globalMode === 'ALWAYS' ? alwaysFields : globalMode === 'FAILURE_ONLY' ? failureFields : successFields
            target[key] = val
          }
        })
      }
    } catch (e) {
      // ignore JSON parse errors
    }
  }

  return {
    request: {
      headers: {
        'X-API-Key': appInfo.value?.apiKey || 'your-api-key',
        'X-Timestamp': '1234567890',
        'X-Sign': 'md5(secret + timestamp + code)'
      },
      params: {
        code: 'XXXX-XXXX-XXXX',
        machine: 'machine-code-123'
      }
    },
    successResponse: {
      success: true,
      code: 0,
      message: '核销成功',
      expireAt: 4100688000,
      expireAtReadable: '永久有效',
      ...alwaysFields,
      ...successFields
    },
    failureResponse: {
      success: false,
      code: 1005,
      message: '核销失败，可能是机器码已达上限或其他限制',
      ...alwaysFields,
      ...failureFields
    }
  }
})

const noticeExamples = computed(() => ({
  request: {
    headers: {
      'X-API-Key': appInfo.value?.apiKey || 'your-api-key',
      'X-Timestamp': '1234567890',
      'X-Sign': 'md5(secret + timestamp)'
    }
  },
  successResponse: {
    success: true,
    announcement: appInfo.value?.announcement || '欢迎使用本系统',
    version: appInfo.value?.version || '1.0.0',
    changelog: appInfo.value?.changelog || '初始版本'
  }
}))

onMounted(() => {
  loadApp()
})
</script>

<template>
  <div class="api-docs-page">
    <div class="page-header">
      <h1 class="page-title">API 调用示例</h1>
      <p class="page-desc">在线测试 API 接口，查看请求响应示例和代码调用示例</p>
    </div>

    <!-- 未选择应用提示 -->
    <div v-if="!appInfo || !appInfo.id" class="warning-banner">
      <svg viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
      </svg>
      <div class="warning-content">
        <h3>未选择应用</h3>
        <p>请先在<router-link to="/apps" class="warning-link">「应用」</router-link>页面点击一个应用，然后再使用调用示例功能</p>
      </div>
    </div>

    <!-- API 类型选项卡 -->
    <div class="api-tabs">
      <button 
        class="api-tab" 
        :class="{ active: activeTab === 'redeem' }"
        @click="activeTab = 'redeem'"
      >
        <svg viewBox="0 0 20 20" fill="currentColor">
          <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
          <path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd" />
        </svg>
        卡密验证接口
      </button>
      <button 
        class="api-tab" 
        :class="{ active: activeTab === 'notice' }"
        @click="activeTab = 'notice'"
      >
        <svg viewBox="0 0 20 20" fill="currentColor">
          <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z" />
        </svg>
        通知接口
      </button>
    </div>

    <!-- 卡密验证接口 -->
    <div v-if="activeTab === 'redeem'" class="api-content">
      <!-- 在线测试 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">在线测试</h2>
          <p class="section-desc">填写参数后点击"发起请求"按钮测试接口</p>
        </div>
        <div class="section-body">
          <div class="test-form">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">卡密码 *</label>
                <input v-model="redeemForm.code" type="text" class="form-input" placeholder="请输入卡密码" />
              </div>
              <div class="form-group">
                <label class="form-label">机器码</label>
                <input v-model="redeemForm.machine" type="text" class="form-input" placeholder="可选" />
              </div>
            </div>
            <button @click="callRedeemApi" :disabled="loading" class="btn btn-primary">
              <svg v-if="loading" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>发起请求</span>
            </button>
          </div>

          <!-- 返回结果 -->
          <div v-if="redeemResult" class="result-container">
            <div class="result-header">
              <h3>返回数据</h3>
              <button @click="copyToClipboard(JSON.stringify(redeemResult, null, 2))" class="copy-btn">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                复制
              </button>
            </div>
            <pre class="code-block" :class="{ success: redeemSuccess, error: !redeemSuccess }">{{ JSON.stringify(redeemResult, null, 2) }}</pre>
            
            <div v-if="decryptedRedeemResult && appInfo?.secure" class="result-header">
              <h3>解密后数据</h3>
              <button @click="copyToClipboard(JSON.stringify(decryptedRedeemResult, null, 2))" class="copy-btn">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                复制
              </button>
            </div>
            <pre v-if="decryptedRedeemResult && appInfo?.secure" class="code-block decrypted" :class="{ success: redeemSuccess, error: !redeemSuccess }">{{ JSON.stringify(decryptedRedeemResult, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 请求示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">请求示例</h2>
        </div>
        <div class="section-body">
          <div class="example-block">
            <h3>请求头</h3>
            <pre class="code-block">{{ JSON.stringify(redeemExamples.request.headers, null, 2) }}</pre>
          </div>
          <div class="example-block">
            <h3>请求参数</h3>
            <pre class="code-block">{{ JSON.stringify(redeemExamples.request.params, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 返回示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">返回示例</h2>
        </div>
        <div class="section-body">
          <div class="example-block">
            <h3>成功响应</h3>
            <pre class="code-block success">{{ JSON.stringify(redeemExamples.successResponse, null, 2) }}</pre>
          </div>
          <div class="example-block">
            <h3>失败响应</h3>
            <pre class="code-block error">{{ JSON.stringify(redeemExamples.failureResponse, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 状态码说明 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">状态码说明</h2>
          <p class="section-desc">返回数据中的 code 字段表示具体的业务状态</p>
        </div>
        <div class="section-body">
          <div class="status-code-table">
            <div class="status-code-row header-row">
              <div class="status-code-cell">状态码</div>
              <div class="status-code-cell">说明</div>
              <div class="status-code-cell">success 值</div>
            </div>
            <div class="status-code-row success-row">
              <div class="status-code-cell"><code class="status-badge success-badge">0</code></div>
              <div class="status-code-cell">核销成功</div>
              <div class="status-code-cell"><code>true</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1001</code></div>
              <div class="status-code-cell">卡密不存在</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1002</code></div>
              <div class="status-code-cell">卡密已被禁用</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1003</code></div>
              <div class="status-code-cell">卡密不属于当前应用</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1004</code></div>
              <div class="status-code-cell">卡密已过期</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1005</code></div>
              <div class="status-code-cell">核销失败，可能是机器码已达上限或其他限制</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
            <div class="status-code-row error-row">
              <div class="status-code-cell"><code class="status-badge error-badge">1006</code></div>
              <div class="status-code-cell">机器码不能为空</div>
              <div class="status-code-cell"><code>false</code></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 代码示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">代码示例</h2>
        </div>
        <div class="section-body">
          <div class="code-tabs">
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'java' }"
              @click="codeTab = 'java'"
            >Java</button>
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'js' }"
              @click="codeTab = 'js'"
            >JavaScript</button>
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'python' }"
              @click="codeTab = 'python'"
            >Python</button>
          </div>
          <div class="code-container">
            <button @click="copyToClipboard(redeemCodeExamples[codeTab])" class="code-copy-btn">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
              </svg>
              复制代码
            </button>
            <pre class="code-block code-example">{{ redeemCodeExamples[codeTab] }}</pre>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知接口 -->
    <div v-if="activeTab === 'notice'" class="api-content">
      <!-- 在线测试 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">在线测试</h2>
          <p class="section-desc">点击"发起请求"按钮测试接口</p>
        </div>
        <div class="section-body">
          <div class="test-form">
            <button @click="callNoticeApi" :disabled="loading" class="btn btn-primary">
              <svg v-if="loading" class="btn-spinner" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 3a7 7 0 100 14 7 7 0 000-14zm0 2a5 5 0 110 10 5 5 0 010-10z" opacity="0.3" />
                <path d="M10 3a7 7 0 017 7h-2a5 5 0 00-5-5V3z" />
              </svg>
              <span v-else>发起请求</span>
            </button>
          </div>

          <!-- 返回结果 -->
          <div v-if="noticeResult" class="result-container">
            <div class="result-header">
              <h3>返回数据</h3>
              <button @click="copyToClipboard(JSON.stringify(noticeResult, null, 2))" class="copy-btn">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                复制
              </button>
            </div>
            <pre class="code-block">{{ JSON.stringify(noticeResult, null, 2) }}</pre>
            
            <div v-if="decryptedNoticeResult && appInfo?.secure" class="result-header">
              <h3>解密后数据</h3>
              <button @click="copyToClipboard(JSON.stringify(decryptedNoticeResult, null, 2))" class="copy-btn">
                <svg viewBox="0 0 20 20" fill="currentColor">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                复制
              </button>
            </div>
            <pre v-if="decryptedNoticeResult && appInfo?.secure" class="code-block decrypted">{{ JSON.stringify(decryptedNoticeResult, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 请求示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">请求示例</h2>
        </div>
        <div class="section-body">
          <div class="example-block">
            <h3>请求头</h3>
            <pre class="code-block">{{ JSON.stringify(noticeExamples.request.headers, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 返回示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">返回示例</h2>
        </div>
        <div class="section-body">
          <div class="example-block">
            <h3>成功响应</h3>
            <pre class="code-block success">{{ JSON.stringify(noticeExamples.successResponse, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- 代码示例 -->
      <div class="api-section">
        <div class="section-header">
          <h2 class="section-title">代码示例</h2>
        </div>
        <div class="section-body">
          <div class="code-tabs">
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'java' }"
              @click="codeTab = 'java'"
            >Java</button>
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'js' }"
              @click="codeTab = 'js'"
            >JavaScript</button>
            <button 
              class="code-tab" 
              :class="{ active: codeTab === 'python' }"
              @click="codeTab = 'python'"
            >Python</button>
          </div>
          <div class="code-container">
            <button @click="copyToClipboard(noticeCodeExamples[codeTab])" class="code-copy-btn">
              <svg viewBox="0 0 20 20" fill="currentColor">
                <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
              </svg>
              复制代码
            </button>
            <pre class="code-block code-example">{{ noticeCodeExamples[codeTab] }}</pre>
          </div>
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
  </div>
</template>

<style scoped>
.api-docs-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 15px;
  color: var(--text-2);
  margin: 0;
}

/* 警告横幅 */
.warning-banner {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1) 0%, rgba(251, 191, 36, 0.1) 100%);
  border: 2px solid rgba(245, 158, 11, 0.3);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.1);
}

.warning-banner svg {
  width: 24px;
  height: 24px;
  color: #f59e0b;
  flex-shrink: 0;
  margin-top: 2px;
}

.warning-content {
  flex: 1;
}

.warning-content h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: #d97706;
}

.warning-content p {
  margin: 0;
  font-size: 14px;
  color: var(--text-2);
  line-height: 1.6;
}

.warning-link {
  color: var(--brand);
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
}

.warning-link:hover {
  color: #5b21b6;
  text-decoration: underline;
}

/* API 选项卡 */
.api-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  border-bottom: 2px solid rgba(0, 0, 0, 0.06);
}

.api-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  background: transparent;
  color: var(--text-2);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s ease;
}

.api-tab svg {
  width: 20px;
  height: 20px;
}

.api-tab:hover {
  color: var(--brand);
  background: rgba(124, 58, 237, 0.05);
}

.api-tab.active {
  color: var(--brand);
  border-bottom-color: var(--brand);
}

/* 区块 */
.api-section {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  overflow: visible;
  margin-bottom: 24px;
  transition: all 0.3s ease;
}

.api-section:hover {
  border-color: rgba(124, 58, 237, 0.2);
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.1);
}

.section-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.03) 0%, rgba(79, 70, 229, 0.03) 100%);
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

/* 状态码表格 */
.status-code-table {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.5);
}

.status-code-row {
  display: grid;
  grid-template-columns: 120px 1fr 120px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

.status-code-row:last-child {
  border-bottom: none;
}

.status-code-row.header-row {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.08) 0%, rgba(79, 70, 229, 0.08) 100%);
  font-weight: 600;
  color: var(--text-1);
}

.status-code-row.success-row {
  background: rgba(16, 185, 129, 0.02);
}

.status-code-row.success-row:hover {
  background: rgba(16, 185, 129, 0.05);
}

.status-code-row.error-row {
  background: rgba(255, 255, 255, 0.5);
}

.status-code-row.error-row:hover {
  background: rgba(239, 68, 68, 0.02);
}

.status-code-cell {
  padding: 14px 18px;
  font-size: 14px;
  color: var(--text-1);
  display: flex;
  align-items: center;
}

.status-code-cell code {
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 13px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-1);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
}

.success-badge {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.error-badge {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* 测试表单 */
.test-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

.form-input {
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

.form-input:hover {
  border-color: rgba(79, 70, 229, 0.3);
  background: rgba(255, 255, 255, 1);
}

.form-input:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
  background: rgba(255, 255, 255, 1);
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

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 结果容器 */
.result-container {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px dashed rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  margin-top: 20px;
}

.result-header:first-child {
  margin-top: 0;
}

.result-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 6px;
  background: rgba(124, 58, 237, 0.05);
  color: var(--brand);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn svg {
  width: 14px;
  height: 14px;
}

.copy-btn:hover {
  background: rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.4);
}

/* 代码块 */
.code-block {
  margin: 0;
  padding: 16px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-1);
  overflow-x: auto;
  white-space: pre;
}

.code-block.success {
  background: rgba(16, 185, 129, 0.05);
  border-color: rgba(16, 185, 129, 0.2);
  color: #059669;
}

.code-block.error {
  background: rgba(239, 68, 68, 0.05);
  border-color: rgba(239, 68, 68, 0.2);
  color: #dc2626;
}

.code-block.decrypted {
  background: rgba(124, 58, 237, 0.05);
  border-color: rgba(124, 58, 237, 0.2);
  color: var(--brand);
}

.code-block.code-example {
  max-height: 500px;
  overflow-y: auto;
}

/* 示例块 */
.example-block {
  margin-bottom: 20px;
}

.example-block:last-child {
  margin-bottom: 0;
}

.example-block h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 10px 0;
}

/* 代码选项卡 */
.code-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.code-tab {
  padding: 8px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.5);
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.code-tab:hover {
  background: rgba(124, 58, 237, 0.05);
  border-color: rgba(124, 58, 237, 0.2);
  color: var(--brand);
}

.code-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: white;
}

.code-container {
  position: relative;
}

.code-copy-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  color: var(--text-1);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 10;
}

.code-copy-btn svg {
  width: 14px;
  height: 14px;
}

.code-copy-btn:hover {
  background: rgba(124, 58, 237, 0.1);
  border-color: rgba(124, 58, 237, 0.3);
  color: var(--brand);
}

/* Toast */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  color: var(--text-1);
  font-size: 14px;
  font-weight: 500;
  z-index: 10000;
  min-width: 200px;
}

.toast svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.toast-success {
  border-color: rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.toast-error {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.toast-info {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(100px);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .api-docs-page {
    padding: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .api-tabs {
    overflow-x: auto;
  }

  .api-tab {
    padding: 10px 16px;
    font-size: 14px;
  }

  .section-body {
    padding: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .code-tabs {
    overflow-x: auto;
  }
  
  /* 响应式状态码表格 */
  .status-code-row {
    grid-template-columns: 80px 1fr 80px;
  }
  
  .status-code-cell {
    padding: 10px 12px;
    font-size: 13px;
  }
  
  .status-badge {
    min-width: 50px;
    padding: 4px 8px;
    font-size: 12px;
  }

  .toast {
    left: 16px;
    right: 16px;
    min-width: auto;
  }
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .api-section {
    background: rgba(22, 22, 26, 0.6);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .api-section:hover {
    border-color: rgba(124, 58, 237, 0.4);
  }

  .section-header {
    border-color: rgba(255, 255, 255, 0.1);
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.1) 0%, rgba(79, 70, 229, 0.1) 100%);
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

  .code-block {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .code-tab {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  /* 深色模式状态码表格 */
  .status-code-table {
    border-color: rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.2);
  }
  
  .status-code-row {
    border-bottom-color: rgba(255, 255, 255, 0.06);
  }
  
  .status-code-row.header-row {
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.15) 0%, rgba(79, 70, 229, 0.15) 100%);
  }
  
  .status-code-row.success-row {
    background: rgba(16, 185, 129, 0.05);
  }
  
  .status-code-row.success-row:hover {
    background: rgba(16, 185, 129, 0.1);
  }
  
  .status-code-row.error-row {
    background: rgba(255, 255, 255, 0.02);
  }
  
  .status-code-row.error-row:hover {
    background: rgba(239, 68, 68, 0.05);
  }
  
  .status-code-cell code {
    background: rgba(255, 255, 255, 0.1);
  }

  .toast {
    background: rgba(22, 22, 26, 0.98);
    border-color: rgba(255, 255, 255, 0.2);
  }
}
</style>

