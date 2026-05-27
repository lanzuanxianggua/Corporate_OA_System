<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">预算管理</span>
          <el-button type="primary" @click="openDialog()">新增预算</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column label="部门" min-width="80" align="center">
          <template #default="{ row }">{{ getDeptName(row.deptId) }}</template>
        </el-table-column>
        <el-table-column label="预算年月" min-width="100" align="center">
          <template #default="{ row }">{{ row.budgetYear }}-{{ String(row.budgetMonth).padStart(2, '0') }}</template>
        </el-table-column>
        <el-table-column label="预算金额" min-width="120" align="right">
          <template #default="{ row }">{{ row.amount?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column label="已使用" min-width="120" align="right">
          <template #default="{ row }">{{ row.usedAmount?.toFixed(2) || "0.00" }}</template>
        </el-table-column>
        <el-table-column label="剩余" min-width="120" align="right">
          <template #default="{ row }">{{ ((row.amount || 0) - (row.usedAmount || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.usedAmount || 0) > (row.amount || 0) ? 'danger' : 'success'" size="small">
              {{ (row.usedAmount || 0) > (row.amount || 0) ? "已超支" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑预算' : '新增预算'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" style="width: 100%">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算年份" prop="budgetYear">
          <el-input-number v-model="form.budgetYear" :min="2000" :max="2099" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预算月份" prop="budgetMonth">
          <el-select v-model="form.budgetMonth" placeholder="请选择月份" style="width: 100%">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getBudgetPage, addBudget, updateBudget, deleteBudget } from "@/api/budget";
import { getDeptTree } from "@/api/dept";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const deptList = ref<any[]>([]);

const flattenDepts = (list: any[]): any[] => {
  const result: any[] = [];
  for (const item of list) {
    result.push(item);
    if (item.children?.length) result.push(...flattenDepts(item.children));
  }
  return result;
};

const getDeptName = (deptId: number) => {
  const dept = deptList.value.find(d => d.id === deptId);
  return dept ? dept.deptName : deptId;
};

const fetchDeptList = async () => {
  try {
    const res: any = await getDeptTree();
    const tree = Array.isArray(res.data) ? res.data : [];
    deptList.value = flattenDepts(tree);
  } catch { /* ignore */ }
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getBudgetPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, deptId: undefined as number | undefined, budgetYear: new Date().getFullYear(), budgetMonth: new Date().getMonth() + 1, amount: 0 });
const rules = reactive<FormRules>({
  deptId: [{ required: true, message: "请输入部门", trigger: "blur" }],
  budgetYear: [{ required: true, message: "请输入预算年份", trigger: "blur" }],
  budgetMonth: [{ required: true, message: "请选择预算月份", trigger: "change" }],
  amount: [{ required: true, message: "请输入预算金额", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, deptId: row.deptId, budgetYear: row.budgetYear, budgetMonth: row.budgetMonth, amount: row.amount || 0 });
  } else {
    Object.assign(form, { id: undefined, deptId: undefined, budgetYear: new Date().getFullYear(), budgetMonth: new Date().getMonth() + 1, amount: 0 });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) await updateBudget(form);
    else await addBudget(form);
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deleteBudget(id);
  ElMessage.success("删除成功");
  fetchList();
};

onMounted(() => { fetchList(); fetchDeptList(); });
</script>
