/*
 Navicat Premium Dump SQL

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80027 (8.0.27)
 Source Host           : localhost:3306
 Source Schema         : ops_twin_db

 Target Server Type    : MySQL
 Target Server Version : 80027 (8.0.27)
 File Encoding         : 65001

 Date: 06/05/2026 17:34:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_host
-- ----------------------------
DROP TABLE IF EXISTS `asset_host`;
CREATE TABLE `asset_host`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hostname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机名',
  `ip_addr` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'IP地址',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(1健康/2报警/3宕机)',
  `cpu_cores` int NULL DEFAULT 0 COMMENT 'CPU核数',
  `memory_gb` int NULL DEFAULT 0 COMMENT '内存大小(GB)',
  `cabinet_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '机柜编号',
  `rack_pos` int NULL DEFAULT NULL COMMENT '机架位置(U位)',
  `pos_x` float NULL DEFAULT 0 COMMENT '3D坐标X',
  `pos_y` float NULL DEFAULT 0 COMMENT '3D坐标Y',
  `pos_z` float NULL DEFAULT 0 COMMENT '3D坐标Z',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物理服务器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_host
-- ----------------------------
INSERT INTO `asset_host` VALUES (1, 'SRV-WEB-01', '192.168.1.101', 1, 16, 64, 'CAB-01', 1, -5, 0, 0, '2026-04-25 19:51:11', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (2, 'SRV-DB-01', '192.168.1.105', 1, 32, 128, 'CAB-02', 1, 0, 0, 0, '2026-04-25 19:51:11', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (4, 'ser-aba-02', '192.168.43.11', 1, 8, 16, 'CAB-04', 1, 0, 0, 5, '2026-04-25 20:02:39', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (5, 'yuquan-app', '192.168.43.191', 1, 8, 16, 'CAB-04', 2, 0, 0, 5, '2026-04-25 20:14:28', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (6, 'SRV-NODE-01', '192.168.10.11', 1, 8, 16, 'CAB-01', 2, -5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (7, 'SRV-NODE-02', '192.168.10.12', 1, 8, 16, 'CAB-01', 3, -5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (8, 'SRV-NODE-03', '192.168.10.13', 2, 16, 32, 'CAB-01', 4, -5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (9, 'SRV-KAFKA-01', '10.0.0.21', 1, 4, 8, 'CAB-03', 1, 5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (10, 'SRV-KAFKA-02', '10.0.0.22', 1, 4, 8, 'CAB-03', 2, 5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (11, 'SRV-KAFKA-03', '10.0.0.23', 3, 4, 8, 'CAB-03', 3, 5, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (12, 'SRV-REDIS-01', '172.16.50.1', 1, 8, 32, 'CAB-02', 2, 0, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (13, 'SRV-REDIS-02', '172.16.50.2', 2, 8, 32, 'CAB-02', 3, 0, 0, 0, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (14, 'SRV-LB-01', '192.168.20.1', 1, 2, 4, 'CAB-04', 3, 0, 0, 5, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (15, 'SRV-LB-02', '192.168.20.2', 1, 2, 4, 'CAB-04', 4, 0, 0, 5, '2026-04-25 20:18:51', '2026-05-06 17:16:00');

-- ----------------------------
-- Table structure for asset_service
-- ----------------------------
DROP TABLE IF EXISTS `asset_service`;
CREATE TABLE `asset_service`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务名: 如 支付网关',
  `owner` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_service
-- ----------------------------

-- ----------------------------
-- Table structure for audit_event
-- ----------------------------
DROP TABLE IF EXISTS `audit_event`;
CREATE TABLE `audit_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_event
-- ----------------------------

-- ----------------------------
-- Table structure for pipeline_log
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_log`;
CREATE TABLE `pipeline_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NULL DEFAULT NULL,
  `log_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pipeline_log
-- ----------------------------

-- ----------------------------
-- Table structure for pipeline_task
-- ----------------------------
DROP TABLE IF EXISTS `pipeline_task`;
CREATE TABLE `pipeline_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NULL DEFAULT NULL,
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'INJECT, HEAL',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pipeline_task
-- ----------------------------

-- ----------------------------
-- Table structure for service_host_map
-- ----------------------------
DROP TABLE IF EXISTS `service_host_map`;
CREATE TABLE `service_host_map`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_id` bigint NULL DEFAULT NULL,
  `host_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of service_host_map
-- ----------------------------

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NULL DEFAULT 0,
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单名称',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由路径',
  `component` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `permission_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '按钮权限标识',
  `sort` int NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, 0, '看板中心', '/dashboard', 'Layout', 'Monitor', NULL, 0);
INSERT INTO `sys_permission` VALUES (2, 1, '3D孪生', '/dashboard/index', 'dashboard/index', 'Platform', NULL, 0);
INSERT INTO `sys_permission` VALUES (3, 1, '效能分析', '/dashboard/analysis', 'dashboard/analysis', 'PieChart', NULL, 0);
INSERT INTO `sys_permission` VALUES (4, 0, '资产中心', '/assets', 'Layout', 'Box', NULL, 0);
INSERT INTO `sys_permission` VALUES (5, 4, '物理资产', '/assets/host', 'assets/host', 'Cpu', NULL, 0);
INSERT INTO `sys_permission` VALUES (6, 4, '服务映射', '/assets/service', 'assets/service', 'Share', NULL, 0);
INSERT INTO `sys_permission` VALUES (7, 0, '任务中心', '/tasks', 'Layout', 'Checked', NULL, 0);
INSERT INTO `sys_permission` VALUES (8, 7, '演练编排', '/tasks/workflow', 'tasks/workflow', 'Operation', NULL, 0);
INSERT INTO `sys_permission` VALUES (9, 7, '实时终端', '/tasks/terminal', 'tasks/terminal', 'Console', NULL, 0);
INSERT INTO `sys_permission` VALUES (10, 0, '系统管理', '/system', 'Layout', 'Setting', NULL, 0);
INSERT INTO `sys_permission` VALUES (11, 10, '权限管理', '/system/role', 'system/role', 'Lock', NULL, 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role_id` int NULL DEFAULT 1 COMMENT '1:Admin, 2:Viewer',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/6494747b-6c92-405d-807a-2672848c00f8.png', 1, '2026-04-25 19:09:43');
INSERT INTO `sys_user` VALUES (3, 'yq', '81dc9bdb52d04dc20036dbd8313ed055', '/uploads/2dc0b232-f379-4f04-bcd3-4d7523e21225.jpg', 2, '2026-04-26 14:12:03');
INSERT INTO `sys_user` VALUES (4, 'qu', 'e10adc3949ba59abbe56e057f20f883e', '', 2, '2026-04-26 14:12:30');
INSERT INTO `sys_user` VALUES (5, 'dev_user', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (6, 'test_user', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (7, 'ops_manager', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 1, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (8, 'guest_01', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (9, 'guest_02', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (10, 'security_auditor', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (11, 'architect', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 1, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (12, 'data_engineer', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (13, 'front_end_lead', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');
INSERT INTO `sys_user` VALUES (14, 'back_end_dev', 'e10adc3949ba59abbe56e057f20f883e', '/uploads/default.png', 2, '2026-04-26 14:14:28');

SET FOREIGN_KEY_CHECKS = 1;
