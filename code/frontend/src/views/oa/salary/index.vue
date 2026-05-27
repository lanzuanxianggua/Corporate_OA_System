<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">薪资管理</span>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="薪资结构" name="structure">
          <div class="mb-4">
            <el-button type="primary" @click="openStructDialog()">新增薪资项</el-button>
          </div>

          <el-table :data="structList" v-loading="structLoading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="empId" label="员工ID" width="80" align="center" />
            <el-table-column prop="empName" label="员工姓名" min-width="100" />
            <el-table-column prop="baseSalary" label="基本工资" min-width="110" align="right">
              <template #default="{ row }">{{ row.baseSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="postSalary" label="岗位工资" min-width="110" align="right">
              <template #default="{ row }">{{ row.postSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="meritSalary" label="绩效工资" min-width="110" align="right">
              <template #default="{ row }">{{ row.meritSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="allowance" label="津贴" min-width="100" align="right">
              <template #default="{ row }">{{ row.allowance?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="effectiveDate" label="生效日期" min-width="110" align="center" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">{{ row.status === "0" ? "启用" : "停用" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openStructDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="薪资记录" name="record">
          <div class="mb-4">
            <el-date-picker v-model="recordMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="fetchRecordList" />
          </div>

          <el-table :data="recordList" v-loading="recordLoading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="empName" label="员工" min-width="100" />
            <el-table-column prop="salaryMonth" label="月份" width="80" align="center" />
            <el-table-column prop="baseSalary" label="基本工资" min-width="100" align="right">
              <template #default="{ row }">{{ row.baseSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="postSalary" label="岗位工资" min-width="100" align="right">
              <template #default="{ row }">{{ row.postSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="meritSalary" label="绩效工资" min-width="100" align="right">
              <template #default="{ row }">{{ row.meritSalary?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="allowance" label="津贴" min-width="90" align="right">
              <template #default="{ row }">{{ row.allowance?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="deduction" label="扣款" min-width="90" align="right">
              <template #default="{ row }">{{ row.deduction?.toFixed(2) || "-" }}</template>
            </el-table-column>
            <el-table-column prop="actualAmount" label="实发工资" min-width="100" align="right">
              <template #default="{ row }">
                <span class="font-medium">{{ row.actualAmount?.toFixed(2) || "-" }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="structDialogVisible" :title="structForm.id ? '编辑薪资结构' : '新增薪资结构'" width="500px" :close-on-click-modal="false">
      <el-form ref="structFormRef" :model="structForm" :rules="structRules" label-width="90px">
        <el-form-item label="员工" prop="empId">
          <el-select v-model="structForm.empId" placeholder="请选择员工" filterable style="width: 100%">
            <el-option v-for="emp in employeeList" :key="emp.id" :label="emp.empName" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="基本工资" prop="baseSalary">
          <el-input-number v-model="structForm.baseSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="岗位工资" prop="postSalary">
          <el-input-number v-model="structForm.postSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="绩效工资" prop="meritSalary">
          <el-input-number v-model="structForm.meritSalary" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="津贴" prop="allowance">
          <el-input-number v-model="structForm.allowance" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="structForm.effectiveDate" type="date" placeholder="选择生效日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="structForm.status" placeholder="请选择状态">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="structDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="structSaving" @click="handleSaveStruct">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getStructurePage, addStructure, updateStructure, getRecordPage } from "@/api/salary";
import { getEmployeePage } from "@/api/employee";

const activeTab = ref("structure");
const employeeList = ref<any[]>([]);

const fetchEmployeeList = async () => {
  try {
    const res: any = await getEmployeePage({ pageNum: 1, pageSize: 200 });
    employeeList.value = res.data?.list || [];
  } catch { /* ignore */ }
};

// --- 薪资结构 ---
const structLoading = ref(false);
const structList = ref<any[]>([]);

const fetchStructList = async () => {
  structLoading.value = true;
  try {
    const res: any = await getStructurePage({ pageNum: 1, pageSize: 100 });
    structList.value = res.data?.list || [];
  } finally {
    structLoading.value = false;
  }
};

const structDialogVisible = ref(false);
const structSaving = ref(false);
const structFormRef = ref<FormInstance>();
const structForm = reactive({ id: undefined as number | undefined, empId: undefined as number | undefined, baseSalary: 0, postSalary: 0, meritSalary: 0, allowance: 0, effectiveDate: "", status: "0" });
const structRules = reactive<FormRules>({
  empId: [{ required: true, message: "请输入员工ID", trigger: "blur" }],
  baseSalary: [{ required: true, message: "请输入基本工资", trigger: "blur" }],
  effectiveDate: [{ required: true, message: "请选择生效日期", trigger: "change" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }]
});

const openStructDialog = (row?: any) => {
  if (row) {
    Object.assign(structForm, { id: row.id, empId: row.empId, baseSalary: row.baseSalary, postSalary: row.postSalary, meritSalary: row.meritSalary, allowance: row.allowance, effectiveDate: row.effectiveDate, status: row.status });
  } else {
    Object.assign(structForm, { id: undefined, empId: undefined, baseSalary: 0, postSalary: 0, meritSalary: 0, allowance: 0, effectiveDate: "", status: "0" });
  }
  structDialogVisible.value = true;
};

const handleSaveStruct = async () => {
  if (!structFormRef.value) return;
  await structFormRef.value.validate();
  structSaving.value = true;
  try {
    if (structForm.id) await updateStructure(structForm);
    else await addStructure(structForm);
    ElMessage.success("保存成功");
    structDialogVisible.value = false;
    fetchStructList();
  } finally {
    structSaving.value = false;
  }
};

// --- 薪资记录 ---
const recordLoading = ref(false);
const recordList = ref<any[]>([]);
const recordMonth = ref("");

const fetchRecordList = async () => {
  recordLoading.value = true;
  try {
    const res: any = await getRecordPage({ pageNum: 1, pageSize: 100, salaryMonth: recordMonth.value || undefined });
    recordList.value = res.data?.list || [];
  } finally {
    recordLoading.value = false;
  }
};

onMounted(() => { fetchStructList(); fetchRecordList(); fetchEmployeeList(); });
</script>
