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
    AssetServiceController.java # CRUD /api/asset/service/* + topology + hosts list
    PipelineController.java    # Legacy: /api/pipeline/execute + logs
    SysController.java         # GET /api/system/menus?roleId= + /permissions/all
    UserController.java        # User CRUD + avatar upload
    TaskPlanController.java    # CRUD /api/task/plan/* (+ audit logging)
    TaskRecordController.java  # POST trigger, GET list, GET {id}, POST {id}/terminate (+ audit)
    RoleController.java        # CRUD role + permission assignment (+ audit)
    AuditController.java       # GET /api/audit/list (6 event types) + log write
  entity/                      # MyBatis-Plus @Data entities (13 tables)
  mapper/                      # BaseMapper<T> interfaces (11 mappers)
  service/
    TaskExecutionEngine.java   # @Async step executor with cancel + service validation
    TaskPlanService.java       # IService<TaskPlan> + unique name check
    TaskRecordService.java     # IService<TaskRecord> + triggerAsync + terminate
    SysRoleService.java        # IService<SysRole>
    AuditEventService.java     # IService<AuditEvent>
    impl/                       # ServiceImpl classes (5)
    PipelineService.java       # Legacy @Async simulation
  websocket/
    TaskLogWebSocketHandler.java # Per-recordId session map, broadcast() for log push
```

## Database Tables (13)

| Table | Purpose |
|---|---|
| `sys_user` | User accounts (username, md5 password, avatar, role_id) |
| `sys_role` | Roles (role_name, role_code) — Stage 4 |
| `sys_permission` | Menu/button permission tree (type=1 menu, type=2 button, permission_code) |
| `sys_role_permission` | Role-permission many-to-many — Stage 4 |
| `audit_event` | Security audit log (operator, event_type, detail) — Stage 4 |
| `asset_cabinet` | Physical cabinets (cabinet_id, pos_x, pos_z, max_u) |
| `asset_host` | Physical servers (hostname, ip, status, cpu, memory, host_type, cabinet_id, rack_pos) |
| `asset_service` | Logical service registry (service_name, owner, description, topology_json) |
| `service_host_map` | Service-to-host many-to-many |
| `task_plan` | Drill plan library (plan_name, service_id, plan_type, priority, steps_json, status) |
| `task_record` | Execution records (run_status: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED) |
| `pipeline_task` | Legacy chaos drill tasks (deprecated) |
| `pipeline_log` | Legacy drill log entries (deprecated) |

## Key Flows

### Plan Execution (trigger → engine → WebSocket → terminal)
1. `TaskRecordController.trigger(planId)` — validates plan & service, calls `triggerAsync()`
2. `TaskRecordServiceImpl.triggerAsync()` — creates PENDING record, calls `executionEngine.execute()`
3. `TaskExecutionEngine.execute()` — @Async: validates service/hosts, parses steps_json, runs steps, pushes via WebSocket
4. Frontend terminal connects `ws://localhost:8080/ws/task/log/{recordId}`

### RBAC Permissions
1. Login → `GET /api/system/menus?roleId=` → returns `{ menus, permissions }`
2. Frontend stores in Pinia + localStorage, sidebar renders from `menuSections`
3. Buttons: `v-if="userStore.hasPerm('strategy:add')"`

### Audit Events (6 types)
CREATE_PLAN / UPDATE_PLAN / DELETE_PLAN / EXECUTE_PLAN / UPDATE_ROLE_PERM / OTHER

## Conventions

- `@CrossOrigin(origins = "*")` on every controller
- Every response wrapped in `Result<T>` with code 200/500
- Passwords stored as MD5, default: `123456`
- Login returns mock token — no real JWT
- No tests written yet
- Avatar files saved to `uploads/`
