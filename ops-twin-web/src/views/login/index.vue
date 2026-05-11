<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon"></div>
        <h1 class="project-title">智维方舟</h1>
        <p class="project-subtitle">Ops-Twin Digital Twin Platform</p>
      </div>

      <el-form :model="loginForm" class="login-form">
        <el-form-item>
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入管理员账号"
            prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            class="login-btn" 
            :loading="loading"
            @click="handleLogin"
          >
            同步连接 (Login)
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>推荐使用 Chrome 浏览器以获得最佳 3D 体验</span>
      </div>
    </div>
    
    <!-- 背景装饰：流光效果 -->
    <div class="bg-decoration"></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore() // 获取全局用户仓库
const loading = ref(false)
const loginForm = ref({
  username: 'admin',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.value.password) {
    return ElMessage.warning('请输入密码')
  }
  
  loading.value = true
  try {
    const res: any = await request.post('/api/auth/login', loginForm.value)
    localStorage.setItem('token', res.data.token)
    userStore.setUserInfo(res.data.user)

    // 拉取角色对应的权限
    const roleId = res.data.user?.roleId || 1
    try {
      const permRes: any = await request.get('/api/system/menus', { params: { roleId } })
      if (permRes.code === 200) {
        userStore.setPermissions(permRes.data.permissions || [])
        userStore.setMenus(permRes.data.menus || [])
      }
    } catch { /* 降级：权限为空 */ }

    ElMessage.success('身份验证成功，正在同步孪生空间...')
    router.push('/')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at center, #1e1e2f 0%, #0a0a0c 100%);
  overflow: hidden;
  position: relative;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  z-index: 10;
  text-align: center;
}

.login-header {
  margin-bottom: 40px;
}

.logo-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #409eff, #36cfc9);
  margin: 0 auto 20px;
  border-radius: 12px;
  box-shadow: 0 0 20px rgba(64, 158, 255, 0.4);
}

.project-title {
  font-size: 32px;
  color: #fff;
  margin: 0;
  letter-spacing: 4px;
}

.project-subtitle {
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
  margin-top: 8px;
}

.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.1);
  height: 45px;
}

.custom-input :deep(.el-input__inner) {
  color: #fff !important;
}

.login-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  letter-spacing: 2px;
  margin-top: 10px;
  background: linear-gradient(90deg, #409eff, #36cfc9);
  border: none;
}

.login-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.login-footer {
  margin-top: 30px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
}

.bg-decoration {
  position: absolute;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1) 0%, transparent 70%);
  top: -400px;
  right: -400px;
  pointer-events: none;
}
</style>
