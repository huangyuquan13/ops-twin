<template>
  <div class="role-container">
    <div class="role-layout">
      <!-- 左侧角色列表 -->
      <div class="role-list-panel">
        <div class="panel-header">
          <h3>角色列表</h3>
          <el-button type="primary" size="small" :icon="Plus" @click="handleAddRole">新增</el-button>
        </div>
        <el-menu :default-active="activeRoleId" @select="handleSelectRole">
          <el-menu-item v-for="r in roleList" :key="r.id" :index="String(r.id)">
            <span>{{ r.roleName }}</span>
            <el-tag size="small" style="margin-left: auto;">{{ r.roleCode }}</el-tag>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 右侧权限树 -->
      <div class="perm-panel">
        <div class="panel-header" v-if="activeRole">
          <h3>权限配置 · {{ activeRole.roleName }}</h3>
          <div class="panel-actions">
            <el-button type="primary" :loading="saving" @click="savePermissions">保存权限</el-button>
            <el-button type="danger" plain @click="handleDeleteRole">删除角色</el-button>
          </div>
        </div>
        <div v-if="!activeRole" class="empty-hint">请先选择左侧角色</div>
        <div v-else class="perm-tree-wrap">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
            <el-checkbox
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
              style="font-weight: 600;"
            >全选/全不选</el-checkbox>
            <el-button size="small" :icon="Plus" @click="handleAddPerm()">新增根节点</el-button>
          </div>
          <el-tree
            ref="treeRef"
            :data="permTree"
            show-checkbox
            node-key="id"
            :default-checked-keys="checkedPermIds"
            :props="{ label: 'title', children: 'children' }"
            default-expand-all
            check-strictly
            @check="onCheck"
          >
            <template #default="{ data }">
              <div class="perm-node">
                <span style="display: flex; align-items: center; gap: 8px; flex: 1;">
                  <span>{{ data.title }}</span>
                  <el-tag v-if="data.type === 2" size="small" type="info">按钮</el-tag>
                  <el-tag v-else size="small" type="primary">菜单</el-tag>
                  <span v-if="data.permissionCode" style="color: #909399; font-size: 11px;">
                    {{ data.permissionCode }}
                  </span>
                </span>
                <span class="perm-actions">
                  <el-button link size="small" @click.stop="handleAddPerm(data)" :icon="Plus">子节点</el-button>
                  <el-button link size="small" @click.stop="handleEditPerm(data)" :icon="Edit">编辑</el-button>
                  <el-button link size="small" type="danger" @click.stop="handleDeletePerm(data)" :icon="Delete">删除</el-button>
                </span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>
    </div>

    <!-- 新增/编辑权限节点弹窗 -->
    <el-dialog :title="permDialogTitle" v-model="permDialogVisible" width="420px">
      <el-form :model="permForm" :rules="permRules" ref="permFormRef" label-width="90px">
        <el-form-item label="节点标题" prop="title">
          <el-input v-model="permForm.title" placeholder="如：预案方案库" />
        </el-form-item>
        <el-form-item label="节点类型" prop="type">
          <el-radio-group v-model="permForm.type">
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径" v-if="permForm.type === 1">
          <el-input v-model="permForm.path" placeholder="如：/tasks/strategy" />
        </el-form-item>
        <el-form-item label="权限编码" v-if="permForm.type === 2">
          <el-input v-model="permForm.permissionCode" placeholder="如：strategy:add" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="permForm.icon" placeholder="如：Setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPermForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="420px">
      <el-form :model="roleForm" :rules="rules" ref="roleFormRef" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="如：运维工程师" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="如：OPS" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoleForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import request from '@/api/request';
import { useUserStore } from '@/store/user';

const userStore = useUserStore();

const roleList = ref<any[]>([]);
const activeRoleId = ref('');
const activeRole = computed(() => roleList.value.find(r => String(r.id) === activeRoleId.value));
const saving = ref(false);
const treeRef = ref();

// 权限树（从后端全量拉取，非角色相关）
const allPerms = ref<any[]>([]);
const checkedPermIds = ref<number[]>([]);

const permTree = computed(() => buildTree(allPerms.value));
const allPermIds = computed(() => {
  const ids: number[] = [];
  const walk = (nodes: any[]) => {
    nodes.forEach(n => { ids.push(n.id); if (n.children) walk(n.children); });
  };
  walk(permTree.value);
  return ids;
});
const selectAll = computed(() => checkedPermIds.value.length > 0 && checkedPermIds.value.length === allPermIds.value.length);
const isIndeterminate = computed(() => checkedPermIds.value.length > 0 && checkedPermIds.value.length < allPermIds.value.length);

const buildTree = (flat: any[]) => {
  const map = new Map<number, any>();
  const roots: any[] = [];
  flat.forEach(p => { map.set(p.id, { ...p, children: [] }); });
  flat.forEach(p => {
    const node = map.get(p.id)!;
    if (p.parentId && map.has(p.parentId)) {
      map.get(p.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
};

// 权限节点 CRUD
const permDialogVisible = ref(false);
const permDialogTitle = ref('新增权限节点');
const permFormRef = ref();
const permForm = ref<any>({ title: '', type: 1, path: '', permissionCode: '', icon: '', sort: 0, parentId: null, id: null });
const permRules = {
  title: [{ required: true, message: '请输入节点标题' }],
  type: [{ required: true, message: '请选择节点类型' }],
};

const handleAddPerm = (parent?: any) => {
  permDialogTitle.value = parent ? `添加子节点 · ${parent.title}` : '新增权限节点';
  permForm.value = { title: '', type: 1, path: '', permissionCode: '', icon: '', sort: 0, parentId: parent?.id ?? null, id: null };
  permDialogVisible.value = true;
};

const handleEditPerm = (node: any) => {
  permDialogTitle.value = `编辑 · ${node.title}`;
  permForm.value = { ...node };
  permDialogVisible.value = true;
};

const handleDeletePerm = (node: any) => {
  ElMessageBox.confirm(`确认删除「${node.title}」及其所有子节点？`, '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/api/system/permission/delete/${node.id}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchAllPerms();
      if (activeRoleId.value) handleSelectRole(activeRoleId.value);
    }
  }).catch(() => {});
};

const submitPermForm = async () => {
  if (!permFormRef.value) return;
  await permFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    const res: any = await request.post('/api/system/permission/save', permForm.value);
    if (res.code === 200) {
      ElMessage.success(permForm.value.id ? '更新成功' : '新增成功');
      permDialogVisible.value = false;
      fetchAllPerms();
    }
  });
};

const fetchRoles = async () => {
  const res: any = await request.get('/api/system/role/list');
  if (res.code === 200) roleList.value = res.data;
};

const fetchAllPerms = async () => {
  const res: any = await request.get('/api/system/permissions/all');
  if (res.code === 200) {
    allPerms.value = res.data;
    buildParentMap(res.data);
  }
};

const handleSelectRole = async (index: string) => {
  activeRoleId.value = index;
  const res: any = await request.get(`/api/system/role/${index}/permissions`);
  if (res.code === 200) {
    const ids = ensureParents(res.data);
    checkedPermIds.value = ids;
    await nextTick();
    treeRef.value?.setCheckedKeys(ids);
  }
};

// 构建 parentId 映射，用于回溯祖先
let parentMap: Record<number, number | null> = {};

const buildParentMap = (list: any[]) => {
  list.forEach(p => { parentMap[p.id] = p.parentId || null; });
};

// 补祖先：如果子节点勾中，必须补上所有祖先
const ensureParents = (ids: number[]): number[] => {
  const set = new Set(ids);
  ids.forEach(id => {
    let parent = parentMap[id];
    while (parent) { set.add(parent); parent = parentMap[parent] || null; }
  });
  return Array.from(set);
};

// 递归获取某节点所有子孙 ID
const getDescendants = (nodeId: number, tree: any[]): number[] => {
  const result: number[] = [];
  const walk = (nodes: any[]) => {
    nodes.forEach(n => {
      result.push(n.id);
      if (n.children?.length) walk(n.children);
    });
  };
  const node = findNode(nodeId, tree);
  if (node?.children?.length) walk(node.children);
  return result;
};

const findNode = (id: number, nodes: any[]): any => {
  for (const n of nodes) {
    if (n.id === id) return n;
    if (n.children?.length) {
      const found = findNode(id, n.children);
      if (found) return found;
    }
  }
  return null;
};

const handleSelectAll = (val: boolean) => {
  if (val) {
    const ids = allPermIds.value;
    checkedPermIds.value = ids;
    treeRef.value?.setCheckedKeys(ids);
  } else {
    checkedPermIds.value = [];
    treeRef.value?.setCheckedKeys([]);
  }
};

const onCheck = (currentNode: any, data: any) => {
  let ids: number[] = data.checkedKeys.map(Number);

  // 保存之前的菜单节点（type=1），子变更不应丢掉菜单
  const prevMenuIds = new Set(
    checkedPermIds.value.filter(id => {
      const p = allPerms.value.find((i: any) => i.id === id);
      return p?.type === 1;
    })
  );

  // 如果当前节点被取消且是父节点（用户主动取消菜单）→ 允许取消
  if (!ids.includes(currentNode.id) && currentNode.children?.length) {
    const descendants = getDescendants(currentNode.id, permTree.value);
    ids = ids.filter(id => !descendants.includes(id));
    treeRef.value?.setCheckedKeys(ids);
  }

  // 补上祖先：子节点勾中 → 父必须勾中
  ids = ensureParents(ids);

  // 补回之前存在的菜单节点（不被子节点取消牵连）
  // 但如果用户主动取消的就是该菜单，允许
  prevMenuIds.forEach(id => {
    if (!ids.includes(id) && id !== currentNode.id) {
      ids.push(id);
    }
  });

  checkedPermIds.value = ids;
};

const refreshCurrentUserMenus = async () => {
  const roleId = userStore.userInfo?.roleId;
  if (!roleId) return;
  try {
    const permRes: any = await request.get('/api/system/menus', { params: { roleId } });
    if (permRes.code === 200) {
      userStore.setPermissions(permRes.data.permissions || []);
      userStore.setMenus(permRes.data.menus || []);
    }
  } catch { /* 降级 */ }
};

const savePermissions = async () => {
  if (!activeRoleId.value) return;
  saving.value = true;
  try {
    const res: any = await request.post(`/api/system/role/${activeRoleId.value}/permissions`, {
      permissionIds: checkedPermIds.value
    });
    if (res.code === 200) {
      ElMessage.success('权限保存成功');
      // 如果改的是当前用户自己的角色，刷新全局 menus 和权限
      if (String(activeRoleId.value) === String(userStore.userInfo?.roleId)) {
        await refreshCurrentUserMenus();
      }
    }
  } finally { saving.value = false; }
};

// 角色 CRUD
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const roleFormRef = ref();
const roleForm = ref({ id: null, roleName: '', roleCode: '', description: '' });
const rules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }],
};

const handleAddRole = () => {
  dialogTitle.value = '新增角色';
  roleForm.value = { id: null, roleName: '', roleCode: '', description: '' };
  dialogVisible.value = true;
};

const submitRoleForm = async () => {
  if (!roleFormRef.value) return;
  await roleFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return;
    const res: any = await request.post('/api/system/role/save', roleForm.value);
    if (res.code === 200) {
      ElMessage.success('保存成功');
      dialogVisible.value = false;
      fetchRoles();
      if (!activeRoleId.value) activeRoleId.value = String(res.data.id);
    }
  });
};

const handleDeleteRole = () => {
  if (!activeRoleId.value) return;
  ElMessageBox.confirm('确认删除该角色及其权限配置？', '警告', { type: 'warning' }).then(async () => {
    const res: any = await request.delete(`/api/system/role/delete/${activeRoleId.value}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      activeRoleId.value = '';
      fetchRoles();
    }
  }).catch(() => {});
};

onMounted(() => {
  fetchRoles();
  fetchAllPerms();
});
</script>

<style scoped>
.role-container { height: calc(100vh - 84px); background: #f0f2f5; margin: -20px; }
.role-layout { display: flex; height: 100%; }
.role-list-panel { width: 220px; background: #fff; border-right: 1px solid #e4e7ed; display: flex; flex-direction: column; }
.panel-header { padding: 15px; border-bottom: 1px solid #e4e7ed; display: flex; justify-content: space-between; align-items: center; }
.panel-header h3 { margin: 0; font-size: 15px; }
.perm-panel { flex: 1; padding: 15px 24px; overflow-y: auto; }
.panel-actions { display: flex; gap: 8px; }
.perm-tree-wrap { margin-top: 8px; }
.empty-hint { text-align: center; color: #909399; margin-top: 80px; }

/* 权限树节点 hover 操作按钮 */
.perm-node { display: flex; align-items: center; width: 100%; }
.perm-actions { display: none; margin-left: auto; white-space: nowrap; }
.perm-node:hover .perm-actions { display: inline-flex; gap: 2px; }
</style>
