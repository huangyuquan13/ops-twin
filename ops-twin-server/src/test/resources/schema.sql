DROP TABLE IF EXISTS service_host_map;
DROP TABLE IF EXISTS task_record;
DROP TABLE IF EXISTS task_plan;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS asset_host;
DROP TABLE IF EXISTS asset_cabinet;
DROP TABLE IF EXISTS asset_service;
DROP TABLE IF EXISTS audit_event;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    role_id INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name VARCHAR(50) NOT NULL,
    perm_code VARCHAR(100) NOT NULL UNIQUE,
    parent_id BIGINT DEFAULT 0,
    perm_type VARCHAR(20) DEFAULT 'menu',
    path VARCHAR(255),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator VARCHAR(50),
    event_type VARCHAR(50),
    detail TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS asset_host (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hostname VARCHAR(100) NOT NULL,
    ip_addr VARCHAR(50) NOT NULL,
    status TINYINT DEFAULT 1,
    cpu_cores INT DEFAULT 0,
    memory_gb INT DEFAULT 0,
    cabinet_id VARCHAR(50),
    rack_pos INT,
    host_type VARCHAR(20) DEFAULT 'APP',
    description VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS asset_cabinet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cabinet_id VARCHAR(50) NOT NULL UNIQUE,
    cabinet_name VARCHAR(100),
    pos_x DECIMAL(10,2) DEFAULT 0.00,
    pos_z DECIMAL(10,2) DEFAULT 0.00,
    max_u INT DEFAULT 42,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS asset_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    owner VARCHAR(50),
    description VARCHAR(255),
    topology_json TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    service_id BIGINT NOT NULL,
    plan_type VARCHAR(30) DEFAULT 'DRILL',
    priority TINYINT DEFAULT 2,
    steps_json TEXT,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,
    create_by VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    plan_name VARCHAR(100),
    service_id BIGINT,
    run_status VARCHAR(20) DEFAULT 'PENDING',
    duration_ms BIGINT,
    operator VARCHAR(50),
    result_msg VARCHAR(500),
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service_host_map (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    host_id BIGINT NOT NULL
);
