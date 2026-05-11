<template>
  <div class="login-page">
    <!-- 数据大厅背景层 -->
    <div class="bg-layers">
      <!-- 地板网格 -->
      <div class="floor-grid"></div>
      <!-- 扫描线 -->
      <div class="scan-line"></div>
      <!-- 粒子光点 -->
      <div class="particles">
        <span v-for="i in 40" :key="i" class="dot" :style="randomDot(i)"></span>
      </div>
      <!-- 左侧服务器立柱装饰 -->
      <div class="server-pillar pillar-left">
        <div class="led-row" v-for="i in 8" :key="'L'+i"><span class="led" :class="i % 3 === 0 ? 'led-amber' : 'led-cyan'"></span></div>
      </div>
      <div class="server-pillar pillar-right">
        <div class="led-row" v-for="i in 6" :key="'R'+i"><span class="led" :class="i === 1 ? 'led-amber' : 'led-blue'"></span></div>
      </div>
    </div>

    <!-- 主登录卡片 -->
    <div class="login-card">
      <!-- 顶部状态条 -->
      <div class="status-bar">
        <span class="status-dot status-green"></span>
        <span class="status-text">SYS::ONLINE</span>
        <span class="status-spacer"></span>
        <span class="status-dot status-amber"></span>
        <span class="status-text">NODES::15</span>
      </div>

      <!-- 品牌区 -->
      <div class="brand-block">
        <div class="logo-mark">
          <div class="logo-hex">
            <span class="hex-char">双</span>
          </div>
        </div>
        <h1 class="title-cn">智维方舟</h1>
        <p class="title-en">O P S — T W I N</p>
        <div class="divider-line"><span></span></div>
        <p class="tagline">数字孪生 · 运维中枢</p>
      </div>

      <!-- 表单 -->
      <el-form :model="loginForm" class="login-form" @keyup.enter="handleLogin">
        <div class="input-group">
          <label class="input-label">IDENT</label>
          <el-input
            v-model="loginForm.username"
            placeholder="admin"
            :prefix-icon="User"
            class="custom-input"
          />
        </div>
        <div class="input-group">
          <label class="input-label">AUTH</label>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="········"
            :prefix-icon="Lock"
            show-password
            class="custom-input"
          />
        </div>

        <el-button
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          <span class="btn-content">
            <span class="btn-label">{{ loading ? '验证中...' : '初始化连接' }}</span>
            <span class="btn-arrow">→</span>
          </span>
        </el-button>
      </el-form>

      <div class="card-footer">
        <span class="footer-icon">▸</span>
        <span>Chrome 以获得最佳 3D 孪生体验</span>
      </div>
    </div>

    <!-- 右下角版本号 -->
    <div class="version-tag">v2.4 · STAGE 4</div>
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
const userStore = useUserStore()
const loading = ref(false)
const loginForm = ref({ username: 'admin', password: '' })

const randomDot = (i: number) => ({
  left: `${(i * 73 + 17) % 100}%`,
  top: `${(i * 47 + 23) % 100}%`,
  animationDelay: `${(i * 0.7) % 6}s`,
  animationDuration: `${2 + (i % 3)}s`,
})

const handleLogin = async () => {
  if (!loginForm.value.password) return ElMessage.warning('请输入密码')
  loading.value = true
  try {
    const res: any = await request.post('/api/auth/login', loginForm.value)
    localStorage.setItem('token', res.data.token)
    userStore.setUserInfo(res.data.user)
    const roleId = res.data.user?.roleId || 1
    try {
      const permRes: any = await request.get('/api/system/menus', { params: { roleId } })
      if (permRes.code === 200) {
        userStore.setPermissions(permRes.data.permissions || [])
        userStore.setMenus(permRes.data.menus || [])
      }
    } catch { /* 降级 */ }
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
/* ============================================
   智维方舟 · 登录 — 数据大厅 Sanctum 风格
   ============================================ */

/* --- 根背景 --- */
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #060b14;
  overflow: hidden;
  position: relative;
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace;
}

/* --- 背景层 --- */
.bg-layers { position: absolute; inset: 0; pointer-events: none; }

/* 地板网格 — 透视纵深 */
.floor-grid {
  position: absolute; bottom: 0; left: 0; right: 0; height: 55%;
  background:
    linear-gradient(90deg, rgba(0,229,255,0.04) 1px, transparent 1px),
    linear-gradient(0deg,   rgba(0,229,255,0.06) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: linear-gradient(to top, rgba(0,0,0,0.5) 0%, transparent 90%);
  transform: perspective(600px) rotateX(60deg);
  transform-origin: bottom center;
}

/* 扫描线 — 横向缓慢扫过 */
.scan-line {
  position: absolute; top: 0; left: 0; right: 0; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0,229,255,0.25), transparent);
  animation: scanDown 8s linear infinite;
  opacity: 0.5;
}
@keyframes scanDown {
  0%   { top: -2px; }
  100% { top: 100%; }
}

/* 粒子光点 */
.particles { position: absolute; inset: 0; }
.dot {
  position: absolute;
  width: 2px; height: 2px;
  border-radius: 50%;
  background: rgba(0,229,255,0.6);
  animation: dotPulse 3s ease-in-out infinite;
}
@keyframes dotPulse {
  0%, 100% { opacity: 0.2; transform: scale(1); }
  50%      { opacity: 0.9; transform: scale(1.8); }
}

/* 服务器立柱 */
.server-pillar {
  position: absolute; top: 0; bottom: 0; width: 48px;
  display: flex; flex-direction: column; justify-content: center; gap: 18px;
  padding: 40px 0;
}
.pillar-left  { left: 6%; }
.pillar-right { right: 6%; }
.led-row { display: flex; justify-content: center; }
.led {
  display: block; width: 4px; height: 4px; border-radius: 50%;
  animation: ledBlink 2s ease-in-out infinite;
}
.led-cyan  { background: #00e5ff; box-shadow: 0 0 6px #00e5ff; animation-delay: 0s; }
.led-blue  { background: #409eff; box-shadow: 0 0 6px #409eff; animation-delay: 0.8s; }
.led-amber { background: #ffb74d; box-shadow: 0 0 6px #ffb74d; animation-delay: 0.4s; }
@keyframes ledBlink {
  0%, 100% { opacity: 1; }
  30%      { opacity: 0.2; }
  60%      { opacity: 0.8; }
}

/* --- 登录卡片 --- */
.login-card {
  position: relative; z-index: 10;
  width: 420px;
  background: rgba(8, 16, 30, 0.85);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(0, 229, 255, 0.12);
  border-radius: 2px;
  padding: 0;
  box-shadow:
    0 0 80px rgba(0, 117, 255, 0.08),
    0 0 0 1px rgba(0, 229, 255, 0.04) inset;
}

/* 顶部状态条 */
.status-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(0, 229, 255, 0.08);
  font-size: 10px; color: rgba(255,255,255,0.4);
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-green  { background: #00e676; box-shadow: 0 0 6px #00e676; }
.status-amber  { background: #ffb74d; box-shadow: 0 0 6px #ffb74d; }
.status-spacer { flex: 1; }

/* 品牌区 */
.brand-block { text-align: center; padding: 36px 40px 28px; }
.logo-hex {
  width: 56px; height: 56px; margin: 0 auto 16px;
  background: linear-gradient(135deg, #0a1628, #102040);
  border: 1.5px solid rgba(0, 229, 255, 0.3);
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 30px rgba(0, 229, 255, 0.1);
}
.hex-char {
  font-size: 22px; color: #00e5ff; font-weight: 700;
  text-shadow: 0 0 12px rgba(0, 229, 255, 0.5);
}
.title-cn {
  margin: 0; font-size: 26px; font-weight: 700;
  color: #e8f4f8; letter-spacing: 8px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.title-en {
  margin: 6px 0 0; font-size: 11px; letter-spacing: 6px;
  color: rgba(0, 229, 255, 0.5); font-weight: 400;
}
.divider-line {
  display: flex; align-items: center; justify-content: center; margin: 18px 0 10px;
}
.divider-line span {
  display: block; width: 40px; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0,229,255,0.3), transparent);
}
.tagline { margin: 0; font-size: 12px; color: rgba(255,255,255,0.3); letter-spacing: 4px; }

/* 表单 */
.login-form { padding: 0 40px 32px; }
.input-group { margin-bottom: 18px; }
.input-label {
  display: block; font-size: 10px; color: rgba(0,229,255,0.45);
  letter-spacing: 2px; margin-bottom: 6px;
  padding-left: 2px;
}

/* 输入框深度覆盖 */
.custom-input :deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.4) !important;
  box-shadow: none !important;
  border: 1px solid rgba(0, 229, 255, 0.12) !important;
  border-radius: 2px;
  height: 44px;
  transition: border-color 0.3s, box-shadow 0.3s;
}
.custom-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 229, 255, 0.3) !important;
}
.custom-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(0, 229, 255, 0.5) !important;
  box-shadow: 0 0 0 2px rgba(0, 229, 255, 0.06) !important;
}
.custom-input :deep(.el-input__inner) {
  color: #c8dce8 !important;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace !important;
  font-size: 13px;
  letter-spacing: 1px;
}
.custom-input :deep(.el-input__inner::placeholder) {
  color: rgba(255,255,255,0.12) !important;
}
.custom-input :deep(.el-input__prefix) {
  color: rgba(0, 229, 255, 0.3);
}
.custom-input :deep(.el-input__suffix) {
  color: rgba(255,255,255,0.2);
}

/* 登录按钮 */
.login-btn {
  width: 100%; height: 44px; margin-top: 6px;
  background: transparent !important;
  border: 1px solid rgba(0, 229, 255, 0.25) !important;
  border-radius: 2px !important;
  color: #00e5ff !important;
  font-size: 13px !important;
  letter-spacing: 3px !important;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace !important;
  transition: all 0.35s !important;
  position: relative; overflow: hidden;
}
.login-btn::before {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(0,229,255,0), rgba(0,229,255,0.06), rgba(0,229,255,0));
  transform: translateX(-100%);
  transition: transform 0.5s;
}
.login-btn:hover::before { transform: translateX(100%); }
.login-btn:hover {
  border-color: rgba(0, 229, 255, 0.6) !important;
  box-shadow: 0 0 24px rgba(0, 229, 255, 0.15), inset 0 0 24px rgba(0, 229, 255, 0.04) !important;
}
.login-btn:active { transform: scale(0.98); }
.btn-content {
  display: flex; align-items: center; justify-content: center; gap: 10px;
}
.btn-arrow {
  font-size: 14px; transition: transform 0.3s;
}
.login-btn:hover .btn-arrow { transform: translateX(3px); }

/* 底部 */
.card-footer {
  padding: 14px 40px;
  border-top: 1px solid rgba(0, 229, 255, 0.06);
  text-align: center; font-size: 11px; color: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.footer-icon { color: rgba(0,229,255,0.3); }

/* 右下角版本号 */
.version-tag {
  position: absolute; bottom: 24px; right: 28px; z-index: 10;
  font-size: 10px; color: rgba(255,255,255,0.15); letter-spacing: 2px;
}
</style>
