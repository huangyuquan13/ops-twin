# 智维方舟 (Ops-Twin)

面向数据中心运维的 3D 数字孪生智能运维平台。

## 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3, TypeScript, Element Plus, Three.js, ECharts, Vue Flow, Xterm.js |
| **后端** | Spring Boot 3.2, MyBatis Plus 3.5, JWT, Spring Security, Spring AOP |
| **数据库** | MySQL 8.0 |
| **实时通信** | WebSocket |
| **容器化** | Docker + Docker Compose |

## 核心功能

- **3D 孪生大屏**: 基于 Three.js 的 InstancedMesh 高性能渲染，真实物理机柜与插槽绑定
- **效能大盘**: ECharts 驱动的总算力、总内存、告警占比实时分析
- **资产中心**: 物理机柜与主机台账 CRUD，空间坐标冲突检测，Vue Flow 逻辑服务拓扑编排
- **任务中心**: 预案方案库，可视化工作流编辑器，异步执行引擎，WebSocket 实时终端
- **系统管理**: 用户/角色/权限（RBAC），操作审计日志，动态侧边栏，按钮级权限控制

## 快速启动（Docker 一键部署）

```bash
# 1. 构建后端 JAR
cd ops-twin-server
mvn clean package -DskipTests

# 2. 启动所有服务（MySQL + Server + Nginx）
cd ..
docker compose up -d
```

访问 http://localhost 即可进入系统。

默认管理员账号：`admin` / `123456`

## 本地开发

### 后端

```bash
cd ops-twin-server
mvn spring-boot:run
```

后端运行在 http://localhost:8080。

### 前端

```bash
cd ops-twin-web
npm install
npm run dev
```

前端运行在 http://localhost:5173（默认），API 代理自动转发至后端。

### 数据库

MySQL 8.0，初始化脚本见 `ops-twin-server/ops_twin_db2.sql`。默认配置：

```
url: jdbc:mysql://localhost:3306/ops_twin_db
username: root
password: 123456
```

## 测试

```bash
# 后端测试
cd ops-twin-server
mvn test

# 前端测试
cd ops-twin-web
npm run test
```

## 文档

- [项目需求文档 (PRD)](docs/01_PRD_项目需求文档.md)
- [数据库设计文档](docs/02_DB_数据库图表设计.md)
- [后端接口文档 (API)](docs/03_API_后端接口文档.md)
- [部署文档](docs/04_DEPLOY_部署文档.md)
- [实施计划](docs/implementation_plan.md)
- [待完善事项 (Backlog)](docs/backlog_待完善事项.md)
