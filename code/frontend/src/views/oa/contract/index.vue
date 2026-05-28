<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">合同管理</span>
          <el-button type="primary" @click="openDialog()">新增合同</el-button>
        </div>
      </template>

      <div class="mb-4 flex gap-3">
        <el-input v-model="searchKey" placeholder="搜索合同名称/编号" style="width: 240px" clearable @clear="fetchList" @keyup.enter="fetchList">
          <template #append>
            <el-button @click="fetchList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
        <el-button type="warning" plain @click="fetchExpiring">即将到期</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="contractNo" label="合同编号" width="120" />
        <el-table-column prop="contractName" label="合同名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="partyA" label="甲方" width="100" />
        <el-table-column prop="partyB" label="乙方" width="100" />
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="endDate" label="结束日期" width="110" />
        <el-table-column prop="amount" label="金额" width="100" align="right">
          <template #default="{ row }">{{ row.amount?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 0 ? 'warning' : 'danger'" size="small">
              {{ row.status === 1 ? "生效" : row.status === 0 ? "草稿" : "到期" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无合同数据" />
        </template>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑合同' : '新增合同'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="合同编号" prop="contractNo">
          <el-input v-model="form.contractNo" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="甲方" prop="partyA">
          <el-input v-model="form.partyA" placeholder="请输入甲方" />
        </el-form-item>
        <el-form-item label="乙方" prop="partyB">
          <el-input v-model="form.partyB" placeholder="请输入乙方" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" placeholder="请选择开始日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" placeholder="请选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额">
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
import { Search } from "@element-plus/icons-vue";
import { getContractPage, addContract, updateContract, deleteContract, getExpiringContracts } from "@/api/contract";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getContractPage({ pageNum: pageNum.value, pageSize: pageSize.value, contractName: searchKey.value || undefined, contractNo: searchKey.value || undefined });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const fetchExpiring = async () => {
  loading.value = true;
  try {
    const res: any = await getExpiringContracts({ days: 30 });
    tableData.value = res.data?.list || res.data || [];
    total.value = tableData.value.length;
  } catch {
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  id: undefined as number | undefined, contractNo: "", contractName: "",
  partyA: "", partyB: "", startDate: "", endDate: "", amount: 0
});
const rules = reactive<FormRules>({
  contractNo: [{ required: true, message: "请输入合同编号", trigger: "blur" }],
  contractName: [{ required: true, message: "请输入合同名称", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, contractNo: row.contractNo, contractName: row.contractName, partyA: row.partyA, partyB: row.partyB, startDate: row.startDate, endDate: row.endDate, amount: row.amount || 0 });
  } else {
    Object.assign(form, { id: undefined, contractNo: "", contractName: "", partyA: "", partyB: "", startDate: "", endDate: "", amount: 0 });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) await updateContract(form);
    else await addContract(form);
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteContract(id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(() => { fetchList(); });
</script>
