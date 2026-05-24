<template>
  <div class="user-container">
    <div class="page-header">
      <div class="title-section">
        <h2 class="page-title">系统用户管理</h2>
        <p class="page-desc">管理系统登录人员、分配角色与头像</p>
      </div>
      <div class="action-section">
        <el-button v-if="userStore.hasPerm('user:add')" type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
        <el-button :icon="Refresh" @click="fetchList">刷新</el-button>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <div class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model.trim="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <el-table :data="userList" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <!-- 头像展示列：重点！ -->
        <el-table-column label="头像" width="100" align="center">
          <template #default="scope">
            <el-avatar
              :size="40"
              :src="scope.row.avatar && !scope.row.avatar.startsWith('http') ? getAvatarUrl(scope.row.avatar) : ''"
              shape="square"
            >
              <el-icon :size="24"><UserFilled /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="用户名" min-width="150" />
        
        <el-table-column prop="roleId" label="角色" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.roleId === 1 ? 'danger' : 'info'">
              {{ scope.row.roleId === 1 ? '超级管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button v-if="userStore.hasPerm('user:edit')" link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="userStore.hasPerm('user:delete')" link type="danger" @click="handleDelete(scope.row)">删除</el-button>
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
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="custom-form">
        
        <!-- 头像上传核心区 -->
        <el-form-item label="用户头像" class="avatar-form-item">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadFile"
            :before-upload="beforeAvatarUpload"
          >
            <!-- 回显刚刚上传成功的头像 -->
            <img v-if="isCustomAvatar(form.avatar)" :src="getAvatarUrl(form.avatar)" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon" :size="32"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议上传 1:1 比例的正方形图片，大小不超过 2MB</div>
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入系统登录名" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" placeholder="请输入密码(默认123456)" type="password" show-password />
        </el-form-item>

        <el-form-item label="分配角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择系统角色" style="width: 100%;">
            <el-option label="超级管理员" :value="1" />
            <el-option label="普通用户" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">保存用户</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, UserFilled } from '@element-plus/icons-vue'
import request from '@/api/request' // 使用我们封装好的 Axios
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 列表状态
const loading = ref(false)
const userList = ref([])
const total = ref(0)
const queryParams = reactive({
  current: 1,
  size: 10,
  username: ''
})

// 表单状态
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref()
const form = ref({
  id: null,
  username: '',
  password: '',
  avatar: '',
  roleId: 2
})

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  roleId: [{ required: true, message: '必须为用户分配角色', trigger: 'change' }]
}

// 核心逻辑：计算头像真实访问地址
const getAvatarUrl = (path: string) => {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${import.meta.env.VITE_API_BASE}${path}`
}

// 判断是否为自定义上传的头像（相对路径），而非外部默认头像
const isCustomAvatar = (path: string) => {
  if (!path) return false
  if (path.startsWith('http://') || path.startsWith('https://')) return false
  return true
}

// =======================
// 上传头像核心功能
// =======================
const beforeAvatarUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('上传头像图片只能是图片格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 覆盖默认上传行为，使用 axios 上传，自动带上 Token
const uploadFile = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  
  try {
    // 这里的接口对应我们刚才写的后端 UserController
    const res: any = await request.post('/api/system/user/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    if (res.code === 200) {
      // res.data 是后端返回的短路径，如 /uploads/xxxx.jpg
      form.value.avatar = res.data
      ElMessage.success('头像上传成功')
      options.onSuccess(res)
    } else {
      ElMessage.error(res.message || '上传失败')
      options.onError(res)
    }
  } catch (error) {
    ElMessage.error('上传发生网络错误')
    options.onError(error)
  }
}

// =======================
// CRUD 常规操作
// =======================
const fetchList = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/system/user/list', {
      params: queryParams
    })
    if (res.code === 200) {
      userList.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.current = 1
  fetchList()
}

const handleReset = () => {
  queryParams.username = ''
  queryParams.current = 1
  fetchList()
}

const handleSizeChange = (val: number) => {
  queryParams.size = val
  fetchList()
}

const handleCurrentChange = (val: number) => {
  queryParams.current = val
  fetchList()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  form.value = { id: null, username: '', password: '', avatar: '', roleId: 2 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑用户'
  form.value = { ...row }
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        await request.post('/api/system/user/save', form.value)
        ElMessage.success('用户保存成功')
        dialogVisible.value = false
        fetchList()
        
        // 【Pinia 联动核心】: 如果修改的是当前登录用户的资料，顺便更新 Vuex(Pinia) 里的头像
        if (userStore.userInfo.id === form.value.id) {
            userStore.updateAvatar(form.value.avatar)
        }
        
      } catch (error) {
        console.error(error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除用户 [${row.username}] 吗？`, '警告', { type: 'warning' })
    .then(async () => {
      await request.delete(`/api/system/user/delete/${row.id}`)
      ElMessage.success('用户已删除')
      fetchList()
    })
    .catch(() => {})
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
/* 继承之前的页面结构样式 */
.user-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.title-section .page-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}
.title-section .page-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
.search-card {
  background: #fff;
  padding: 24px 24px 0 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
}
.table-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,21,41,0.08);
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* =======================
   头像上传器定制样式
======================= */
.avatar-form-item {
  display: flex;
  flex-direction: column;
}
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}
.avatar-uploader:hover {
  border-color: #409eff;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  line-height: 100px;
}
.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.4;
}
</style>