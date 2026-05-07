-- 物理资产表 (CMDB核心)
CREATE TABLE IF NOT EXISTS `asset_host` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `hostname` VARCHAR(100) NOT NULL COMMENT '主机名',
    `ip_addr` VARCHAR(50) NOT NULL COMMENT 'IP地址',
    `status` TINYINT DEFAULT 1 COMMENT '状态(1健康/2报警/3宕机)',
    `cpu_cores` INT DEFAULT 0 COMMENT 'CPU核数',
    `memory_gb` INT DEFAULT 0 COMMENT '内存大小(GB)',
    `cabinet_id` VARCHAR(50) COMMENT '机柜编号',
    `rack_pos` INT COMMENT '机架位置(U位)',
    `pos_x` FLOAT DEFAULT 0 COMMENT '3D坐标X',
    `pos_y` FLOAT DEFAULT 0 COMMENT '3D坐标Y',
    `pos_z` FLOAT DEFAULT 0 COMMENT '3D坐标Z',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理服务器表';

-- 插入一些演示数据
INSERT INTO `asset_host` (hostname, ip_addr, status, cpu_cores, memory_gb, pos_x, pos_y, pos_z) VALUES 
('SRV-WEB-01', '192.168.1.101', 1, 16, 64, -2, 0, 0),
('SRV-DB-01', '192.168.1.105', 1, 32, 128, 0, 0, 0),
('SRV-APP-01', '192.168.1.110', 2, 8, 32, 2, 0, 0);
