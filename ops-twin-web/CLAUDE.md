# CLAUDE.md — ops-twin-web

Vue 3 frontend for the Ops-Twin digital twin platform.

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | Vue 3.5 (Composition API, `<script setup>`) |
| Language | TypeScript 6.0 |
| Build | Vite 8.0 |
| UI | Element Plus 2.13 |
| State | Pinia 3.0 (manual localStorage persistence) |
| Router | Vue Router 4 (HTML5 history mode) |
| 3D | Three.js 0.184 + Tween.js 0.25 |
| Charts | ECharts 6.0 |
| Flow | @vue-flow/core (topology editing in service + workflow pages) |
| HTTP | Axios 1.15 |

## Commands

```bash
npm run dev      # Vite dev server (default port 5173)
npm run build    # vue-tsc type-check + vite build
npm run preview  # Preview production build
```

## Architecture

```
src/
  api/request.ts        # Axios instance, base URL localhost:8080, token interceptor
  router/index.ts       # Routes with auth guard (localStorage token check)
  store/user.ts         # Pinia user store with manual localStorage sync
  layout/index.vue      # App shell: sidebar + header + <router-view>
  views/
    login/              # Login page
    dashboard/
      index.vue         # 3D digital twin (Three.js, L1/L2/L3 drill-down)
      analysis.vue      # ECharts analytics dashboard
    assets/
      host.vue          # Server CRUD + cabinet/RackU binding (real)
      service.vue       # Logical service mapping via Vue Flow drag-drop topology (real)
    tasks/
      index.vue         # Task hub dashboard (stats + plan list + quick actions) (real)
      strategy.vue      # Drill plan library CRUD + execute trigger (real)
      workflow.vue      # Visual step editor (param-gated via onMounted) (real)
      terminal.vue      # Real-time log console (WebSocket + Xterm dark theme) (real)
    system/
      user.vue          # User CRUD + avatar + button permission control (real)
      role.vue          # Role list + el-tree permission assignment (real)
      audit.vue         # Audit log table + search filter (real)
```

## Key Pages & Interactions

| `tasks/index.vue` | Task hub dashboard: KPI stats cards (total plans/today runs/success rate) + plan list with quick [编排][执行] buttons. [设定方案] button jumps to strategy with auto-open add dialog. |
| `assets/service.vue` | Left: service list. Right: Vue Flow canvas. Drag hosts from right sidebar onto canvas to build logical topology. Edges represent network links. |
| `tasks/strategy.vue` | Plan CRUD. "执行" calls POST trigger → gets recordId → router.push to terminal. "编排" → workflow.vue. |
| `tasks/workflow.vue` | Left: action palette (STOP_NODE, HEALTH_CHECK, etc.). Center: Vue Flow canvas. Drag actions → configure target/waitMs in drawer → save as steps_json. |
| `tasks/terminal.vue` | Reads recordId from query → WebSocket ws://localhost:8080/ws/task/log/{id} → real-time colored log stream. Terminate button (POST /api/task/record/{id}/terminate). |

## Conventions

- All `.vue` files use `<script setup lang="ts">` exclusively
- All UI text is in Chinese
- Dark theme with sci-fi aesthetic for dashboard/terminal; light theme for CRUD pages
- No ESLint/Prettier configured — rely on TypeScript strict flags
- Button-level RBAC: `v-if="userStore.hasPerm('strategy:add')"` on action buttons, permissions loaded at login via `GET /api/system/menus?roleId=`
- API calls go through the shared Axios instance in `api/request.ts`
- Vue Flow pages must import CSS: `@vue-flow/core/dist/style.css` + theme/controls/minimap CSS

## Backend API Contract

All requests go to `http://localhost:8080`. Response format: `{code: 200|500, message, data}`.

Key endpoints: see `docs/03_API_后端接口文档.md`.
