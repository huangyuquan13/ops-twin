<template>
  <div class="terminal-root">
    <!-- 顶部状态条 -->
    <div class="terminal-topbar">
      <div class="topbar-left">
        <span class="topbar-icon">⚡</span>
        <span class="topbar-title">智维方舟 · 演练实时终端</span>
        <div class="status-badge" :class="statusClass">{{ statusLabel }}</div>
      </div>
      <div class="topbar-right">
        <span class="topbar-meta" v-if="planName">预案：{{ planName }}</span>
        <span class="topbar-meta" v-if="recordId">流水 #{{ recordId }}</span>
        <el-button size="small" :icon="ArrowLeft" @click="goToIndex">总览</el-button>
        <el-button v-if="hasDashboardState" size="small" type="primary" @click="backToDashboard">返回大屏</el-button>
        <el-button size="small" :icon="ArrowLeft" @click="goToStrategy">方案库</el-button>
        <el-button
          v-if="recordId && (runStatus === 'RUNNING' || runStatus === 'PENDING')"
          size="small"
          type="danger"
          :icon="VideoPause"
          :disabled="['SUCCESS','FAILED','CANCELLED'].includes(runStatus)"
          @click="handleTerminate"
        >
          {{ ['SUCCESS','FAILED','CANCELLED'].includes(runStatus) ? '已完成' : '终止命令' }}
        </el-button>
        <el-button size="small" :icon="Delete" @click="clearLogs">清屏</el-button>
      </div>
    </div>

    <!-- 步骤进度条（仅在运行中显示） -->
    <div class="progress-bar-wrap" v-if="(runStatus === 'RUNNING' || runStatus === 'PENDING')">
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
      </div>
      <span class="progress-label">{{ progressPct }}% · 正在执行中...</span>
    </div>

    <!-- 终端主体 -->
    <div class="terminal-body" ref="terminalRef">
      <!-- 欢迎头 -->
      <div class="term-line term-dim" v-if="logs.length === 0 && !isConnected">
        <span>正在等待任务触发...</span>
      </div>
      <div class="term-line term-dim" v-if="logs.length === 0 && isConnected">
        <span>WebSocket 已连接，等待日志流推送...</span>
      </div>

      <!-- 日志行 -->
      <div
        v-for="(line, idx) in logs"
        :key="idx"
        class="term-line"
        :class="getLineClass(line)"
      >
        <span class="term-text" v-html="colorize(line)"></span>
      </div>

      <!-- 光标闪烁（运行中才显示） -->
      <div class="term-line" v-if="runStatus === 'RUNNING' || runStatus === 'PENDING'">
        <span class="cursor-blink">▌</span>
      </div>
    </div>

    <!-- 底部信息条 -->
    <div class="terminal-footer">
      <span class="footer-dot" :class="{ 'dot-active': isConnected }"></span>
      <span class="footer-msg">{{ isConnected ? 'WebSocket 已连接' : 'WebSocket 未连接' }}</span>
      <span class="footer-sep">|</span>
      <span class="footer-msg">共 {{ logs.length }} 条日志</span>
      <span class="footer-sep">|</span>
      <span class="footer-msg">运行时长: {{ elapsedStr }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Delete, VideoPause } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import request from '@/api/request';

const router   = useRouter();
const routeObj = useRoute();

// ============ 路由参数 ============
const recordId = ref<string>(routeObj.query.recordId as string || '');
const planName = ref<string>(routeObj.query.planName as string || '');
const initialStatus = ref<string>(routeObj.query.status as string || '');
const hasDashboardState = ref(!!sessionStorage.getItem('dashboardState'));

// ============ 终端状态 ============
const logs       = ref<string[]>([]);
const runStatus  = ref<string>('PENDING');   // PENDING / RUNNING / SUCCESS / FAILED
const isConnected = ref(false);
const terminalRef = ref<HTMLElement | null>(null);

// ============ 进度模拟 ============
const progressPct = ref(0);
let progressTimer: ReturnType<typeof setInterval> | null = null;

// ============ 计时器 ============
const elapsedMs  = ref(0);
const elapsedStr = computed(() => {
  const s = Math.floor(elapsedMs.value / 1000);
  return s < 60 ? `${s}s` : `${Math.floor(s / 60)}m ${s % 60}s`;
});
let elapsedTimer: ReturnType<typeof setInterval> | null = null;

// ============ WebSocket ============
let ws: WebSocket | null = null;

const statusClass = computed(() => ({
  'badge-pending': runStatus.value === 'PENDING',
  'badge-running': runStatus.value === 'RUNNING',
  'badge-success': runStatus.value === 'SUCCESS',
  'badge-failed':  runStatus.value === 'FAILED',
  'badge-cancelled': runStatus.value === 'CANCELLED',
}));

const statusLabel = computed(() => ({
  PENDING: '⏳ 等待中',
  RUNNING: '🟢 执行中',
  SUCCESS: '✅ 成功',
  FAILED:  '❌ 失败',
  CANCELLED: '⏹ 已终止',
}[runStatus.value] ?? runStatus.value));

// ============ 连接 WebSocket ============
let wsRetryCount = 0;
const MAX_WS_RETRY = 3;

const connectWs = () => {
  if (!recordId.value) return;

  // 拼接 WebSocket 地址（开发环境指向后端 8080）
  const wsUrl = `ws://localhost:8080/ws/task/log/${recordId.value}`;
  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    wsRetryCount = 0;
    isConnected.value = true;
    runStatus.value   = 'RUNNING';
    startProgressSimulation();
    startElapsedTimer();
  };

  ws.onmessage = (event) => {
    // 检测到 SUCCESS 或 FAILED 关键词，更新状态
    const msg: string = event.data;
    if (msg.includes('[SUCCESS]')) {
      runStatus.value = 'SUCCESS';
      stopProgressSimulation(100);
      stopElapsedTimer();
    } else if (msg.includes('[ERROR]')) {
      runStatus.value = 'FAILED';
      stopProgressSimulation(progressPct.value);
      stopElapsedTimer();
    } else if (msg.includes('用户已终止演练任务')) {
      runStatus.value = 'CANCELLED';
      stopProgressSimulation(progressPct.value);
      stopElapsedTimer();
    }
    logs.value.push(msg);
    scrollToBottom();
  };

  ws.onclose = () => {
    isConnected.value = false;
    if (runStatus.value === 'RUNNING' || runStatus.value === 'PENDING') {
      if (wsRetryCount < MAX_WS_RETRY) { wsRetryCount++; setTimeout(connectWs, 3000); }
      else { logs.value.push('[SYSTEM] WebSocket 连接中断，已停止重连，请刷新页面'); }
      pollFinalStatus();
    }
  };

  ws.onerror = (_e) => {
    isConnected.value = false;
    logs.value.push('[SYSTEM] WebSocket 连接错误，请检查后端服务');
  };
};

// ============ 轮询最终状态（WS 断开后兜底）============
const pollFinalStatus = async () => {
  if (!recordId.value) return;
  try {
    const res: any = await request.get(`/api/task/record/${recordId.value}`);
    if (res.code === 200 && res.data) {
      runStatus.value = res.data.runStatus;
      if (runStatus.value === 'SUCCESS') stopProgressSimulation(100);
    }
  } catch (_) {}
};

// ============ 进度条模拟（线性增长到 95%，SUCCESS 后跳到 100%）============
const startProgressSimulation = () => {
  progressPct.value = 0;
  progressTimer = setInterval(() => {
    if (progressPct.value < 95) {
      progressPct.value = Math.min(95, progressPct.value + 0.5);
    }
  }, 200);
};

const stopProgressSimulation = (finalPct: number) => {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null; }
  progressPct.value = finalPct;
};

// ============ 运行时长计时器 ============
const startElapsedTimer = () => {
  elapsedMs.value = 0;
  elapsedTimer = setInterval(() => { elapsedMs.value += 1000; }, 1000);
};

const stopElapsedTimer = () => {
  if (elapsedTimer) { clearInterval(elapsedTimer); elapsedTimer = null; }
};

// ============ 自动滚动到底部 ============
const scrollToBottom = () => {
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight;
    }
  });
};

// ============ 日志着色 ============
const getLineClass = (line: string) => ({
  'line-success': line.includes('[SUCCESS]'),
  'line-error':   line.includes('[ERROR]'),
  'line-warn':    line.includes('[WARN]'),
  'line-info':    line.includes('[INFO]'),
  'line-step':    line.includes('[STEP'),
  'line-sim':     line.includes('[SIM]'),
  'line-system':  line.includes('[SYSTEM]'),
  'line-dim':     line === '',
});

const colorize = (line: string) => {
  // 对 ASCII 框线字符做染色（保留等宽字体美观）
  return line
    .replace(/(\[SUCCESS\])/g, '<span class="hl-success">$1</span>')
    .replace(/(\[ERROR\])/g,   '<span class="hl-error">$1</span>')
    .replace(/(\[WARN\])/g,    '<span class="hl-warn">$1</span>')
    .replace(/(\[INFO\])/g,    '<span class="hl-info">$1</span>')
    .replace(/(\[STEP[^\]]*\])/g, '<span class="hl-step">$1</span>')
    .replace(/(\[SIM\])/g,     '<span class="hl-sim">$1</span>')
    .replace(/(\[SYSTEM\])/g,  '<span class="hl-system">$1</span>');
};

// ============ 清屏 ============
const clearLogs = () => { logs.value = []; sessionStorage.removeItem('dashboardState'); hasDashboardState.value = false; };

// ============ 终止命令 ============
const handleTerminate = async () => {
  try {
    await ElMessageBox.confirm(
      '终止后将无法恢复，确定要终止当前演练任务吗？',
      '确认终止',
      { confirmButtonText: '确定终止', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return; // 用户取消
  }

  try {
    const res: any = await request.post(`/api/task/record/${recordId.value}/terminate`);
    if (res.code === 200) {
      logs.value.push('[SYSTEM] ⏹ 用户已发起终止命令...');
      runStatus.value = 'CANCELLED';
      stopProgressSimulation(progressPct.value);
      stopElapsedTimer();
    }
  } catch {
    // ignore
  }
};

// ============ 返回导航 ============
const goToIndex = () => {
  if (ws) ws.close();
  router.push('/tasks/index');
};
const goToStrategy = () => {
  if (ws) ws.close();
  router.push('/tasks/strategy');
};
const backToDashboard = () => { router.push('/dashboard/index'); };

// ============ 生命周期 ============
onMounted(async () => {
  // 无 recordId 直接访问终端页，不做任何操作
  if (!recordId.value) {
    logs.value.push('[SYSTEM] 未指定任务流水 ID，请从预案方案库执行演练后自动跳转');
    return;
  }

  if (hasDashboardState.value) {
    const saved = sessionStorage.getItem('dashboardState');
    if (saved) {
      try {
        const state = JSON.parse(saved);
        if (state.miniTerm?.logs?.length) {
          logs.value = state.miniTerm.logs;
          nextTick(() => scrollToBottom());
        }
      } catch (_) {}
    }
  }

  // 如果从浮动终端传来已知终态，直接显示
  if (initialStatus.value && ['SUCCESS', 'FAILED', 'CANCELLED'].includes(initialStatus.value)) {
    runStatus.value = initialStatus.value;
    if (initialStatus.value === 'SUCCESS') progressPct.value = 100;
    // 不连 WebSocket，不跑进度条
    return;
  }

  // 先查 API 拿真实状态
  try {
    const res: any = await request.get(`/api/task/record/${recordId.value}`);
    if (res.code === 200 && res.data) {
      const s = res.data.runStatus;
      if (['SUCCESS', 'FAILED', 'CANCELLED'].includes(s)) {
        runStatus.value = s;
        if (s === 'SUCCESS') progressPct.value = 100;
        return;  // 终态，不连 WS
      }
    }
  } catch (_) {}

  // 运行中或未知，正常连 WebSocket
  connectWs();
});

onUnmounted(() => {
  if (ws) ws.close();
  stopProgressSimulation(0);
  stopElapsedTimer();
});
</script>

<style scoped>
/* ========== 根容器 ========== */
.terminal-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0d0d0d;
  color: #c5f37b;
  font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace;
  overflow: hidden;
}

/* ========== 顶部状态条 ========== */
.terminal-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #1a1a1a;
  border-bottom: 1px solid #2a2a2a;
  flex-shrink: 0;
  gap: 12px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.topbar-icon {
  font-size: 18px;
}

.topbar-title {
  font-size: 15px;
  font-weight: 700;
  color: #c5f37b;
  letter-spacing: 1px;
}

.topbar-meta {
  font-size: 12px;
  color: #666;
  background: #222;
  padding: 2px 8px;
  border-radius: 4px;
}

/* ========== 状态徽章 ========== */
.status-badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
}

.badge-pending { color: #faad14; border-color: #faad14; background: rgba(250,173,20,0.1); }
.badge-running { color: #52c41a; border-color: #52c41a; background: rgba(82,196,26,0.1); animation: badgePulse 1.5s infinite; }
.badge-success { color: #c5f37b; border-color: #c5f37b; background: rgba(197,243,123,0.1); }
.badge-failed    { color: #ff4d4f; border-color: #ff4d4f; background: rgba(255,77,79,0.1); }
.badge-cancelled { color: #faad14; border-color: #faad14; background: rgba(250,173,20,0.1); }

@keyframes badgePulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.6; }
}

/* ========== 进度条 ========== */
.progress-bar-wrap {
  padding: 8px 20px;
  background: #111;
  border-bottom: 1px solid #1e1e1e;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.progress-track {
  flex: 1;
  height: 4px;
  background: #2a2a2a;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #52c41a, #c5f37b);
  border-radius: 2px;
  transition: width 0.3s ease;
  box-shadow: 0 0 8px rgba(197, 243, 123, 0.5);
}

.progress-label {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
}

/* ========== 终端主体 ========== */
.terminal-body {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
  background: #0d0d0d;
  line-height: 1.7;
}

/* 自定义滚动条 */
.terminal-body::-webkit-scrollbar { width: 6px; }
.terminal-body::-webkit-scrollbar-track  { background: #0d0d0d; }
.terminal-body::-webkit-scrollbar-thumb  { background: #2a2a2a; border-radius: 3px; }

/* ========== 日志行 ========== */
.term-line {
  display: block;
  font-size: 13px;
  min-height: 22px;
  white-space: pre-wrap;
  word-break: break-all;
}

.term-dim   { color: #444; }
.line-success { color: #c5f37b; }
.line-error   { color: #ff6b6b; }
.line-warn    { color: #ffd666; }
.line-info    { color: #79c5f3; }
.line-step    { color: #e8a838; }
.line-sim     { color: #9e9e9e; }
.line-system  { color: #b39ddb; }
.line-dim     { color: transparent; min-height: 10px; }

/* ========== 内联高亮标签 ========== */
.term-text :deep(.hl-success) { color: #c5f37b; font-weight: 700; }
.term-text :deep(.hl-error)   { color: #ff6b6b; font-weight: 700; }
.term-text :deep(.hl-warn)    { color: #ffd666; font-weight: 700; }
.term-text :deep(.hl-info)    { color: #79c5f3; }
.term-text :deep(.hl-step)    { color: #e8a838; font-weight: 700; }
.term-text :deep(.hl-sim)     { color: #7ab5d0; }
.term-text :deep(.hl-system)  { color: #b39ddb; }

/* ========== 光标 ========== */
.cursor-blink {
  color: #c5f37b;
  animation: cursorBlink 1s infinite;
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0; }
}

/* ========== 底部信息条 ========== */
.terminal-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 20px;
  background: #111;
  border-top: 1px solid #1e1e1e;
  font-size: 11px;
  color: #555;
  flex-shrink: 0;
}

.footer-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #444;
  display: inline-block;
}

.footer-dot.dot-active {
  background: #52c41a;
  box-shadow: 0 0 6px #52c41a;
}

.footer-sep { color: #333; }
.footer-msg { color: #555; }

/* ========== Element Plus 按钮在暗黑背景覆盖 ========== */
:deep(.el-button) {
  background: #1e1e1e !important;
  border-color: #333 !important;
  color: #aaa !important;
  font-size: 12px;
}
:deep(.el-button:hover) {
  background: #2a2a2a !important;
  border-color: #555 !important;
  color: #c5f37b !important;
}
</style>
