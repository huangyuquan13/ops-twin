<template>
  <div class="workflow-container">
    <!-- 顶部工具条 -->
    <div class="workflow-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" circle @click="goBack" />
        <h3 class="title">演练编排工作站：{{ planName || '未命名预案' }}</h3>
      </div>
      <div class="header-right">
        <el-button type="info" :icon="QuestionFilled" plain>帮助</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存编排</el-button>
      </div>
    </div>

    <div class="workflow-content">
      <!-- 左侧动作面板 -->
      <div class="node-panel">
        <div class="panel-title">动作库</div>
        <div class="node-list">
          <div
            v-for="item in availableActions"
            :key="item.type"
            class="dnd-node"
            :class="'node-' + item.color"
            draggable="true"
            @dragstart="onDragStart($event, item.type)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </div>
        <div class="panel-tip">💡 拖拽动作到右侧画布开始编排</div>
      </div>

      <!-- 右侧画布 -->
      <div class="canvas-area" @drop="onDrop" @dragover.prevent>
        <VueFlow
          v-model="nodes"
          v-model:edges="edges"
          :class="{ 'dark-theme': true }"
          :default-viewport="{ x: 0, y: 0, zoom: 1 }"
          @connect="onConnect"
        >
          <!-- 背景与网关 -->
          <Background pattern-color="#333" :gap="20" />
          <Controls />
          <MiniMap />
        </VueFlow>
      </div>
    </div>

    <!-- 节点配置侧边抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="节点参数配置"
      size="320px"
      destroy-on-close
    >
      <div v-if="selectedNode" class="config-form">
        <el-form label-position="top">
          <el-form-item label="动作类型">
            <el-tag :type="getActionTag(selectedNode.data.action).color">
              {{ getActionTag(selectedNode.data.action).label }}
            </el-tag>
          </el-form-item>
          <el-form-item label="目标对象 (Target)">
            <el-input v-model="selectedNode.data.target" placeholder="输入主机名或资源标识" />
          </el-form-item>
          <el-form-item label="模拟耗时 (ms)">
            <el-input-number v-model="selectedNode.data.waitMs" :step="500" :min="0" style="width: 100%" />
          </el-form-item>
        </el-form>
        <div class="drawer-footer">
          <el-button type="danger" plain @click="removeNode(selectedNode.id)">删除节点</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Check, QuestionFilled, VideoPlay, Refresh, Monitor, Bell, Timer } from '@element-plus/icons-vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { MiniMap } from '@vue-flow/minimap';

import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';
import '@vue-flow/minimap/dist/style.css';
import request from '@/api/request';

const route = useRoute();
const router = useRouter();
const { addEdges, addNodes, project } = useVueFlow();

// ============ 状态定义 ============
const planId   = ref(route.query.id as string);
const planName = ref(route.query.name as string);
const nodes    = ref<any[]>([]);
const edges    = ref<any[]>([]);

const drawerVisible = ref(false);
const selectedNode  = ref<any>(null);

// 可用的动作类型
const availableActions = [
  { type: 'STOP_NODE',     label: '停止节点', icon: VideoPlay, color: 'danger' },
  { type: 'PROMOTE_SLAVE', label: '提升从库', icon: Refresh,   color: 'warning' },
  { type: 'HEALTH_CHECK',  label: '健康检查', icon: Monitor,   color: 'success' },
  { type: 'NOTIFY',        label: '发送通知', icon: Bell,      color: 'primary' },
  { type: 'WAIT',          label: '等待监测', icon: Timer,     color: 'info' },
];

// ============ 初始化加载 ============
onMounted(async () => {
  if (planId.value) {
    const res: any = await request.get(`/api/task/plan/list`, { params: { current: 1, size: 1 } }); // 简化逻辑，实际应有详情接口
    // 这里暂时解析已有的 steps_json 转换为节点（由于逻辑较复杂，初始版本先支持从零创建）
    // TODO: 转换逻辑
  }
});

// ============ 拖拽与连接逻辑 ============
const onDragStart = (event: DragEvent, type: string) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', type);
    event.dataTransfer.effectAllowed = 'move';
  }
};

const onDrop = (event: DragEvent) => {
  const type = event.dataTransfer?.getData('application/vueflow');
  if (!type) return;

  const position = { x: event.clientX - 300, y: event.clientY - 100 }; // 粗略偏移计算

  const newNode = {
    id: `node_${Date.now()}`,
    type: 'default',
    position,
    label: '', // Vue Flow 默认 label，我们会自定义显示
    data: {
      action: type,
      target: '',
      waitMs: 1500,
    },
    style: getNodeStyle(type),
  };

  nodes.value.push(newNode);
};

const onConnect = (params: any) => {
  edges.value.push({ ...params, animated: true, style: { stroke: '#409eff' } });
};

// ============ 节点配置 ============
watch(nodes, () => {
  // 监听节点点击（简化版：点击最后一个被修改的节点）
  const active = nodes.value.find(n => n.selected);
  if (active) {
    selectedNode.value = active;
    drawerVisible.value = true;
  }
}, { deep: true });

const removeNode = (id: string) => {
  nodes.value = nodes.value.filter(n => n.id !== id);
  edges.value = edges.value.filter(e => e.source !== id && e.target !== id);
  drawerVisible.value = false;
};

// ============ 保存逻辑 ============
const handleSave = async () => {
  // 核心逻辑：将拓扑图转换为线性 steps_json
  // 这里采用简单的拓扑排序思想：根据 source/target 关系排序
  const steps = nodes.value.map((n, index) => ({
    step: index + 1,
    action: n.data.action,
    target: n.data.target,
    waitMs: n.data.waitMs
  }));

  try {
    const res: any = await request.post('/api/task/plan/save', {
      id: Number(planId.value),
      stepsJson: JSON.stringify(steps)
    });
    if (res.code === 200) {
      ElMessage.success('编排保存成功');
      router.push('/tasks/strategy');
    }
  } catch (e) {
    ElMessage.error('保存失败');
  }
};

const goBack = () => router.back();

// ============ 工具函数 ============
const getNodeStyle = (type: string) => {
  const colors: Record<string, string> = {
    STOP_NODE:     '#f56c6c',
    PROMOTE_SLAVE: '#e6a23c',
    HEALTH_CHECK:  '#67c23a',
    NOTIFY:        '#409eff',
    WAIT:          '#909399',
  };
  return {
    border: `2px solid ${colors[type] || '#ccc'}`,
    padding: '10px',
    borderRadius: '8px',
    background: '#1a1a1a',
    color: '#fff',
    width: '150px',
    fontSize: '12px',
    fontWeight: 'bold',
    textAlign: 'center'
  };
};

const getActionTag = (action: string) => {
  const found = availableActions.find(a => a.type === action);
  return found ? found : { label: action, color: 'info' };
};
</script>

<style scoped>
.workflow-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #0f101a;
  border-radius: 12px;
  overflow: hidden;
}

.workflow-header {
  height: 60px;
  background: #1a1c2e;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.title {
  color: #fff;
  margin: 0;
  font-size: 16px;
}

.workflow-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧面板 */
.node-panel {
  width: 240px;
  background: #141625;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-title {
  color: #8c8fb5;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dnd-node {
  padding: 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  cursor: grab;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  transition: all 0.3s;
}

.dnd-node:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
}

.node-danger { border-left: 4px solid #f56c6c; }
.node-warning { border-left: 4px solid #e6a23c; }
.node-success { border-left: 4px solid #67c23a; }
.node-primary { border-left: 4px solid #409eff; }
.node-info { border-left: 4px solid #909399; }

.panel-tip {
  margin-top: auto;
  font-size: 12px;
  color: #5c5f82;
  font-style: italic;
}

/* 画布区域 */
.canvas-area {
  flex: 1;
  background: #0b0c15;
}

/* Vue Flow 暗黑主题覆盖 */
:deep(.vue-flow__node-default) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}

.config-form {
  padding: 0 20px;
}

.drawer-footer {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: center;
}
</style>