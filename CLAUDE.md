# CLAUDE.md — Ops-Twin (智维方舟)

3D data center digital twin and operations automation platform.

## Project Structure

```
vue3_424/
  ops-twin-web/      # Vue 3 + Three.js frontend
  ops-twin-server/   # Spring Boot + MyBatis-Plus backend
  docs/              # PRD, DB design, API docs, implementation plan
```

## Current Status (as of 2026-05-06)

- **Stage 1-4:** DONE (scaffolding, auth, dashboard, 3D screen)
- **Stage 5:** IN PROGRESS — Logical service topology (Vue Flow drag-and-drop)
- **Stage 6:** NOT STARTED — Automated drill orchestration

## Development Workflow

- **Code writing:** Done in Antigravity AI editor
- **Code management (git/GitHub):** Done via Claude Code
- **Network:** TUN mode enabled, no proxy needed for CLI tools

## Key Docs

- `docs/01_PRD_项目需求文档.md` — Full product requirements
- `docs/02_DB_数据库图表设计.md` — Database schema
- `docs/03_API_后端接口文档.md` — API endpoint spec
- `docs/implementation_plan.md` — Live progress tracker

## Quick Reference

- Frontend: `cd ops-twin-web && npm run dev` → Vite dev server
- Backend: `cd ops-twin-server && mvn spring-boot:run` → localhost:8080
- Database: MySQL `ops_twin_db` on localhost:3306, user `root`, password `123456`
