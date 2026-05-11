<template>
  <div class="service-mapping-container">
    <div class="sidebar left-sidebar">
      <div class="sidebar-header">
        <h3>逻辑服务列表</h3>
        <el-button type="primary" size="small" :icon="Plus" @click="handleAddService">新建服务</el-button>
      </div>
      <el-menu :default-active="activeServiceId" @select="handleSelectService" class="service-menu">
        <el-menu-item v-for="item in serviceList" :key="item.id" :index="item.id.toString()">
          <el-icon><Menu /></el-icon>
          <span>{{ item.serviceName }}</span>
        </el-menu-item>
      </el-menu>
    </div>

    <div class="main-canvas" @drop="onDrop" @dragover="onDragOver">
      <div class="canvas-header" v-if="activeService">
        <div class="header-left">
          <el-tag type="info" effect="plain" class="service-tag">逻辑服务</el-tag>
          <span class="service-title">{{ activeService.serviceName }}</span>
          <span v-if="isDirty" class="status-indicator dirty">● 未保存</span>
          <span v-else class="status-indicator saved">● 已保存</span>
        </div>
        <div class="header-right">
          <el-button 
            type="success" 
            :icon="Check" 
            :disabled="!isDirty" 
            :loading="isSaving"
            @click="saveTopology"
          >保存拓扑</el-button>
          <el-button type="danger" plain :icon="Delete" @click="deleteService">删除服务</el-button>
        </div>
      </div>
      
      <div v-if="!activeService" class="empty-state">
        <el-empty description="请先在左侧选择或新建一个逻辑服务" />
      </div>
      
      <VueFlow 
        v-else 
        v-model="elements" 
        :default-viewport="{ zoom: 1 }" 
        :fit-view-on-init="true" 
        @nodes-change="onNodesChange"
        @edges-change="onEdgesChange"
        @connect="onConnect"
        @edge-double-click="onEdgeDoubleClick"
      >
        <Background pattern-color="#aaa" :gap="15" />
        <Controls />
      </VueFlow>
    </div>

    <div class="sidebar right-sidebar">
      <div class="sidebar-header">
        <h3>物理资产池</h3>
        <el-input v-model="hostSearch" placeholder="搜索主机名/IP" size="small" clearable />
      </div>
      <div class="host-list">
        <div 
          v-for="host in filteredHosts" 
          :key="host.id" 
          class="host-item"
          draggable="true"
          @dragstart="(event) => onDragStart(event, host)"
        >
          <div class="host-icon"><el-icon><Cpu /></el-icon></div>
          <div class="host-info">
            <div class="host-name">
              {{ host.hostname }}
              <el-tag size="small" :type="getHostTypeColor(host.hostType).tag" style="margin-left: 5px">
                {{ host.hostType || 'SERVER' }}
              </el-tag>
            </div>
            <div class="host-ip">{{ host.ipAddr }}</div>
            <div class="host-desc" v-if="host.description">{{ host.description }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建/编辑服务弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="400px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" placeholder="如 支付核心网关" />
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="form.owner" placeholder="如 张三" />
        </el-form-item>
        <el-form-item label="服务描述" prop="description">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitServiceForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Menu, Check, Delete, Cpu } from '@element-plus/icons-vue';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import { Controls } from '@vue-flow/controls';
import { onBeforeRouteLeave } from 'vue-router';
import request from '@/api/request';

// 引入样式
import '@vue-flow/core/dist/style.css';
import '@vue-flow/core/dist/theme-default.css';
import '@vue-flow/controls/dist/style.css';

const { project, addEdges } = useVueFlow();

const serviceList = ref<any[]>([]);
const activeServiceId = ref('');
const activeService = computed(() => serviceList.value.find(s => s.id.toString() === activeServiceId.value));

const hostList = ref<any[]>([]);
const hostSearch = ref('');
const filteredHosts = computed(() => {
  if (!hostSearch.value) return hostList.value;
  const keyword = hostSearch.value.toLowerCase();
  return hostList.value.filter(h => 
    h.hostname.toLowerCase().includes(keyword) || 
    h.ipAddr.toLowerCase().includes(keyword)
  );
});

// Vue Flow 数据
const elements = ref<any[]>([]);

// 状态追踪
const isDirty = ref(false);
const isSaving = ref(false);
let skipDirty = false; // 用于在初始化加载数据时跳过脏检查

// 连线逻辑
const onConnect = (params: any) => {
  // 可以在连线时增加一些默认样式，比如动画效果
  params.animated = false;
  addEdges([params]);
  isDirty.value = true;
};

// 连线双击事件：给线加文字标签
const onEdgeDoubleClick = ({ edge }: any) => {
  ElMessageBox.prompt('请输入连线说明文字（如果想清除文字，请清空输入框后点确定）', '编辑连线标签', {
    confirmButtonText: '确定保存',
    cancelButtonText: '取消',
    inputValue: edge.label || '',
  }).then(({ value }) => {
    const index = elements.value.findIndex(e => e.id === edge.id);
    if (index !== -1) {
      elements.value[index].label = value;
      // 只有当有文字时才设置样式，否则清空样式
      if (value) {
        elements.value[index].style = { strokeWidth: 2, stroke: '#909399' };
        elements.value[index].labelBgStyle = { fill: '#ffffff', color: '#fff', fillOpacity: 0.8 };
        elements.value[index].labelBgPadding = [4, 4];
        elements.value[index].labelBgBorderRadius = 4;
        elements.value[index].labelStyle = { fill: '#303133', fontWeight: 600, fontSize: 12 };
      } else {
        elements.value[index].style = {};
      }
      elements.value = [...elements.value]; // 触发响应式更新
      isDirty.value = true;
    }
  }).catch(() => {
    // 用户点击取消或关闭窗口，什么都不做，保留原标签
  });
};

// 变更监听
const onNodesChange = () => { 
  if (skipDirty) return;
  isDirty.value = true; 
};
const onEdgesChange = () => { 
  if (skipDirty) return;
  isDirty.value = true; 
};

// 表单相关
const dialogVisible = ref(false);
const dialogTitle = ref('新建服务');
const formRef = ref();
const form = ref({ id: null, serviceName: '', owner: '', description: '' });
const rules = {
  serviceName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }]
};

const fetchServices = async () => {
  const res: any = await request.get('/api/asset/service/list', { params: { size: 100 } });
  if (res.code === 200) {
    serviceList.value = res.data.records;
  }
};

const fetchHosts = async () => {
  const res: any = await request.get('/api/asset/host/list', { params: { size: 1000 } });
  if (res.code === 200) {
    hostList.value = res.data.records;
  }
};

const handleAddService = () => {
  form.value = { id: null, serviceName: '', owner: '', description: '' };
  dialogVisible.value = true;
};

const submitServiceForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      const res: any = await request.post('/api/asset/service/save', form.value);
      if (res.code === 200) {
        ElMessage.success('保存成功');
        dialogVisible.value = false;
        fetchServices();
        if (!activeServiceId.value) {
          activeServiceId.value = res.data.id.toString();
        }
      } else {
        ElMessage.error(res.msg || '保存失败');
      }
    }
  });
};

const handleSelectService = async (index: string) => {
  // 如果当前有未保存的修改，拦截切换
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm(
        '当前拓扑图有未保存的修改，切换服务将丢失这些修改，确定继续吗？',
        '提示',
        { confirmButtonText: '确定切换', cancelButtonText: '取消', type: 'warning' }
      );
    } catch (e) {
      return;
    }
  }

  activeServiceId.value = index;
  const res: any = await request.get(`/api/asset/service/topology/${index}`);
  if (res.code === 200) {
    skipDirty = true; // 锁定：忽略因加载数据引起的内部变更事件
    const topology = res.data.topologyJson;
    if (topology) {
      elements.value = JSON.parse(topology);
    } else {
      elements.value = [];
    }
    
    // 给 Vue Flow 足够的初始化和布局计算时间
    setTimeout(() => {
      isDirty.value = false;
      skipDirty = false;
    }, 150);
  }
};

// 路由离开守卫：防止跳转到其他页面时丢失数据
onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value) {
    ElMessageBox.confirm(
      '您有未保存的拓扑变更，离开此页面将导致修改丢失，确定离开吗？',
      '确认离开',
      { confirmButtonText: '离开', cancelButtonText: '留在页面', type: 'warning' }
    ).then(() => {
      next();
    }).catch(() => {
      next(false);
    });
  } else {
    next();
  }
});

const deleteService = () => {
  if (!activeServiceId.value) return;
  ElMessageBox.confirm('确认删除该逻辑服务及关联拓扑吗？', '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/api/asset/service/delete/${activeServiceId.value}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      activeServiceId.value = '';
      elements.value = [];
      fetchServices();
    }
  }).catch(() => {});
};

const saveTopology = async () => {
  if (!activeServiceId.value) return;
  isSaving.value = true;
  try {
    const hostIds = elements.value
      .filter(e => !e.source)
      .map(e => e.data?.hostId)
      .filter(id => id != null);

    const payload = {
      serviceId: activeServiceId.value,
      topologyJson: JSON.stringify(elements.value),
      hostIds
    };
    const res: any = await request.post('/api/asset/service/topology/save', payload);
    if (res.code === 200) {
      ElMessage.success('拓扑保存成功');
      isDirty.value = false;
    } else {
      ElMessage.error('拓扑保存失败');
    }
  } finally {
    isSaving.value = false;
  }
};

// 获取主机类型的颜色和标签配置
const getHostTypeColor = (type: string) => {
  const upperType = type ? type.toUpperCase() : '';
  switch(upperType) {
    case 'WEB': return { bg: '#e1f3d8', border: '#67c23a', tag: 'success' };
    case 'APP': return { bg: '#d9ecff', border: '#409eff', tag: 'primary' };
    case 'DB': return { bg: '#faecd8', border: '#e6a23c', tag: 'warning' };
    case 'CACHE': return { bg: '#e1f3d8', border: '#b3e19d', tag: 'success' };
    case 'LB': return { bg: '#fde2e2', border: '#f56c6c', tag: 'danger' };
    default: return { bg: '#f4f4f5', border: '#909399', tag: 'info' };
  }
};

// 拖拽逻辑
const onDragStart = (event: any, host: any) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', JSON.stringify(host));
    event.dataTransfer.effectAllowed = 'move';
  }
};

const onDragOver = (event: any) => {
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move';
  }
};

const onDrop = (event: any) => {
  const hostData = event.dataTransfer?.getData('application/vueflow');
  if (!hostData || !activeService.value) return;
  
  const host = JSON.parse(hostData);
  if (elements.value.some(e => e.data?.hostId === host.id)) {
    ElMessage.warning('该物理资产已在拓扑图中');
    return;
  }

  // 获取画布相对于视口的边界
  const target = event.currentTarget;
  const { left, top } = target.getBoundingClientRect();

  // 精确计算坐标：鼠标视口坐标 - 画布起始坐标
  const position = project({ 
    x: event.clientX - left, 
    y: event.clientY - top 
  }); 
  
  const typeColor = getHostTypeColor(host.hostType);
  const newNode = {
    id: `host-${host.id}`,
    type: 'default',
    position,
    data: { 
      label: `[${host.hostType || 'SERVER'}] ${host.hostname}\n(${host.ipAddr})`, 
      hostId: host.id 
    },
    style: { 
      background: typeColor.bg, 
      border: `2px solid ${typeColor.border}`, 
      borderRadius: '6px', 
      padding: '10px',
      fontWeight: 'bold',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
    }
  };
  
  elements.value.push(newNode);
  isDirty.value = true;
};

onMounted(() => {
  fetchServices();
  fetchHosts();
});
</script>

<style scoped>
.service-mapping-container {
  display: flex;
  height: calc(100vh - 84px);
  background: #f0f2f5;
  margin: -20px; /* 抵消 app-main 的 padding */
}

.sidebar {
  width: 250px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.right-sidebar {
  border-right: none;
  border-left: 1px solid #e4e7ed;
}

.sidebar-header {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.service-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
}

.main-canvas {
  flex: 1;
  position: relative;
  background: #fafafa;
}

.canvas-header {
  position: absolute;
  top: 15px;
  left: 15px;
  right: 15px;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  padding: 10px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.service-tag {
  font-weight: bold;
}

.service-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.status-indicator {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status-indicator.dirty {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
}

.status-indicator.saved {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.header-right {
  display: flex;
  gap: 10px;
}

.empty-state {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.host-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.host-item {
  display: flex;
  align-items: center;
  padding: 10px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.3s;
}

.host-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.host-item:active {
  cursor: grabbing;
}

.host-icon {
  font-size: 24px;
  color: #409eff;
  margin-right: 10px;
}

.host-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
}

.host-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.host-ip {
  font-size: 12px;
  color: #909399;
}
</style>
