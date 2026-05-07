<template>
  <div class="page-container">
    <div class="glass-card">
      <div class="header">
        <h2 class="title">演练编排</h2>
        <div class="status-tag">建设中</div>
      </div>
      <div class="content">
        <div class="placeholder-icon">
          <div class="pulse-circle"></div>
        </div>
        <p class="description">该模块正在高效开发中，敬请期待...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 演练编排组件
</script>

<style scoped>
.page-container {
  padding: 24px;
  height: 100%;
  min-height: calc(100vh - 120px);
  background: radial-gradient(circle at top left, #1a1c2e 0%, #0f101a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.glass-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  animation: fadeIn 0.8s ease-out;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  margin: 0;
  background: linear-gradient(120deg, #409eff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.status-tag {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  border: 1px solid rgba(64, 158, 255, 0.2);
}

.content {
  text-align: center;
}

.placeholder-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.pulse-circle {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #409eff;
  box-shadow: 0 0 0 rgba(64, 158, 255, 0.4);
  animation: pulse 2s infinite;
}

.description {
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  letter-spacing: 1px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 20px rgba(64, 158, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}
</style>