<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">考勤组管理</span>
          <el-button type="primary" @click="openDialog()">新增考勤组</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="groupName" label="考勤组名称" min-width="120" />
        <el-table-column prop="workStartTime" label="上班时间" width="100" />
        <el-table-column prop="workEndTime" label="下班时间" width="100" />
        <el-table-column label="工作日" min-width="150">
          <template #default="{ row }">{{ formatWorkDays(row.workDays) }}</template>
        </el-table-column>
        <el-table-column prop="empCount" label="员工数" width="80" align="center" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openAssignDialog(row)">分配员工</el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑考勤组' : '新增考勤组'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入考勤组名称" />
        </el-form-item>
        <el-form-item label="上班时间" prop="workStartTime">
          <el-time-picker v-model="form.workStartTime" format="HH:mm" value-format="HH:mm" placeholder="上班时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下班时间" prop="workEndTime">
          <el-time-picker v-model="form.workEndTime" format="HH:mm" value-format="HH:mm" placeholder="下班时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="工作日" prop="workDays">
          <el-checkbox-group v-model="form.workDays">
            <el-checkbox v-for="d in weekDays" :key="d.value" :value="d.value">{{ d.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配员工" width="500px" :close-on-click-modal="false">
      <el-select v-model="assignEmpIds" multiple filterable placeholder="请选择员工" style="width: 100%">
        <el-option v-for="emp in employeeList" :key="emp.id" :label="emp.empName" :value="emp.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getAttendanceGroupPage, addAttendanceGroup, updateAttendanceGroup, deleteAttendanceGroup, assignEmployees } from "@/api/attendanceGroup";
import { getEmployeePage } from "@/api/employee";

const weekDays = [
  { label: "周一", value: 1 }, { label: "周二", value: 2 }, { label: "周三", value: 3 },
  { label: "周四", value: 4 }, { label: "周五", value: 5 }, { label: "周六", value: 6 }, { label: "周日", value: 7 }
];

const formatWorkDays = (days: number[]) => {
  if (!days?.length) return "-";
  return days.map(d => weekDays.find(w => w.value === d)?.label || d).join("、");
};

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getAttendanceGroupPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, groupName: "", workStartTime: "", workEndTime: "", workDays: [] as number[] });
const rules = reactive<FormRules>({
  groupName: [{ required: true, message: "请输入考勤组名称", trigger: "blur" }],
  workStartTime: [{ required: true, message: "请选择上班时间", trigger: "change" }],
  workEndTime: [{ required: true, message: "请选择下班时间", trigger: "change" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, groupName: row.groupName, workStartTime: row.workStartTime, workEndTime: row.workEndTime, workDays: row.workDays || [] });
  } else {
    Object.assign(form, { id: undefined, groupName: "", workStartTime: "", workEndTime: "", workDays: [] });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) {
      await updateAttendanceGroup(form);
    } else {
      await addAttendanceGroup(form);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deleteAttendanceGroup(id);
  ElMessage.success("删除成功");
  fetchList();
};

const assignDialogVisible = ref(false);
const assigning = ref(false);
const currentGroupId = ref<number>();
const assignEmpIds = ref<number[]>([]);
const employeeList = ref<any[]>([]);

const fetchEmployeeList = async () => {
  try {
    const res: any = await getEmployeePage({ pageNum: 1, pageSize: 200 });
    employeeList.value = res.data?.list || [];
  } catch { /* ignore */ }
};

const openAssignDialog = (row: any) => {
  currentGroupId.value = row.id;
  assignEmpIds.value = [];
  assignDialogVisible.value = true;
};

const handleAssign = async () => {
  if (!currentGroupId.value) return;
  if (!assignEmpIds.value.length) { ElMessage.warning("请选择员工"); return; }
  assigning.value = true;
  try {
    await assignEmployees({ groupId: currentGroupId.value, empIds: assignEmpIds.value });
    ElMessage.success("分配成功");
    assignDialogVisible.value = false;
    fetchList();
  } finally {
    assigning.value = false;
  }
};

onMounted(() => { fetchList(); fetchEmployeeList(); });
</script>
