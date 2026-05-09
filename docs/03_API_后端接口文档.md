# 后端 Spring Boot 接口文档

请保持良好的前后端分离修养，所有的接口均返回统一结构：
```json
{
  "code": 200,      // 业务状态码
  "message": "OK",  // 提示信息
  "data": ...       // 承载的数据主体
}
```

## 1. 看板中心 (Dashboard)
### 1.1 核心 KPI 聚合汇总
**接口路径**: `GET /api/analysis/kpi-stats`
- 说明：计算资产总数、报警数、物理 CPU 总算力及内存总容量。

### 1.2 机柜物理资产分布
**接口路径**: `GET /api/analysis/asset-distribution`
- 说明：按 cabinet_id 聚合计算每个物理机柜的资产占比，渲染大盘饼图。

### 1.3 拉取 3D 节点相对信息
**接口路径**: `GET /api/asset/nodes/scence`
- 说明：专门给 Three.js 用的轻量化同步接口。

## 2. 资产中心 (Asset Center)

### 2.0 机柜台账管理 (新增)
**接口路径**: `GET /api/asset/cabinet/list` , `POST /api/asset/cabinet/save` , `DELETE /api/asset/cabinet/delete/{id}`
- 说明：用于维护机房内的物理机柜及真实的 X、Z 空间坐标。

### 2.1 物理主机列表查询 (分页)
**接口路径**: `GET /api/asset/host/list`
- 说明：用于资产管理表格展示。支持按 cabinetId 搜索。
- Params: `current, size, hostname, ipAddr, cabinetId`

### 2.2 物理主机增删改
**接口路径**: `POST /api/asset/host/save` , `DELETE /api/asset/host/delete/{id}`
- 说明：支持绑定 cabinet_id 和 rack_pos。保存时不再接收坐标字段，彻底解耦物理坐标。

## 3. 任务中心 (Task Center)
### 3.1 预案方案库 CRUD ✔已实现
**接口路径**: `GET /api/task/plan/list`
- Params: `current, size, planName, serviceId, planType`
- 说明：分页查询，支持按名称/服务/类型筛选，按优先级升序、创建时间降序排列。

**接口路径**: `POST /api/task/plan/save`
- Body: TaskPlan 对象
- 说明：新增或更新预案（saveOrUpdate），同服务下同名预案校验唯一性。

**接口路径**: `DELETE /api/task/plan/delete/{id}`

**接口路径**: `PUT /api/task/plan/toggle/{id}`
- 说明：启用/禁用预案，切换 status 字段。

### 3.2 触发演练执行 ✔已实现
**接口路径**: `POST /api/task/record/trigger/{planId}`
- Params: `operator` (可选，默认 admin)
- 说明：核心异步接口。根据 planId 创建流水记录并立即返回 recordId，随后在后台开启独立线程解析 steps_json 执行演练，并通过 WebSocket 实时推流日志。
- 返回: `{ recordId: 42, wsPath: "/ws/task/log/42", planName: "..." }`

### 3.3 任务流水列表查询 ✔已实现
**接口路径**: `GET /api/task/record/list`
- Params: `current, size, planId`
- 说明：分页查询演练执行历史。

### 3.4 终端实时日志拉取 (WebSocket) ✔已实现
**接口路径**: `WS /ws/task/log/{recordId}`
- 说明：基于 Spring WebSocket 的文本协议通道。前端 terminal.vue 进入后自动建立连接，实时接收带有语义标签（如 [INFO], [SUCCESS]）的日志行。

### 3.5 终止正在执行的任务 ✔已实现
**接口路径**: `POST /api/task/record/{id}/terminate`
- 说明：向引擎发送取消信号，引擎在下一步执行前检测到标志后立即中止，推送终止通知到 WebSocket，数据库状态更新为 CANCELLED。
- 返回：`{ code: 200, data: "已发送终止信号" }` 或 `{ code: 500, message: "任务不存在或已结束，无法终止" }`

### 3.6 逻辑服务列表（含主机数）✔已实现
**接口路径**: `GET /api/asset/service/list`
- 说明：分页查询逻辑服务，每条记录附加 `hostCount` 字段表示已绑定的物理主机数量，供前端下拉框展示。

### 3.7 查询服务绑定的物理主机列表 ✔已实现
**接口路径**: `GET /api/asset/service/{id}/hosts`
- 说明：根据服务 ID 查询该逻辑服务下绑定的所有物理主机，供演练工作流的目标下拉框使用。
- 返回：`List<AssetHost>`

## 4. 系统管理 (System Management)
### 4.1 登录获取令牌
**接口路径**: `POST /api/auth/login`
- 说明：验证用户名和密码，返回 Token。

### 4.2 动态菜单查询
**接口路径**: `GET /api/system/menus`
- 说明：根据角色返回前端侧边栏树结构，Vue 动态生成路由。

### 4.3 角色权限配置
**接口路径**: `POST /api/system/roles/permissions`
- 说明：配置角色可访问的菜单与按钮权限。
