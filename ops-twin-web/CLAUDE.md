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
      host.vue          # Server CRUD (real implementation)
      service.vue       # Service mapping (placeholder)
    tasks/
      workflow/strategy/terminal  # All placeholders
    system/
      user.vue          # User CRUD with avatar upload (real implementation)
      role/audit        # Placeholders
```

## Conventions

- All `.vue` files use `<script setup lang="ts">` exclusively
- All UI text is in Chinese
- Dark theme with sci-fi aesthetic (#020508 base, #00e5ff accent)
- No ESLint/Prettier configured — rely on TypeScript strict flags
- API calls go through the shared Axios instance in `api/request.ts`
- CRUD pages follow a consistent pattern: search card + el-table + el-pagination + dialog form

## Implemented Pages

4 of 10 routes have real implementations:
1. `dashboard/index.vue` — 3D twin (555 lines, Three.js scene with L1/L2/L3 drill-down)
2. `dashboard/analysis.vue` — ECharts charts (KPI cards + line/donut/bar)
3. `assets/host.vue` — Server asset CRUD
4. `system/user.vue` — User management CRUD

The other 6 routes are placeholder "under construction" pages.

## Backend API Contract

All requests go to `http://localhost:8080`. Response format: `{code: 200|500, message, data}`.

Key endpoints: see `docs/03_API_后端接口文档.md`.
