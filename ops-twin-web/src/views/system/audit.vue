<template>
  <div class="audit-container">
    <div class="page-header">
      <div>
        <h2>操作审计</h2>
        <p>记录所有敏感操作，安全可溯源。日志持久化存储在数据库，不会每日清除。</p>
      </div>
      <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-card">
      <el-form :inline="true">
        <el-form-item label="操作人">
          <el-input v-model.trim="query.operator" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="query.eventType" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="t in eventTypes" :key="t" :label="typeLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 日志表格 -->
    <div class="table-card">
      <el-table :data="logList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column label="事件类型" width="130">
          <template #default="scope">
            <el-tag :type="typeTag(scope.row.eventType)" size="small">{{ typeLabel(scope.row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
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
import { Refresh } from '@element-plus/icons-vue';
import request from '@/api/request';

const loading = ref(false);
const logList = ref<any[]>([]);
const total = ref(0);
const eventTypes = ref<string[]>([]);

const query = reactive({ current: 1, size: 10, operator: '', eventType: '' });

const typeTag = (t: string) => {
  const map: Record<string, string> = {
    LOGIN: 'success', EXECUTE_PLAN: 'primary', DELETE_PLAN: 'danger',
    CREATE_PLAN: 'success', UPDATE_PLAN: 'info', UPDATE_ROLE_PERM: 'warning'
  };
  return map[t] || 'info';
};
const typeLabel = (t: string) => {
  const map: Record<string, string> = {
    LOGIN: '登录', EXECUTE_PLAN: '执行预案', DELETE_PLAN: '删除预案',
    CREATE_PLAN: '新增预案', UPDATE_PLAN: '编辑预案', UPDATE_ROLE_PERM: '修改权限'
  };
  return map[t] || t;
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get('/api/audit/list', { params: query });
    if (res.code === 200) {
      logList.value = res.data.records;
      total.value = res.data.total;
    }
  } finally { loading.value = false; }
};

const fetchTypes = async () => {
  const res: any = await request.get('/api/audit/types');
  if (res.code === 200) eventTypes.value = res.data;
};

const resetQuery = () => { query.operator = ''; query.eventType = ''; query.current = 1; fetchList(); };

onMounted(() => { fetchList(); fetchTypes(); });
</script>

<style scoped>
.audit-container { padding: 20px; background: #f5f6fa; min-height: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header h2 { margin: 0 0 4px; font-size: 22px; color: #1a1a2e; }
.page-header p { margin: 0; color: #909399; font-size: 13px; }
.search-card { background: #fff; padding: 18px 20px 4px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); margin: 16px 0; }
.table-card { background: #fff; padding: 16px 20px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.pagination-wrap { margin-top: 14px; display: flex; justify-content: flex-end; }
</style>
