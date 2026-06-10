<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[var(--oa-text)]">我的已办任务</span>
      </template>

      <el-table class="oa-desktop-table" max-height="calc(100vh - 300px)" :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <template #empty>
          <el-empty description="暂无已办任务" :image-size="72" />
        </template>
        <el-table-column label="流程名称" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ taskProcessName(row) }}</template>
        </el-table-column>
        <el-table-column label="任务节点" min-width="100">
          <template #default="{ row }">{{ taskNodeName(row) }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="90">
          <template #default="{ row }">{{ taskApplicant(row) }}</template>
        </el-table-column>
        <el-table-column label="处理结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="resultType(row.status)" size="small">{{ resultText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批意见" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ taskOpinion(row) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="处理时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.completeTime || row.updateTime || row.actionTime || row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div v-loading="loading" class="oa-mobile-list">
        <el-empty v-if="!tableData.length" description="暂无已办任务" :image-size="72" />
        <div v-else class="oa-mobile-card-list">
          <article v-for="row in tableData" :key="row.id" class="oa-mobile-card">
            <div class="oa-mobile-card-main">
              <div class="oa-mobile-card-title">
                <span>{{ taskProcessName(row) }}</span>
                <el-tag :type="resultType(row.status)" size="small">{{ resultText(row.status) }}</el-tag>
              </div>
              <div class="oa-mobile-card-subtitle">{{ taskNodeName(row) }}</div>
              <div class="oa-mobile-card-meta">
                <div class="oa-mobile-meta-row">
                  <span>申请人</span>
                  <strong>{{ taskApplicant(row) }}</strong>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>审批意见</span>
                  <span>{{ taskOpinion(row) }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>处理时间</span>
                  <span>{{ formatTime(row.completeTime || row.updateTime || row.actionTime || row.createTime) }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>
      </div>

      <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getHandledTasks } from "@/api/workflow";
import type { WorkflowTask } from "@/types/api";

const loading = ref(false);
const tableData = ref<WorkflowTask[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const businessTypeLabels: Record<string, string> = {
  leave: "请假审批",
  trip: "出差审批",
  outing: "外出审批",
  purchase: "采购审批",
  expense: "经费审批",
  overtime: "加班审批",
  loan: "借支审批",
  contract: "合同审批"
};

const firstText = (...values: Array<unknown>) => {
  const value = values.find(v => v !== null && v !== undefined && String(v).trim() !== "");
  return value === undefined ? "-" : String(value);
};

const taskProcessName = (row: WorkflowTask) => {
  const businessType = row.businessType || row.instance?.businessType;
  return firstText(row.processName, businessType ? businessTypeLabels[businessType] : undefined, businessType);
};

const taskNodeName = (row: WorkflowTask) => firstText(row.nodeName, row.taskName);

const taskApplicant = (row: WorkflowTask) =>
  firstText(row.applicant, row.instance?.initiatorName, row.instance?.initiatorId);

const taskOpinion = (row: WorkflowTask) => firstText(row.remark, row.opinion);

const resultText = (s?: number | string) =>
  ({ 1: "已通过", 2: "已驳回", 3: "已转办", 5: "已退回" }[Number(s ?? -1)] || "已处理");
const resultType = (s?: number | string) =>
  ({ 1: "success", 2: "danger", 3: "info", 5: "warning" }[Number(s ?? -1)] || "info") as "success" | "danger" | "info" | "warning";

const formatTime = (time?: string) => time ? time.replace("T", " ").substring(0, 16) : "-";

const readPageList = (data: any) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.list)) return data.list;
  if (Array.isArray(data?.records)) return data.records;
  return [];
};

const readPageTotal = (data: any, fallback: number) => {
  const totalValue = data?.total ?? data?.totalCount;
  return Number.isFinite(Number(totalValue)) ? Number(totalValue) : fallback;
};

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getHandledTasks({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = readPageList(res.data);
    total.value = readPageTotal(res.data, tableData.value.length);
  } finally {
    loading.value = false;
  }
};

onMounted(() => fetchList());
</script>
