# 后端代码理解指南

> 写给前端开发者看的后端文档。你不需要成为 Spring Boot 专家，只需要理解业务流程和数据流向。

---

## 简历四句话 → 后端文件对照表

| # | 简历描述 | 对应的后端文件 | 优先级 |
|---|---------|--------------|--------|
| 1 | Three.js 3D + WebSocket 状态推送 | `DashboardWebSocketHandler.java` | ★★★ |
| 2 | 路由 + 权限双层控制 | `JwtAuthFilter.java`, `SysController.java`, `RoleController.java`, `SysPermissionController.java` | ★★ |
| 3 | Vue Flow 编排 + 终端日志 | `TaskExecutionEngine.java`, `TaskLogWebSocketHandler.java`, `TaskPlanController.java` | ★★★ |
| 4 | Docker + Nginx 部署 | `docker-compose.yml`, 两个 `Dockerfile` | ★ |

---

## 目录

1. [简历点 1：3D WebSocket 实时状态推送](#1-简历点-13d-websocket-实时状态推送)
2. [简历点 2：路由权限双层控制](#2-简历点-2路由权限双层控制)
3. [简历点 3：演练编排 + 终端日志](#3-简历点-3演练编排--终端日志)
4. [简历点 4：Docker 部署](#4-简历点-4docker-部署)
5. [Spring Boot 基础概念速查](#5-spring-boot-基础概念速查)
6. [面试高频追问清单](#6-面试高频追问清单)

---

## 0. 前置知识：你的路由其实不是"动态路由"

简历上写的"动态路由方案"在面试时可能会被追问。事实是：

**本项目是静态路由 + 动态权限**，不是动态路由。
- 路由表写死在 `router/index.ts`，13 条固定不变
- 权限数据（menus + permissions）从后端动态拉取，控制的是侧边栏显隐和按钮权限
- 路由守卫按 `userMenus` 路径白名单做 403 拦截

**建议说法**（面试时）："基于 RBAC 的静态路由 + 动态菜单权限方案，菜单结构由后端下发、前端运行时按权限过滤渲染，路由守卫配合 localStorage 做菜单级 403 拦截。"

这会让你听起来很专业，因为你能区分"动态路由"和"动态权限"。

---

## 1. 简历点 1：3D WebSocket 实时状态推送

**简历原文**："通过 WebSocket 实时推送设备状态并动态更新模型颜色与 LED 指示灯"

### 后端干了什么

```
TaskExecutionEngine 执行一个步骤
  → 改了 host.status（1→2→3）
    → DashboardWebSocketHandler.broadcast(json)
      → 所有在线的 3D 看板收到 JSON
```

### 只需要看 2 个文件

| 文件 | 看什么 | 10 分钟够 |
|------|--------|----------|
| `websocket/DashboardWebSocketHandler.java` | 全局广播：`CopyOnWriteArrayList<Session>` 存所有 3D 客户端，`broadcast()` 遍历发消息 | 5 分钟 |
| `TaskExecutionEngine.java:159-161` | 调 `broadcast()` 的那一行，看推送的 JSON 格式 | 2 分钟 |

### 推送 JSON 格式

```json
{"type":"HOST_STATUS","hostname":"web-01","status":3,"cabinetId":1}
```

前端 `dashboard/index.vue` 收到后：查 hostname 对应的 3D mesh → 改 material.color（1=绿/2=黄/3=红）→ 触发 LED 脉冲动画。

## 2. 简历点 2：路由权限双层控制

**简历原文**："基于 Vue Router 与 Pinia 实现多角色路由方案，菜单结构由后端下发；结合路由守卫，覆盖菜单级至按钮级双层权限控制"

### 后端干了什么：4 个文件一条链

```
用户登录 → JwtAuthFilter 放行 /api/auth/login
    ↓
AuthController.login() → 生成 JWT token（含 roleId）→ 返回前端
    ↓
前端用 token 调 GET /api/system/menus?roleId=1
    ↓
SysController.getMenus(roleId)
    ├─ 查 sys_role_permission 表 → 该角色有权的 permIds
    ├─ 查 sys_permission 表 → 完整的菜单+按钮数据
    ├─ type=1 → menus 数组（侧边栏渲染）


    └─ type=2 → permissions 数组（按钮 hasPerm）
```

### 只需要看 3 个文件

| 文件 | 关键方法 | 看什么 |
|------|---------|--------|
| `security/JwtAuthFilter.java` | `doFilterInternal()` | 哪三类请求不用 token（白名单），其他全拦截 → 401 |
| `controller/SysController.java` | `getMenus()` | 查 role 的权限 ID → 查出完整菜单树 → 洗干净返回 |
| `controller/RoleController.java` | `savePermissions()` | 角色-权限的增删：先删旧映射，再批量插新映射 |

### 整个链路的后端关键代码

**生成 token 时已经埋了 roleId：（你登录页面完全没感知这件事）**
```java
// AuthController.java:62
String token = jwtUtils.generateToken(user.getUsername(), user.getId(), user.getRoleId());
```

**JWT Filter 放行登录接口 + WebSocket：（没有 token 校验就返回 401）**
```java
// JwtAuthFilter.java:33-36
if (path.equals("/api/auth/login") || path.startsWith("/ws/") || path.startsWith("/uploads/")) {
    filterChain.doFilter(request, response);  // 直接放行
    return;
}
```

**菜单接口根据 roleId 筛选权限的 SQL 逻辑：**
```java
// SysController.getMenus(roleId)
// Step 1：查角色-权限关联表 → 拿到权限 ID 列表
List<Long> permIds = sysRolePermissionMapper.selectList(
    query.eq(SysRolePermission::getRoleId, roleId)
).stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());

// Step 2：根据 ID 查出完整权限信息
List<SysPermission> all = sysPermissionMapper.selectBatchIds(permIds);

// Step 3：type=1 是菜单，type=2 是按钮
menus = all.filter(p -> p.getType() == 1);      // 侧边栏用
permissions = all.filter(p -> p.getType() == 2)  // 按钮权限用
              .map(SysPermission::getPermissionCode);
```

### 权限管理页用得上的后端接口（RoleManage）

| 前端操作 | 后端接口 | Controller |
|---------|---------|------------|
| 加载角色列表 | `GET /api/system/role/list` | `RoleController.list()` |
| 选择角色 → 回显已有权限 | `GET /api/system/role/{id}/permissions` | `RoleController.getPermissions()` |
| 勾选权限 → 保存 | `POST /api/system/role/{id}/permissions` | `RoleController.savePermissions()` |
| 新增/编辑权限节点 | `POST /api/system/permission/save` | `SysPermissionController.save()` |
| 删除权限节点 | `DELETE /api/system/permission/delete/{id}` | `SysPermissionController.delete()` |

---

## 3. 简历点 3：演练编排 + 终端日志

**简历原文**："引入 Vue Flow 实现容灾演练流程的可视化编排，支持拖拽式节点连接与策略配置；自研 WebSocket 驱动的仿终端日志组件，实时流式渲染脚本执行输出"

### 从"点击执行"到"终端看到日志"的完整链路

```
前端 strategy.vue → 点击"执行"按钮
  ↓
POST /api/task/record/trigger/{planId}
  ↓
TaskRecordController.trigger(planId)
  ├─ 查 TaskPlan（拿到 steps_json）
  ├─ 校验：plan 已启用 + 没有正在跑的同计划任务
  └─ 调 TaskRecordServiceImpl.triggerAsync()
       ├─ 建 TaskRecord（runStatus=PENDING）
       └─ 调 executionEngine.execute(recordId, plan)  ← @Async 异步！
  ↓ HTTP 接口立即返回 { recordId, wsPath }
  ↓
前端收到 → router.push → dashboard → WebSocket 连接 /ws/task/log/{recordId}
  ↓
TaskExecutionEngine（另开线程）解析 steps_json → 逐个 runStep
  └─ 每步：改 DB → pushLog → broadcast 3D 事件
```

### 只需要看 3 个文件

| 文件 | 看什么 | 花的时间 |
|------|--------|---------|
| `service/TaskExecutionEngine.java` | `execute()` 异步入口 + `runStep()` 改状态+推日志 (**理解核心**) | 30 分钟 |
| `websocket/TaskLogWebSocketHandler.java` | 按 recordId 分组存 session，`broadcast(recordId, msg)` 推日志文本 | 10 分钟 |
| `controller/TaskRecordController.java` | `trigger()` 触发 + `terminate()` 终止 | 10 分钟 |

### runStep 到底干了什么（简版）

```java
void runStep(Long recordId, Long serviceId, StepNode step) {
    // ① 查主机：hostname → AssetHost
    // ② 算新状态：STOP_NODE→3(down), START_NODE→1(healthy)
    // ③ 改 DB：host.setStatus(newStatus) → hostMapper.updateById(host)
    // ④ 改映射：FAILOVER/SCALE 改 service_host_map 绑定关系
    // ⑤ 推终端：pushLog(recordId, "[STEP] web-01 → down")
    // ⑥ 推 3D：  DashboardWebSocketHandler.broadcast(host_status_json)
}
```

### WebSocket 双通道对比

| | 日志通道 | 3D 事件通道 |
|---|---|---|
| URL | `/ws/task/log/{recordId}` | `/ws/dashboard/events` |
| Java 文件 | `TaskLogWebSocketHandler.java` | `DashboardWebSocketHandler.java` |
| 存储结构 | `Map<recordId, List<Session>>` — 按任务隔离 | `List<Session>` — 所有看板共享 |
| 推送方式 | 只推给连接了该任务的终端 | 广播给所有 3D 客户端 |
| 前端 | `terminal.vue` (Xterm 渲染) | `dashboard/index.vue` (改颜色+LED) |

---

## 4. 简历点 4：Docker 部署

**简历原文**："通过 Docker 一键部署全套服务，配合 Nginx 反向代理实现前后端分离"

### 文件结构

```
根目录/
├── docker-compose.yml              ← 一键编排：MySQL + Spring Boot + Nginx
├── ops-twin-server/Dockerfile      ← 后端镜像
└── ops-twin-web/Dockerfile         ← 前端镜像
```

### docker-compose 干的事

```yaml
services:
  mysql:      # MySQL 8.0 数据库，端口 3306
  server:     # Spring Boot 后端，端口 8080，依赖 mysql
  nginx:      # Nginx 反代，端口 80，依赖 server
```

**Nginx 反代的作用**：

```
用户访问 http://服务器IP
  → Nginx（80 端口）
    ├─ /api/*  → 转发给 server:8080（后端处理）
    ├─ /ws/*   → 转发给 server:8080（WebSocket 直连）
    └─ /*      → 返回前端静态文件（nginx 内置）
```

这样就实现了"同一个 IP 访问前后端"，浏览器不用配置跨端口访问。

### 面试时说

> "用 Docker Compose 把 MySQL、Spring Boot、Nginx 三个容器编排在一个桥接网络里。Nginx 反向代理解决同域访问，前端 `/api` 请求转发给 Java 后端，`/ws` WebSocket 也是 Nginx 转发。一条 `docker-compose up -d` 就起全套服务。"

---

## 5. Spring Boot 基础概念速查

| 注解/概念 | 一句话解释 | 类比前端 |
|-----------|-----------|---------|
| `@RestController` | HTTP 接口控制器 | Vue Router route handler |
| `@GetMapping/@PostMapping` | GET/POST 请求 | `axios.get()` / `axios.post()` |
| `@Async` | 方法在独立线程执行 | `Promise` / `setTimeout` |
| `@Autowired` | 自动注入依赖 | `import` 一个模块 |
| `Result<T>` | 统一响应格式 | `{ code: 200, message: "ok", data: {...} }` |
| `BaseMapper<T>` | 直接操作数据库 | Prisma Client / Drizzle |

---

## 6. 面试高频追问清单

### Q1: 演练执行时系统崩溃了怎么办？
> `StaleTaskCleanup` 监听 `ApplicationReadyEvent`，系统启动时把所有 RUNNING/PENDING 任务置为 CANCELLED。执行循环中每步检查 `cancelFlags`，支持手动终止。不会留下永久脏数据。

### Q2: 预案的 steps_json 格式是谁设计的？
> 前端 Vue Flow 拖拽编排 → 导出 JSON（type + target + waitMs）→ 存 `steps_json` 字段。后端 `parseSteps()` 反序列化，跳过 `layout-meta` 类型节点，执行真实的运维动作节点。

### Q3: 3D 是怎么联动 WebSocket 的？
> 后端 `runStep()` 改完主机状态后调 `DashboardWebSocketHandler.broadcast(json)` → 所有 3D 看板收到 JSON → 前端遍历 `meshList`，匹配 hostname → 改 material.color + 触发 LED 动画。

### Q4: 权限怎么实现双层控制？
> 菜单级：`userMenus[]` → 侧边栏动态渲染 + 路由守卫 403 拦截。按钮级：`permissions[]` → `hasPerm(code)` → `v-if` 控制按钮显隐。后端 `SysController.getMenus(roleId)` 返回两份数据。

### Q5: Nginx 反代有什么用？
> 前端 5173、后端 8080、WebSocket 8080 → Nginx 把 `/api` 和 `/ws` 全反代到同一个 80 端口，浏览器只看到一个 IP，解决跨域 + 同域名访问。

### Q6（附加题）: 你简历写"动态路由"，能展开说说吗？
> 严格说本项目是"静态路由 + 动态菜单权限"。路由表固定注册 13 条，不同角色的菜单由后端按 roleId 过滤返回。和真正动态路由（如 Umi patchClientRoutes 运行时注入）的区别是路由表本身不变，变的是可见范围。两种方案我都做过，各有适用场景。

---

## 你现在该先看哪个文件

按简历四句话的优先级：

```
第一遍（搞懂简历点 1+3 = WebSocket 双通道）：
  → DashboardWebSocketHandler.java（10 分钟）
  → TaskLogWebSocketHandler.java（10 分钟）
  → TaskExecutionEngine.execute()（20 分钟）

第二遍（搞懂简历点 2 = 权限双层控制）：
  → JwtAuthFilter.java（10 分钟）
  → SysController.getMenus()（10 分钟）
  → RoleController.savePermissions()（10 分钟）

第三遍（浏览简历点 4 = Docker）：
  → docker-compose.yml（5 分钟）
  → 随便看一个 Dockerfile（2 分钟）
```

全部看完约 2 小时。你已经理解了前端全部逻辑，后端这些代码你带着"这个接口给前端哪个页面用的"的问题去看，会非常快。
