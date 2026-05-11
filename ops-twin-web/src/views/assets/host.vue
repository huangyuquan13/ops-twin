<template>
  <div class="asset-host-container">
    <div class="page-header">
      <div class="title-section">
        <h2 class="page-title">物理资产台账</h2>
        <p class="page-desc">管理机房物理服务器、架式设备及其 3D 空间坐标</p>
      </div>
      <div class="action-section">
        <el-button v-if="userStore.hasPerm('host:add')" type="primary" :icon="Plus" @click="handleAdd"
          >新增资产</el-button
        >
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <div class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="主机名称">
          <el-input
            v-model.trim="queryParams.hostname"
            placeholder="请输入主机名"
            clearable
          />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model.trim="queryParams.ipAddr" placeholder="请输入IP" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="归属机柜">
          <el-input v-model.trim="queryParams.cabinetId" placeholder="请输入机柜编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <el-table
        :data="hostList"
        v-loading="loading"
        style="width: 100%"
        border
        stripe
      >
        <!-- ... 列定义保持不变 ... -->
        <el-table-column prop="hostname" label="主机名称" min-width="150" />
        <el-table-column prop="hostType" label="主机类型" width="120">
          <template #default="scope">
            <el-tag size="small" v-if="scope.row.hostType">{{ scope.row.hostType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="用途描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ipAddr" label="IP地址" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cabinetId" label="归属机柜" width="120" />
        <el-table-column label="机柜插槽 (U位)" width="130">
          <template #default="scope">
            <span class="val-blue">{{ scope.row.rackPos ? scope.row.rackPos + 'U' : '未上架' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="硬件配置" min-width="150">
          <template #default="scope">
            <span class="config-text"
              >{{ scope.row.cpuCores }}核 / {{ scope.row.memoryGb }}GB</span
            >
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button v-if="userStore.hasPerm('host:edit')" link type="primary" @click="handleEdit(scope.row)"
              >编辑</el-button
            >
            <el-button v-if="userStore.hasPerm('host:delete')" link type="danger" @click="handleDelete(scope.row)"
              >删除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="650px"
      destroy-on-close
    >
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="120px"
        class="custom-form"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="主机名称" prop="hostname">
              <el-input v-model="form.hostname" placeholder="如：SRV-WEB-01" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主机类型" prop="hostType">
              <el-select v-model="form.hostType" placeholder="如：WEB, DB, APP" filterable allow-create default-first-option>
                <el-option label="WEB (前置网关)" value="WEB" />
                <el-option label="APP (应用服务)" value="APP" />
                <el-option label="DB (数据库)" value="DB" />
                <el-option label="CACHE (缓存)" value="CACHE" />
                <el-option label="LB (负载均衡)" value="LB" />
                <el-option label="MQ (消息队列)" value="MQ" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用途描述" prop="description">
              <el-input v-model="form.description" placeholder="一句话描述用途" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="IP地址" prop="ipAddr">
              <el-input v-model="form.ipAddr" placeholder="192.168.x.x" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运行状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择">
                <el-option label="健康" :value="1" />
                <el-option label="报警" :value="2" />
                <el-option label="宕机" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="CPU核数">
              <el-input-number v-model="form.cpuCores" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内存(GB)">
              <el-input-number v-model="form.memoryGb" :min="1" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 新增：3D 孪生拓扑绑定 -->
        <el-divider content-position="left">机架插槽拓扑</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="归属机柜编号" prop="cabinetId">
              <el-select v-model="form.cabinetId" placeholder="请选择机柜" style="width: 100%">
                <el-option
                  v-for="item in cabinetOptions"
                  :key="item.cabinetId"
                  :label="`${item.cabinetId} (${item.cabinetName})`"
                  :value="item.cabinetId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="插槽位置(U位)" prop="rackPos">
              <el-input-number v-model="form.rackPos" :min="1" :max="form.cabinetId ? (cabinetOptions.find(c => c.cabinetId === form.cabinetId)?.maxU || 42) : 42" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading"
          >保存到 CMDB</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import request from "@/api/request";
import { useUserStore } from '@/store/user';
const userStore = useUserStore();

// 数据列表状态
const loading = ref(false);
const hostList = ref([]);
const total = ref(0);
const cabinetOptions = ref<any[]>([]);
//响应式状态管理 统一的状态源
const queryParams = reactive({
  current: 1,
  size: 10,
  hostname: "",
  ipAddr: "",
  cabinetId: "",
});

// 表单与对话框状态
const dialogVisible = ref(false);
const dialogTitle = ref("");
const submitLoading = ref(false);
const formRef = ref();
const form = ref({
  id: null,
  hostname: "",
  hostType: "APP",
  description: "",
  ipAddr: "",
  status: 1,
  cpuCores: 8,
  memoryGb: 16,
  cabinetId: "",
  rackPos: 1,
});

const rules = {
  hostname: [{ required: true, message: "请输入主机名称", trigger: "blur" }],
  ipAddr: [
    { required: true, message: "请输入 IP 地址", trigger: "blur" },
    { pattern: /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/, message: "IP 地址格式不正确", trigger: "blur" }
  ],
  cabinetId: [{ required: true, message: "请选择归属机柜", trigger: "change" }],
  rackPos: [{ required: true, message: "请输入插槽位置", trigger: "blur" }],
};

// 核心功能：获取后端资产列表
const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get("/api/asset/host/list", {
      params: queryParams,
    });
    if (res.code === 200) {
      hostList.value = res.data.records; // 注意这里改成了 records
      total.value = res.data.total; // 获取总条数
    }
  } catch (error) {
    console.error("获取资产列表失败:", error);
  } finally {
    loading.value = false;
  }
};

// 获取所有机柜列表供下拉选择
const fetchCabinetList = async () => {
  try {
    const res: any = await request.get("/api/asset/cabinet/list/all");
    if (res.code === 200) {
      cabinetOptions.value = res.data;
    }
  } catch (error) {
    console.error("获取机柜列表失败:", error);
  }
};

// 分页操作
const handleSizeChange = (val: number) => {
  queryParams.size = val;
  fetchList();
};

const handleCurrentChange = (val: number) => {
  queryParams.current = val;
  fetchList();
};

// 搜索操作
const handleSearch = () => {
  queryParams.current = 1; // 搜索时重置为第一页
  fetchList();
};

// 重置搜索条件
const handleReset = () => {
  queryParams.hostname = "";
  queryParams.ipAddr = "";
  queryParams.cabinetId = "";
  queryParams.current = 1;
  fetchList();
};

const handleAdd = () => {
  dialogTitle.value = "新增物理资产";
  form.value = {
    id: null,
    hostname: "",
    hostType: "APP",
    description: "",
    ipAddr: "",
    status: 1,
    cpuCores: 8,
    memoryGb: 16,
    cabinetId: "",
    rackPos: 1,
  };
  dialogVisible.value = true;
};

// 打开编辑弹窗
const handleEdit = (row: any) => {
  dialogTitle.value = "编辑资产信息";
  form.value = { ...row }; // 浅拷贝，避免直接修改列表数据
  dialogVisible.value = true;
};

// 核心功能：提交数据（新增或更新）
const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        await request.post("/api/asset/host/save", form.value);
        ElMessage.success("CMDB 数据同步成功");
        dialogVisible.value = false;
        fetchList(); // 刷新列表
      } catch (error) {
        console.error("保存失败:", error);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

// 核心功能：删除资产
const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    `确定要从系统中永久移除主机 ${row.hostname} 吗？`,
    "警告",
    {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    },
  ).then(async () => {
    try {
      await request.delete(`/api/asset/host/delete/${row.id}`);
      ElMessage.success("资产已移除");
      fetchList();
    } catch (error) {
      console.error("删除失败:", error);
    }
  });
};

// UI 辅助函数
const getStatusType = (status: number) => {
  const map: any = { 1: "success", 2: "warning", 3: "danger" };
  return map[status] || "info";
};

const getStatusLabel = (status: number) => {
  const map: any = { 1: "健康", 2: "报警", 3: "宕机" };
  return map[status] || "未知";
};

// 页面挂载时立即获取数据
onMounted(() => {
  fetchList();
  fetchCabinetList(); // 加载机柜下拉框
});
</script>

<style scoped>
.asset-host-container {
  padding: 24px;
  background: transparent;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 26px;
  font-weight: 600;
  color: #303133; /* 改为深色文字 */
  margin: 0;
  letter-spacing: 1px;
}

.page-desc {
  color: #909399; /* 改为深色文字 */
  font-size: 14px;
  margin-top: 6px;
}

.search-card {
  background: #f5f7fa; /* 改为浅色背景 */
  padding: 24px 24px 6px;
  border-radius: 12px;
  margin-bottom: 24px;
  border: 1px solid #ebeef5;
}

.table-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

:deep(.el-table) {
  color: #303133; /* 强制表格文字为深色 */
}

/* 修复输入框文字颜色 */
:deep(.el-input__inner) {
  color: #303133 !important;
}

/* 修复数字输入框文字颜色 */
:deep(.el-input-number .el-input__inner) {
  color: #303133 !important;
}

/* 坐标输入项专项优化 */
.coord-item :deep(.el-form-item__label) {
  width: auto !important;
  padding-right: 8px;
}

.coord-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

:deep(.el-table__row:hover > td) {
  background-color: rgba(64, 158, 255, 0.05) !important;
}

.config-text {
  font-family: "JetBrains Mono", monospace;
  color: #409eff;
  font-weight: 500;
}

.coord-tag {
  background: rgba(54, 207, 201, 0.1);
  border-color: rgba(54, 207, 201, 0.2);
  color: #36cfc9;
}

.custom-form :deep(.el-input-number) {
  width: 100%;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
