<template>
  <div>
    <!-- Search bar -->
    <div class="oa-mobile-toolbar flex items-center gap-3 mb-4 flex-wrap">
      <el-input v-model="searchName" placeholder="搜索员工姓名/工号" clearable class="w-56" :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="searchDeptId" placeholder="部门" clearable class="w-44">
        <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id!" />
      </el-select>
      <el-select v-model="searchStatus" placeholder="状态" clearable class="w-32">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div class="flex-1" />
      <el-button v-if="userStore.hasPermission('system:user:add')" type="primary" :icon="Plus" @click="openDialog()">新增员工</el-button>
      <el-button type="danger" :icon="Delete" :disabled="!selectedIds.length" @click="handleBatchDelete">批量删除</el-button>
      <el-button type="warning" :disabled="!selectedIds.length" @click="handleBatchStatus(0)">批量禁用</el-button>
      <el-button type="success" :disabled="!selectedIds.length" @click="handleBatchStatus(1)">批量启用</el-button>
    </div>

    <el-card>
      <el-table class="oa-desktop-table" :data="userList" v-loading="loading" stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="工号" prop="empCode" width="120" />
        <el-table-column label="姓名" min-width="120">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-avatar :size="28" :src="row.avatar || undefined">
                {{ row.empName?.charAt(0) || '' }}
              </el-avatar>
              <span>{{ row.empName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="部门" min-width="120">
          <template #default="{ row }">{{ getDeptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" width="130" />
        <el-table-column label="邮箱" prop="email" min-width="180" show-overflow-tooltip />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <template v-if="row.roles && row.roles.length">
              <el-tag v-for="role in row.roles" :key="role.id" size="small" class="mr-1">{{ role.roleName }}</el-tag>
            </template>
            <el-tag v-else size="small" type="info">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="warning" link @click="openRoleDialog(row)">角色</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无员工数据" />
        </template>
      </el-table>

      <div v-loading="loading" class="oa-mobile-list">
        <el-empty v-if="!userList.length" description="暂无员工数据" :image-size="72" />
        <div v-else class="oa-mobile-card-list">
          <article v-for="row in userList" :key="row.id || row.empCode" class="oa-mobile-card">
            <div class="oa-mobile-card-main">
              <div class="oa-mobile-card-title">
                <span class="flex items-center gap-2 min-w-0">
                  <el-avatar :size="30" :src="row.avatar || undefined">
                    {{ row.empName?.charAt(0) || '' }}
                  </el-avatar>
                  <span>{{ row.empName || '-' }}</span>
                </span>
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? "启用" : "禁用" }}
                </el-tag>
              </div>
              <div class="oa-mobile-card-subtitle">{{ row.empCode || '-' }}</div>
              <div class="oa-mobile-card-meta">
                <div class="oa-mobile-meta-row">
                  <span>部门</span>
                  <span>{{ getDeptName(row.deptId) }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>手机号</span>
                  <span>{{ row.phone || '-' }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>邮箱</span>
                  <span>{{ row.email || '-' }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>角色</span>
                  <span>
                    <template v-if="employeeRoles(row).length">
                      <el-tag v-for="role in employeeRoles(row)" :key="role.id" size="small" class="mr-1">{{ role.roleName }}</el-tag>
                    </template>
                    <el-tag v-else size="small" type="info">未分配</el-tag>
                  </span>
                </div>
              </div>
            </div>
            <div class="oa-mobile-card-actions">
              <el-button type="primary" plain @click="openDialog(row)">编辑</el-button>
              <el-button type="warning" plain @click="openRoleDialog(row)">角色</el-button>
              <el-button type="danger" plain :disabled="!row.id" @click="row.id && handleDelete(row.id)">删除</el-button>
            </div>
          </article>
        </div>
      </div>

      <div class="flex justify-end mt-4">
        <OaPagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchData" />
      </div>
    </el-card>

    <!-- Employee edit/add dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑员工' : '新增员工'" width="550px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="工号" prop="empCode"><el-input v-model="form.empCode" placeholder="请输入工号" /></el-form-item>
        <el-form-item label="姓名" prop="empName"><el-input v-model="form.empName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTreeData"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="选择部门"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editRoleIds" multiple placeholder="请选择角色" class="w-full">
            <el-option v-for="role in roleList" :key="role.id" :label="role.name || role.roleName" :value="role.id!" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则自动生成" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Role assignment dialog -->
    <el-dialog v-model="roleDialogVisible" title="角色分配" width="450px" :close-on-click-modal="false">
      <div class="mb-3">
        <span class="text-[var(--oa-muted)]">员工：</span>
        <span class="font-medium">{{ roleDialogEmpName }}</span>
      </div>
      <el-checkbox-group v-model="roleDialogRoleIds">
        <el-checkbox v-for="role in roleList" :key="role.id" :value="role.id!">{{ role.name || role.roleName }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="handleRoleAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Plus, Delete, Search } from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { getEmployeePage, addEmployee, updateEmployee, deleteEmployee } from "@/api/employee";
import { getAllRoles, getEmpRoles, assignRoles } from "@/api/system";
import { getDeptTree } from "@/api/dept";
import type { Employee, Role, Dept } from "@/types/api";

function generateRandomPassword(length = 10): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  let pwd = "";
  for (let i = 0; i < length; i++) {
    pwd += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return pwd;
}

const userStore = useUserStore();
const loading = ref(false);
const searchName = ref("");
const searchDeptId = ref<number | undefined>(undefined);
const searchStatus = ref<number | undefined>(undefined);
const userList = ref<Employee[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const selectedRows = ref<Employee[]>([]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const deptOptions = ref<Dept[]>([]);
const deptTreeData = ref<Dept[]>([]);
const roleList = ref<Role[]>([]);
const editRoleIds = ref<number[]>([]);
const formRef = ref<FormInstance>();

const formRules = reactive<FormRules>({
  empCode: [
    { required: true, message: "请输入工号", trigger: "blur" },
    { pattern: /^[A-Za-z0-9]+$/, message: "工号只能包含字母和数字", trigger: "blur" }
  ],
  empName: [
    { required: true, message: "请输入姓名", trigger: "blur" },
    { min: 2, message: "姓名至少2个字符", trigger: "blur" }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的11位手机号", trigger: "blur" }
  ],
  email: [
    { type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }
  ]
});

const form = reactive({
  id: undefined as number | undefined,
  empCode: "",
  empName: "",
  phone: "",
  email: "",
  deptId: undefined as number | undefined,
  password: ""
});

// Role dialog state
const roleDialogVisible = ref(false);
const roleDialogEmpId = ref<number>();
const roleDialogEmpName = ref("");
const roleDialogRoleIds = ref<number[]>([]);
const roleSaving = ref(false);

const flattenDepts = (list: Dept[]): Dept[] => {
  const result: Dept[] = [];
  for (const item of list) {
    result.push(item);
    if (item.children?.length) result.push(...flattenDepts(item.children));
  }
  return result;
};

const getDeptName = (deptId: number | undefined) => {
  if (!deptId) return "-";
  return deptOptions.value.find((d: any) => d.id === deptId)?.deptName || "-";
};

const employeeRoles = (row: Employee) => ((row as any).roles || []) as Role[];

const fetchRoles = async () => {
  try {
    const res = await getAllRoles();
    roleList.value = res.data || [];
  } catch {
    roleList.value = [];
  }
};

const handleSearch = () => {
  page.value = 1;
  fetchData();
};

const resetSearch = () => {
  searchName.value = "";
  searchDeptId.value = undefined;
  searchStatus.value = undefined;
  page.value = 1;
  fetchData();
};

const handleSelectionChange = (rows: Employee[]) => {
  selectedRows.value = rows;
  selectedIds.value = rows.map((r) => r.id!);
};

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 名员工？`, "批量删除", { type: "warning" });
    for (const id of selectedIds.value) {
      await deleteEmployee(id);
    }
    ElMessage.success("批量删除成功");
    fetchData();
  } catch { /* cancelled */ }
};

const handleBatchStatus = async (status: number) => {
  const label = status === 1 ? "启用" : "禁用";
  try {
    await ElMessageBox.confirm(`确定${label}选中的 ${selectedIds.value.length} 名员工？`, `批量${label}`, { type: "warning" });
    for (const row of selectedRows.value) {
      await updateEmployee({ id: row.id, empCode: row.empCode, empName: row.empName, phone: row.phone, email: row.email, deptId: row.deptId, status } as any);
    }
    ElMessage.success(`批量${label}成功`);
    fetchData();
  } catch { /* cancelled */ }
};

const openDialog = async (row?: Employee) => {
  isEdit.value = !!row;
  editRoleIds.value = [];
  if (row) {
    Object.assign(form, {
      id: row.id,
      empCode: row.empCode || "",
      empName: row.empName || "",
      phone: row.phone || "",
      email: row.email || "",
      deptId: row.deptId,
      password: ""
    });
    try {
      const res = await getEmpRoles(row.id!);
      editRoleIds.value = (res.data || []).map((id) => Number(id));
    } catch {
      editRoleIds.value = [];
    }
  } else {
    Object.assign(form, { id: undefined, empCode: "", empName: "", phone: "", email: "", deptId: undefined, password: "" });
  }
  dialogVisible.value = true;
};

const openRoleDialog = async (row: Employee) => {
  roleDialogEmpId.value = row.id;
  roleDialogEmpName.value = row.empName || "";
  roleDialogRoleIds.value = [];
  try {
    const res = await getEmpRoles(row.id!);
    roleDialogRoleIds.value = (res.data || []).map((id) => Number(id));
  } catch {
    roleDialogRoleIds.value = [];
  }
  roleDialogVisible.value = true;
};

const handleRoleAssign = async () => {
  if (!roleDialogEmpId.value) return;
  roleSaving.value = true;
  try {
    await assignRoles(roleDialogEmpId.value, roleDialogRoleIds.value);
    ElMessage.success("角色分配成功");
    roleDialogVisible.value = false;
    fetchData();
  } catch (e: any) {
    ElMessage.error(e.message || "角色分配失败");
  } finally {
    roleSaving.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNum: page.value, pageSize: pageSize.value };
    if (searchName.value) params.empName = searchName.value;
    if (searchDeptId.value !== undefined) params.deptId = searchDeptId.value;
    if (searchStatus.value !== undefined) params.status = searchStatus.value;
    const r = await getEmployeePage(params as any);
    if (r.data?.list) {
      userList.value = r.data.list;
      total.value = r.data.total || 0;
    }
  } catch { /* error handled by interceptor */ }
  finally { loading.value = false; }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      await updateEmployee({ id: form.id, empCode: form.empCode, empName: form.empName, phone: form.phone, email: form.email, deptId: form.deptId });
      await assignRoles(form.id, editRoleIds.value);
    } else {
      const res = await addEmployee({ empCode: form.empCode, empName: form.empName, phone: form.phone, email: form.email, deptId: form.deptId, password: form.password || generateRandomPassword() });
      const newId = res.data;
      if (newId && editRoleIds.value.length > 0) {
        await assignRoles(newId, editRoleIds.value);
      }
    }
    ElMessage.success(isEdit.value ? "编辑成功" : "新增成功");
    dialogVisible.value = false;
    await fetchData();
  } catch (e: any) { ElMessage.error(e.message || "操作失败"); }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm("确定删除该员工？", "提示", { type: "warning" });
    await deleteEmployee(id);
    ElMessage.success("删除成功");
    await fetchData();
  } catch { /* cancelled */ }
};

onMounted(async () => {
  fetchRoles();
  fetchData();
  try {
    const r = await getDeptTree();
    if (r.data) {
      const raw = Array.isArray(r.data) ? r.data : [];
      deptTreeData.value = raw;
      deptOptions.value = flattenDepts(raw);
    }
  } catch { /* ignore */ }
});
</script>
