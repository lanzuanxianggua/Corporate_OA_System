<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">我的待办</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已完成</el-radio-button>
            <el-radio-button :value="2">已忽略</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="title" label="待办事项" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(todoStatusType(row.status) as any)" size="small" effect="light">{{ todoStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" link size="small" @click="handleDone(row)">完成</el-button>
              <el-button type="info" link size="small" @click="handleIgnore(row)">忽略</el-button>
            </template>
            <span v-else class="text-[#c0c4cc] text-xs">已处理</span>
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
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getTodoPage, doneTodo, ignoreTodo } from "@/api/todo";
import { formatTime } from "@/utils/format";
import type { Todo } from "@/types/api";

const loading = ref(false);
const tableData = ref<Todo[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | undefined>(undefined);

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== undefined) params.status = statusFilter.value;
    const res = await getTodoPage(params as any);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pageNum.value = 1;
  fetchList();
};

const handleDone = async (row: Todo) => {
  await doneTodo(row.id!);
  ElMessage.success("已标记完成");
  fetchList();
};

const handleIgnore = async (row: Todo) => {
  await ignoreTodo(row.id!);
  ElMessage.success("已忽略");
  fetchList();
};

const todoStatusText = (status?: number) => {
  const map: Record<number, string> = { 0: "待处理", 1: "已完成", 2: "已忽略" };
  return map[status ?? -1] || "未知";
};

const todoStatusType = (status?: number) => {
  const map: Record<number, string> = { 0: "warning", 1: "success", 2: "info" };
  return map[status ?? -1] || "info";
};

onMounted(() => { fetchList(); });
</script>
