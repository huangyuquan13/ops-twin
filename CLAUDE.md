# CLAUDE.md — Ops-Twin (智维方舟)

3D data center digital twin and operations automation platform.

## Project Structure

```
vue3_424/
  ops-twin-web/      # Vue 3 + Three.js frontend
  ops-twin-server/   # Spring Boot + MyBatis-Plus backend
  docs/              # PRD, DB design, API docs, implementation plan
```

## Current Status (as of 2026-05-13)

All 10 stages complete:

| Stage | Name | Status |
|---|---|---|
| 1 | Infrastructure & Dashboard (scaffolding, 3D, analytics) | DONE |
| 2 | Asset Center (cabinet + host CRUD, logical service mapping) | DONE |
| 3 | Task Center (plan library, workflow editor, execution engine, terminal) | DONE |
| 3.5 | Task Center UX (task hub dashboard, param guard, plan linkage) | DONE |
| 4 | System Admin (JWT + bcrypt, RBAC roles, audit logging, button permissions) | DONE |
| 5 | Real Execution Engine (DB status change, service_host_map CRUD, dual WS) | DONE |
| 6 | Drill Linkage (3D live color, floating terminal, DRILL auto-revert) | DONE |
| 7 | Dashboard Enhancement (ECharts KPI, responsive layout) | DONE |
| 8 | AOP Audit Logging (@AuditLog annotation + aspect) | DONE |
| 9 | Docker (Compose: MySQL + Spring Boot + Nginx) | DONE |
| 10 | Documentation (PRD, DB, API, deploy docs, implementation plan) | DONE |

### Key Numbers
- **8 physical cabinets**, 60 hosts across 6 types (WEB/APP/DB/CACHE/LB/MQ)
- **26 drill plans** across 11 logical services
- **15 backend tests** (JUnit + MockMvc + H2) + **4 frontend Vitest tests**
- **Dual WebSocket**: `/ws/task/log/{id}` (terminal) + `/ws/dashboard/events` (3D events)

## Development Workflow

- **Code writing:** Done in Antigravity AI editor
- **Code management (git/GitHub):** Done via Claude Code
- **Network:** TUN mode enabled, no proxy needed for CLI tools

## Testing

```bash
# Backend (15 tests: JUnit 5 + MockMvc + H2 in-memory DB)
cd ops-twin-server && mvn test

# Frontend (4 tests: Vitest)
cd ops-twin-web && npx vitest run
```

## Key Docs

- `docs/01_PRD_项目需求文档.md` — Full product requirements + execution coverage
- `docs/02_DB_数据库图表设计.md` — Database schema (13 tables)
- `docs/03_API_后端接口文档.md` — API endpoint spec (JWT, REST, WebSocket)
- `docs/04_DEPLOY_部署文档.md` — Docker + manual deployment
- `docs/implementation_plan.md` — 10-stage implementation tracker + final summary

## Quick Reference

- Frontend: `cd ops-twin-web && npm run dev` → Vite dev server
- Backend: `cd ops-twin-server && mvn spring-boot:run` → localhost:8080
- Database: MySQL `ops_twin_db` on localhost:3306, user `root`, password `123456`
