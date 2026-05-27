<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="240px">
      <div class="logo">OPS-TWIN</div>
      <!-- 默认选中 默认展开 背景黑色 选中的文本颜色蓝色 可以跳转 -->
      <el-menu
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        background-color="#1e1e1e"
        text-color="#fff"
        active-text-color="#409eff"
        router
      >
        <!-- 这里的绑定的index是为了知道路由有什么 当菜单变化也会相应的变化 -->
        <template v-for="section in topSections" :key="section.id">
          <!-- 这里的index是为了找到路由应该展开什么 -->
          <el-sub-menu :index="String(section.id)">
            <template #title
              ><span>{{ section.title }}</span></template
            >
            <!-- 开启了路由跳转 通过index来跳转 显示高亮 -->
            <el-menu-item
              v-for="item in getChildren(section.id)"
              :key="item.id"
              :index="item.path"
              >{{ item.title }}</el-menu-item
            >
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header>
        <div class="header-content">
          <!-- 引入全局用户头像 -->
          <!-- 直接使用 Store 提供的加工好的头像地址 -->
          <el-avatar :size="36" :src="userStore.avatarUrl" class="user-avatar">
            <el-icon :size="18"><UserFilled /></el-icon>
          </el-avatar>

          <div class="user-info">
            <span class="username">{{ userStore.userInfo.username }}</span>
            <!-- 使用计算属性自动判断角色标签 -->
            <el-tag size="small" :type="userStore.isAdmin ? 'danger' : 'info'">
              {{ userStore.isAdmin ? "超级管理员" : "普通用户" }}
            </el-tag>
          </div>

          <el-divider direction="vertical" />
          <el-button link @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>

      <!-- 主要内容区 (二级路由出口) -->
      <el-main>
        <router-view />
        <!-- 关键：子路由的内容会渲染在这里 -->
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { UserFilled } from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { computed } from "vue";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// 顶层菜单（parentId=0 的 section）
//比较函数 根据ab的计算值来判断 负数a在前 正数b在前 0不变
const topSections = computed(() => {
  return (userStore.menuSections[0] || []).sort(
    (a: any, b: any) => (a.sort ?? 0) - (b.sort ?? 0),
  );
});

const getChildren = (parentId: number) => {
  return (userStore.menuSections[parentId] || []).sort(
    (a: any, b: any) => (a.sort ?? 0) - (b.sort ?? 0),
  );
};

// 动态高亮：/tasks/workflow 无独立菜单项 → 回退到 /tasks/strategy
const activeMenu = computed(() => {
  const p = route.path;
  if (p === "/tasks/workflow") return "/tasks/strategy";
  return p;
});

// 路径属于哪个 sub-menu 就展开哪个
const defaultOpeneds = computed(() => {
  const p = route.path;
  // 从 flat menus 中找当前路径对应的 section id
  for (const m of userStore.menus) {
    if (m.path === p && m.parentId && m.parentId > 0) {
      return [String(m.parentId)];
    }
  }
  // fallback：根据路径前缀匹配
  if (p.startsWith("/dashboard")) return ["1"];
  if (p.startsWith("/assets")) return ["2"];
  if (p.startsWith("/tasks")) return ["3"];
  if (p.startsWith("/system")) return ["4"];
  return [];
});

const handleLogout = () => {
  userStore.clearUserInfo();
  router.push("/login");
  ElMessage.success("已安全退出");
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.el-aside {
  background-color: #1e1e1e;
  border-right: 1px solid #333;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #409eff;
  font-weight: bold;
  font-size: 20px;
}
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
