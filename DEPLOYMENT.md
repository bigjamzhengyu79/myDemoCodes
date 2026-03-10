# 云部署指南

本文档提供了如何将你的全栈应用部署到各个主流云平台的详细步骤。

## 目录

- [Railway 部署](#railway-部署)
- [Render 部署](#render-部署)
- [Vercel + Railway 组合](#vercel--railway-组合)
- [Azure 部署](#azure-部署)
- [环境变量配置](#环境变量配置)

---

## Railway 部署

Railway是最简单的选择，一个平台可以部署前后端和数据库。

### 步骤1：创建Railway项目

1. 访问 [railway.app](https://railway.app)
2. 使用GitHub账号登录
3. 点击 "Create New Project"

### 步骤2：部署数据库

1. 选择 "Provision New" → "MySQL"
2. 等待数据库初始化完成
3. 记下数据库连接信息

### 步骤3：部署后端

1. 创建新的服务 "Provision New" → "GitHub Repo"
2. 选择你的项目仓库
3. 配置启动命令：
   ```
   cd backend && mvn clean package -DskipTests && java -jar target/springboot-app-1.0.0.jar
   ```
4. 设置环境变量（参考下文）
5. 点击 "Deploy"

### 步骤4：部署前端

1. 创建新的服务 "Provision New" → "GitHub Repo"
2. 选择你的项目仓库
3. 配置构建命令：
   ```
   cd frontend && npm install && npm run build
   ```
4. 配置启动命令：
   ```
   npm run preview
   ```
5. 点击 "Deploy"

### 步骤5：配置API端点

在前端环境变量中设置后端URL：
```
VITE_API_URL=https://your-backend-url.railway.app/api
```

---

## Render 部署

Render提供将应用部署分离的选项。

### 部署后端

1. 访问 [render.com](https://render.com)
2. 账号注册/登录
3. 创建 "New Web Service"
4. 连接GitHub仓库
5. 配置：
   - **Build Command**: `cd backend && mvn clean package -DskipTests`
   - **Start Command**: `java -jar backend/target/springboot-app-1.0.0.jar`
   - **Root Directory**: `/`

6. 添加环境变量（参考下文）
7. 创建PostgreSQL数据库服务

### 部署前端

1. 创建 "New Static Site"
2. 连接GitHub仓库
3. 配置：
   - **Build Command**: `cd frontend && npm install && npm run build`
   - **Publish Directory**: `frontend/dist`

---

## Vercel + Railway 组合

最佳实践：前端用Vercel，后端+数据库用Railway。

### 部署前端到Vercel

1. 访问 [vercel.com](https://vercel.com)
2. 连接GitHub账号
3. 导入项目仓库
4. 配置：
   - **Framework**: Vue.js
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

5. 添加环境变量：
   ```
   VITE_API_URL=https://your-backend.railway.app/api
   ```
6. 点击 "Deploy"

### 部署后端到Railway

参考上文 Railway 部署步骤。

---

## Azure 部署

### 使用Azure App Service

1. 访问 [portal.azure.com](https://portal.azure.com)
2. 创建 "App Service"
3. 配置运行时栈为 "Java 17"
4. 通过 "Deployment Center" 连接GitHub
5. 配置 GitHub Actions 工作流

### 部署数据库

1. 创建 "Azure Database for MySQL"
2. 配置防火墙规则允许App Service访问

### 环境变量配置

在App Service的 "Configuration" 中添加：
```
SPRING_DATASOURCE_URL=jdbc:mysql://your-db-server.mysql.database.azure.com:3306/aiproject?useSSL=true
SPRING_DATASOURCE_USERNAME=admin@your-db-server
SPRING_DATASOURCE_PASSWORD=YourPassword
```

---

## 环境变量配置

### 后端环境变量

```properties
# 数据库配置
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/aiproject
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver

# Hibernate配置
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQL8Dialect

# 日志
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_EXAMPLE=DEBUG

# 服务器
SERVER_PORT=8080
```

### 前端环境变量

```javascript
// .env.production
VITE_API_URL=https://your-backend-api-url.com
```

---

## 使用Docker部署

所有云平台都支持Docker部署。

### 构建和推送Docker镜像

```bash
# 构建后端镜像
docker build -f Dockerfile.backend -t your-registry/aiproject-backend:latest .

# 构建前端镜像
docker build -f Dockerfile.frontend -t your-registry/aiproject-frontend:latest .

# 推送到Docker Registry
docker push your-registry/aiproject-backend:latest
docker push your-registry/aiproject-frontend:latest
```

### 使用Docker Compose部署

```bash
# 在服务器上运行
docker-compose -f docker-compose.prod.yml up -d
```

---

## 故障排除

### 连接数据库失败

- 检查数据库URL和凭证
- 检查防火墙/安全组规则
- 确认数据库已启动

### 前端无法连接后端

- 检查CORS配置
- 验证API URL是否正确
- 检查后端是否返回CORS头

### 应用启动缓慢

- 增加内存配置
- 优化构建步骤
- 检查日志查看具体问题

---

## 监控和维护

### 查看日志

- **Railway**: 在部署面板查看实时日志
- **Render**: 在 "Logs" 标签页查看
- **Vercel**: 在 "Deployments" 中查看
- **Azure**: 在 "Log stream" 查看

### 设置告警

大多数平台都提供监控和告警功能，根据需要配置。

---

## 成本估计

| 服务 | 免费额度 | 收费 |
|------|---------|------|
| Railway | $5/月 | 超出后按量计费 |
| Render | 部分免费 | 付费计划从$7开始 |
| Vercel | 无限制 | 按使用量计费 |
| Azure | 12个月免费 | 按服务类型计费 |

---

**最后更新**: 2026年3月10日

如有疑问，请查看各平台官方文档。
