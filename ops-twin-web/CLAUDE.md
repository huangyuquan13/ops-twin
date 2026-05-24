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
npx vitest run   # Run 4 component/store tests
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
      index.vue         # 3D digital twin: L1/L2/L3 drill-down, InstancedMesh rendering
                        #   Multi-cabinet L2: bounding box calculation frames ALL affected cabinets
                        #   CSS2D drill host labels: blade-level hostname tags during execution
                        #   Camera flies ONCE at drill start (no per-event jumping)
                        #   LED pulse animation (green→yellow→red), blade box emissive glow
                        #   Floating mini terminal auto-pops + sessionStorage cross-page state merge
                        #   DRILL_REVERT: clears labels, restores visibility, stays at L2 (no L1 jump)
      analysis.vue      # ECharts analytics dashboard (KPI cards + pie + trend)
    assets/
      cabinet.vue       # Cabinet CRUD + button permission control (real)
      host.vue          # Server CRUD + cabinet/RackU binding + button permission (real)
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
| `tasks/strategy.vue` | Plan CRUD. "执行" → safety gate: checks for residual logs → confirm clearscreen → POST trigger → router.push to dashboard with recordId. "重置" shown when status=0 (DRILL/FAILOVER/SCALE all supported). "编排" → workflow.vue. |
| `tasks/workflow.vue` | Left: action palette (STOP_NODE, HEALTH_CHECK, etc.). Center: Vue Flow canvas. Drag actions → configure target/waitMs in drawer → save as steps_json. |
| `tasks/terminal.vue` | Reads recordId from query → WS connect. Every log line saves to sessionStorage in REAL TIME. onMounted restores logs even without recordId. onUnmounted saves. "Back to dashboard" restores 3D L2 + labels + mini terminal via sessionStorage merge. |
| `system/user.vue` | User CRUD + avatar upload. Buttons gated by user:add/edit/delete permissions. |
| `system/role.vue` | Left: role list. Right: el-tree permission tree (menu+button). check-strictly + ensureParents linkage. |
| `system/audit.vue` | Audit log table with operator/eventType search. 6 event types with color tags. |

## Store

`store/user.ts` — Pinia store with localStorage persistence:
- `userInfo` / `permissions[]` / `menus[]` — restored from localStorage on init
- `hasPerm(code)` — check button-level permission
- `menuSections` — computed tree from flat menus for dynamic sidebar rendering
- `setMenus()` / `setPermissions()` — auto-persist to localStorage
- `clearUserInfo()` — wipe all state + localStorage on logout

## Conventions

- All `.vue` files use `<script setup lang="ts">` exclusively
- All UI text is in Chinese
- Dark theme with sci-fi aesthetic for dashboard/terminal; light theme for CRUD pages
- No ESLint/Prettier configured — rely on TypeScript strict flags
- Button-level RBAC: `v-if="userStore.hasPerm('strategy:add')"` on action buttons, permissions loaded at login via `GET /api/system/menus?roleId=`
- API calls go through the shared Axios instance in `api/request.ts`
- Vue Flow pages must import CSS: `@vue-flow/core/dist/style.css` + theme/controls/minimap CSS
- **Dual WebSocket**: terminal log stream (`/ws/task/log/{id}`) + 3D dashboard events (`/ws/dashboard/events`)
- **sessionStorage**: `dashboardState` key — cross-page state (L2 view, camera, thermal, drill hostnames, miniTerm logs). Dashboard and terminal both read/write with merge (`...prev`) to prevent overwrite. Terminal saves logs in real-time per WebSocket message. Restore always attempts L2 + labels even when `active` is false.

## Backend API Contract

All requests go to `http://localhost:8080`. Response format: `{code: 200|500, message, data}`.

Key endpoints: see `docs/03_API_后端接口文档.md`.
