# 后端 Spring Boot 接口文档

请保持良好的前后端分离修养，所有的接口均返回统一结构：
```json
{
  "code": 200,      // 业务状态码
  "message": "OK",  // 提示信息
  "data": ...       // 承载的数据主体
}
```

## 1. 认证与菜单模块
### 1.1 登录获取令牌
**接口路径**: `POST /api/auth/login`
- 说明：验证用户名和密码。
- Request Body: `{ "username": "admin", "password": "123" }`
- Response Data: `{ "token": "xxxxxx", "roleId": 1 }`

### 1.2 动态菜单查询
**接口路径**: `GET /api/system/menus`
- 说明：根据角色返回前端侧边栏树结构，Vue 动态生成路由。

## 2. 资产管理模块 (CRUD 深度)
### 2.1 节点列表查询 (分页)
**接口路径**: `GET /api/asset/nodes/list`
- 说明：用于资产管理表格展示。
- Params: `page, size, hostname`

### 2.2 节点坐标/信息更新
**接口路径**: `PUT /api/asset/nodes/{id}`
- 说明：关键！这里更新了坐标，3D大屏会实时反应。
- Request Body: `{ "pos_x": 1.2, "pos_z": 5.4, "status": 1 ... }`

## 3. 3D 大屏专属模块
### 3.1 拉取所有 3D 节点相对信息
**接口路径**: `GET /api/asset/nodes/scence`
- 说明：专门给 Three.js 用的轻量化同步接口。

## 4. 流水线调度中心
### 4.1 任务列表查询
**接口路径**: `GET /api/pipeline/tasks`
- 说明：展示所有历史执行过的运维动作。

### 4.2 触发动作指令
**接口路径**: `POST /api/pipeline/execute`
- 说明：下发指令。

### 4.3 终端实时日志拉取
**接口路径**: `GET /api/pipeline/logs/{taskId}`
- 说明：前端控制台不断轮训此口获取增量日志。

## 5. 权限管理模块
### 5.1 角色权限配置
**接口路径**: `POST /api/system/roles/permissions`
- 说明：配置哪些角色能点哪些按钮的功能（权限分离核心）。

## 6. 效能大盘模块 (大屏看板)
### 6.1 核心 KPI 聚合汇总
**接口路径**: `GET /api/analysis/kpi-stats`
- 说明：计算资产总数、报警数、物理 CPU 总算力及内存总容量。

### 6.2 机柜物理资产分布
**接口路径**: `GET /api/analysis/asset-distribution`
- 说明：按 cabinet_id 聚合计算每个物理机柜的资产占比，渲染大盘饼图。
