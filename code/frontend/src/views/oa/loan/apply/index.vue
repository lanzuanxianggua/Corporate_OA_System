<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的借支记录</span>
              <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">{{ formatAmount(row.loanAmount) }}</template>
            </el-table-column>
            <el-table-column prop="loanReason" label="原因" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="160" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
                <el-button v-if="row.status !== 0" type="info" link size="small" class="ml-1" @click="showDetail(row)">详情</el-button>
                <el-button v-if="row.status === 0" type="warning" link size="small" @click="handleWithdraw(row)">撤回</el-button>
                <el-button v-if="row.status === 0" type="info" link size="small" @click="handleUrge(row)">催办</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background small @change="fetchList" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">申请借支</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="借支金额" prop="loanAmount">
              <el-input v-model.number="form.loanAmount" type="number" placeholder="请输入借支金额">
                <template #prepend>￥</template>
              </el-input>
            </el-form-item>
            <el-form-item label="借支原因" prop="loanReason">
              <el-input v-model="form.loanReason" type="textarea" :rows="4" placeholder="请输入借支原因" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="还款计划" prop="repaymentPlan">
              <el-input v-model="form.repaymentPlan" type="textarea" :rows="3" placeholder="请输入还款计划" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="审批详情" width="600px">
      <ApprovalTimeline v-if="detailRow?.id" business-type="loan" :business-id="detailRow.id" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { withdrawApplication, urgeTask } from "@/api/workflow";
import { submitLoan, getLoanPage } from "@/api/loan";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getLoanPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const statusText = (status?: number) => {
  const map: Record<number, string> = { 0: "待审批", 1: "已通过", 2: "已拒绝" };
  return map[status ?? -1] || "未知";
};

const statusTagType = (status?: number) => {
  const map: Record<number, string> = { 0: "warning", 1: "success", 2: "danger" };
  return map[status ?? -1] || "info";
};

const formatAmount = (amount?: number) => {
  if (amount == null) return "-";
  return `￥${amount.toFixed(2)}`;
};

// --- 详情 ---
const detailVisible = ref(false);
const detailRow = ref<any>(null);
const showDetail = (row: any) => {
  detailRow.value = row;
  detailVisible.value = true;
};

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive({ loanAmount: undefined as number | undefined, loanReason: "", repaymentPlan: "" });
const rules = reactive<FormRules>({
  loanAmount: [{ required: true, message: "请输入借支金额", trigger: "blur" }],
  loanReason: [{ required: true, message: "请输入借支原因", trigger: "blur" }],
  repaymentPlan: [{ required: true, message: "请输入还款计划", trigger: "blur" }]
});

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    await submitLoan({ empId: userStore.userInfo?.empId, ...form });
    ElMessage.success("借支申请已提交");
    resetForm();
    pageNum.value = 1;
    fetchList();
  } finally {
    submitting.value = false;
  }
};

const resetForm = () => { formRef.value?.resetFields(); };

const handleWithdraw = async (row: any) => {
  try {
    await ElMessageBox.confirm("确定要撤回此申请吗？", "撤回确认", { type: "warning" });
    await withdrawApplication({ businessType: "loan", businessId: row.id });
    ElMessage.success("申请已撤回");
    fetchList();
  } catch {}
};

const handleUrge = async (row: any) => {
  try {
    await urgeTask({ businessType: "loan", businessId: row.id });
    ElMessage.success("已发送催办提醒");
  } catch {}
};

onMounted(() => { fetchList(); });
</script>
