# 数学作业系统（Vue 3 + Spring Boot）

基于 Vue 3、Spring Boot 3 与 MySQL 的数学作业管理系统，含题库、作业、学习目标与班级管理。

## 📋 项目结构

```
.
├── backend/                      # Spring Boot后端应用
│   ├── pom.xml                  # Maven配置
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── Application.java           # 启动类（唯一 Spring Boot 入口）
│       │   │   ├── config/                    # WebMvc 等配置
│       │   │   ├── controller/                # User / ClassGroup / FileUpload
│       │   │   ├── service/                   # 业务逻辑
│       │   │   ├── repository/                # 数据访问
│       │   │   ├── entity/                    # User / ClassGroup
│       │   │   ├── dto/                       # 数据传输对象
│       │   │   ├── goal/                      # 学习目标模块
│       │   │   └── homework/                  # 作业与题库模块
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
│           ├── Home.vue                     # 首页
│           ├── UserList.vue                 # 用户管理
│           ├── ClassGroupList.vue           # 班级管理
│           ├── GoalView.vue                 # 学习目标
│           ├── GoalTemplateLibraryView.vue  # 目标模板库
│           ├── GoalStudentProgressView.vue  # 学生目标进度
│           ├── UnitTestView.vue             # 单元测试
│           └── homework/                    # 作业与题库页面
├── docker-compose.yml           # Docker容器编排
├── .gitignore                   # Git忽略配置
└── README.md                    # 项目说明
```

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17或更高版本
- **Node.js**: 18或更高版本
- **Maven**: 3.6或更高版本
- **MySQL**: 8.0（必需，项目不支持其他数据库）
- **Docker & Docker Compose**: 用于快速启动数据库（可选）

### 安装步骤

#### 1. 启动数据库

**方式A: 使用Docker Compose（推荐）**

```bash
docker-compose up -d mysql
```

容器名为 `aiproject-mysql`。注意 `docker-compose.yml` 建的库是 `aiproject`，
而应用默认连接 `mathedu` —— 需要手动建库，或用 `DB_HOST` 环境变量指向 `aiproject`。

**方式B: 手动安装**

- 创建数据库 `mathedu`（或通过环境变量指向已有库）

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

数据源支持环境变量三层回退，本地不设任何变量时使用最内层默认值：

```properties
spring.datasource.url=${DB_HOST:${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/mathedu?...}}
spring.datasource.username=${DB_USER:${SPRING_DATASOURCE_USERNAME:root}}
spring.datasource.password=${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:root}}
```

优先级：`DB_HOST` / `DB_USER` / `DB_PASSWORD` > `SPRING_DATASOURCE_*` > 内置默认值。
部署到 Render 等平台时通过 `SPRING_DATASOURCE_*` 注入，无需改动配置文件。

> **注意**：本项目仅支持 MySQL。配置里使用了 `MySQL8Dialect` 与 `com.mysql.cj.jdbc.Driver`，
> 并无 PostgreSQL 配置可供切换。`docker-compose.yml` 里的 PostgreSQL 服务未被后端使用。

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

### 班级管理API

基础路径 `/api/class-groups`，除标准增删改查外还包括：

| 方法 | 端点 | 说明 |
|------|------|------|
| PUT | `/api/class-groups/{id}/teacher` | 变更班主任 |
| GET | `/api/class-groups/{id}/students` | 班级学生列表 |
| POST | `/api/class-groups/{id}/students` | 添加学生 |
| DELETE | `/api/class-groups/{id}/students/{studentId}` | 移除学生 |
| GET | `/api/class-groups/by-teacher/{teacherId}` | 按教师查询 |
| GET | `/api/class-groups/by-student/{studentId}` | 按学生查询 |

### 其他

- `/api/upload` —— 文件上传（上传目录 `./uploads`，单文件上限 10MB）
- 作业、题库与学习目标模块的接口分别位于 `homework/`、`goal/` 包中，此处未逐一列出

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

后端配置了 JPA 自动建表（`spring.jpa.hibernate.ddl-auto=update`），修改实体类后会自动更新表结构。

> **⚠️ 仓库根目录的 `schema.sql` 不可作为结构依据。**
> 它只有 17 张表，而实际运行库有 26 张。差的 9 张是 Hibernate 按实体自动创建的，
> 从未回写到 `schema.sql`。需要准确结构时，用 `tools/sync-questions/2a-export-schema.sh`
> 从实际库导出，不要依赖 `schema.sql`。

这正是 `ddl-auto=update` 的代价：结构的真实来源是实体类与运行库，而非任何 SQL 文件。

### 题库同步到线上

`tools/sync-questions/` 是一套把本地题库同步到线上 TiDB 的编号脚本（按 0→6 顺序执行），
采用「暂存表 + ID 重映射」而非直接 `mysqldump` 导入 —— 因为子表外键指向的是本地 ID，
线上重建的 `users` 表中 `teacher01` 未必拿到相同 ID，直接导入会导致 `created_by` 错位。

使用要点：

- 线上凭据通过 `ONLINE_HOST` / `ONLINE_USER` / `ONLINE_PASS` 环境变量传入，切勿硬编码
- `4-load-online.sh` 默认为演练模式，跑完自动 `ROLLBACK`；确认无误后再用 `MODE=commit` 写入
- TiDB 端口为 `4000`（非 3306）且强制 TLS；不支持 MySQL 8.0 专有的 `utf8mb4_0900_ai_ci`
  排序规则，导出脚本会自动替换为 `utf8mb4_general_ci`

详见 [tools/sync-questions/README.md](tools/sync-questions/README.md)。

> **⚠️ 认证目前为明文口令比对。** `AuthService.login()` 使用
> `Objects.equals(明文, user.getPassword())` 直接比对，注入的 `passwordEncoder` 从未被调用，
> 数据库中存储的也是明文。因此向 `users` 表写入 bcrypt 哈希反而会导致登录失败 ——
> 种子数据、数据同步、测试夹具中的口令都必须写明文。这是已知的安全债，上线前必须修复。

## 📦 构建和部署

### 构建后端

```bash
cd backend
mvn clean package
# 生成文件: target/springboot-app-1.0.0.jar
```

**关于插件版本固定**：`pom.xml` 中的 `spring-boot-maven-plugin`（3.2.0）与
`maven-compiler-plugin`（3.11.0）均通过 properties 显式声明版本。未固定版本的插件会让 Maven
在每次导入时联网查询 Central 的 `maven-metadata.xml` 以解析「最新版」，既拖慢 IDE 的项目导入
（Java 语言服务器配置项目时尤其明显），也使构建结果不可复现。升级插件时改 properties 即可。

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

### Q: 可以使用PostgreSQL替代MySQL吗？
A: 目前不能。配置中固定使用 `MySQL8Dialect` 与 MySQL 驱动，实体与 SQL 也按 MySQL 编写。
`docker-compose.yml` 里保留的 PostgreSQL 服务位于 `postgres` profile 之下，默认不启动，
后端也从未连接它。如需支持需另行改造。

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License

---

**最后更新**: 2026年3月10日

如有问题，请提交Issue或联系开发者。
