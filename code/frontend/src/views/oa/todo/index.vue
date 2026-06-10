<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="oa-mobile-toolbar flex items-center justify-between gap-3">
          <span class="text-base font-semibold text-[var(--oa-text)]">我的待办</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button :value="''">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已完成</el-radio-button>
            <el-radio-button :value="2">已忽略</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table class="oa-desktop-table" max-height="calc(100vh - 300px)" :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <el-table-column prop="title" label="待办事项" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ todoTypeText(row.todoType) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="120">
          <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
        </el-table-column>
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
            <template v-if="isPendingStatus(row.status)">
              <el-button type="success" link size="small" @click="handleDone(row)">完成</el-button>
              <el-button type="info" link size="small" @click="handleIgnore(row)">忽略</el-button>
            </template>
            <span v-else class="text-[var(--oa-subtle)] text-xs">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-loading="loading" class="oa-mobile-list">
        <el-empty v-if="!tableData.length" description="暂无待办事项" :image-size="72" />
        <div v-else class="oa-mobile-card-list">
          <article v-for="row in tableData" :key="row.id" class="oa-mobile-card">
            <div class="oa-mobile-card-main">
              <div class="oa-mobile-card-title">
                <span>{{ row.title || '-' }}</span>
                <el-tag :type="(todoStatusType(row.status) as any)" size="small" effect="light">
                  {{ todoStatusText(row.status) }}
                </el-tag>
              </div>
              <div class="oa-mobile-card-meta">
                <div class="oa-mobile-meta-row">
                  <span>类型</span>
                  <span>{{ todoTypeText(row.todoType) }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>来源</span>
                  <span>{{ businessTypeText(row.businessType) }}</span>
                </div>
                <div class="oa-mobile-meta-row">
                  <span>创建时间</span>
                  <span>{{ formatTime(row.createTime) }}</span>
                </div>
              </div>
            </div>
            <div v-if="isPendingStatus(row.status)" class="oa-mobile-card-actions">
              <el-button type="success" plain @click="handleDone(row)">完成</el-button>
              <el-button type="info" plain @click="handleIgnore(row)">忽略</el-button>
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
import { ElMessage } from "element-plus";
import { getTodoPage, doneTodo, ignoreTodo } from "@/api/todo";
import { formatTime, isPendingStatus } from "@/utils/format";
import type { Todo } from "@/types/api";

const loading = ref(false);
const tableData = ref<Todo[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | "">("");

const fetchList = async () => {
  loading.value = true;
  try {
    const params: Record<string, unknown> = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== "") params.status = statusFilter.value;
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

const todoStatusText = (status?: number | string) => {
  const map: Record<number, string> = { 0: "待处理", 1: "已完成", 2: "已忽略" };
  return map[Number(status ?? -1)] || "未知";
};

const todoStatusType = (status?: number | string) => {
  const map: Record<number, string> = { 0: "warning", 1: "success", 2: "info" };
  return map[Number(status ?? -1)] || "info";
};

const todoTypeText = (type?: string | number) => {
  const map: Record<string, string> = {
    approval: "审批",
    meeting: "会议",
    notice: "公告",
    task: "任务",
    cc: "抄送"
  };
  return map[String(type || "")] || String(type || "-");
};

const businessTypeText = (type?: string) => {
  const map: Record<string, string> = {
    leave: "请假",
    trip: "出差",
    outing: "外出",
    purchase: "采购",
    expense: "经费",
    overtime: "加班",
    loan: "借支",
    contract: "合同",
    meeting: "会议"
  };
  return map[type || ""] || type || "-";
};

onMounted(() => { fetchList(); });
</script>
