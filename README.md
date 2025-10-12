# xyz验证 - 卡密验证管理系统

**作者**: ktbtw
**版本**: 1.0.0
**最后更新**: 2025-10-11

## 📖 项目简介

xyz验证是一个功能完善的卡密验证管理系统，支持多应用管理、安全传输加密、卡密生命周期管理等功能。

## ✨ 主要特性

- 🔐 **多应用管理**：支持创建和管理多个应用
- 🛡️ **传输安全**：支持 RC4 和 AES 加密算法
- 🎫 **卡密管理**：支持批量生成、激活、禁用卡密
- ⏰ **灵活时效**：支持分/时/天/月/季/年/永久等多种时效单位
- 🖥️ **机器码绑定**：支持限制绑定机器数量
- 📊 **使用日志**：完整的卡密使用记录
- 🎨 **现代化 UI**：基于 Vue 3 的精美界面

## 🏗️ 技术栈

### 前端

- Vue 3 (Composition API)
- TypeScript
- Vue Router
- Axios
- Vite

### 后端

- Spring Boot 3.5.6
- MyBatis
- MySQL 5.4+
- Spring Security

## 📦 项目结构

```
verfiy/
├── verfiy_client/          # 前端项目
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── components/     # UI 组件
│   │   ├── router/         # 路由配置
│   │   └── utils/          # 工具函数
│   └── package.json
├── verfiy_server/          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/       # Java 源码
│   │   │   └── resources/  # 配置文件
│   └── pom.xml
├── database-schema.sql     # 数据库初始化脚本
├── deploy.sh              # 自动化部署脚本
├── server-setup.sh        # 服务器环境设置脚本
├── DEPLOYMENT.md          # 详细部署文档
└── README-DEPLOY.md       # 快速部署指南
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

快速部署命令：

```bash
# 1. 服务器环境设置（首次部署）
bash server-setup.sh

# 2. 一键部署
./deploy.sh
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

### 1. 卡密验证接口

```
POST /verfiy/api/redeem
```

### 2. 通知接口

```
GET /verfiy/api/notice
```

详细 API 文档和调用示例可在管理后台的"调用示例"页面查看。

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
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👤 作者

**ktbtw**

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者！

---

**⭐ 如果这个项目对你有帮助，请给一个星标！**
