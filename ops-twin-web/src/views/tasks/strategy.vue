<template>
  <div class="plan-container">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">演练预案方案库</h2>
        <p class="page-desc">管理容灾演练预案，与逻辑服务深度绑定，驱动自动化故障模拟与自愈</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增预案</el-button>
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <!-- 搜索条件栏 -->
    <div class="search-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="预案名称">
          <el-input v-model.trim="queryParams.planName" placeholder="请输入预案名称" clearable />
        </el-form-item>
        <el-form-item label="关联服务">
          <el-select v-model="queryParams.serviceId" placeholder="请选择逻辑服务" clearable>
            <el-option
              v-for="s in serviceOptions"
              :key="s.id"
              :label="s.serviceName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预案类型">
          <el-select v-model="queryParams.planType" placeholder="全部类型" clearable>
            <el-option label="演练 (DRILL)"       value="DRILL" />
            <el-option label="故障切换 (FAILOVER)" value="FAILOVER" />
            <el-option label="扩缩容 (SCALE)"     value="SCALE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <el-table :data="planList" v-loading="loading" border stripe>

        <el-table-column prop="planName" label="预案名称" min-width="180" />

        <el-table-column label="关联逻辑服务" width="160">
          <template #default="scope">
            <el-tag type="info">{{ getServiceName(scope.row.serviceId) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="预案类型" width="130">
          <template #default="scope">
            <el-tag :type="getPlanTypeTag(scope.row.planType).color">
              {{ getPlanTypeTag(scope.row.planType).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="100" align="center">
          <template #default="scope">
            <el-tag
              :type="scope.row.priority === 1 ? 'danger' : scope.row.priority === 2 ? 'warning' : 'info'"
              size="small"
            >
              {{ scope.row.priority === 1 ? '🔴 高' : scope.row.priority === 2 ? '🟡 中' : '🟢 低' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="预案说明" min-width="220" show-overflow-tooltip />

        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="toggleStatus(scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createBy" label="创建人" width="100" />

        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <div class="action-row">
                <el-button link type="primary" :icon="Operation" @click="handleWorkflow(row)">编排</el-button>
                <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
              </div>
              <div class="action-row">
                <el-button link type="success" :loading="runLoading[row.id]" @click="handleRun(row)">执行</el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
              </div>
            </div>
          </template>
        </el-table-column>

      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="680px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">

        <el-form-item label="预案名称" prop="planName">
          <el-input v-model="form.planName" placeholder="如：数据库主从切换测试" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联服务" prop="serviceId">
              <el-select v-model="form.serviceId" placeholder="请选择逻辑服务" style="width: 100%">
                <el-option
                  v-for="s in serviceOptions"
                  :key="s.id"
                  :label="s.serviceName"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预案类型" prop="planType">
              <el-select v-model="form.planType" style="width: 100%">
                <el-option label="演练 (DRILL)"       value="DRILL" />
                <el-option label="故障切换 (FAILOVER)" value="FAILOVER" />
                <el-option label="扩缩容 (SCALE)"     value="SCALE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option label="🔴 高优先级" :value="1" />
                <el-option label="🟡 中优先级" :value="2" />
                <el-option label="🟢 低优先级" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建人" prop="createBy">
              <el-input v-model="form.createBy" placeholder="如：admin" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="预案说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简要描述此预案的演练目标" />
        </el-form-item>

        <el-form-item label="执行步骤">
          <el-input
            v-model="form.stepsJson"
            type="textarea"
            :rows="5"
            placeholder='JSON 格式编排，如：[{"step":1,"action":"STOP_NODE","target":"Pay-DB-Master"}]'
            style="font-family: monospace; font-size: 13px;"
          />
        </el-form-item>

      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">保存预案</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Operation, Edit, Delete } from '@element-plus/icons-vue';
import request from '@/api/request';

const router = useRouter();

// ============ 状态定义 ============
const loading        = ref(false);
const planList       = ref<any[]>([]);
const total          = ref(0);
const serviceOptions = ref<any[]>([]);
const runLoading     = ref<Record<number, boolean>>({});  // 每行独立 loading 状态，防重复点击

const queryParams = reactive({
  current:   1,
  size:      10,
  planName:  '',
  serviceId: null as null | number,
  planType:  '',
});

const dialogVisible  = ref(false);
const dialogTitle    = ref('新增预案');
const submitLoading  = ref(false);
const formRef        = ref();
const form = ref({
  id:          null as null | number,
  planName:    '',
  serviceId:   null as null | number,
  planType:    'DRILL',
  priority:    2,
  description: '',
  createBy:    'admin',
  stepsJson:   '',
  status:      1,
});

const rules = {
  planName:  [{ required: true, message: '请输入预案名称',       trigger: 'blur' }],
  serviceId: [{ required: true, message: '请选择关联的逻辑服务', trigger: 'change' }],
  planType:  [{ required: true, message: '请选择预案类型',       trigger: 'change' }],
  priority:  [{ required: true, message: '请选择优先级',         trigger: 'change' }],
};

// ============ 数据获取 ============
const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/api/task/plan/list', { params: queryParams });
    if (res.code === 200) {
      planList.value = res.data.records;
      total.value    = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const fetchServiceOptions = async () => {
  const res: any = await request.get('/api/asset/service/list', { params: { current: 1, size: 999 } });
  if (res.code === 200) {
    serviceOptions.value = res.data.records;
  }
};

onMounted(() => {
  fetchList();
  fetchServiceOptions();
});

// ============ 搜索与重置 ============
const handleSearch = () => { queryParams.current = 1; fetchList(); };
const handleReset = () => {
  queryParams.planName  = '';
  queryParams.serviceId = null;
  queryParams.planType  = '';
  queryParams.current   = 1;
  fetchList();
};

// ============ 新增 / 编辑 ============
const handleAdd = () => {
  dialogTitle.value = '新增预案';
  form.value = { id: null, planName: '', serviceId: null, planType: 'DRILL', priority: 2, description: '', createBy: 'admin', stepsJson: '', status: 1 };
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑预案';
  form.value = { ...row };
  dialogVisible.value = true;
};

// 跳转到可视化编排
const handleWorkflow = (row: any) => {
  router.push({
    path: '/tasks/workflow',
    query: { id: row.id, name: row.planName }
  });
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      const res: any = await request.post('/api/task/plan/save', form.value);
      if (res.code === 200) {
        ElMessage.success('预案保存成功');
        dialogVisible.value = false;
        fetchList();
      } else {
        ElMessage.error(res.message || '保存失败');
      }
    } finally {
      submitLoading.value = false;
    }
  });
};

// ============ 删除 ============
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除预案【${row.planName}】吗？此操作不可恢复。`, '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/api/task/plan/delete/${row.id}`);
    if (res.code === 200) { ElMessage.success('删除成功'); fetchList(); }
  }).catch(() => {});
};

// ============ 启用/禁用 ============
const toggleStatus = async (row: any) => {
  const res: any = await request.put(`/api/task/plan/toggle/${row.id}`);
  if (res.code !== 200) {
    // 接口失败时将 switch 反转回来
    row.status = row.status === 1 ? 0 : 1;
  } else {
    ElMessage.success(row.status === 1 ? '预案已启用' : '预案已禁用');
  }
};

// ============ 执行预案 ============
const handleRun = (row: any) => {
  ElMessageBox.confirm(
    `即将触发演练预案【${row.planName}】，确认执行？\n\n执行后将自动跳转至实时监控终端。`,
    '执行确认',
    {
      confirmButtonText: '确认执行',
      cancelButtonText:  '再想想',
      type: 'warning',
    }
  ).then(async () => {
    // 标记该行 loading
    runLoading.value[row.id] = true;
    try {
      const res: any = await request.post(`/api/task/record/trigger/${row.id}`, null, {
        params: { operator: 'admin' }
      });
      if (res.code === 200) {
        const { recordId, planName } = res.data;
        ElMessage.success(`演练引擎已启动，流水 ID：${recordId}，正在跳转终端...`);
        // 跳转至实时终端页，通过 query 传参
        router.push({
          path: '/tasks/terminal',
          query: { recordId: String(recordId), planName }
        });
      } else {
        ElMessage.error(res.message || '触发失败');
      }
    } finally {
      runLoading.value[row.id] = false;
    }
  }).catch(() => {});
};

// ============ 工具函数 ============
const getServiceName = (id: number) => {
  const found = serviceOptions.value.find(s => s.id === id);
  return found ? found.serviceName : `服务#${id}`;
};

const getPlanTypeTag = (type: string) => {
  const map: Record<string, { label: string; color: any }> = {
    DRILL:    { label: '演练',    color: 'primary' },
    FAILOVER: { label: '故障切换', color: 'danger' },
    SCALE:    { label: '扩缩容',  color: 'success' },
  };
  return map[type] ?? { label: type, color: 'info' };
};

const formatTime = (time: string) => {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
};
</script>

<style scoped>
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.action-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.action-row .el-button {
  margin: 0 !important;
  padding: 4px 0;
  flex: 1;
  justify-content: flex-start;
}

.plan-container {
  padding: 20px;
  background: #f5f6fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  margin: 0 0 4px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.header-right {
  display: flex;
  gap: 10px;
}

.search-card {
  background: #fff;
  padding: 18px 20px 4px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}

.table-card {
  background: #fff;
  padding: 16px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>