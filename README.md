# Vue 3 + Spring Boot 全栈项目

这是一个现代化的全栈Web应用示例项目，展示了如何使用Vue 3、Spring Boot 3和关系型数据库构建完整的应用。

## 📋 项目结构

```
.
├── backend/                      # Spring Boot后端应用
│   ├── pom.xml                  # Maven配置
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── Application.java           # 启动类
│       │   │   ├── controller/
│       │   │   │   └── UserController.java    # REST API控制器
│       │   │   ├── service/
│       │   │   │   └── UserService.java       # 业务逻辑
│       │   │   ├── repository/
│       │   │   │   └── UserRepository.java    # 数据访问
│       │   │   ├── entity/
│       │   │   │   └── User.java              # 实体类
│       │   │   └── dto/
│       │   │       └── UserDTO.java           # 数据传输对象
│       │   └── resources/
│       │       └── application.properties     # 配置文件
│       └── test/                              # 测试文件
├── frontend/                     # Vue 3前端应用
│   ├── package.json             # Node依赖
│   ├── vite.config.js           # Vite配置
│   ├── index.html               # HTML入口
│   └── src/
│       ├── main.js              # 应用入口
│       ├── App.vue              # 根组件
│       ├── router/
│       │   └── index.js         # 路由配置
│       └── views/
│           ├── Home.vue         # 首页
│           └── UserList.vue     # 用户管理页面
├── docker-compose.yml           # Docker容器编排
├── .gitignore                   # Git忽略配置
└── README.md                    # 项目说明
```

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17或更高版本
- **Node.js**: 18或更高版本
- **Maven**: 3.6或更高版本
- **MySQL**: 8.0或 **PostgreSQL**: 15（可选）
- **Docker & Docker Compose**: 用于快速启动数据库（可选）

### 安装步骤

#### 1. 启动数据库

**方式A: 使用Docker Compose（推荐）**

```bash
# 启动MySQL
docker-compose up -d mysql

# 或启动PostgreSQL
docker-compose --profile postgres up -d postgres
```

**方式B: 手动安装**

- MySQL: 创建数据库 `aiproject`
- PostgreSQL: 创建数据库 `aiproject`

#### 2. 启动后端应用

```bash
cd backend

# 编译和打包
mvn clean package

# 运行应用（Maven）
mvn spring-boot:run

# 或直接运行JAR文件
java -jar target/springboot-app-1.0.0.jar

# 后端将在 http://localhost:8080 启动
```

检查后端是否正常：
```bash
curl http://localhost:8080/api/users/health
```

#### 3. 启动前端应用

```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 前端将在 http://localhost:5173 启动
```

在浏览器中打开 `http://localhost:5173` 即可访问应用。

## 🔧 配置文件

### 后端配置 (`backend/src/main/resources/application.properties`)

**MySQL配置**（默认）：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/aiproject?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
```

**PostgreSQL配置**（取消注释即可使用）：
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/aiproject
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 前端API配置 (`frontend/vite.config.js`)

Vite已配置代理，将 `/api` 请求转发到后端：
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

## 📚 API 端点

### 用户管理API

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 获取指定用户 |
| POST | `/api/users` | 创建新用户 |
| PUT | `/api/users/{id}` | 更新用户信息 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/users/health` | 服务健康检查 |

#### 示例请求

**创建用户**：
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "123456"
  }'
```

**获取所有用户**：
```bash
curl http://localhost:8080/api/users
```

## 🛠️ 开发指南

### 添加新的API端点

1. **创建实体类** (`backend/src/main/java/com/example/entity/`)
2. **创建Repository** (`backend/src/main/java/com/example/repository/`)
3. **创建Service** (`backend/src/main/java/com/example/service/`)
4. **创建Controller** (`backend/src/main/java/com/example/controller/`)

### 添加新的前端页面

1. **创建Vue组件** (`frontend/src/views/`)
2. **在路由中添加** (`frontend/src/router/index.js`)
3. **在导航中添加链接** (`frontend/src/App.vue`)

### 数据库迁移

后端配置了JPA自动建表功能（`spring.jpa.hibernate.ddl-auto=update`），修改实体类后会自动更新数据库表结构。

## 📦 构建和部署

### 构建后端

```bash
cd backend
mvn clean package
# 生成文件: target/springboot-app-1.0.0.jar
```

### 构建前端

```bash
cd frontend
npm run build
# 生成文件: dist/
```

### 部署到云服务

参考部署指南（下方提供）。

## ☁️ 云部署指南

### Railway 部署（推荐）

1. **连接GitHub仓库** 到 Railway
2. **设置环境变量**：
   - `SPRING_DATASOURCE_URL`: 数据库URL
   - `SPRING_DATASOURCE_USERNAME`: 数据库用户名
   - `SPRING_DATASOURCE_PASSWORD`: 数据库密码

3. **后端部署**：
   - 选择 Maven 构建工具
   - 输入启动命令：`java -jar target/springboot-app-1.0.0.jar`

4. **前端部署**：
   - 使用 Vercel 或 Netlify
   - 连接GitHub仓库到Vercel
   - 构建命令：`npm run build`
   - 输出目录：`dist`

### Azure 部署

参考 [Azure App Service 部署指南](https://docs.microsoft.com/zh-cn/azure/app-service/)

### Heroku 替代方案

使用 Railway、Render 或 Fly.io（Heroku已停止免费服务）

## 🧪 测试

### 后端测试

```bash
cd backend
mvn test
```

### 前端测试

```bash
cd frontend
npm test
```

## 📝 常见问题

### Q: 如何修改数据库？
A: 修改 `backend/application.properties` 中的数据库配置，同时调整Hibernate方言配置。

### Q: 前端无法连接后端？
A: 
1. 检查后端是否在 `localhost:8080` 运行
2. 检查前端是否在 `localhost:5173` 运行
3. 检查CORS配置是否正确

### Q: 如何使用PostgreSQL替代MySQL？
A: 
1. 在 `application.properties` 中取消PostgreSQL配置的注释
2. 注释MySQL配置
3. 重启后端应用

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

---

**最后更新**: 2026年3月10日

如有问题，请提交Issue或联系开发者。
