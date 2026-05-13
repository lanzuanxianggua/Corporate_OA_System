<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-input v-model="searchUsername" placeholder="搜索用户名" clearable class="w-48" />
      <el-select v-model="searchStatus" placeholder="状态" clearable class="w-32">
        <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增员工</el-button>
    </div>
    <el-card>
      <el-table :data="userList" stripe>
        <el-table-column label="用户名" prop="username" />
        <el-table-column label="昵称" prop="nickname" />
        <el-table-column label="部门">
          <template #default="{ row }">{{ row.dept?.name || "-" }}</template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="邮箱" prop="email" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" class="mr-1">{{ role.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑员工' : '新增员工'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" class="w-full">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.name || d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" class="w-full">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="默认123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { getUserPage, getAllRoles, getDeptList } from "@/api/system";
import { addEmployee, updateEmployee, deleteEmployee } from "@/api/employee";

const searchUsername = ref("");
const searchStatus = ref<number | "">("");
const userList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const roleOptions = ref<any[]>([]);
const deptOptions = ref<any[]>([]);

const form = reactive({ id: undefined as number | undefined, username: "", nickname: "", phone: "", email: "", deptId: undefined as number | undefined, roleIds: [] as number[], password: "" });

const flattenDepts = (list: any[]): any[] => {
  const result: any[] = [];
  for (const item of list) {
    result.push(item);
    if (item.children?.length) result.push(...flattenDepts(item.children));
  }
  return result;
};

const openDialog = async (row?: any) => {
  isEdit.value = !!row;
  if (row) { Object.assign(form, { ...row, roleIds: row.roles?.map((r: any) => r.id) || [] }); }
  else { Object.assign(form, { id: undefined, username: "", nickname: "", phone: "", email: "", deptId: undefined, roleIds: [], password: "" }); }
  dialogVisible.value = true;
};

const fetchData = async () => {
  try {
    const r: any = await getUserPage({ page: page.value, pageSize: pageSize.value, username: searchUsername.value || undefined, status: searchStatus.value as any });
    if (r.data?.list) { userList.value = r.data.list; total.value = r.data.total || 0; }
  } catch {}
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      await updateEmployee({ id: form.id, username: form.username, empName: form.nickname, phone: form.phone, email: form.email, deptId: form.deptId });
    } else {
      await addEmployee({ username: form.username, empName: form.nickname, phone: form.phone, email: form.email, deptId: form.deptId, password: form.password || "123456" });
    }
    ElMessage.success(isEdit.value ? "编辑成功" : "新增成功");
    dialogVisible.value = false;
    await fetchData();
  } catch (e: any) { ElMessage.error(e.message || "操作失败"); }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await ElMessageBox.confirm("确定删除该员工？", "提示", { type: "warning" }); await deleteEmployee(id); ElMessage.success("删除成功"); await fetchData(); } catch {}
};

onMounted(async () => {
  fetchData();
  try { const r: any = await getAllRoles(); if (r.data) roleOptions.value = r.data; } catch {}
  try {
    const r: any = await getDeptList();
    if (r.data) {
      const flat = flattenDepts(Array.isArray(r.data) ? r.data : []);
      deptOptions.value = flat;
    }
  } catch {}
});
</script>
