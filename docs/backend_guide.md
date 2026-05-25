# 后端代码理解指南

> 写给前端开发者看的后端文档。你不需要成为 Spring Boot 专家，只需要理解业务流程和数据流向。

---

## 目录

1. [整体架构速览](#1-整体架构速览)
2. [你需要重点理解的 4 个文件](#2-你需要重点理解的-4-个文件)
3. [演练执行全流程（核心）](#3-演练执行全流程核心)
4. [双通道 WebSocket 实时联动](#4-双通道-websocket-实时联动)
5. [其他 Controller 一览](#5-其他-controller-一览)
6. [Spring Boot 基础概念速查](#6-spring-boot-基础概念速查)
7. [简历面试要点](#7-简历面试要点)

---

## 1. 整体架构速览

```
src/main/java/com/ops/twin/
├── OpsTwinApplication.java         ← Spring Boot 入口
├── common/Result.java              ← 统一响应格式 {code, message, data}
├── config/                         ← 配置类
├── controller/                     ← HTTP 接口层（12 个 Controller）
├── entity/                         ← 数据库表对应的 Java 类（13 张表）
├── mapper/                         ← MyBatis-Plus 数据访问层（零 SQL XML）
├── service/                        ← 业务逻辑层
│   ├── TaskExecutionEngine.java    ← ★ 核心：异步执行引擎
│   └── impl/                       ← Service 实现类
├── security/                       ← JWT 认证 + Spring Security
├── audit/                          ← AOP 操作审计（注解 + 切面）
└── websocket/                      ← ★ WebSocket 双通道
    ├── TaskLogWebSocketHandler.java    ← 终端日志推送
    └── DashboardWebSocketHandler.java  ← 3D 事件推送
```

**分层关系：**

```
Controller（接收 HTTP 请求）
  → Service（业务逻辑）
    → Mapper（数据库操作）
    → WebSocket Handler（实时推送）
```

---

## 2. 你需要重点理解的 4 个文件

按重要性排序：

| 优先级 | 文件 | 对应的前端 | 花的时间 |
|--------|------|-----------|---------|
| ★★★ | `service/TaskExecutionEngine.java` | 终端实时日志 + 3D 状态变化 | 2 天 |
| ★★★ | `websocket/TaskLogWebSocketHandler.java` | `terminal.vue` 的日志流 | 半天 |
| ★★ | `service/impl/TaskRecordServiceImpl.java` | 点击"执行"按钮后的触发逻辑 | 半天 |
| ★★ | `websocket/DashboardWebSocketHandler.java` | `dashboard/index.vue` 的 3D 联动 | 半天 |

其他 Controller、Entity、Mapper 基本是标准 CRUD，你看一眼就能对应到前端的表格页面。

---

## 3. 演练执行全流程（核心）

这是整个项目最有技术含量的部分。对照下面的时序理解：

### 3.1 触发阶段（从前端点击到异步执行）

```
前端: 点击"执行"按钮 (strategy.vue)
  │
  ▼
POST /api/task/record/trigger/{planId}
  │
  ▼
TaskRecordController.trigger()
  ├── ① 查出 TaskPlan（预案定义）
  ├── ② 校验 plan.status === 1（已启用）
  ├── ③ 检查该 plan 没有正在运行的任务
  ├── ④ 调用 TaskRecordServiceImpl.triggerAsync()
  │     ├── 创建 TaskRecord（runStatus = PENDING）
  │     └── 调用 executionEngine.execute(recordId, plan)  ← @Async 异步！
  └── ⑤ 立即返回 { recordId, wsPath: "/ws/task/log/123" }
       ↑ HTTP 响应到此结束，用户不会等待

前端: 收到 recordId → 跳转 dashboard + 连接 WebSocket
```

**关键设计点：** `@Async` 注解让 `execute()` 方法在另一个线程执行，HTTP 请求立即返回。这意味着执行一个 10 步的预案只需要毫秒级的接口响应时间。

### 3.2 执行阶段（TaskExecutionEngine.execute()）

这是核心中的核心，逐行理解：

```java
@Async  // ← Spring 会在线程池中执行这个方法
public void execute(Long recordId, TaskPlan plan) {
```

**步骤 1：解析步骤 JSON**

```java
// plan.getStepsJson() 就是你在 workflow.vue 编排画布里拖出来的节点 JSON
// 格式：[{id:"node1", type:"STOP_NODE", data:{target:"web-01", waitMs:2000}}, ...]
List<StepNode> steps = parseSteps(plan.getStepsJson());
```

**步骤 2：跳过布局节点**

```java
// workflow.vue 的画布上有些节点只是用来做视觉连接的（layout-meta）
// 后端执行时要跳过它们，只执行真正的操作步骤
steps = steps.stream()
    .filter(s -> !"layout-meta".equals(s.getType()))
    .collect(toList());
```

**步骤 3：逐个执行步骤（runStep）**

```java
for (StepNode step : realSteps) {
    // 检查是否被取消了
    if (cancelFlags.get(recordId)) { ... break; }

    runStep(recordId, plan.getServiceId(), step);
    Thread.sleep(step.getWaitMs());  // 等待步骤间的时间间隔
}
```

**步骤 4：runStep 内部做了什么**

```java
void runStep(Long recordId, Long serviceId, StepNode step) {
    // ① 根据 hostname 查到对应主机
    AssetHost host = hostMapper.findByHostname(step.getTarget());

    // ② 根据动作类型，决定新的状态值
    // STOP_NODE → status=3 (down)
    // START_NODE → status=1 (healthy)
    // HEALTH_CHECK → 随机 1 或 3
    Integer newStatus = mapActionToStatus(step.getType());

    // ③ 更新数据库中的主机状态
    host.setStatus(newStatus);
    hostMapper.updateById(host);

    // ④ 特殊操作：修改服务-主机映射关系
    // PROMOTE_SLAVE → 从库提升为主库（改 service_host_map）
    // FAILOVER_TO  → 故障转移（切换绑定）
    // REMOVE_NODE  → 下线节点（删除绑定）
    modifyServiceHostMap(serviceId, step);

    // ⑤ ★ 推送日志到终端 WebSocket
    pushLog(recordId, "[" + step.getType() + "] " + host.getHostname()
            + " → 状态变为 " + newStatus);

    // ⑥ ★ 推送事件到 3D 看板 WebSocket
    DashboardWebSocketHandler.broadcast(json);
    // 发送的 JSON 格式：
    // {type:"HOST_STATUS", hostname:"web-01", status:3, cabinetId:1}
}
```

**步骤 5：执行完毕**

```java
// 执行完毕后自动禁用预案（防止重复执行）
plan.setStatus(0);
planMapper.updateById(plan);

// 更新执行记录的最终状态
record.setRunStatus("SUCCESS");
record.setDurationMs(System.currentTimeMillis() - start);
recordMapper.updateById(record);
```

### 3.3 终止阶段

```
前端: 点击"终止"按钮 (terminal.vue)
  │
  ▼
POST /api/task/record/{id}/terminate
  │
  ▼
executionEngine.cancel(recordId)
  └── cancelFlags.put(recordId, true)  // 设置取消标记
      └── execute() 循环中检测到标记 → break 退出
```

### 3.4 预案重置（plan reset）

演练执行完后主机状态被改了，需要恢复。`TaskPlanController.reset(planId)`：

- **DRILL 类型**：把所有受影响的主机状态恢复为 1 (healthy)
- **FAILOVER/SCALE 类型**：从 topology JSON 重建 service_host_map 绑定关系
- 最后把 plan 的 status 设回 1（重新启用）

---

## 4. 双通道 WebSocket 实时联动

这是你简历上最亮眼的技术点。两个 WebSocket 通道各司其职：

### 4.1 架构图

```
浏览器
├── WebSocket 1: /ws/task/log/{recordId}
│      ↓ 单向推送（后端→前端）
│   TaskLogWebSocketHandler
│      ↓ 每执行一个步骤推送一行
│   terminal.vue（Xterm 终端显示）
│
└── WebSocket 2: /ws/dashboard/events
       ↓ 单向推送（后端→前端）
    DashboardWebSocketHandler
       ↓ 主机状态变化时推送 JSON
    dashboard/index.vue（3D 机柜变色 + LED 动画）
```

### 4.2 TaskLogWebSocketHandler（日志通道）

```java
// 核心数据结构：一个任务可以有多个订阅者
ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocketSession>> sessions;

// 建立连接时（前端 new WebSocket(url)）
void afterConnectionEstablished(session) {
    Long recordId = 从 URI 路径 /ws/task/log/{recordId} 中提取;
    sessions.get(recordId).add(session);
}

// 推送日志（由 TaskExecutionEngine.runStep 调用）
void broadcast(Long recordId, String message) {
    for (WebSocketSession s : sessions.get(recordId)) {
        s.sendMessage(new TextMessage("[14:30:22] STOP_NODE web-01 → down"));
    }
}
```

**前端对应代码**（`terminal.vue`）：
```javascript
const ws = new WebSocket(`ws://localhost:8080/ws/task/log/${recordId}`)
ws.onmessage = (event) => {
  // event.data = "[14:30:22] STOP_NODE web-01 → down"
  terminal.write(event.data + '\n')
  // 同时存入 sessionStorage（跨页面恢复用）
}
```

### 4.3 DashboardWebSocketHandler（3D 事件通道）

```java
// 只维护一个全局会话列表（所有 3D 客户端共享）
CopyOnWriteArrayList<WebSocketSession> sessions;

// 广播给所有连接的 3D 看板
static void broadcast(String json) {
    for (WebSocketSession s : sessions) {
        s.sendMessage(new TextMessage(json));
    }
}
```

**推送的 JSON 格式：**
```json
{
  "type": "HOST_STATUS",
  "hostname": "web-01",
  "status": 3,
  "cabinetId": 1,
  "timestamp": 1715856000000
}
```

**前端对应代码**（`dashboard/index.vue`）：
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/dashboard/events')
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  if (data.type === 'HOST_STATUS') {
    // ① 更新 3D 机柜中对应主机的颜色
    //    status 1 → 绿色，2 → 黄色，3 → 红色
    // ② 触发 LED 脉冲动画（绿→黄→红的过渡）
    // ③ 如果有对应标签，更新 CSS2D 标签颜色
  }
}
```

### 4.4 为什么用两个通道而不是一个？

- **隔离性**：日志通道是"一对一"的（每个任务独立），3D 通道是"广播"的（所有看板共享）
- **生命周期不同**：日志通道在任务结束后关闭，3D 通道随页面常驻
- **职责清晰**：终端只关心日志文本，看板只关心状态事件，互不干扰

---

## 5. 其他 Controller 一览

这些基本都是标准 CRUD，你对照前端页面就能理解：

| Controller | 对应前端页面 | 特别说明 |
|-----------|-------------|---------|
| `AuthController` | `login/index.vue` | JWT 签发，bcrypt 密码验证 |
| `UserController` | `system/user.vue` | 含头像上传（MultipartFile） |
| `RoleController` | `system/role.vue` | 角色 CRUD + 权限树分配 |
| `AuditController` | `system/audit.vue` | 操作审计日志查询 |
| `AssetCabinetController` | `assets/cabinet.vue` | 机柜 CRUD，删除前检查有无主机 |
| `AssetController` | `assets/host.vue` | 主机 CRUD，含机柜槽位校验 |
| `AssetServiceController` | `assets/service.vue` | 逻辑服务 + Vue Flow 拓扑保存 |
| `TaskPlanController` | `tasks/strategy.vue` | 预案 CRUD + 启用/禁用 + 重置 |
| `TaskRecordController` | 执行触发 | 触发执行、查询记录、终止任务 |
| `AnalysisController` | `dashboard/analysis.vue` | KPI 统计（部分 mock 数据） |
| `SysController` | 登录后初始化 | 返回角色菜单树 + 权限码列表 |

### 特别注意：`AssetServiceController` 的拓扑保存

```java
POST /api/asset/service/topology/save
// 前端 Vue Flow 画布保存时：
// ① 保存 topologyJson（节点坐标、连线关系）
// ② 解析 JSON 中的 hostId，重建 service_host_map 绑定
// 这样就实现了"拖拽主机到服务 = 建立映射关系"
```

---

## 6. Spring Boot 基础概念速查

你不写 Java 代码，但看代码时需要知道这些：

| 概念 | 一句话解释 | 类比前端 |
|------|-----------|---------|
| `@RestController` | HTTP 接口控制器 | Vue Router 的一个 route handler |
| `@RequestMapping("/api/xxx")` | 接口路径前缀 | `router.get('/api/xxx')` |
| `@GetMapping/@PostMapping` | GET/POST 请求 | `axios.get()` / `axios.post()` |
| `@Async` | 方法在独立线程执行 | `Promise` 或 `setTimeout` |
| `@Autowired` / `@Resource` | 自动注入依赖 | `import` 一个模块 |
| `@Component` | 声明一个 Spring 管理的 Bean | 不需要类比，知道它会被自动创建就行 |
| `@Transactional` | 数据库事务 | 一组操作要么全成功要么全回滚 |
| `IService<T>` | MyBatis-Plus 提供的 CRUD 接口 | 一个自带增删改查的 API |
| `BaseMapper<T>` | 直接操作数据库的层 | Prisma Client / Drizzle |
| `Result<T>` | 统一响应格式 | `{ code: 200, message: "ok", data: {...} }` |

**执行引擎为什么不是 Service 而是独立 Component？**

`TaskExecutionEngine` 直接依赖 Mapper（不通过 Service），是为了避免循环依赖：Service 层要调用引擎，引擎如果又依赖 Service 就会形成循环。这是一种务实的取舍。

---

## 7. 简历面试要点

### 7.1 面试官可能会问的问题

**Q: 这个项目的技术难点是什么？**

> 答：核心技术难点是**演练执行引擎和 3D 数字孪生的实时联动**。我设计了一个双通道 WebSocket 架构：
> - 通道一（`/ws/task/log/{id}`）：一对一推送每个执行步骤的日志到前端 Xterm 终端
> - 通道二（`/ws/dashboard/events`）：广播主机状态变化事件到 3D 看板
>
> 后端用 `@Async` 异步执行，HTTP 接口毫秒级返回，执行过程通过 WebSocket 实时推流。3D 看板收到事件后实时更新机柜颜色、触发 LED 动画、显示主机标签。

**Q: 怎么保证执行过程中断或崩溃不会留下脏数据？**

> 答：应用启动时有一个 `StaleTaskCleanup` 监听器，会扫描数据库中所有 RUNNING/PENDING 状态的记录并自动标记为 CANCELLED。执行过程中每一步都检查取消标记，支持手动终止。

**Q: 预案的步骤 JSON 怎么设计的？**

> 答：前端 Vue Flow 画布拖拽编排节点，保存为 JSON 数组存到 `steps_json` 字段。每个节点包含 type（动作类型）、target（目标主机）、waitMs（等待时间）。后端解析 JSON 后跳过布局元数据节点，按顺序执行真实操作步骤，每步更新数据库状态并推送到 WebSocket。

### 7.2 简历上建议这样写

> **智维方舟 — 数据中心 3D 数字孪生运维平台**
>
> - 基于 **Vue 3 + Three.js + Spring Boot** 全栈独立开发
> - 设计并实现**双通道 WebSocket 实时联动架构**：演练执行引擎异步执行步骤，通过 `/ws/task/log` 推送实时日志到 Xterm 终端，通过 `/ws/dashboard/events` 驱动 3D 机柜实时变色与 LED 动画
> - 实现 **Vue Flow 拖拽编排引擎**：支持 8 种运维动作节点（启停/健康检查/主从切换/故障转移等），覆盖 11 个逻辑服务、26 个演练预案
> - 三级 3D 视景（L1 全局 → L2 多机柜包围盒 → L3 单刀片），执行时自动计算包围盒 + 镜头飞行，CSS2D 标签实时标注故障主机
> - RBAC 权限模型 + AOP 操作审计 + JWT 认证，13 张数据表，15 个后端单元测试

### 7.3 和下一个 React + AntV X6 项目的衔接

核心概念是相通的：

| Vue Flow (本项目) | AntV X6 (下个项目) |
|-------------------|-------------------|
| 节点 `type` + `data` | 节点 `shape` + `data` |
| 边 `source` → `target` | 边 `source` → `target` |
| `stepsJson` 序列化 | 同样的 JSON 序列化 |
| `layout-meta` 过滤 | 画布元数据过滤 |

你在这项目里理解的"画布编排 → JSON 存储 → 引擎解析执行"这个链路，下个项目可以直接复用思维模型。
