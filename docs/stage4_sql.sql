-- ============================================================
-- Stage 4: 系统管理 - 权限体系 SQL
-- 执行方式：在 ops_twin_db 库中执行本文件
-- ============================================================

-- 1. sys_permission 加 type 字段（1=菜单 2=按钮）
ALTER TABLE `sys_permission` ADD COLUMN `type` int NULL DEFAULT 1 COMMENT '1菜单/2按钮' AFTER `permission_code`;

-- 2. 清空旧数据，插入完整权限树（菜单 + 按钮）
DELETE FROM `sys_permission`;

-- ============ 看板中心 ============
INSERT INTO `sys_permission` VALUES (1, 0, '看板中心', '/dashboard', 'Layout', 'Monitor', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (2, 1, '3D孪生', '/dashboard/index', 'dashboard/index', 'Platform', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (3, 1, '效能分析', '/dashboard/analysis', 'dashboard/analysis', 'PieChart', NULL, 1, 0);

-- ============ 资产中心 ============
INSERT INTO `sys_permission` VALUES (10, 0, '资产中心', '/assets', 'Layout', 'Box', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (11, 10, '物理机柜', '/assets/cabinet', 'assets/cabinet', 'Help', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (12, 10, '物理资产', '/assets/host', 'assets/host', 'Cpu', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (13, 12, '新增主机', NULL, NULL, NULL, 'host:add', 2, 0);
INSERT INTO `sys_permission` VALUES (14, 12, '编辑主机', NULL, NULL, NULL, 'host:edit', 2, 0);
INSERT INTO `sys_permission` VALUES (15, 12, '删除主机', NULL, NULL, NULL, 'host:delete', 2, 0);
INSERT INTO `sys_permission` VALUES (11, 10, '物理机柜', '/assets/cabinet', 'assets/cabinet', 'Help', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (18, 11, '新增机柜', NULL, NULL, NULL, 'cabinet:add', 2, 0);
INSERT INTO `sys_permission` VALUES (19, 11, '编辑机柜', NULL, NULL, NULL, 'cabinet:edit', 2, 0);
INSERT INTO `sys_permission` VALUES (41, 11, '删除机柜', NULL, NULL, NULL, 'cabinet:delete', 2, 0);
INSERT INTO `sys_permission` VALUES (16, 10, '服务映射', '/assets/service', 'assets/service', 'Share', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (17, 16, '保存拓扑', NULL, NULL, NULL, 'service:save', 2, 0);

-- ============ 任务中心 ============
INSERT INTO `sys_permission` VALUES (20, 0, '任务中心', '/tasks', 'Layout', 'Checked', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (21, 20, '任务总览', '/tasks/index', 'tasks/index', 'DataAnalysis', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (22, 20, '预案方案库', '/tasks/strategy', 'tasks/strategy', 'Collection', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (23, 22, '新增预案', NULL, NULL, NULL, 'strategy:add', 2, 0);
INSERT INTO `sys_permission` VALUES (24, 22, '编辑预案', NULL, NULL, NULL, 'strategy:edit', 2, 0);
INSERT INTO `sys_permission` VALUES (25, 22, '删除预案', NULL, NULL, NULL, 'strategy:delete', 2, 0);
INSERT INTO `sys_permission` VALUES (26, 22, '启用禁用', NULL, NULL, NULL, 'strategy:toggle', 2, 0);
INSERT INTO `sys_permission` VALUES (27, 22, '执行预案', NULL, NULL, NULL, 'strategy:execute', 2, 0);
INSERT INTO `sys_permission` VALUES (28, 22, '编排预案', NULL, NULL, NULL, 'strategy:workflow', 2, 0);
INSERT INTO `sys_permission` VALUES (29, 20, '实时终端', '/tasks/terminal', 'tasks/terminal', 'Console', NULL, 1, 0);

-- ============ 系统管理 ============
INSERT INTO `sys_permission` VALUES (30, 0, '系统管理', '/system', 'Layout', 'Setting', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (31, 30, '用户中心', '/system/user', 'system/user', 'User', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (32, 31, '新增用户', NULL, NULL, NULL, 'user:add', 2, 0);
INSERT INTO `sys_permission` VALUES (33, 31, '编辑用户', NULL, NULL, NULL, 'user:edit', 2, 0);
INSERT INTO `sys_permission` VALUES (34, 31, '删除用户', NULL, NULL, NULL, 'user:delete', 2, 0);
INSERT INTO `sys_permission` VALUES (35, 30, '权限管理', '/system/role', 'system/role', 'Lock', NULL, 1, 0);
INSERT INTO `sys_permission` VALUES (36, 35, '管理权限', NULL, NULL, NULL, 'role:manage', 2, 0);
INSERT INTO `sys_permission` VALUES (37, 30, '操作审计', '/system/audit', 'system/audit', 'Document', NULL, 1, 0);

-- ============ 角色表 ============
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `description` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_role` (id, role_name, role_code, description) VALUES (1, '超级管理员', 'ADMIN', '拥有所有权限');
INSERT INTO `sys_role` (id, role_name, role_code, description) VALUES (2, '普通用户', 'VIEWER', '只读查看，不可增删改');

-- ============ 角色-权限关联表 ============
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ADMIN 拥有所有权限
INSERT INTO `sys_role_permission` (role_id, permission_id)
SELECT 1, id FROM `sys_permission`;

-- VIEWER 只拥有菜单，无按钮权限（type=1 的记录）
INSERT INTO `sys_role_permission` (role_id, permission_id)
SELECT 2, id FROM `sys_permission` WHERE type = 1;
