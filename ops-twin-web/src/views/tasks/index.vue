<template>
  <div class="task-hub">
    <!-- KPI 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(64,158,255,0.15); color: #409eff;">📋</div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.totalPlans }}</div>
          <div class="stat-label">预案总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(103,194,58,0.15); color: #67c23a;">▶</div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.todayRuns }}</div>
          <div class="stat-label">今日执行</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(230,162,60,0.15); color: #e6a23c;">📊</div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
    </div>

    <!-- 预案列表（只读总览 + 快捷操作） -->
    <div class="table-card">
      <div class="table-header">
        <h3 class="table-title">演练预案</h3>
        <el-button type="primary" :icon="Plus" @click="goToStrategyAdd">设定方案</el-button>
      </div>
      <el-table :data="planList" v-loading="loading" stripe>
        <el-table-column prop="planName" label="预案名称" min-width="160" />
        <el-table-column label="关联服务" width="160">
          <template #default="scope">
            <el-tag type="info">{{ getServiceName(scope.row.serviceId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">
            <el-tag :type="planTypeTag[scope.row.planType] ?? 'info'" size="small">
              {{ planTypeLabel[scope.row.planType] ?? scope.row.planType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90" align="center">
          <template #default="scope">
            <el-tag :type="['', 'danger', 'warning', 'info'][scope.row.priority] ?? 'info'" size="small">
              {{ ['', '高', '中', '低'][scope.row.priority] ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <span :style="{ color: scope.row.status === 1 ? '#67c23a' : '#909399' }">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleWorkflow(row)" :disabled="row.status !== 1">
              编排
            </el-button>
            <el-button link type="success" :loading="runLoading[row.id]" @click="handleRun(row)" :disabled="row.status !== 1">
              执行
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit } from '@element-plus/icons-vue';
import request from '@/api/request';

const router = useRouter();

const loading = ref(false);
const planList = ref<any[]>([]);
const total = ref(0);
const serviceOptions = ref<any[]>([]);
const runLoading = ref<Record<number, boolean>>({});

const page = reactive({ current: 1, size: 10 });
const stats = reactive({ totalPlans: 0, todayRuns: 0, successRate: 0 });

const planTypeTag: Record<string, string> = { DRILL: 'primary', FAILOVER: 'danger', SCALE: 'success' };
const planTypeLabel: Record<string, string> = { DRILL: '演练', FAILOVER: '故障切换', SCALE: '扩缩容' };

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/api/task/plan/list', { params: page });
    if (res.code === 200) {
      planList.value = res.data.records;
      total.value = res.data.total;
      stats.totalPlans = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const fetchStats = async () => {
  try {
    const res: any = await request.get('/api/task/record/list', { params: { current: 1, size: 9999 } });
    if (res.code === 200 && res.data.records) {
      const records = res.data.records;
      const today = new Date().toISOString().slice(0, 10);
      const todayRecords = records.filter((r: any) => r.createTime?.startsWith(today));
      const successRecords = todayRecords.filter((r: any) => r.runStatus === 'SUCCESS');
      stats.todayRuns = todayRecords.length;
      stats.successRate = todayRecords.length > 0 ? Math.round((successRecords.length / todayRecords.length) * 100) : 0;
    }
  } catch { /* ignore */ }
};

const fetchServiceOptions = async () => {
  const res: any = await request.get('/api/asset/service/list', { params: { size: 999 } });
  if (res.code === 200) {
    serviceOptions.value = res.data.records;
  }
};

const getServiceName = (id: number) => {
  const found = serviceOptions.value.find((s: any) => s.id === id);
  return found ? found.serviceName : `服务#${id}`;
};

onMounted(() => {
  fetchList();
  fetchStats();
  fetchServiceOptions();
});

// ============ 跳转 ============
const goToStrategyAdd = () => {
  router.push({ path: '/tasks/strategy', query: { openAdd: '1' } });
};

const handleWorkflow = (row: any) => {
  router.push({
    path: '/tasks/workflow',
    query: { id: row.id, name: row.planName, serviceId: row.serviceId }
  });
};

const handleRun = (row: any) => {
  ElMessageBox.confirm(
    `即将触发演练预案【${row.planName}】，确认执行？`,
    '执行确认',
    { confirmButtonText: '确认执行', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    runLoading.value[row.id] = true;
    try {
      const res: any = await request.post(`/api/task/record/trigger/${row.id}`, null, {
        params: { operator: 'admin' }
      });
      if (res.code === 200) {
        const { recordId, planName } = res.data;
        ElMessage.success(`引擎已启动，流水 #${recordId}`);
        router.push({
          path: '/tasks/terminal',
          query: { recordId: String(recordId), planName }
        });
      }
    } finally {
      runLoading.value[row.id] = false;
    }
  }).catch(() => {});
};
</script>

<style scoped>
.task-hub { padding: 20px; background: #f5f6fa; min-height: 100%; }

.stats-row { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card {
  flex: 1;
  display: flex; align-items: center; gap: 14px;
  background: #fff; border-radius: 10px; padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; }
.stat-label { font-size: 13px; color: #909399; }

.table-card { background: #fff; padding: 16px 20px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { margin: 0; font-size: 16px; color: #1a1a2e; font-weight: 600; }
.pagination-wrap { margin-top: 14px; display: flex; justify-content: flex-end; }
</style>
