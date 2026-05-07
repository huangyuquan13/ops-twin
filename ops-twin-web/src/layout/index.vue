<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="240px">
      <div class="logo">OPS-TWIN</div>
              <el-menu
        default-active="/dashboard/index"
        background-color="#1e1e1e"
        text-color="#fff"
        active-text-color="#409eff"
        router
      >
        <!-- 看板中心 -->
        <el-sub-menu index="1">
          <template #title><span>看板中心</span></template>
          <el-menu-item index="/dashboard/index">3D孪生大屏</el-menu-item>
          <el-menu-item index="/dashboard/analysis">效能大盘分析</el-menu-item>
        </el-sub-menu>

        <!-- 资产中心 -->
        <el-sub-menu index="2">
          <template #title><span>资产中心</span></template>
          <el-menu-item index="/assets/cabinet">物理机柜台账</el-menu-item>
          <el-menu-item index="/assets/host">物理服务器台账</el-menu-item>
          <el-menu-item index="/assets/service">逻辑服务映射</el-menu-item>
        </el-sub-menu>

        <!-- 任务中心 -->
        <el-sub-menu index="3">
          <template #title><span>任务中心</span></template>
          <el-menu-item index="/tasks/workflow">演练工作流</el-menu-item>
          <el-menu-item index="/tasks/strategy">预案方案库</el-menu-item>
          <el-menu-item index="/tasks/terminal">实时监控终端</el-menu-item>
        </el-sub-menu>

        <!-- 系统管理 -->
        <el-sub-menu index="4">
          <template #title><span>系统管理</span></template>
          <el-menu-item index="/system/user">用户中心</el-menu-item>
          <el-menu-item index="/system/role">权限配置</el-menu-item>
          <el-menu-item index="/system/audit">操作审计</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header>
        <div class="header-content">
          <!-- 引入全局用户头像 -->
          <!-- 直接使用 Store 提供的加工好的头像地址 -->
          <el-avatar 
            :size="36" 
            :src="userStore.avatarUrl" 
            class="user-avatar"
          />
          
          <div class="user-info">
            <span class="username">{{ userStore.userInfo.username }}</span>
            <!-- 使用计算属性自动判断角色标签 -->
            <el-tag size="small" :type="userStore.isAdmin ? 'danger' : 'info'">
              {{ userStore.isAdmin ? '超级管理员' : '普通用户' }}
            </el-tag>
          </div>
          
          <el-divider direction="vertical" />
          <el-button link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 主要内容区 (二级路由出口) -->
      <el-main>
        <router-view /> <!-- 关键：子路由的内容会渲染在这里 -->
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { onMounted } from 'vue'

const router = useRouter()
const userStore = useUserStore()

// 我们可以直接在模板中使用 userStore.avatarUrl 了，无需在每个组件里写一遍拼接逻辑

// Layout 的代码变得极其干净，因为脏活都交给 userStore 内部去做了
const handleLogout = () => {
  userStore.clearUserInfo() // 只需要这一句话，清理状态、删 Token、删本地硬盘，一条龙服务！
  router.push('/login')
  ElMessage.success('已安全退出')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.el-aside { background-color: #1e1e1e; border-right: 1px solid #333; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #409eff; font-weight: bold; font-size: 20px; }
.el-header { 
  border-bottom: 1px solid #eee; 
  display: flex; 
  align-items: center; 
  justify-content: flex-end; 
}
.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  line-height: 1.2;
}
.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}
</style>
