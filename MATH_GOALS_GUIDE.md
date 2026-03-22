# 数学学习目标管理系统 - 快速开始指南

## 📋 项目概述

已为您的项目添加了完整的**数学学习目标管理系统**，包括后端API和前端UI。

## 🎯 新增功能

### 后端（Spring Boot）
- **Entity**: `MathGoal.java` - 学习目标实体
- **Repository**: `MathGoalRepository.java` - 数据库访问层
- **Service**: `MathGoalService.java` - 业务逻辑层
- **Controller**: `MathGoalController.java` - REST API端点

### 前端（Vue 3）
- **Component**: `MathGoals.vue` - 目标管理页面
- **Routes**: 已在路由配置中添加 `/math-goals` 路由
- **Navigation**: 已在导航菜单中添加链接

## 🚀 快速启动

### 1. 创建数据库表

连接到您的PostgreSQL数据库（或MySQL），执行以下SQL命令：

```sql
-- PostgreSQL版本
CREATE TABLE IF NOT EXISTS math_goals (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    target_date DATE,
    progress INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT '进行中',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_math_goals_status ON math_goals(status);
CREATE INDEX idx_math_goals_category ON math_goals(category);
CREATE INDEX idx_math_goals_created_at ON math_goals(created_at);
```

**MySQL版本**:
```sql
CREATE TABLE IF NOT EXISTS math_goals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    target_date DATE,
    progress INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT '进行中',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_math_goals_status ON math_goals(status);
CREATE INDEX idx_math_goals_category ON math_goals(category);
CREATE INDEX idx_math_goals_created_at ON math_goals(created_at);
```

### 2. 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端将运行在 `http://localhost:8080`

### 3. 启动前端应用

```bash
cd frontend
npm install
npm run dev
```

前端将运行在 `http://localhost:5173`

### 4. 访问应用

- 打开浏览器访问 `http://localhost:5173`
- 点击导航栏中的 **"📚 数学目标"** 按钮
- 开始管理您的学习目标！

## 📊 功能介绍

### 主要功能

1. **创建目标** - 添加新的学习目标
   - 目标名称、描述
   - 选择分类（代数、几何、微积分、概率、数论、其他）
   - 设置目标完成日期
   - 初始进度（0-100%）

2. **查看统计** - 显示概览
   - 进行中的目标数量
   - 已完成的目标数量
   - 已放弃的目标数量

3. **分类显示** - 按状态分类显示
   - 🎯 进行中 - 正在进行的目标
   - ✅ 已完成 - 完成的目标
   - ❌ 已放弃 - 放弃的目标

4. **编辑目标** - 更新现有目标信息
   - 修改标题、描述、分类、日期、进度、状态

5. **标记完成** - 快速将目标标记为已完成
   - 自动设置进度为100%

6. **删除目标** - 移除不需要的目标

7. **进度追踪** - 可视化进度条
   - 直观展示完成百分比

## 🎨 分类颜色编码

- 🔴 代数: 红色 (#FF6B6B)
- 🔵 几何: 青色 (#4ECDC4)
- 💙 微积分: 浅蓝色 (#45B7D1)
- 🟠 概率: 浅橙色 (#FFA07A)
- 🟢 数论: 浅绿色 (#98D8C8)
- ⚪ 其他: 灰色 (#A8A8A8)

## 📡 API端点

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/math-goals` | 获取所有目标 |
| GET | `/api/math-goals/{id}` | 获取单个目标 |
| POST | `/api/math-goals` | 创建新目标 |
| PUT | `/api/math-goals/{id}` | 更新目标 |
| DELETE | `/api/math-goals/{id}` | 删除目标 |
| GET | `/api/math-goals/status/{status}` | 按状态筛选 |
| GET | `/api/math-goals/category/{category}` | 按分类筛选 |

## 📝 数据模型

```
MathGoal {
  id: Long,                    // 目标ID
  title: String,              // 目标名称
  description: String,        // 详细描述
  category: String,           // 分类（代数/几何/微积分/概率/数论/其他）
  targetDate: LocalDate,      // 目标完成日期
  progress: Integer,          // 进度百分比 (0-100)
  status: String,             // 状态（进行中/已完成/已放弃）
  createdAt: LocalDateTime,   // 创建时间
  updatedAt: LocalDateTime    // 更新时间
}
```

## ⚙️ 配置说明

### 后端 CORS 配置

已在 `Application.java` 中配置CORS，允许来自以下地址的请求：
- `http://localhost:5173` (Vite开发服务器)
- `http://localhost:3000` (其他可能的前端端口)

### 前端 API 基础URL

在 `MathGoals.vue` 中配置：
```javascript
const apiBase = 'http://localhost:8080/api/math-goals'
```

如需修改，请更新所有的 `fetch()` 调用中的URL。

## 🔍 故障排查

### 问题1: 后端数据库连接错误
**解决方案**：检查 `application.properties` 中的数据库配置

### 问题2: 前端无法连接后端
**解决方案**：
- 确保后端运行在 `http://localhost:8080`
- 检查浏览器控制台的CORS错误
- 验证防火墙配置

### 问题3: 页面显示为空
**解决方案**：
- 打开浏览器开发者工具 (F12)
- 检查 Network 标签中是否有API请求
- 检查 Console 标签中是否有错误信息

## 📦 项目文件结构

```
backend/
├── src/main/java/com/example/
│   ├── entity/MathGoal.java
│   ├── repository/MathGoalRepository.java
│   ├── service/MathGoalService.java
│   └── controller/MathGoalController.java
└── src/main/resources/
    └── schema.sql

frontend/
├── src/
│   ├── views/MathGoals.vue
│   └── router/index.js (已更新)
└── src/App.vue (已更新)
```

## 🎓 下一步改进建议

1. **用户认证** - 添加用户登录，让每个用户有自己的目标
2. **数据导出** - 支持将目标导出为CSV或PDF
3. **提醒功能** - 添加截止日期提醒
4. **目标分享** - 允许用户分享他们的学习目标
5. **学习日志** - 记录每天的学习进度
6. **成就徽章** - 完成目标后获得徽章
7. **移动app** - 使用React Native或Flutter创建移动版本

## 🤝 需要帮助？

如有任何问题或需要进一步的功能改进，请随时告诉我！

---

**祝您学习愉快！** 📚✨
