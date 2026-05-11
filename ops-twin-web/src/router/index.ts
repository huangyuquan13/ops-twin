import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes: Array<RouteRecordRaw> = [
    {
        path: '/login',
        component: () => import('@/views/login/index.vue')
    },
    {
        path: '/',
        component: Layout,
        redirect: '/dashboard/index',
        children: [
            // 看板中心
            {
                path: 'dashboard/index',
                component: () => import('@/views/dashboard/index.vue'),
                meta: { title: '3D孪生' }
            },
            {
                path: 'dashboard/analysis',
                component: () => import('@/views/dashboard/analysis.vue'),
                meta: { title: '效能分析' }
            },
            // 资产中心
            {
                path: 'assets/cabinet',
                component: () => import('@/views/assets/cabinet.vue'),
                meta: { title: '物理机柜' }
            },
            {
                path: 'assets/host',
                component: () => import('@/views/assets/host.vue'),
                meta: { title: '物理资产' }
            },
            {
                path: 'assets/service',
                component: () => import('@/views/assets/service.vue'),
                meta: { title: '服务映射' }
            },
            // 任务中心
            {
                path: 'tasks/index',
                component: () => import('@/views/tasks/index.vue'),
                meta: { title: '任务总览' }
            },
            {
                path: 'tasks/strategy',
                component: () => import('@/views/tasks/strategy.vue'),
                meta: { title: '预案方案库' }
            },
            {
                path: 'tasks/workflow',
                component: () => import('@/views/tasks/workflow.vue'),
                meta: { title: '演练编排', hidden: true }
            },
            {
                path: 'tasks/terminal',
                component: () => import('@/views/tasks/terminal.vue'),
                meta: { title: '实时终端', hidden: true }
            },
            // 系统管理
            {
                path: 'system/user',
                component: () => import('@/views/system/user.vue'),
                meta: { title: '用户中心' }
            },
            {
                path: 'system/role',
                component: () => import('@/views/system/role.vue'),
                meta: { title: '权限管理' }
            },
            {
                path: 'system/audit',
                component: () => import('@/views/system/audit.vue'),
                meta: { title: '操作审计' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫
router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token')
    const isLogin = !!(token && token !== 'null' && token !== 'undefined')

    // console.log(`[路由守卫] 目标: ${to.path}, 已登录: ${isLogin}`)

    // 1. 如果去的是登录页
    if (to.path === '/login') {
        if (isLogin) {
            next('/') // 已登录则跳回首页
        } else {
            next()    // 未登录则放行
        }
    }
    // 2. 如果去的是非登录页（受保护页面）
    else {
        if (isLogin) {
            next()    // 已登录则放行
        } else {
            // console.warn('[路由守卫] 未检测到有效 Token，拦截并跳转至登录页')
            next('/login') // 未登录则强制跳转登录
        }
    }
})

export default router
