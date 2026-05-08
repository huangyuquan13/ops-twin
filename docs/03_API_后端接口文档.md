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

### 3.2 触发演练执行（待实现）
**接口路径**: `POST /api/task/record/trigger`
- 说明：根据 planId 创建执行流水记录，异步驱动演练步骤，结合 WebSocket 推送日志。

### 3.3 终端实时日志拉取（待实现）
**接口路径**: `WS /ws/task/log/{taskId}`
- 说明：WebSocket 双向通道，前端 Xterm.js 订阅日志流。

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
