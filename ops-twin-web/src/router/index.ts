import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import Layout from "@/layout/index.vue";

const routes: Array<RouteRecordRaw> = [
  {
    path: "/login",
    component: () => import("@/views/login/index.vue"),
  },
  {
    path: "/403",
    component: () => import("@/views/error/403.vue"),
  },
  {
    path: "/",
    component: Layout,
    redirect: "/dashboard/index",
    children: [
      // 看板中心
      {
        path: "dashboard/index",
        component: () => import("@/views/dashboard/index.vue"),
        meta: { title: "3D孪生" },
      },
      {
        path: "dashboard/analysis",
        component: () => import("@/views/dashboard/analysis.vue"),
        meta: { title: "效能分析" },
      },
      // 资产中心
      {
        path: "assets/cabinet",
        component: () => import("@/views/assets/cabinet.vue"),
        meta: { title: "物理机柜" },
      },
      {
        path: "assets/host",
        component: () => import("@/views/assets/host.vue"),
        meta: { title: "物理资产" },
      },
      {
        path: "assets/service",
        component: () => import("@/views/assets/service.vue"),
        meta: { title: "服务映射" },
      },
      // 任务中心
      {
        path: "tasks/index",
        component: () => import("@/views/tasks/index.vue"),
        meta: { title: "任务总览" },
      },
      {
        path: "tasks/strategy",
        component: () => import("@/views/tasks/strategy.vue"),
        meta: { title: "预案方案库" },
      },
      {
        path: "tasks/workflow",
        component: () => import("@/views/tasks/workflow.vue"),
        meta: { title: "演练编排", hidden: true, permissionParent: "/tasks/strategy" },
      },
      {
        path: "tasks/terminal",
        component: () => import("@/views/tasks/terminal.vue"),
        meta: { title: "实时终端", hidden: true },
      },
      // 系统管理
      {
        path: "system/user",
        component: () => import("@/views/system/user.vue"),
        meta: { title: "用户中心" },
      },
      {
        path: "system/role",
        component: () => import("@/views/system/role.vue"),
        meta: { title: "权限管理" },
      },
      {
        path: "system/audit",
        component: () => import("@/views/system/audit.vue"),
        meta: { title: "操作审计" },
      },
    ],
  },
];
//创建router实例 使用history方法 将routers添加进来 这里的url就没有#号  createWebHashHistory()的url会有
const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 全局路由守卫 每次路由跳转的时候都会执行
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  //双重否定转布尔值 与运算 全1才为真
  const isLogin = !!(token && token !== "null" && token !== "undefined");

  // 1. 如果去的是登录页
  if (to.path === "/login") {
    if (isLogin) {
      next("/"); // 已登录则跳回首页
    } else {
      next(); // 未登录则放行
    }
    return;
  }

  // 2. 如果未登录 → 拦截
  if (!isLogin) {
    next("/login");
    return;
  }

  // 3. 已登录 → 菜单级权限校验
  // 白名单：根路径只做 redirect、403 不限权限
  const whiteList = ["/", "/403"];
  if (whiteList.includes(to.path)) {
    next();
    return;
  }

  // 从 localStorage 获取该用户能访问的菜单路径列表
  const menus = JSON.parse(localStorage.getItem("userMenus") || "[]");
  const allowedPaths: string[] = menus.map((m: any) => m.path);

  if (allowedPaths.includes(to.path)) {
    next();
    return;
  }

  // 隐藏路由的权限继承：如果当前路径有 permissionParent，且父路径在 menus 中 → 放行
  const parentPath = to.meta?.permissionParent as string | undefined;
  if (parentPath && allowedPaths.includes(parentPath)) {
    next();
    return;
  }

  // 无权限 → 403
  next("/403");
});

export default router;
