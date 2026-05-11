-- Users (password is MD5 of "123456")
INSERT INTO sys_user (username, password, role_id) VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', 1);
INSERT INTO sys_user (username, password, role_id) VALUES ('viewer', 'e10adc3949ba59abbe56e057f20f883e', 2);

-- Roles
INSERT INTO sys_role (id, role_name, role_code) VALUES (1, '超级管理员', 'admin');
INSERT INTO sys_role (id, role_name, role_code) VALUES (2, '普通用户', 'viewer');

-- Permissions
INSERT INTO sys_permission (id, perm_name, perm_code, parent_id, perm_type) VALUES (1, '资产管理', 'assets', 0, 'menu');
INSERT INTO sys_permission (id, perm_name, perm_code, parent_id, perm_type) VALUES (2, '物理资产', 'assets:host', 1, 'menu');

-- Cabinets
INSERT INTO asset_cabinet (id, cabinet_id, cabinet_name, pos_x, pos_z, max_u) VALUES (1, 'CAB-01', '核心机柜', 0.00, 0.00, 10);
