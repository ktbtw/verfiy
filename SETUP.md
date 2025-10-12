# 项目配置指南

本文档说明如何配置项目的敏感信息。

## 📋 配置清单

### 1. 后端数据库配置

复制示例配置文件：
```bash
cd verfiy_server/src/main/resources
cp application.properties.example application.properties
```

编辑 `application.properties`，修改以下配置：

```properties
# 数据库连接
spring.datasource.url=jdbc:mysql://your_host:3306/verfiy?...
spring.datasource.username=your_username
spring.datasource.password=your_password

# 如果需要 HTTPS，配置证书
server.ssl.key-store=classpath:jks/your-certificate.jks
server.ssl.key-store-password=your_jks_password
```

### 2. 部署脚本配置（可选）

如果需要使用部署脚本，复制示例文件：
```bash
cp deploy.sh.example deploy.sh
chmod +x deploy.sh
```

编辑 `deploy.sh`，修改以下配置：

```bash
# 服务器配置
SERVER_HOST="your_server_ip"
SSH_KEY="/path/to/your/ssh_key.pem"

# 前端配置
FRONTEND_DIR="/path/to/verfiy_client"
FRONTEND_TARGET_DIR="/www/wwwroot/your_domain"

# 后端配置
BACKEND_DIR="/path/to/verfiy_server"
```

### 3. SSH 密钥配置

将你的 SSH 私钥文件放置在项目根目录（或其他安全位置），并在 `deploy.sh` 中配置路径。

**注意**：确保私钥文件权限正确：
```bash
chmod 600 your_key.pem
```

### 4. 数据库初始化

使用 `database-schema.sql` 初始化数据库结构。

默认管理员账号需要在数据库初始化时创建。

## 🔒 安全提示

1. **不要提交敏感文件**：
   - `application.properties` - 包含数据库密码
   - `deploy.sh` - 包含服务器 IP 和 SSH 密钥路径
   - `*.pem, *.key` - SSH 密钥文件
   - `data/` 目录 - 数据库文件

2. **这些文件已被 .gitignore 忽略**：
   - 确保不要使用 `git add -f` 强制添加
   - 推送前使用 `git status` 检查

3. **环境变量**（推荐）：
   - 考虑使用环境变量替代硬编码配置
   - Spring Boot 支持 `SPRING_DATASOURCE_PASSWORD` 等环境变量

## 🚀 快速开始

1. 克隆项目后，复制所有 `.example` 文件并去掉 `.example` 后缀
2. 根据你的环境修改配置
3. 确保敏感文件不会被 Git 跟踪

```bash
# 检查是否有敏感文件被跟踪
git status

# 如果敏感文件已被跟踪，从 Git 中移除（但保留本地文件）
git rm --cached verfiy_server/src/main/resources/application.properties
git rm --cached deploy.sh
git rm --cached *.pem
```

## 📝 配置文件说明

| 文件 | 用途 | 是否提交 |
|------|------|----------|
| `application.properties.example` | 配置模板 | ✅ 提交 |
| `application.properties` | 实际配置（含密码） | ❌ 不提交 |
| `deploy.sh.example` | 部署脚本模板 | ✅ 提交 |
| `deploy.sh` | 实际部署脚本（含服务器信息） | ❌ 不提交 |
| `*.pem, *.key` | SSH 密钥 | ❌ 不提交 |
| `data/` | 数据库文件 | ❌ 不提交 |

## ❓ 常见问题

**Q: 如何与团队共享配置？**
A: 只共享 `.example` 文件，团队成员根据自己的环境创建实际配置。

**Q: 不小心提交了敏感信息怎么办？**
A: 需要从 Git 历史中完全删除，使用 `git filter-branch` 或 BFG Repo-Cleaner，并立即更改所有暴露的密码/密钥。

**Q: 如何在生产环境使用环境变量？**
A: 在服务器上设置环境变量，Spring Boot 会自动读取：
```bash
export SPRING_DATASOURCE_PASSWORD=your_password
java -jar verify.jar
```

