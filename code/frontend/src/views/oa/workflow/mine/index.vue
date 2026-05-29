<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">我的申请</span>
      </template>

      <div class="mb-4 flex items-center gap-3 flex-wrap">
        <el-radio-group v-model="activeType" @change="handleTypeChange">
          <el-radio-button v-for="t in businessTypes" :key="t.key" :value="t.key">{{ t.label }}</el-radio-button>
        </el-radio-group>
        <el-select v-model="statusFilter" placeholder="状态" clearable class="w-32" @change="fetchList">
          <el-option label="待审批" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="已撤回" :value="3" />
        </el-select>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="业务类型" width="90">
          <template #default>
            <el-tag size="small" effect="plain">{{ currentLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要" min-width="200">
          <template #default="{ row }">{{ getSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 || row.status === '0'" type="warning" size="small" plain @click="handleWithdraw(row)">撤回</el-button>
            <span v-else class="text-[#909399] text-sm">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { withdrawApplication } from "@/api/workflow";
import { getLeavePage } from "@/api/leave";
import { getBusinessTripPage } from "@/api/businessTrip";
import { getOutingPage } from "@/api/outing";
import { getPurchasePage } from "@/api/purchase";
import { getExpensePage } from "@/api/expense";
import { getOvertimePage } from "@/api/overtime";
import { getLoanPage } from "@/api/loan";

const businessTypes = [
  { key: "leave", label: "请假" },
  { key: "trip", label: "出差" },
  { key: "outing", label: "外出" },
  { key: "purchase", label: "采购" },
  { key: "expense", label: "报销" },
  { key: "overtime", label: "加班" },
  { key: "loan", label: "借款" }
];

const fetchers: Record<string, (params: any) => Promise<any>> = {
  leave: getLeavePage,
  trip: getBusinessTripPage,
  outing: getOutingPage,
  purchase: getPurchasePage,
  expense: getExpensePage,
  overtime: getOvertimePage,
  loan: getLoanPage
};

const summaryFields: Record<string, (row: any) => string> = {
  leave: (r) => r.reason || "-",
  trip: (r) => r.destination ? `${r.destination} - ${r.purpose || ""}` : "-",
  outing: (r) => r.destination ? `${r.destination} - ${r.reason || ""}` : r.reason || "-",
  purchase: (r) => r.itemName ? `${r.itemName} x${r.quantity || 1}` : "-",
  expense: (r) => r.title || r.description || "-",
  overtime: (r) => r.reason || "-",
  loan: (r) => r.loanReason || "-"
};

const activeType = ref("leave");
const statusFilter = ref<number | undefined>(undefined);
const currentLabel = computed(() => businessTypes.find(t => t.key === activeType.value)?.label || "");

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const statusText = (s?: number | string) =>
  ({ 0: "待审批", 1: "已通过", 2: "已驳回", 3: "已撤回" }[Number(s ?? -1)] || "未知");
const statusTagType = (s?: number | string) =>
  ({ 0: "warning", 1: "success", 2: "danger", 3: "info" }[Number(s ?? -1)] || "info") as "success" | "danger" | "warning" | "info";

const formatTime = (t?: string) => t ? t.replace("T", " ").substring(0, 16) : "-";

const getSummary = (row: any) => {
  const fn = summaryFields[activeType.value];
  return fn ? fn(row) : "-";
};

const fetchList = async () => {
  loading.value = true;
  try {
    const fetcher = fetchers[activeType.value];
    if (!fetcher) return;
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== undefined) params.status = statusFilter.value;
    const res: any = await fetcher(params);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleTypeChange = () => {
  pageNum.value = 1;
  statusFilter.value = undefined;
  fetchList();
};

const handleWithdraw = async (row: any) => {
  try {
    await ElMessageBox.confirm("确定撤回该申请？", "撤回确认", { type: "warning" });
    await withdrawApplication({ businessType: activeType.value, businessId: row.id });
    ElMessage.success("已撤回");
    fetchList();
  } catch { /* cancelled */ }
};

onMounted(() => fetchList());
</script>
