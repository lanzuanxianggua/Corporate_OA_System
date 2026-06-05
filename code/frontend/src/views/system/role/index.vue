<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <el-input v-model="searchKey" placeholder="搜索角色名称" clearable class="w-56" :prefix-icon="Search" @keyup.enter="fetchRoles" @clear="fetchRoles" />
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
    </div>
    <el-card>
      <el-table :data="roleList" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="角色名称" prop="roleName" min-width="150" />
        <el-table-column label="角色标识" prop="roleKey" min-width="120" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="roleSort" width="80" align="center" />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openPermDialog(row)">权限</el-button>
            <el-popconfirm title="确定删除该角色?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无角色数据" />
        </template>
      </el-table>
    </el-card>

    <!-- Role edit/add dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="name"><el-input v-model="form.name" placeholder="请输入角色名称" /></el-form-item>
        <el-form-item label="角色标识" prop="code"><el-input v-model="form.code" placeholder="如 ADMIN、USER" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- Permission assignment dialog -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="550px" :close-on-click-modal="false">
      <div class="mb-3">
        <span class="text-[#606266]">角色：</span>
        <span class="font-medium">{{ permDialogRoleName }}</span>
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :props="{ label: 'menuName', children: 'children' }"
        :check-strictly="false"
        default-expand-all
      />
      <div v-if="menuTree.length === 0" class="text-center py-4 text-[#909399]">加载菜单中...</div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="handlePermSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getRolePage, addRole, updateRole, deleteRole } from "@/api/system";
import { getMenuTree, getMenuByRole, assignRoleMenus } from "@/api/menu";

const formRef = ref<FormInstance>();
const formRules: FormRules = {
  name: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  code: [{ required: true, message: "请输入角色标识", trigger: "blur" }]
};

const loading = ref(false);
const roleList = ref<any[]>([]);
const searchKey = ref("");
const dialogVisible = ref(false);
const saving = ref(false);
const form = reactive({ id: undefined as number | undefined, name: "", code: "", status: "0" });

// Permission dialog state
const permDialogVisible = ref(false);
const permDialogRoleId = ref<number>();
const permDialogRoleName = ref("");
const permSaving = ref(false);
const menuTree = ref<any[]>([]);
const checkedMenuIds = ref<number[]>([]);
const menuTreeRef = ref<any>();

const fetchRoles = async () => {
  loading.value = true;
  try {
    const r: any = await getRolePage();
    let list: any[] = [];
    if (r.data?.list) list = r.data.list;
    else if (Array.isArray(r.data)) list = r.data;
    // Client-side search filter
    if (searchKey.value.trim()) {
      const key = searchKey.value.trim().toLowerCase();
      list = list.filter((item: any) => (item.roleName || "").toLowerCase().includes(key));
    }
    roleList.value = list;
  } catch { /* ignore */ }
  finally { loading.value = false; }
};

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, name: row.roleName, code: row.roleKey, status: row.status ?? 1 });
  } else {
    Object.assign(form, { id: undefined, name: "", code: "", status: 1 });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    const payload: any = {
      ...form,
      roleName: form.name,
      roleKey: form.code
    };
    if (form.id) {
      payload.id = form.id;
      await updateRole(payload);
    } else {
      await addRole(payload);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchRoles();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteRole(id);
    ElMessage.success("删除成功");
    fetchRoles();
  } catch {
    ElMessage.error("删除失败");
  }
};

// Permission assignment
const openPermDialog = async (row: any) => {
  permDialogRoleId.value = row.id;
  permDialogRoleName.value = row.roleName || "";
  checkedMenuIds.value = [];
  permDialogVisible.value = true;

  // Load menu tree
  try {
    const res: any = await getMenuTree();
    menuTree.value = res.data || [];
  } catch {
    menuTree.value = [];
  }

  // Load current role's menus
  try {
    const res: any = await getMenuByRole(row.id);
    checkedMenuIds.value = (res.data || []).map((m: any) => m.id);
  } catch {
    checkedMenuIds.value = [];
  }
};

const handlePermSave = async () => {
  if (!permDialogRoleId.value) return;
  permSaving.value = true;
  try {
    const tree = menuTreeRef.value;
    // Get checked + half-checked to include parent nodes
    const checkedKeys = tree ? tree.getCheckedKeys() : [];
    const halfCheckedKeys = tree ? tree.getHalfCheckedKeys() : [];
    const allKeys = [...checkedKeys, ...halfCheckedKeys].map(Number);

    await assignRoleMenus({ roleId: permDialogRoleId.value, menuIds: allKeys });
    ElMessage.success("权限分配成功");
    permDialogVisible.value = false;
  } catch {
    ElMessage.error("权限分配失败");
  } finally {
    permSaving.value = false;
  }
};

onMounted(fetchRoles);
</script>
