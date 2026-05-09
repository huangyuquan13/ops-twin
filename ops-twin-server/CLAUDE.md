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
| Utility | Lombok 1.18.34, Fastjson2 |

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

## Architecture

```
src/main/java/com/ops/twin/
  OpsTwinApplication.java     # @SpringBootApplication @MapperScan @EnableAsync
  common/Result.java           # Unified response wrapper {code, message, data}
  config/
    MybatisPlusConfig.java     # Pagination interceptor for MySQL
    WebConfig.java             # Static resource mapping /uploads/**
    WebSocketConfig.java       # WebSocket endpoint registration (/ws/task/log/*)
  controller/
    AuthController.java        # POST /api/auth/login
    AnalysisController.java    # GET /api/analysis/kpi-stats, asset-distribution
    AssetController.java       # CRUD /api/asset/host/* (cabinet + rack binding)
    AssetCabinetController.java # CRUD /api/asset/cabinet/*
    AssetServiceController.java # CRUD /api/asset/service/* + topology save/load
    PipelineController.java    # Legacy: /api/pipeline/execute + logs
    SysController.java         # GET /api/system/menus
    UserController.java        # User CRUD + avatar upload
    TaskPlanController.java    # CRUD /api/task/plan/* (plan library)
    TaskRecordController.java  # POST trigger/{planId}, GET list, GET {id}, POST {id}/terminate
  entity/                      # MyBatis-Plus @Data entities (10 tables)
  mapper/                      # BaseMapper<T> interfaces (no XML)
  service/
    TaskExecutionEngine.java   # @Async step executor with cancel support (ConcurrentHashMap flag)
    TaskPlanService.java       # IService<TaskPlan> + unique name check
    TaskRecordService.java     # IService<TaskRecord> + triggerAsync + terminate
    impl/TaskPlanServiceImpl.java
    impl/TaskRecordServiceImpl.java
    PipelineService.java       # Legacy @Async simulation
  websocket/
    TaskLogWebSocketHandler.java # Per-recordId session map, broadcast() for log push
```

## Database Tables (10)

| Table | Purpose |
|---|---|
| `sys_user` | User accounts (username, md5 password, avatar, role_id) |
| `sys_permission` | Menu/permission tree (path, component, icon, permission_code) |
| `asset_cabinet` | Physical cabinets (cabinet_id, pos_x, pos_z, max_u) |
| `asset_host` | Physical servers (hostname, ip, status, cpu, memory, cabinet_id, rack_pos) |
| `asset_service` | Logical service registry (service_name, owner, description) |
| `service_host_map` | Service-to-host many-to-many mapping |
| `task_plan` | Drill plan library (plan_name, service_id, plan_type, priority, steps_json, status) |
| `task_record` | Execution records (plan_id, run_status: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED, duration_ms) |
| `pipeline_task` | Legacy chaos drill tasks |
| `pipeline_log` | Legacy drill log entries |
| `audit_event` | Fault event audit trail |

## Key Flows

### Plan Execution (trigger → engine → WebSocket → terminal)
1. `TaskRecordController.trigger(planId)` — validates plan exists & enabled, calls `taskRecordService.triggerAsync()`
2. `TaskRecordServiceImpl.triggerAsync()` — creates PENDING record, calls `executionEngine.execute(recordId, plan)`
3. `TaskExecutionEngine.execute()` — @Async, parses steps_json, loops through steps with Thread.sleep(), pushes logs via `wsHandler.broadcast()`, updates run_status
4. Frontend terminal connects `ws://localhost:8080/ws/task/log/{recordId}` to receive stream

### Task Termination (cancel)
1. Frontend calls `POST /api/task/record/{id}/terminate`
2. `TaskRecordController.terminate()` → `taskRecordService.terminate()` → `executionEngine.cancel(recordId)` sets `cancelFlags[recordId] = true`
3. Engine checks flag before each step, pushes `[WARN] 用户已终止演练任务` then returns

## Conventions

- Controllers use `@CrossOrigin(origins = "*")` (wide-open CORS, no Spring Security)
- Every response wrapped in `Result<T>` with code 200/500
- Passwords stored as MD5, default new user password: `123456`
- Login returns hardcoded mock token — no real JWT implementation
- No tests written yet
- Avatar files saved to `uploads/` directory, served via WebConfig static mapping
