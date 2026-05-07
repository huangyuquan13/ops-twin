<template>
  <div class="app-container">
    <!-- 搜索与操作区域 -->
    <div class="filter-card">
      <el-form :inline="true" :model="queryParams" class="filter-form">
        <el-form-item label="机柜编号">
          <el-input
            v-model.trim="queryParams.cabinetId"
            placeholder="如 CAB-01"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="机柜名称">
          <el-input v-model.trim="queryParams.cabinetName" placeholder="如 核心数据库机柜" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="action-bar">
        <el-button type="success" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增机柜
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <el-table :data="cabinetList" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="cabinetId" label="机柜编号" width="120" />
        <el-table-column prop="cabinetName" label="机柜名称" min-width="150" />
        <el-table-column prop="posX" label="X 坐标 (米)" width="120" align="center">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.posX }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="posZ" label="Z 坐标 (米)" width="120" align="center">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.posZ }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxU" label="高度 (U位)" width="100" align="center">
          <template #default="scope">
            {{ scope.row.maxU }}U
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
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

    <!-- 表单弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="机柜编号" prop="cabinetId">
          <el-input v-model="form.cabinetId" placeholder="请输入机柜编号(全局唯一)" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="机柜名称" prop="cabinetName">
          <el-input v-model="form.cabinetName" placeholder="请输入机柜描述" />
        </el-form-item>
        <el-form-item label="最大U位" prop="maxU">
          <el-input-number v-model="form.maxU" :min="8" :max="42" style="width: 150px" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="X 坐标" prop="posX">
              <el-input-number v-model="form.posX" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Z 坐标" prop="posZ">
              <el-input-number v-model="form.posZ" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import request from "@/api/request";

const loading = ref(false);
const cabinetList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  current: 1,
  size: 10,
  cabinetId: "",
  cabinetName: "",
});

const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref();
const form = ref<any>({
  id: null,
  cabinetId: "",
  cabinetName: "",
  posX: 0,
  posZ: 0,
  maxU: 42
});

const rules = {
  cabinetId: [{ required: true, message: "请输入机柜编号", trigger: "blur" }],
  cabinetName: [{ required: true, message: "请输入机柜名称", trigger: "blur" }],
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await request.get("/api/asset/cabinet/list", { params: queryParams });
    if (res.code === 200) {
      cabinetList.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    console.error("获取列表失败:", error);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.current = 1;
  fetchList();
};

const handleReset = () => {
  queryParams.cabinetId = "";
  queryParams.cabinetName = "";
  queryParams.current = 1;
  fetchList();
};

const handleSizeChange = (val: number) => {
  queryParams.size = val;
  fetchList();
};

const handleCurrentChange = (val: number) => {
  queryParams.current = val;
  fetchList();
};

const handleAdd = () => {
  dialogTitle.value = "新增机柜";
  form.value = {
    id: null,
    cabinetId: "",
    cabinetName: "",
    posX: 0,
    posZ: 0,
    maxU: 42
  };
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  dialogTitle.value = "编辑机柜";
  form.value = { ...row };
  dialogVisible.value = true;
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确认删除机柜 "${row.cabinetId}" 吗？如果机柜下有资产将无法删除。`, "警告", {
    type: "warning",
  }).then(async () => {
    const res: any = await request.delete(`/api/asset/cabinet/delete/${row.id}`);
    if (res.code === 200) {
      ElMessage.success("删除成功");
      fetchList();
    } else {
      ElMessage.error(res.msg || "删除失败");
    }
  }).catch(() => {});
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      const res: any = await request.post("/api/asset/cabinet/save", form.value);
      if (res.code === 200) {
        ElMessage.success("保存成功");
        dialogVisible.value = false;
        fetchList();
      } else {
        ElMessage.error(res.msg || "保存失败");
      }
    }
  });
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.filter-card {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
}
.table-card {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
