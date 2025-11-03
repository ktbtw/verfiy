# xyz验证 - 卡密验证管理系统 + Xposed Hook 平台

**作者**: ktbtw
**版本**: 2.0.0
**最后更新**: 2025-11-03

## 📖 项目简介

xyz验证是一个功能完善的卡密验证管理系统 + Xposed Hook 在线编译平台，支持多应用管理、安全传输加密、卡密生命周期管理、在线 Hook 代码编辑与编译等功能。

## ✨ 主要特性

### 卡密验证系统

- 🔐 **多应用管理**：支持创建和管理多个应用
- 🛡️ **传输安全**：支持 RC4 和 AES 加密算法
- 🎫 **卡密管理**：支持批量生成、激活、禁用卡密
- ⏰ **灵活时效**：支持分/时/天/月/季/年/永久等多种时效单位
- 🖥️ **机器码绑定**：支持限制绑定机器数量
- 📊 **使用日志**：完整的卡密使用记录

### Xposed Hook 平台 🆕

- 📝 **在线代码编辑器**：基于 Monaco Editor 的专业 Java 编辑器
- 🔨 **在线 Dex 编译**：云端编译 Java 代码为 Dex 文件
- 📁 **文件树管理**：可视化项目结构，支持创建包和类
- 🛠️ **内置工具类**：HookHelper 工具类，简化 Hook 开发
- 📚 **依赖库支持**：内置 Fastjson、Commons IO、BouncyCastle 等常用库
- 💡 **智能提示**：自动导包、代码补全、语法高亮
- 🔒 **编译优化**：自动排除工具类，减小 Dex 体积
- 📦 **Hook 配置管理**：可视化配置 Hook 信息和 Dex 方法
- 📱 **Android 客户端**：配套的 Xposed 模块客户端

### 通用特性

- 🎨 **现代化 UI**：基于 Vue 3 的精美界面
- 🌓 **深色模式**：支持亮色/暗色主题切换
- 📱 **响应式设计**：完美适配各种屏幕尺寸

## 🏗️ 技术栈

### 前端

- Vue 3 (Composition API)
- TypeScript
- Vue Router
- Axios
- Vite
- Monaco Editor（代码编辑器）
- 本地存储（LocalStorage）

### 后端

- Spring Boot 3.5.6
- MyBatis
- MySQL 5.7+
- Spring Security
- Android SDK Build Tools（D8 编译器）
- Java Compiler API（javac）

### Android 客户端

- Xposed Framework
- Gradle Build System
- Android SDK

## 📦 项目结构

```
verfiy/
├── verfiy_client/                  # 前端项目（Web 管理平台）
│   ├── src/
│   │   ├── views/                  # 页面组件
│   │   │   ├── JavaEditorPage.vue  # Java 代码编辑器页面
│   │   │   ├── HookEditPage.vue    # Hook 配置编辑页面
│   │   │   ├── HookManagementPage.vue  # Hook 管理页面
│   │   │   ├── CardsPage.vue       # 卡密管理页面
│   │   │   ├── AppsPage.vue        # 应用管理页面
│   │   │   └── ...                 # 其他页面
│   │   ├── components/             # UI 组件
│   │   │   └── ui/                 # 基础 UI 组件
│   │   ├── router/                 # 路由配置
│   │   └── utils/                  # 工具函数
│   └── package.json
│
├── verfiy_server/                  # 后端项目（API 服务）
│   ├── src/main/
│   │   ├── java/com/xy/verfiy/
│   │   │   ├── controller/         # 控制器
│   │   │   ├── service/            # 业务逻辑
│   │   │   │   └── DexCompileService.java  # Dex 编译服务
│   │   │   ├── mapper/             # MyBatis 映射器
│   │   │   ├── domain/             # 实体类
│   │   │   ├── dto/                # 数据传输对象
│   │   │   └── util/               # 工具类
│   │   └── resources/
│   │       ├── application.properties  # 应用配置
│   │       └── mappers/            # MyBatis XML
│   └── pom.xml
│
│
├── jar_lib/                        # Java 依赖库（用于编译）
│   ├── android.jar                 # Android SDK (26MB)
│   ├── XposedBridgeApi-82.jar      # Xposed API (28KB)
│   ├── fastjson-1.2.76.jar         # JSON 处理库 (648KB)
│   ├── commons-io-2.6.jar          # IO 工具库 (260KB)
│   └── bcprov-jdk16-1.46.jar       # 加密库 (2.0MB)
│
├── database-schema.sql             # 数据库结构脚本
├── database-migration-*.sql        # 数据库迁移脚本
├── deploy.sh                       # 自动化部署脚本
├── build.sh                        # 构建脚本
└── README.md                       # 项目说明文档
```

## 🚀 快速开始

### 本地开发

#### 前端开发

```bash
cd verfiy_client
npm install
npm run dev
```

访问：http://localhost:5173

#### 后端开发

```bash
cd verfiy_server

# 配置数据库
# 修改 src/main/resources/application.properties 中的数据库连接信息

# 启动服务
mvn spring-boot:run
```

访问：http://localhost:8084/verfiy

> **注意**：开发环境使用 HTTP，生产环境由 Nginx 统一处理 HTTPS

### 部署到生产环境

快速部署命令：

```bash
# 1. 服务器环境设置（首次部署）
bash server-setup.sh

# 2. 一键部署
./deploy.sh
```

### 编译环境配置（Hook 功能必需）

如需使用 Xposed Hook 在线编译功能，需要配置 Android SDK：

```bash
# 1. 安装 Android SDK
# 参考：https://developer.android.com/studio

# 2. 配置环境变量（在 application.properties 中）
dex.compile.android-home=/path/to/android-sdk
dex.compile.build-tools-version=30.0.3
dex.compile.jar-lib-path=/path/to/verfiy/jar_lib

# 3. 确保 jar_lib 目录包含必要的依赖
# - android.jar
# - XposedBridgeApi-82.jar
# - fastjson-1.2.76.jar
# - commons-io-2.6.jar
# - bcprov-jdk16-1.46.jar
```

## 📝 配置说明

### 环境变量

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=verfiy
DB_USERNAME=verfiy_user
DB_PASSWORD=your_password

# 应用配置
SERVER_PORT=8084
CONTEXT_PATH=/verfiy

# Dex 编译配置（可选）
ANDROID_HOME=/path/to/android-sdk
BUILD_TOOLS_VERSION=30.0.3
JAR_LIB_PATH=/path/to/verfiy/jar_lib
```

### 应用配置文件（application.properties）

```properties
# 数据库连接
spring.datasource.url=jdbc:mysql://localhost:3306/verfiy?useSSL=false&serverTimezone=UTC
spring.datasource.username=verfiy_user
spring.datasource.password=your_password

# Dex 编译配置
dex.compile.android-home=${ANDROID_HOME:/usr/local/android-sdk}
dex.compile.build-tools-version=${BUILD_TOOLS_VERSION:30.0.3}
dex.compile.jar-lib-path=${JAR_LIB_PATH:/opt/verfiy/jar_lib}
dex.compile.workspace-path=${WORKSPACE_PATH:/tmp/dex-compile}
dex.compile.max-compile-size=10485760

# 编译配额（每日限制）
dex.compile.daily-quota=10
```

### 重要提示

⚠️ **安全注意事项**：

- 首次登录后请立即修改默认密码
- 生产环境请使用强密码
- 建议启用 HTTPS
- 定期备份数据库

## 🔐 默认账户

- **用户名**: admin
- **密码**: admin123

⚠️ **请在首次登录后立即修改！**

## 📚 API 文档

系统提供以下主要 API：

### 卡密验证 API

#### 1. 卡密验证接口

```
POST /verfiy/api/redeem
```

#### 2. 通知接口

```
GET /verfiy/api/notice
```

### Hook 管理 API

#### 1. 创建/更新 Hook

```
POST /verfiy/hook/save
```

#### 2. 获取 Hook 列表

```
GET /verfiy/hook/list
```

#### 3. 删除 Hook

```
DELETE /verfiy/hook/delete/{id}
```

### Dex 编译 API

#### 1. 提交编译任务

```
POST /verfiy/dex/compile
Content-Type: application/json

{
  "hookId": 123,
  "files": {
    "com/example/MyHook.java": "package com.example; ..."
  }
}
```

#### 2. 查询编译状态

```
GET /verfiy/dex/status/{taskId}
```

#### 3. 下载编译结果

```
GET /verfiy/dex/download/{taskId}
```

#### 4. 查询编译配额

```
GET /verfiy/dex/quota
```

详细 API 文档和调用示例可在管理后台的"API 文档"页面查看。

## 🛠️ 维护命令

```bash
# 查看服务状态
systemctl status verfiy

# 重启服务
systemctl restart verfiy

# 查看日志
tail -f /opt/verfiy/logs/verfiy.log

# 备份数据库
/opt/verfiy/backup-db.sh

# 清理编译缓存
rm -rf /tmp/dex-compile/*
```

## 📖 Hook 功能使用指南

### 1. 创建 Hook 项目

1. 登录管理后台
2. 进入"Hook 管理"页面
3. 点击"新建 Hook"
4. 填写 Hook 基本信息（名称、描述等）


### 2. 配置 Hook 信息

1. 在"Hook 配置"页面填写Hook配置
2. 填入对应类和方法名及其参数以及需要修改的返回值
3. 保存配置

### 3. 使用 Dex
1. 上传自定义Dex，填写入口类名和方法名

### 4. 激活hook
1. 使用lsposed框架或其他免root框架激活
2. 如果使用lsposed框架并且启用dex需要关闭Xposed API调用保护

### 可用依赖库

编辑器已内置以下依赖，可直接使用：

- **Android SDK**：所有 Android 系统 API
- **Xposed API**：XposedHelpers、XC_MethodHook 等
- **Fastjson**：JSON 处理（com.alibaba.fastjson.*）
- **Commons IO**：文件和 IO 操作（org.apache.commons.io.*）
- **BouncyCastle**：加密库（org.bouncycastle.*）

**注意**：这些依赖仅用于编译检查，不会打包到 Dex 中。如需使用第三方库，需要确保目标应用或 Xposed 模块中已包含该库。

## ❓ 常见问题

### Q: 编译失败怎么办？

A: 检查以下几点：
- 代码语法是否正确
- 是否使用了不支持的依赖
- Dex 入口方法是否为无参数静态方法
- 查看编译日志获取详细错误信息


### Q: 如何使用第三方库？

A: 当前系统只支持内置的几个常用库。如需使用其他库，可以：
1. 将 HookHelper 工具类复制到你的项目中
2. 在本地项目中添加需要的依赖
3. 编译成 Dex 后使用


## 📊 功能对比表

| 功能模块 | 说明 | 状态 |
|---------|------|------|
| 🔐 卡密验证 | 支持多应用、多时效的卡密管理 | ✅ 完成 |
| 🛡️ 加密传输 | RC4/AES 加密保障数据安全 | ✅ 完成 |
| 📝 Hook 编辑器 | 在线 Java 代码编辑，支持语法高亮、代码补全 | ✅ 完成 |
| 🔨 Dex 编译 | 云端编译 Java 到 Dex | ✅ 完成 |
| 🛠️ 内置工具类 | HookHelper 简化 Hook 开发 | ✅ 完成 |
| 📚 依赖管理 | 支持常用 Java 库 | ✅ 完成 |
| 📱 Android 客户端 | Xposed 模块客户端 | ✅ 完成 |
| 📊 使用统计 | 卡密使用日志、编译配额管理 | ✅ 完成 |
| 🎨 主题切换 | 亮色/暗色模式 | ✅ 完成 |

## 🔒 安全性说明

### 代码安全

- ✅ 所有代码在服务器端编译，不会泄露到客户端
- ✅ 编译产物（Dex）经过优化，排除了内部工具类
- ✅ 支持用户隔离，每个用户只能访问自己的 Hook 项目

### 编译安全

- ✅ 编译配额限制，防止滥用
- ✅ 文件大小限制，防止恶意上传
- ✅ 临时文件自动清理
- ✅ 沙箱环境编译（建议配置）

### 传输安全

- ✅ 支持 HTTPS
- ✅ JWT Token 认证
- ✅ 密码加密存储

## 🎯 路线图

- [ ] 支持更多 Java 依赖库
- [ ] 支持 Kotlin 代码编辑和编译
- [ ] 支持代码模板市场
- [ ] 支持团队协作功能
- [ ] 支持 Hook 代码版本控制
- [ ] 支持在线调试功能

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 如何贡献

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 📄 许可证

MIT License

## 👤 作者

**ktbtw**


**⭐ 如果这个项目对你有帮助，请给一个星标！**


**最后更新**: 2025-11-03 | **版本**: 2.0.0 | **作者**: ktbtw
