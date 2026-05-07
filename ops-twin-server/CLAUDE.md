# CLAUDE.md — ops-twin-server

Spring Boot backend for the Ops-Twin digital twin platform.

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| ORM | MyBatis-Plus 3.5.5 (code-first, no XML mappers) |
| Database | MySQL 8.0.27 (`ops_twin_db`) |
| Build | Maven |
| Utility | Lombok 1.18.34 |

## Commands

```bash
mvn spring-boot:run          # Start on port 8080
mvn clean package            # Build JAR
```

## Database

- Host: `localhost:3306`
- Database: `ops_twin_db`
- User: `root` / Password: `123456`
- Full DDL + seed data: `ops_twin_db.sql`
- Standalone asset table DDL: `src/main/resources/sql/init_asset.sql`

## Architecture

```
src/main/java/com/ops/twin/
  OpsTwinApplication.java     # @SpringBootApplication @MapperScan @EnableAsync
  common/Result.java           # Unified response wrapper {code, message, data}
  config/
    MybatisPlusConfig.java     # Pagination interceptor for MySQL
    WebConfig.java             # Static resource mapping /uploads/**
  controller/
    AuthController.java        # POST /api/auth/login (MD5 password, mock JWT)
    AssetController.java       # CRUD for asset_host (with 3D coord collision check)
    AnalysisController.java    # Dashboard stats (kpi, cpu-trend, distribution, network)
    PipelineController.java    # Chaos drill execution + log polling
    SysController.java         # GET /api/system/menus
    UserController.java        # User CRUD + avatar upload
  entity/                      # MyBatis-Plus @Data entities (5 tables)
  mapper/                      # BaseMapper<T> interfaces (no XML)
  service/
    PipelineService.java       # @Async simulated drill execution
```

## Conventions

- Controllers inject Mappers directly (no Service layer except PipelineService)
- Every response wrapped in `Result<T>` with code 200/500
- `@CrossOrigin` on every controller (wide-open CORS)
- Passwords stored as MD5, default new user password: `123456`
- No Spring Security — login returns hardcoded mock token
- No tests written yet
- Avatar files saved to `uploads/` directory, served via WebConfig static mapping

## Database Tables (7)

| Table | Purpose |
|---|---|
| `sys_user` | User accounts (username, md5 password, avatar, role_id) |
| `sys_permission` | Menu/permission tree (path, component, icon, permission_code) |
| `asset_host` | Physical servers (hostname, ip, status, cpu, memory, cabinet_id, rack_pos, pos_x/y/z) |
| `asset_service` | Service registry |
| `service_host_map` | Service-to-host many-to-many |
| `pipeline_task` | Chaos drill tasks (node_id, type, status) |
| `pipeline_log` | Drill log entries |
| `audit_event` | Fault event audit trail |

## Seed Data

- 15 physical servers across 4 cabinets
- 12 users (1 admin, 11 viewers)
- 11 menu/permission items (2-level tree)
