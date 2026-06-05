<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">我的已办任务</span>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="processName" label="流程名称" min-width="120" />
        <el-table-column label="任务节点" min-width="100">
          <template #default="{ row }">{{ row.nodeName || row.taskName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="applicant" label="申请人" width="90" />
        <el-table-column label="处理结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="resultType(row.status)" size="small">{{ resultText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="审批意见" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="处理时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
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

const resultText = (s?: number | string) =>
  ({ 1: "已通过", 2: "已驳回", 3: "已转办", 5: "已退回" }[Number(s ?? -1)] || "已处理");
const resultType = (s?: number | string) =>
  ({ 1: "success", 2: "danger", 3: "info", 5: "warning" }[Number(s ?? -1)] || "info") as "success" | "danger" | "info" | "warning";

const formatTime = (time?: string) => time ? time.replace("T", " ").substring(0, 16) : "-";

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getHandledTasks({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

onMounted(() => fetchList());
</script>
