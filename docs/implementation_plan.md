# 智维方舟 (Ops-Twin) 项目总实施计划 (架构师视界)

## 核心路线图 (基于侧边栏 4 大中枢架构)

### [x] Stage 1: 基础设施与看板中心 (已完成)

- [x] **底层基座**: Vue3 + SpringBoot 脚手架，bcrypt 加密，Pinia 持久化。
- [x] **3D 孪生大屏**: InstancedMesh 性能优化，L1/L2/L3 物理机柜与插槽真实解耦穿透。
- [x] **效能大盘分析**: 对接 MySQL 实时汇总总算力、总内存、告警占比，ECharts 响应式渲染。

### [x] Stage 2: 资产中心 (已完成)

- [x] **物理资产台账**: 完整的 `asset_host`与`asset_cabinet` CRUD，强化了空间冲突与唯一性拦截，作为 3D 渲染和逻辑拓扑的绝对数据源头。
- [x] **逻辑服务映射**: 引入 Vue Flow，支持拖拽将零散的物理服务器组装为“逻辑业务系统”（如支付系统），实现物理架构到业务架构的转换。

### [▶] Stage 3: 任务中心 (自动化演练引擎 - 核心闭环已完成)

- [x] **预案方案库**: task_plan 表建立，完整 CRUD + 启用/禁用，关联逻辑服务时显示主机绑定数。
- [x] **演练工作流**: 可视化步骤编辑器（Vue Flow 拖拽 + 动作库面板），目标参数配置，自动序列化为 steps_json 保存。
- [x] **异步执行引擎**: @Async 解析 steps_json 逐步模拟，支持用户手动终止（CANCELLED），空步骤/无效服务自动报错。
- [x] **实时监控终端**: WebSocket 推流 + Xterm 风格黑底绿字控制台，进度条/状态徽章/语义着色/终止命令按钮。
- [x] **逻辑服务-预案-工作流联动**: 引擎校验关联服务存在性及主机绑定数（0台→FAILED），步骤 target 为空→FAILED。工作流 Target 改为关联服务主机下拉选择。

### [▶] Stage 3.5: 任务中心体验优化 (刚完成)

- [x] **任务总览页**: `tasks/index.vue` 仪表盘（KPI 统计卡片 + 预案列表 + 快捷编排/执行按钮）
- [x] **侧边栏**: 任务中心保留「任务总览」「预案方案库」「实时监控终端」三个入口
- [x] **workflow 参数守卫**: `onMounted` 检测无 planId → 跳回任务总览
- [x] **设定方案联动**: index 点击「设定方案」→ 跳转 strategy 并自动打开新增弹窗

### [x] Stage 4: 系统管理 (安全与审计 — 已完成)

- [x] **JWT 真实认证 + bcrypt 密码 + CORS 集中管控**：jjwt 0.12.5 生成验证 Bearer Token，bcrypt 哈希存储密码，旧 MD5 登录时自动迁移。CORS 统一在 `WebConfig.java` 管控，移除所有 Controller 的 `@CrossOrigin`。
- [x] **用户中心**: 账号、密码、个人资料管理（`system/user.vue` CRUD + 头像上传 + 按钮权限控制）。
- [x] **权限配置**: `sys_role` + `sys_role_permission` 两张新表，角色 CRUD + el-tree 权限树勾选分配。`sys_permission` 扩展 type 字段（1=菜单 2=按钮），permission_code 控制按钮级权限。
- [x] **操作审计**: 6 种事件类型（新增/编辑/删除/执行预案 + 修改角色权限 + 其他），前端列表 + 搜索筛选 + 刷新，操作人改为真实用户名。AOP 切面 `@AuditLog` 自动拦截写入方法，记录到 `audit_event` 表。
- [x] **按钮级权限**: userStore 存储 permissions + menus（localStorage 持久化），strategy/host/user/index/cabinet 五个页面按钮已接入 v-if 权限控制。
- [x] **动态侧边栏**: layout 改为从 userStore.menuSections 动态渲染，不同角色看到不同菜单，刷新不丢失。

### [x] Stage 5: 真实执行引擎 (已完成)

- [x] **真实执行引擎 — 查主机表 + 改状态 + WebSocket 广播 3D 事件**：`TaskExecutionEngine` 根据 hostname 查询 `asset_host` 表，动态修改 host status (1/2/3)，通过 `/ws/dashboard/events` 广播结构化 JSON 事件到 3D 大屏。
- [x] **双通道 WebSocket**：`/ws/task/log/{recordId}` 推流终端日志 + `/ws/dashboard/events` 广播 3D 状态变更事件。

### [x] Stage 6: 演练联动与逻辑拓扑 (已完成)

- [x] **演练联动 — 浮动终端 + 3D 实时变色 + DRILL 自动恢复**：策略页触发演练 → 3D 大屏主机 LED 实时变色（绿→黄→红）→ 浮动迷你终端面板自动弹出。DRILL 执行结束后主机状态自动恢复为健康，FAILOVER 持久化变更。
- [x] **Vue Flow 拓扑编辑器**: 拖拽物理节点到画布，可视化构建逻辑服务拓扑。
- [x] **服务-主机绑定**: ServiceHostMap 映射表，拓扑保存同步更新绑定关系。

### [x] Stage 7: 大屏可视化增强 (已完成)

- [x] **ECharts 图表增强**: 效能大盘 KPI 统计卡片 + 饼图 + 趋势图。
- [x] **响应式适配**: 大屏布局自适应不同分辨率。

### [x] Stage 8: AOP 审计日志 (已完成)

- [x] **@AuditLog 注解**: 自定义方法级审计注解，记录操作类型与描述。
- [x] **AuditLogAspect 切面**: 环绕通知自动拦截注解方法，提取操作人（从 Authorization 头），记录操作结果（SUCCESS / FAILED）。
- [x] **控制器接入**: 6 个核心控制器（AssetCabinet、Asset、User、Role、TaskPlan、AssetService）的创建/删除方法全部接入 AOP 审计。

### [x] Stage 9: Docker 容器化 (已完成)

- [x] **后端 Dockerfile**: 基于 eclipse-temurin:17-jre-alpine 构建 Spring Boot 镜像。
- [x] **前端 Dockerfile**: 多阶段构建（node:20-alpine 编译 + nginx:alpine 运行），nginx 反向代理 API/WS/Uploads。
- [x] **Docker Compose**: 一键编排 MySQL + Server + Nginx，健康检查 + 依赖等待 + 数据持久化。

### [x] Stage 10: 文档完善 (已完成)

- [x] **项目 README**: 技术栈表格 + Docker 快速启动 + 本地开发 + 测试命令 + 文档链接。
- [x] **部署文档**: `04_DEPLOY_部署文档.md` — Docker 部署 + 本地手动部署 + 生产注意事项。
- [x] **API 文档 JWT**: 在 03_API 顶部补充 JWT 认证说明（Bearer Token 格式）。
- [x] **实施计划更新**: 追加 Stage 5-10 完成标记。

## 今日焦点

详见同目录下的 `today_plan.md`。

---

---

## 最终成果总结 (Stage 1-10 全部完成)

### 核心架构

| 维度         | 成果                                                                                                    |
| ------------ | ------------------------------------------------------------------------------------------------------- |
| **前端**     | Vue 3.5 + TypeScript 6 + Vite 8 + Element Plus 2.13 + Pinia 3 + Vue Router 4                            |
| **后端**     | Spring Boot 3.2 + MyBatis-Plus 3.5.5 + Java 17 + Maven                                                  |
| **3D 渲染**  | Three.js 0.184 + InstancedMesh 实例化渲染 + Tween.js 0.25 相机动画                                      |
| **图表**     | ECharts 6.0 响应式效能大盘                                                                              |
| **拓扑编辑** | @vue-flow/core 拖拽式逻辑服务拓扑 + 演练工作流步骤编排                                                  |
| **数据库**   | MySQL 8.0 + 13 张业务表 (系统管理 5 张 + 资产中心 4 张 + 任务中心 3 张 + 流水日志 1 张)                 |
| **认证**     | jjwt 0.12.5 真实 JWT (Bearer Token) + bcrypt 密码哈希 + 旧 MD5 登录时自动迁移                           |
| **CORS**     | WebConfig.java 集中管控，移除所有 Controller 的 @CrossOrigin                                            |
| **权限**     | RBAC 角色-权限模型，sys_role + sys_role_permission 两张表，el-tree 权限树，动态侧边栏，按钮级 v-if 控制 |
| **审计**     | @AuditLog AOP 切面自动拦截写入操作，6 种事件类型，记录操作人/类型/描述/结果                             |
| **容器化**   | Docker Compose 一键编排 MySQL + Spring Boot + Nginx，健康检查 + 依赖等待 + 数据持久化                   |

### 3D 数字孪生能力

- **L1/L2/L3 多层级穿透**：机房层 → 机架层 → 硬件标牌，基于 cabinet_id + rack_pos 100% 物理级真实渲染
- **InstancedMesh 性能优化**：数百台服务器模型流畅渲染
- **热力图模式**：一键切换 3D 场景材质，根据负载分布渲染温度场
- **实时联动**：演练触发后相机自动飞行至目标机柜、自动进入 L2 层级、自动切换热力模式、主机 LED 脉冲动画 (绿→黄→红)、刀片盒发光高亮、机柜线框变色
- **双通道 WebSocket**：`/ws/task/log/{recordId}` 终端日志推流 + `/ws/dashboard/events` 3D 状态变更事件广播
- **浮动迷你终端**：大屏页面内嵌 mini 终端面板，演练执行时自动弹出，展示实时日志流
- **状态恢复**：sessionStorage 保存大屏导航状态，从终端页「返回大屏」时恢复 L2/热力等全部视图状态

### 真实执行引擎

- **任务执行引擎 (TaskExecutionEngine)**：@Async 异步线程解析 steps_json，根据 hostname 查询 asset_host 表，动态修改 status 字段
- **三种演练类型不同行为**：
  - **DRILL (演练)**：执行 → 主机状态变更 (1→3 宕机) → 3D LED 实时变色 → 执行结束**自动恢复**为 1(健康) → 3D LED 恢复绿色
  - **FAILOVER (故障切换)**：执行 → 主机状态变更 → **持久化**不恢复 → 同时更新 service_host_map 绑定关系 (将宕机主机的服务迁移到备用主机) → 执行后自动禁用预案 (status=0)
  - **SCALE (弹性伸缩)**：执行 → 动态修改 service_host_map (增加或移除主机绑定) → 执行后自动禁用预案 (status=0)
- **预案重置 API**：`POST /api/task/plan/reset/{planId}` — 将 FAILOVER/SCALE 执行后的预案状态恢复为启用 (status=1)，还原服务绑定关系
- **服务校验**：引擎执行前校验关联逻辑服务存在性及主机绑定数 (0台 → FAILED)
- **手动终止**：用户可随时终止正在执行的任务，引擎在下一步执行前检测取消信号 → 状态更新为 CANCELLED

### 预案方案库

- **26 个演练预案**，覆盖 11 个逻辑服务
- 预案类型：DRILL (演练) / FAILOVER (故障切换) / SCALE (弹性伸缩)
- 可视化工作流编辑器：Vue Flow 拖拽 + 动作库面板 + 目标主机下拉选择 + 参数配置抽屉
- 预案联动：创建预案时关联逻辑服务，自动显示已绑定主机数

### 物理资产规模

- **8 个物理机柜** (cabinet-A 至 cabinet-H)，含真实 pos_x/pos_z 空间坐标和 max_u 容量
- **60 台物理主机**，分布在 6 种角色类型 (WEB/APP/DB/CACHE/LB/MQ)，绑定到具体机柜和 U 位插槽
- 完整 CRUD + 空间冲突检测 + 唯一性拦截

### 测试覆盖

- **15 个后端集成测试**：JUnit 5 + MockMvc + H2 内存数据库，覆盖认证/资产/预案/角色/审计全部核心 Controller
- **4 个前端 Vitest 测试**：组件渲染 + Store 状态管理 + API 调用
- 测试命令：`mvn test` (后端) / `npx vitest run` (前端)

### 部署方式

- `docker compose up` 一键启动 (MySQL + Spring Boot + Nginx)
- 前端 Nginx 反向代理 API / WebSocket / Uploads
- 本地开发：`mvn spring-boot:run` (后端) + `npm run dev` (前端)

### 联动验证 — 全流程测试步骤

以下步骤用于验证 Ops-Twin 全部 10 个 Stage 的端到端联动效果：

1. **启动环境**：`docker compose up`（MySQL + Spring Boot + Nginx）
2. **登录**：`POST /api/auth/login`，使用 admin 账号获取 JWT Token
3. **资产管理**：在资产中心创建机柜 + 主机，绑定 cabinet_id 和 rack_pos
4. **3D 验证**：打开 3D 孪生大屏，验证主机模型出现在正确机柜插槽位置
5. **逻辑服务**：创建逻辑服务，在 Vue Flow 拓扑中将主机绑定到服务
6. **创建预案**：在预案方案库创建 DRILL 预案，关联逻辑服务
7. **编排步骤**：打开工作流编辑器，拖拽步骤并选择目标主机
8. **执行演练**：点击"执行"触发演练 → 自动跳转到终端页面查看实时日志
9. **3D 联动**：切换到 3D 大屏，观察主机 LED 颜色实时变化（绿→黄→红）
10. **浮动终端**：确认浮动迷你终端面板自动弹出，展示与终端页面同步的日志流
11. **DRILL 恢复**：演练结束后，确认主机状态自动恢复为 1(健康)，3D LED 变回绿色
12. **审计日志**：在操作审计页面查看 EXECUTE_PLAN 事件记录
13. **权限控制**：切换普通用户登录，验证侧边栏菜单和按钮权限生效
14. **测试套件**：运行 `mvn test`（15 后端测试 + 4 前端 Vitest 测试全部通过）
