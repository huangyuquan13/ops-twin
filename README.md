# 智维方舟 (Ops-Twin)

> 面向数据中心运维的 3D 数字孪生智能运维平台

[![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen?logo=vue.js)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue?logo=typescript)](https://www.typescriptlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Three.js](https://img.shields.io/badge/Three.js-r150-black?logo=three.js)](https://threejs.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://www.docker.com/)

---

## 📋 项目简介

**智维方舟** 是一个面向数据中心运维场景的 **AIOps 3D 可视化原型系统**，以空间级数字孪生大屏替代传统 2D 数据看板，内嵌容灾演练流程引擎，支持拖拽式编排与 WebSocket 实时执行反馈。

**核心亮点：**
- 🎨 **3D 数字孪生**: Three.js 构建机房 3D 可视化场景
- 📊 **实时监控大屏**: ECharts 驱动的多维度数据分析
- 🔄 **容灾演练引擎**: 可视化工作流编排 + WebSocket 实时反馈
- 🛡️ **RBAC 权限体系**: 用户/角色/权限三级管理 + 按钮级控制
- 🐳 **Docker 一键部署**: MySQL + Spring Boot + Nginx 容器化

---

## 🛠️ 技术栈

### 前端

| 类别 | 技术 |
|------|------|
| **框架** | Vue 3 (Composition API) + TypeScript |
| **UI 组件** | Element Plus |
| **3D 渲染** | Three.js (InstancedMesh 高性能渲染) |
| **数据可视化** | ECharts |
| **流程编排** | Vue Flow |
| **终端模拟** | Xterm.js |
| **状态管理** | Pinia |
| **路由** | Vue Router |

### 后端

| 类别 | 技术 |
|------|------|
| **框架** | Spring Boot 3.2 |
| **ORM** | MyBatis Plus 3.5 |
| **鉴权** | JWT + Spring Security |
| **AOP** | Spring AOP（操作审计日志） |
| **实时通信** | WebSocket |

### 数据库与部署

| 类别 | 技术 |
|------|------|
| **数据库** | MySQL 8.0 |
| **容器化** | Docker + Docker Compose |
| **反向代理** | Nginx |

---

## ✨ 核心功能

### 1️⃣ 3D 孪生大屏

- **高性能渲染**: 基于 Three.js 的 InstancedMesh，支持 60+ 主机同时渲染
- **三级下钻**: 全景 → 单柜聚焦 → 刀片详情
- **实时状态同步**: WebSocket 推送设备状态，模型颜色实时更新
- **物理绑定**: 真实物理机柜与插槽绑定，空间坐标冲突检测

### 2️⃣ 效能大盘

- **多维度分析**: 总算力、总内存、告警占比实时统计
- **ECharts 可视化**: 饼图、柱状图、折线图多种图表
- **响应式布局**: 适配不同屏幕尺寸

### 3️⃣ 资产中心

- **台账管理**: 物理机柜与主机 CRUD
- **拓扑编排**: Vue Flow 实现逻辑服务拓扑图
- **冲突检测**: 空间坐标自动校验，避免机柜重叠

### 4️⃣ 任务中心

- **预案方案库**: 26 个预置演练方案，覆盖 11 种逻辑服务
- **可视化工作流**: 拖拽式编排，支持条件分支、循环
- **异步执行引擎**: 后台执行任务，不阻塞前端
- **实时终端**: WebSocket 推送执行日志，Xterm.js 模拟终端

### 5️⃣ 系统管理

- **RBAC 权限**: 用户/角色/权限三级管理
- **按钮级控制**: 细粒度权限控制到按钮
- **操作审计**: Spring AOP 记录所有操作日志
- **动态侧边栏**: 根据权限动态生成菜单

---

## 🚀 快速启动（Docker 一键部署）

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+

### 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/huangyuquan13/ops-twin.git
cd ops-twin

# 2. 构建后端 JAR
cd ops-twin-server
mvn clean package -DskipTests

# 3. 启动所有服务（MySQL + Server + Nginx）
cd ..
docker compose up -d
```

### 访问系统

访问 http://localhost 即可进入系统。

**默认管理员账号：** `admin` / `123456`

---

## 💻 本地开发

### 后端

```bash
cd ops-twin-server
mvn spring-boot:run
```

后端运行在 http://localhost:8080

### 前端

```bash
cd ops-twin-web
npm install
npm run dev
```

前端运行在 http://localhost:5173

### 数据库

MySQL 8.0，初始化脚本见 `ops-twin-server/ops_twin_db2.sql`

```properties
url: jdbc:mysql://localhost:3306/ops_twin_db
username: root
password: 123456
```

---

## 📁 项目结构

```
vue3_424/
├── ops-twin-web/              # Vue 3 前端
│   ├── src/
│   │   ├── views/             # 页面
│   │   │   ├── Dashboard/     # 3D 孪生大屏
│   │   │   ├── Asset/         # 资产中心
│   │   │   ├── Task/          # 任务中心
│   │   │   └── System/        # 系统管理
│   │   ├── components/        # 组件
│   │   │   ├── ThreeScene/    # Three.js 场景组件
│   │   │   ├── FlowEditor/    # Vue Flow 编辑器
│   │   │   └── Terminal/      # Xterm.js 终端
│   │   ├── store/             # Pinia 状态管理
│   │   ├── router/            # Vue Router
│   │   └── api/               # API 封装
│   └── package.json
│
├── ops-twin-server/           # Spring Boot 后端
│   ├── src/main/java/
│   │   ├── controller/        # 控制器
│   │   ├── service/           # 业务逻辑
│   │   ├── mapper/            # MyBatis Mapper
│   │   ├── entity/            # 实体类
│   │   ├── config/            # 配置类
│   │   └── aspect/            # AOP 切面
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis XML
│   │   └── application.yml    # 配置文件
│   └── pom.xml
│
├── docs/                      # 项目文档
│   ├── 01_PRD_项目需求文档.md
│   ├── 02_DB_数据库图表设计.md
│   ├── 03_API_后端接口文档.md
│   ├── 04_DEPLOY_部署文档.md
│   └── implementation_plan.md
│
└── docker-compose.yml         # Docker Compose 配置
```

---

## 🧪 测试

### 后端测试

```bash
cd ops-twin-server
mvn test
```

**测试覆盖：** 15 个 JUnit 测试（JUnit 5 + MockMvc + H2 内存数据库）

### 前端测试

```bash
cd ops-twin-web
npm run test
```

**测试覆盖：** 4 个 Vitest 测试

---

## 🎯 技术难点与解决方案

### 1️⃣ 3D 场景性能优化

**问题：** 60+ 主机同时渲染，帧率下降严重。

**解决方案：**
- 使用 `InstancedMesh` 替代多个 `Mesh`，减少 Draw Call
- 实现 LOD（Level of Detail），远距离使用低模
- 使用 `requestAnimationFrame` 优化动画循环

### 2️⃣ WebSocket 实时同步

**问题：** 多个客户端同时在线，需要实时同步设备状态。

**解决方案：**
- 双 WebSocket 通道：`/ws/task/log/{id}`（终端日志）+ `/ws/dashboard/events`（3D 事件）
- 使用 Redis Pub/Sub 实现消息广播（可选）
- 前端使用 Pinia 管理状态，自动更新 UI

### 3️⃣ 流程引擎设计

**问题：** 容灾演练流程复杂，需要支持条件分支、循环、并行。

**解决方案：**
- 使用 Vue Flow 实现可视化编排
- 后端使用状态机模式，支持多种节点类型
- 异步执行引擎，不阻塞前端

---

## 📊 项目数据

- **8 个物理机柜**，60 台主机，6 种类型（WEB/APP/DB/CACHE/LB/MQ）
- **26 个演练方案**，覆盖 11 种逻辑服务
- **15 个后端测试**（JUnit + MockMvc + H2）
- **4 个前端测试**（Vitest）
- **双 WebSocket 通道**，实时推送执行日志和 3D 事件

---

## 📚 文档

- [项目需求文档 (PRD)](docs/01_PRD_项目需求文档.md)
- [数据库设计文档](docs/02_DB_数据库图表设计.md)
- [后端接口文档 (API)](docs/03_API_后端接口文档.md)
- [部署文档](docs/04_DEPLOY_部署文档.md)
- [实施计划](docs/implementation_plan.md)
- [待完善事项 (Backlog)](docs/backlog_待完善事项.md)

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 开源协议

[MIT](LICENSE)

---

**如果这个项目对你有帮助，欢迎 ⭐ Star！**
