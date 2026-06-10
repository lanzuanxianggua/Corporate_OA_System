<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">借支审批</span>
          <div class="flex items-center gap-2">
            <el-radio-group v-model="statusFilter" @change="handleFilterChange">
              <el-radio-button :value="''">全部</el-radio-button>
              <el-radio-button :value="0">待审批</el-radio-button>
              <el-radio-button :value="1">已通过</el-radio-button>
              <el-radio-button :value="2">已拒绝</el-radio-button>
            </el-radio-group>
            <el-button type="success" size="small" plain @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <el-table max-height="calc(100vh - 300px)" :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <template #empty>
          <el-empty description="暂无待审批记录" :image-size="60" />
        </template>
        <el-table-column prop="empName" label="申请人" width="90" />
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.loanAmount) }}</template>
        </el-table-column>
        <el-table-column prop="loanReason" label="原因" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(formatStatusTagType(row.status) as any)" size="small" effect="light">{{ formatStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="isPendingStatus(row.status)">
              <el-button type="success" size="small" plain @click="openDialog(row, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="openDialog(row, 2)">拒绝</el-button>
            </template>
            <span v-else class="text-[var(--oa-subtle)] text-xs">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="approveAction === 1 ? '审批通过' : '审批拒绝'" width="500px" :close-on-click-modal="false">
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="申请人">{{ currentRow.empName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ (currentRow as any).deptName ?? "-" }}</el-descriptions-item>
          <el-descriptions-item label="借支金额">{{ formatAmount(currentRow.loanAmount) }}</el-descriptions-item>
          <el-descriptions-item label="借支原因" :span="2">{{ currentRow.loanReason }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">审批进度</el-divider>
        <ApprovalTimeline v-if="currentRow?.id" business-type="loan" :business-id="currentRow.id" />

        <el-form label-position="top">
          <el-form-item label="审批备注">
            <el-input v-model="remark" type="textarea" :rows="3" placeholder="请输入审批备注（可选）" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="approveAction === 1 ? 'success' : 'danger'" :loading="approving" @click="handleApprove">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { getLoanPage, approveLoan } from "@/api/loan";
import { formatStatusText, formatStatusTagType, isPendingStatus } from "@/utils/format";
import { downloadFile } from "@/utils/download";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | "">("");

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== "") params.status = statusFilter.value;
    const res: any = await getLoanPage(params);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("获取审批列表失败");
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => { pageNum.value = 1; fetchList(); };

const dialogVisible = ref(false);
const approving = ref(false);
const currentRow = ref<any>(null);
const approveAction = ref(1);
const remark = ref("");

const openDialog = (row: any, action: number) => {
  currentRow.value = row;
  approveAction.value = action;
  remark.value = "";
  dialogVisible.value = true;
};

const handleApprove = async () => {
  if (!currentRow.value?.id) return;
  if (approveAction.value === 2 && (!remark.value || !remark.value.trim())) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  approving.value = true;
  try {
    await approveLoan({ id: currentRow.value.id, status: approveAction.value, remark: remark.value });
    ElMessage.success(approveAction.value === 1 ? "已通过" : "已拒绝");
    dialogVisible.value = false;
    fetchList();
  } catch (e) {
    ElMessage.error("审批操作失败");
  } finally {
    approving.value = false;
  }
};

const formatAmount = (amount?: number) => {
  if (amount == null) return "-";
  return `￥${amount.toFixed(2)}`;
};

onMounted(() => { fetchList(); });

const handleExport = async () => {
  try {
    const params = new URLSearchParams();
    if (statusFilter.value !== "") params.set("status", String(statusFilter.value));
    await downloadFile(`/api/loan/export?${params.toString()}`, "借支数据.xlsx");
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  }
};
</script>
