<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-input v-model="searchName" placeholder="搜索员工姓名" clearable class="w-48" />
      <el-select v-model="searchStatus" placeholder="状态" clearable class="w-32">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增员工</el-button>
    </div>
    <el-card>
      <el-table :data="userList" stripe>
        <el-table-column label="工号" prop="username" width="120" />
        <el-table-column label="姓名" prop="nickname" />
        <el-table-column label="部门">
          <template #default="{ row }">{{ row.dept?.name || "-" }}</template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" />
        <el-table-column label="邮箱" prop="email" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag v-for="role in (row.roles || [])" :key="role.id" size="small" class="mr-1">{{ role.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
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
        <el-form-item label="工号"><el-input v-model="form.empCode" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.empName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" clearable class="w-full">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.id" />
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
import { getEmployeePage, addEmployee, updateEmployee, deleteEmployee } from "@/api/employee";
import { getDeptList } from "@/api/system";

const searchName = ref("");
const searchStatus = ref<number | undefined>(undefined);
const userList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const deptOptions = ref<any[]>([]);

const form = reactive({
  id: undefined as number | undefined,
  empCode: "",
  empName: "",
  phone: "",
  email: "",
  deptId: undefined as number | undefined,
  password: ""
});

const flattenDepts = (list: any[]): any[] => {
  const result: any[] = [];
  for (const item of list) {
    result.push(item);
    if (item.children?.length) result.push(...flattenDepts(item.children));
  }
  return result;
};

const openDialog = (row?: any) => {
  isEdit.value = !!row;
  if (row) {
    Object.assign(form, {
      id: row.id,
      empCode: row.username || row.empCode || "",
      empName: row.nickname || row.empName || "",
      phone: row.phone || "",
      email: row.email || "",
      deptId: row.dept?.id || row.deptId,
      password: ""
    });
  } else {
    Object.assign(form, { id: undefined, empCode: "", empName: "", phone: "", email: "", deptId: undefined, password: "" });
  }
  dialogVisible.value = true;
};

const fetchData = async () => {
  try {
    const params: any = { pageNum: page.value, pageSize: pageSize.value };
    if (searchName.value) params.empName = searchName.value;
    if (searchStatus.value !== undefined) params.status = searchStatus.value;
    const r: any = await getEmployeePage(params);
    if (r.data?.list) {
      userList.value = r.data.list;
      total.value = r.data.total || 0;
    }
  } catch {}
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      await updateEmployee({ id: form.id, empCode: form.empCode, empName: form.empName, phone: form.phone, email: form.email, deptId: form.deptId });
    } else {
      await addEmployee({ empCode: form.empCode, empName: form.empName, phone: form.phone, email: form.email, deptId: form.deptId, password: form.password || "123456" });
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
  try {
    const r: any = await getDeptList();
    if (r.data) {
      const flat = flattenDepts(Array.isArray(r.data) ? r.data : []);
      deptOptions.value = flat;
    }
  } catch {}
});
</script>
