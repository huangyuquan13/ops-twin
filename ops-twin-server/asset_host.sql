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

 Date: 07/05/2026 11:10:40
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
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物理服务器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of asset_host
-- ----------------------------
INSERT INTO `asset_host` VALUES (1, 'SRV-WEB-01', '192.168.1.101', 1, 16, 64, 'CAB-01', 1, '2026-04-25 19:51:11', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (2, 'SRV-DB-01', '192.168.1.105', 1, 32, 128, 'CAB-02', 1, '2026-04-25 19:51:11', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (4, 'ser-aba-02', '192.168.43.11', 1, 8, 16, 'CAB-04', 1, '2026-04-25 20:02:39', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (5, 'yuquan-app', '192.168.43.191', 1, 8, 16, 'CAB-04', 2, '2026-04-25 20:14:28', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (6, 'SRV-NODE-01', '192.168.10.11', 1, 8, 16, 'CAB-01', 2, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (7, 'SRV-NODE-02', '192.168.10.12', 1, 8, 16, 'CAB-01', 3, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (8, 'SRV-NODE-03', '192.168.10.13', 2, 16, 32, 'CAB-01', 4, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (9, 'SRV-KAFKA-01', '10.0.0.21', 1, 4, 8, 'CAB-03', 1, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (10, 'SRV-KAFKA-02', '10.0.0.22', 1, 4, 8, 'CAB-03', 2, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (11, 'SRV-KAFKA-03', '10.0.0.23', 3, 4, 8, 'CAB-03', 3, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (12, 'SRV-REDIS-01', '172.16.50.1', 1, 8, 32, 'CAB-02', 2, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (13, 'SRV-REDIS-02', '172.16.50.2', 2, 8, 32, 'CAB-02', 3, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (14, 'SRV-LB-01', '192.168.20.1', 1, 2, 4, 'CAB-04', 3, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (15, 'SRV-LB-02', '192.168.20.2', 1, 2, 4, 'CAB-04', 4, '2026-04-25 20:18:51', '2026-05-06 17:16:00');
INSERT INTO `asset_host` VALUES (16, 'HYQ-01', '199.199.25.3', 1, 8, 16, 'HYQ-01', 1, '2026-05-07 10:42:44', '2026-05-07 10:42:44');
INSERT INTO `asset_host` VALUES (17, 'HYQ-01', '200.200.3.25', 1, 8, 16, 'HYQ-01', 9, '2026-05-07 10:43:54', '2026-05-07 10:43:54');
INSERT INTO `asset_host` VALUES (18, 'DB-Master-01', '10.0.1.10', 1, 32, 128, 'CAB-01', 1, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (19, 'DB-Slave-01', '10.0.1.11', 1, 32, 128, 'CAB-01', 2, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (20, 'DB-Master-02', '10.0.1.12', 1, 32, 128, 'CAB-01', 5, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (21, 'DB-Slave-02', '10.0.1.13', 1, 32, 128, 'CAB-01', 6, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (22, 'DB-Cache-01', '10.0.1.20', 1, 16, 256, 'CAB-01', 9, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (23, 'App-Node-01', '10.0.2.10', 1, 16, 64, 'CAB-02', 1, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (24, 'App-Node-02', '10.0.2.11', 1, 16, 64, 'CAB-02', 2, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (25, 'App-Node-03', '10.0.2.12', 2, 16, 64, 'CAB-02', 4, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (26, 'App-Node-04', '10.0.2.13', 1, 16, 64, 'CAB-02', 5, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (27, 'App-Node-05', '10.0.2.14', 1, 16, 64, 'CAB-02', 6, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (28, 'Storage-01', '10.0.3.10', 1, 8, 256, 'CAB-03', 1, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (29, 'Storage-02', '10.0.3.11', 1, 8, 256, 'CAB-03', 2, '2026-05-07 11:10:08', '2026-05-07 11:10:08');
INSERT INTO `asset_host` VALUES (30, 'Backup-01', '10.0.3.20', 0, 4, 512, 'CAB-03', 12, '2026-05-07 11:10:08', '2026-05-07 11:10:08');

SET FOREIGN_KEY_CHECKS = 1;
