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
      <div class="toolbar" v-if="activeService">
        <span class="service-title">当前服务: {{ activeService.serviceName }}</span>
        <el-button type="success" :icon="Check" @click="saveTopology">保存拓扑</el-button>
        <el-button type="danger" :icon="Delete" @click="deleteService">删除服务</el-button>
      </div>
      
      <div v-if="!activeService" class="empty-state">
        <el-empty description="请先在左侧选择或新建一个逻辑服务" />
      </div>
      
      <VueFlow 
        v-else 
        v-model="elements" 
        :default-viewport="{ zoom: 1 }" 
        :fit-view-on-init="true" 
        @edges-change="onEdgesChange"
        @connect="onConnect"
      >
        <Background pattern-color="#aaa" gap="15" />
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
            <div class="host-name">{{ host.hostname }}</div>
            <div class="host-ip">{{ host.ipAddr }}</div>
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

// 连线逻辑
const onConnect = (params: any) => {
  addEdges([params]);
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
  activeServiceId.value = index;
  const res: any = await request.get(`/api/asset/service/topology/${index}`);
  if (res.code === 200) {
    const topology = res.data.topologyJson;
    if (topology) {
      elements.value = JSON.parse(topology);
    } else {
      elements.value = [];
    }
  }
};

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
  // 获取当前图中的所有节点提取主机 ID
  const hostIds = elements.value
    .filter(e => !e.source) // 过滤掉连线，只保留节点
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
  } else {
    ElMessage.error('拓扑保存失败');
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
  // 防止重复拖入同一主机
  if (elements.value.some(e => e.data?.hostId === host.id)) {
    ElMessage.warning('该物理资产已在拓扑图中');
    return;
  }

  // 计算放置位置
  const position = project({ x: event.clientX - 250, y: event.clientY - 60 }); 
  
  const newNode = {
    id: `host-${host.id}`,
    type: 'default',
    position,
    data: { label: `${host.hostname}\n(${host.ipAddr})`, hostId: host.id },
    style: { background: '#f0f9eb', border: '1px solid #67c23a', borderRadius: '4px', padding: '10px' }
  };
  
  elements.value.push(newNode);
};

const onEdgesChange = (changes: any) => {
  // 可以在这里处理连线事件，比如自动保存线关系
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

.toolbar {
  position: absolute;
  top: 15px;
  left: 15px;
  z-index: 10;
  display: flex;
  gap: 10px;
  align-items: center;
  background: #fff;
  padding: 8px 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.service-title {
  font-weight: bold;
  color: #303133;
  margin-right: 10px;
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
}

.host-ip {
  font-size: 12px;
  color: #909399;
}
</style>
