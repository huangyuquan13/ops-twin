# CLAUDE.md — ops-twin-server

Spring Boot backend for the Ops-Twin digital twin platform.

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| ORM | MyBatis-Plus 3.5.5 (code-first, no XML mappers) |
| Database | MySQL 8.0.27 (`ops_twin_db`) |
| Auth | jjwt 0.12.5 (real JWT Bearer Token) + bcrypt password hashing |
| CORS | WebConfig.java centralized (no @CrossOrigin on controllers) |
| Audit | Custom @AuditLog annotation + AuditLogAspect AOP |
| Test | JUnit 5 + MockMvc + H2 in-memory database |
| Build | Maven |
| Utility | Lombok 1.18.34, Fastjson2 |

## Commands

```bash
mvn spring-boot:run          # Start on port 8080
mvn clean package            # Build JAR
mvn test                     # Run 15 integration tests (JUnit 5 + MockMvc + H2)
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
    WebConfig.java             # CORS centralized (allowed origins/methods/headers) + static resource /uploads/**
    WebSocketConfig.java       # Dual WebSocket registration (/ws/task/log/* + /ws/dashboard/events)
  controller/
    AuthController.java        # POST /api/auth/login (JWT + bcrypt, auto MD5→bcrypt migration)
    AnalysisController.java    # GET /api/analysis/kpi-stats, asset-distribution
    AssetController.java       # CRUD /api/asset/host/* (cabinet + rack binding)
    AssetCabinetController.java # CRUD /api/asset/cabinet/*
    AssetServiceController.java # CRUD /api/asset/service/* + topology + hosts list
    TaskPlanController.java    # CRUD /api/task/plan/* + reset/{planId} (+ audit logging)
    TaskRecordController.java  # POST trigger, GET list, GET {id}, POST {id}/terminate (+ audit)
    SysController.java         # GET /api/system/menus?roleId= + /permissions/all
    UserController.java        # User CRUD + avatar upload
    RoleController.java        # CRUD role + permission assignment (+ audit)
    AuditController.java       # GET /api/audit/list (6 event types) + log write
  entity/                      # MyBatis-Plus @Data entities (13 tables)
  mapper/                      # BaseMapper<T> interfaces (11 mappers)
  service/
    TaskExecutionEngine.java   # @Async real execution engine:
                               #   - Queries asset_host by hostname, changes host status (1/2/3)
                               #   - DRILL: keeps host state after execution (no auto-revert), plan disables
                               #   - FAILOVER/SCALE: persist changes + migrate service_host_map, plan disables
                               #   - ALL types: auto-disable plan (status=0) after execution, manual reset required
                               #   - Validates service existence + host count before running
                               #   - Supports cancel signal (CANCELLED state, reverts partial DRILL changes)
                               #   - Broadcasts 3D events to /ws/dashboard/events
    StaleTaskCleanup.java      # @EventListener(ApplicationReadyEvent) — cancels stale RUNNING/PENDING
                               #   task records on startup (JVM restart kills async threads)
    TaskPlanService.java       # IService<TaskPlan> + unique name check + reset logic
    TaskRecordService.java     # IService<TaskRecord> + triggerAsync + terminate
    SysRoleService.java        # IService<SysRole>
    AuditEventService.java     # IService<AuditEvent>
    impl/                       # ServiceImpl classes (5)
  websocket/
    TaskLogWebSocketHandler.java # /ws/task/log/{recordId} — per-recordId session map, broadcast()
    DashboardWebSocketHandler.java # /ws/dashboard/events — broadcasts HOST_STATUS_CHANGE JSON to 3D
  aspect/
    AuditLogAspect.java        # @Around advice: intercepts @AuditLog methods, extracts operator
                               # from JWT Authorization header, records SUCCESS/FAILED to audit_event
  annotation/
    AuditLog.java              # @AuditLog(value="SAVE_PLAN") — method-level audit marker
```

## Database Tables (13)

| Table | Purpose |
|---|---|
| `sys_user` | User accounts (username, bcrypt password, avatar, role_id). Old MD5 passwords auto-migrate to bcrypt on login. |
| `sys_role` | Roles (role_name, role_code) — Stage 4 |
| `sys_permission` | Menu/button permission tree (type=1 menu, type=2 button, permission_code) |
| `sys_role_permission` | Role-permission many-to-many — Stage 4 |
| `audit_event` | Security audit log (operator, event_type, detail). Auto-populated by @AuditLog AOP — no manual insert needed. |
| `asset_cabinet` | Physical cabinets (cabinet_id, pos_x, pos_z, max_u) |
| `asset_host` | Physical servers (hostname, ip, status, cpu, memory, host_type, cabinet_id, rack_pos). status field (1=healthy/2=warning/3=down) dynamically changed by TaskExecutionEngine during drills. |
| `asset_service` | Logical service registry (service_name, owner, description, topology_json) |
| `service_host_map` | Service-to-host many-to-many. Dynamically modified by FAILOVER (migrate bindings to standby) and SCALE (add/remove hosts). |
| `task_plan` | Drill plan library (plan_name, service_id, plan_type, priority, steps_json, status) |
| `task_record` | Execution records (run_status: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED) |
| `pipeline_task` | Legacy chaos drill tasks (deprecated) |
| `pipeline_log` | Legacy drill log entries (deprecated) |

## Key Flows

### Plan Execution (trigger → engine → WebSocket → terminal + 3D)
1. `TaskRecordController.trigger(planId)` — validates plan is enabled (status=1) + checks no RUNNING/PENDING tasks exist → calls `triggerAsync()`
2. `TaskRecordServiceImpl.triggerAsync()` — creates PENDING record, calls `executionEngine.execute()`
3. `TaskExecutionEngine.execute()` — @Async: validates service/hosts, parses steps_json, runs steps with configurable `waitMs` delay:
   - Queries `asset_host` by hostname, changes status (1→2→3)
   - Pushes logs to `/ws/task/log/{recordId}`
   - Broadcasts 3D events to `/ws/dashboard/events` (HOST_STATUS_CHANGE JSON)
4. **DRILL**: host state preserved (no auto-revert), plan auto-disabled (status=0). Manual reset restores hosts to healthy + re-enables plan
5. **FAILOVER**: changes persist; service_host_map bindings migrated; plan auto-disabled (status=0)
6. **SCALE**: service_host_map bindings added/removed; plan auto-disabled (status=0)
7. **Reset**: `POST /api/task/plan/reset/{planId}` — DRILL: restores affected hosts to status=1 + enables plan. FAILOVER/SCALE: rebuilds service_host_map from topology + restores hosts + enables plan
8. **Startup Cleanup**: `StaleTaskCleanup` cancels all RUNNING/PENDING records on boot (threads died with JVM)

### JWT Authentication
1. `POST /api/auth/login` — validates bcrypt password, returns `{ token, username, roles }`
2. Old MD5 passwords auto-upgraded to bcrypt on first successful login
3. All other endpoints require `Authorization: Bearer <token>` header
4. CORS handled centrally by `WebConfig.java` — no per-controller `@CrossOrigin`

### RBAC Permissions
1. Login → `GET /api/system/menus?roleId=` → returns `{ menus, permissions }`
2. Frontend stores in Pinia + localStorage, sidebar renders from `menuSections`
3. Buttons: `v-if="userStore.hasPerm('strategy:add')"`

### Audit Events (6 types, auto-recorded by AOP)
CREATE_PLAN / UPDATE_PLAN / DELETE_PLAN / EXECUTE_PLAN / UPDATE_ROLE_PERM / OTHER
- `@AuditLog` annotation on Controller methods → `AuditLogAspect` around-advice auto-extracts operator from JWT, records result (SUCCESS/FAILED) to `audit_event` table

## Conventions

- CORS centrally configured in `WebConfig.java` — no `@CrossOrigin` on individual controllers
- Every response wrapped in `Result<T>` with code 200/500
- Passwords stored as **bcrypt** hash; old MD5 passwords auto-migrate to bcrypt on login
- Login returns real JWT (jjwt 0.12.5) — not mock
- 15 integration tests: JUnit 5 + MockMvc + H2 in-memory database
- Avatar files saved to `uploads/`
- `@AuditLog` annotation on write operations — AOP aspect auto-records to `audit_event` table
