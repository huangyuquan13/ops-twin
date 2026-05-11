# 智维方舟 (Ops-Twin) 日常开发计划 (5-9)

## 当前进度概览
Stage 1-3.5 已全部完成。今天启动 Stage 4：系统管理（安全与审计）。

---

## Stage 4: 系统管理 (安全与审计)

### 现状分析

| 模块 | 当前状态 | 缺失 |
|------|---------|------|
| 用户中心 | `system/user.vue` ✅ 已实现 CRUD + 头像 | - |
| 权限管理 | `system/role.vue` ❌ 占位页 | 角色CRUD + 权限树分配 |
| 操作审计 | `system/audit.vue` ❌ 占位页 | 审计日志列表 + 后端记录 |
| sys_permission | 11 条记录 ❌ 过时 | 缺少机柜/任务总览/方案库/用户中心/审计 |
| sys_role | ❌ 不存在 | 需要新建 |
| sys_role_permission | ❌ 不存在 | 需要新建 |
| audit_event | 表存在 ✅ 无数据 | 后端拦截器自动记录 |

### 今日任务

#### 任务一：数据库补全
- **新增 `sys_role` 表**: id, role_name, role_code, description
- **新增 `sys_role_permission` 表**: id, role_id, permission_id
- **更新 `sys_permission` 种子**: 对齐当前路由（+物理机柜 +任务总览 +预案方案库 +用户中心 +操作审计，-演练编排 -实时终端）

#### 任务二：后端接口
- **角色管理 API**: `sys_role` 的 CRUD
- **角色权限分配 API**: `POST /api/system/role/{id}/permissions`
- **动态菜单**: `GET /api/system/menus` 改为根据角色返回
- **审计日志 API**: `GET /api/audit/list` 分页查询
- **审计拦截器**: 登录、删除、执行预案时自动写入 `audit_event`

#### 任务三：前端页面
- **权限管理页**: 角色列表 + 权限树（el-tree + checkbox）+ 保存
- **操作审计页**: 审计日志表格 + 搜索筛选（操作人/事件类型/时间范围）

### 数据库新增表

```sql
-- 角色表
CREATE TABLE sys_role (
  id bigint NOT NULL AUTO_INCREMENT,
  role_name varchar(50) NOT NULL,
  role_code varchar(50) NOT NULL,
  description varchar(200),
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
);

-- 角色-权限关联表
CREATE TABLE sys_role_permission (
  id bigint NOT NULL AUTO_INCREMENT,
  role_id bigint NOT NULL,
  permission_id bigint NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id)
);
```

### 种子数据

```sql
-- 角色
INSERT INTO sys_role VALUES (1, '超级管理员', 'ADMIN', '拥有所有权限');
INSERT INTO sys_role VALUES (2, '普通用户', 'VIEWER', '只读查看权限');

-- 权限菜单（更新后共14条）
INSERT INTO sys_permission VALUES
(1,0,'看板中心','/dashboard','Layout','Monitor',NULL,0),
(2,1,'3D孪生','/dashboard/index','dashboard/index','Platform',NULL,0),
(3,1,'效能分析','/dashboard/analysis','dashboard/analysis','PieChart',NULL,0),
(4,0,'资产中心','/assets','Layout','Box',NULL,0),
(5,4,'物理机柜','/assets/cabinet','assets/cabinet','Help',NULL,0),
(6,4,'物理资产','/assets/host','assets/host','Cpu',NULL,0),
(7,4,'服务映射','/assets/service','assets/service','Share',NULL,0),
(8,0,'任务中心','/tasks','Layout','Checked',NULL,0),
(9,8,'任务总览','/tasks/index','tasks/index','DataAnalysis',NULL,0),
(10,8,'预案方案库','/tasks/strategy','tasks/strategy','Collection',NULL,0),
(11,8,'实时终端','/tasks/terminal','tasks/terminal','Console',NULL,0),
(12,0,'系统管理','/system','Layout','Setting',NULL,0),
(13,12,'用户中心','/system/user','system/user','User',NULL,0),
(14,12,'权限管理','/system/role','system/role','Lock',NULL,0),
(15,12,'操作审计','/system/audit','system/audit','Document',NULL,0);

-- ADMIN 拥有所有权限（15条）
INSERT INTO sys_role_permission SELECT 1, id FROM sys_permission;
```

### API 接口

| 接口 | 说明 |
|------|------|
| `GET /api/system/role/list` | 角色列表 |
| `POST /api/system/role/save` | 新增/更新角色 |
| `DELETE /api/system/role/{id}` | 删除角色 |
| `GET /api/system/role/{id}/permissions` | 获取角色的权限ID列表 |
| `POST /api/system/role/{id}/permissions` | 保存角色权限 |
| `GET /api/system/menus?roleId=` | 动态菜单（按角色过滤） |
| `GET /api/audit/list` | 审计日志分页查询 |
| (拦截器) | 登录/删除/执行预案 → 自动写 audit_event |

### 成功标准 ✅ 全部完成
1. ✅ 权限管理页：角色列表 + el-tree 勾选菜单/按钮权限 + 保存
2. ✅ 不同角色登录后按钮权限生效（VIEWER 所有操作按钮隐藏）
3. ✅ 操作审计页：分页表格 + 按操作人/事件类型搜索
4. ✅ 执行演练/删除预案自动写入 audit_event

### 完成情况
- 后端新增：3 entity + 3 mapper + 2 service + 2 controller + 2 旧控制器改造
- 前端新增/改造：userStore(permissions) + login(拉取权限) + role.vue + audit.vue + 4 页面按钮权限
- 15 个文件变更，所有文档已同步
