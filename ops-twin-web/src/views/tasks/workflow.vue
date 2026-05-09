<template>
  <div class="workflow-container">
    <!-- 顶部工具条 -->
    <div class="workflow-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle @click="handleBack" />
        <h3 class="title">🛠 演练编排工作站 · {{ planName || '未命名预案' }}</h3>
        <el-tag :type="isDirty ? 'warning' : 'success'" size="small" effect="dark" class="status-tag">
          {{ isDirty ? '● 未保存' : '✓ 已保存' }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button plain @click="clearAll">清空画布</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存编排</el-button>
      </div>
    </div>

    <div class="workflow-content">
      <!-- 左侧动作面板 -->
      <div class="node-panel">
        <div class="panel-title">📦 动作库</div>
        <div class="panel-subtitle">将动作拖至右侧画布</div>
        <div class="node-list">
          <div
            v-for="item in availableActions"
            :key="item.type"
            class="dnd-node"
            draggable="true"
            @dragstart="onDragStart($event, item.type)"
          >
            <el-icon :class="'icon-' + item.color"><component :is="item.icon" /></el-icon>
            <div class="dnd-node-info">
              <div class="dnd-node-label">{{ item.label }}</div>
              <div class="dnd-node-desc">{{ item.desc }}</div>
            </div>
          </div>
        </div>
        <div class="panel-tip">
          <span class="tip-content">💡 节点按顺序(从上到下)执行</span>
        </div>
      </div>

      <!-- 右侧画布：必须 relative -->
      <div class="canvas-area" ref="canvasRef" @drop="onDrop" @dragover.prevent>
        <VueFlow
          v-model="nodes"
          v-model:edges="edges"
          :default-viewport="{ x: 50, y: 80, zoom: 0.85 }"
          :fit-view-on-init="true"
          @connect="onConnect"
          @node-click="onNodeClick"
          @edge-double-click="onEdgeDoubleClick"
          @node-drag-stop="markDirty"
        >
          <Background pattern-color="#2a2a3e" :gap="20" />
          <Controls position="bottom-right" />
          <MiniMap position="bottom-left" node-color="#409eff" />
        </VueFlow>

        <div v-if="nodes.length === 0" class="empty-hint">
          <div class="empty-icon">⬅</div>
          <div>从左侧拖入动作节点，开始设计你的演练流程</div>
        </div>
      </div>
    </div>

    <!-- 抽屉略 ... -->
    <el-drawer v-model="drawerVisible" title="⚙️ 节点参数配置" size="340px">
      <div v-if="selectedNode" class="config-form">
        <el-alert :title="getActionMeta(selectedNode.data.action).label" type="info" :closable="false" style="margin-bottom: 20px" />
        <el-form label-position="top">
          <el-form-item label="目标对象 (Target)">
            <el-input v-model="selectedNode.data.target" @input="onConfigChange" />
          </el-form-item>
          <el-form-item label="模拟耗时 (毫秒)">
            <el-input-number v-model="selectedNode.data.waitMs" :step="500" @change="onConfigChange" style="width: 100%"/>
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button type="danger" plain @click="removeNode(selectedNode.id)">🗑 删除节点</el-button>
          <el-button type="primary" @click="drawerVisible = false">✓ 确认</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Check, VideoPlay, Refresh, Monitor, Bell, Timer, Switch } from '@element-plus/icons-vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { MiniMap } from '@vue-flow/minimap';
import request from '@/api/request';

// 重要：必须引入 CSS 否则连线和小窗口会乱掉
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';
import '@vue-flow/minimap/dist/style.css';

const route = useRoute();
const router = useRouter();
const canvasRef = ref<HTMLElement>();
const { project } = useVueFlow();

// ============ 状态 ============
const planId   = ref(route.query.id as string);
const planName = ref((route.query.name as string) || '');
const nodes    = ref<any[]>([]);
const edges    = ref<any[]>([]);
const drawerVisible = ref(false);
const selectedNode  = ref<any>(null);
const isDirty = ref(false); 
let isLoading = false; 

const availableActions = [
  { type: 'STOP_NODE',     label: '停止节点',  desc: '停止指定的服务器节点', icon: VideoPlay, color: 'danger' },
  { type: 'PROMOTE_SLAVE', label: '提升从库',  desc: '将从库提升为新主库',   icon: Switch,    color: 'warning' },
  { type: 'HEALTH_CHECK',  label: '健康检查',  desc: '验证目标节点健康状态', icon: Monitor,   color: 'success' },
  { type: 'NOTIFY',        label: '发送通知',  desc: '推送告警或事件通知',   icon: Bell,      color: 'primary' },
  { type: 'WAIT',          label: '等待监测',  desc: '暂停并等待一段时间',   icon: Timer,     color: 'info' },
  { type: 'RESTART_NODE',  label: '重启节点',  desc: '重启指定的服务器',     icon: Refresh,   color: 'warning' },
];

const getActionMeta = (type: string) => availableActions.find(a => a.type === type) ?? { label: type, desc: '', color: 'info' };

const getNodeStyle = (type: string) => {
  const c = {
    STOP_NODE: { border: '#f56c6c', glow: 'rgba(245,108,108,0.3)' },
    PROMOTE_SLAVE: { border: '#e6a23c', glow: 'rgba(230,162,60,0.3)' },
    HEALTH_CHECK: { border: '#67c23a', glow: 'rgba(103,194,58,0.3)' },
    NOTIFY: { border: '#409eff', glow: 'rgba(64,158,255,0.3)' },
    WAIT: { border: '#909399', glow: 'rgba(144,147,153,0.3)' },
    RESTART_NODE: { border: '#e6a23c', glow: 'rgba(230,162,60,0.3)' },
  }[type] || { border: '#555', glow: 'transparent' };
  return {
    border: `2px solid ${c.border}`, borderRadius: '10px',
    background: 'linear-gradient(135deg, #1e2035, #252840)',
    color: '#e8eaf6', width: '170px', boxShadow: `0 0 12px ${c.glow}`,
    padding: '8px 12px', fontWeight: '600',
  };
};

const buildLabel = (action: string, target: string) => {
  const meta = getActionMeta(action);
  return target ? `${meta.label}\n→ ${target}` : meta.label;
};

const markDirty = () => { if (!isLoading) isDirty.value = true; };

onBeforeRouteLeave(async (to, from, next) => {
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('您有未保存的修改，确定离开吗？', '提示', { type: 'warning' });
      next();
    } catch { next(false); }
  } else next();
});

const loadData = async () => {
  if (!planId.value) return;
  isLoading = true;
  try {
    const res: any = await request.get(`/api/task/plan/${planId.value}`);
    if (res.code === 200 && res.data && res.data.stepsJson) {
      planName.value = res.data.planName;
      const raw = JSON.parse(res.data.stepsJson);
      const layout = raw.layout || (Array.isArray(raw) ? null : raw.layout);
      const steps = Array.isArray(raw) ? raw : (raw.steps || []);

      if (layout && layout.nodes) {
        nodes.value = layout.nodes.map((n: any) => ({
          ...n, label: buildLabel(n.data.action, n.data.target), style: getNodeStyle(n.data.action)
        }));
        edges.value = layout.edges || [];
      } else {
        nodes.value = steps.map((step: any, index: number) => ({
          id: `node_${index}_${Date.now()}`, type: 'default',
          position: step.x !== undefined ? { x: step.x, y: step.y } : { x: 100, y: 100 + index * 120 },
          label: buildLabel(step.action, step.target),
          data: { action: step.action, target: step.target || '', waitMs: step.waitMs || 1500 },
          style: getNodeStyle(step.action),
        }));
      }
      await nextTick();
      setTimeout(() => { isDirty.value = false; isLoading = false; }, 500);
    }
  } catch (e) { isLoading = false; }
};

onMounted(loadData);
watch(() => route.query.id, (newId) => {
  if (newId) { planId.value = newId as string; loadData(); }
});

const onDragStart = (e: DragEvent, type: string) => {
  if (e.dataTransfer) {
    e.dataTransfer.setData('application/vueflow', type);
    e.dataTransfer.effectAllowed = 'move';
  }
};

const onDrop = (e: DragEvent) => {
  const type = e.dataTransfer?.getData('application/vueflow');
  if (!type || !canvasRef.value) return;
  const rect = canvasRef.value.getBoundingClientRect();
  const position = project({ x: e.clientX - rect.left, y: e.clientY - rect.top });
  position.x -= 85; position.y -= 25;

  nodes.value.push({
    id: `node_${Date.now()}`, type: 'default', position,
    label: buildLabel(type, ''), data: { action: type, target: '', waitMs: 1500 },
    style: getNodeStyle(type),
  });
  markDirty();
};

const onConnect = (p: any) => { edges.value.push({ ...p, animated: true, style: { stroke: '#409eff', strokeWidth: 2 } }); markDirty(); };
const onEdgeDoubleClick = ({ edge }: any) => { edges.value = edges.value.filter(e => e.id !== edge.id); markDirty(); };
const onNodeClick = ({ node }: any) => { selectedNode.value = node; drawerVisible.value = true; };
const onConfigChange = () => {
  if (!selectedNode.value) return;
  const n = nodes.value.find(i => i.id === selectedNode.value.id);
  if (n) {
    n.label = buildLabel(selectedNode.value.data.action, selectedNode.value.data.target);
    n.data = { ...selectedNode.value.data };
    markDirty();
  }
};
const removeNode = (id: string) => { nodes.value = nodes.value.filter(n => n.id !== id); edges.value = edges.value.filter(e => e.source !== id && e.target !== id); drawerVisible.value = false; markDirty(); };
const clearAll = () => { nodes.value = []; edges.value = []; markDirty(); };

const handleSave = async () => {
  if (nodes.value.length === 0) return ElMessage.warning('画布为空');
  const sorted = [...nodes.value].filter(n => n?.position).sort((a, b) => (a.position.y - b.position.y));
  const steps = sorted.map((n, i) => ({
    step: i + 1, action: n.data?.action || '', target: n.data?.target || '',
    waitMs: n.data?.waitMs || 1500, x: n.position?.x ?? 0, y: n.position?.y ?? 0
  }));
  const persistenceData = { steps, layout: { nodes: nodes.value.map(n => ({ id: n.id, type: n.type, position: n.position, data: n.data })), edges: edges.value } };
  try {
    const res: any = await request.post('/api/task/plan/save', { id: Number(planId.value), stepsJson: JSON.stringify(persistenceData) });
    if (res.code === 200) { ElMessage.success('✅ 编排保存成功！'); isDirty.value = false; setTimeout(() => router.push('/tasks/strategy'), 800); }
  } catch (e) { ElMessage.error('保存失败'); }
};

const handleBack = () => router.push('/tasks/strategy');
</script>

<style scoped>
.workflow-container { display: flex; flex-direction: column; height: calc(100vh - 84px); background: #0d0e1a; border-radius: 12px; overflow: hidden; position: relative; }
.workflow-header { height: 58px; background: #111228; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; border-bottom: 1px solid rgba(64,158,255,0.2); z-index: 10; }
.header-left { display: flex; align-items: center; gap: 12px; }
.title { color: #e8eaf6; font-size: 15px; margin: 0; }
.workflow-content { flex: 1; display: flex; overflow: hidden; }
.node-panel { 
  width: 220px; 
  background: #0f1022; 
  border-right: 1px solid rgba(255,255,255,0.08); 
  padding: 24px 14px; /* 增加顶部边距，防止被 Header 遮挡 */
  display: flex; 
  flex-direction: column; 
  user-select: none; 
}
.panel-title { 
  color: #e8eaf6; 
  font-size: 14px; 
  font-weight: 600; 
  margin-bottom: 4px; 
}
.panel-subtitle { 
  color: #8a8da8; 
  font-size: 11px; 
  margin-bottom: 15px; 
}
.node-list { display: flex; flex-direction: column; gap: 10px; margin-top: 5px; }
.dnd-node { padding: 12px; border-radius: 8px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); color: #ccd0f0; cursor: grab; display: flex; align-items: center; gap: 12px; }
.icon-danger { color: #f56c6c; } .icon-warning { color: #e6a23c; } .icon-success { color: #67c23a; } .icon-primary { color: #409eff; } .icon-info { color: #909399; }
.panel-tip { margin-top: auto; padding-top: 20px; }
.tip-content { color: #409eff; font-size: 12px; }
.canvas-area { flex: 1; position: relative; background: #0b0c18; overflow: hidden; }
:deep(.vue-flow__edge-path) { stroke-dasharray: 5; animation: dash 10s linear infinite; stroke: #409eff; stroke-width: 2; }
@keyframes dash { from { stroke-dashoffset: 100; } to { stroke-dashoffset: 0; } }
</style>