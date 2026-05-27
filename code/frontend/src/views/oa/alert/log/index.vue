<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">预警记录</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">未处理</el-radio-button>
            <el-radio-button :value="1">已处理</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="ruleName" label="规则名称" min-width="120" />
        <el-table-column prop="alertContent" label="预警内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'warning' : row.level === 2 ? 'danger' : 'info'" size="small">
              {{ row.level === 1 ? "警告" : row.level === 2 ? "严重" : "提示" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTime" label="预警时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.alertTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">{{ row.status === 0 ? "未处理" : "已处理" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" link size="small" @click="handleAlert(row)">处理</el-button>
            <span v-else class="text-[#c0c4cc] text-xs">-</span>
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
import { getAlertLogPage, handleAlert as handleAlertApi } from "@/api/alert";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | undefined>(undefined);

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== undefined) params.status = statusFilter.value;
    const res: any = await getAlertLogPage(params);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => { pageNum.value = 1; fetchList(); };

const handleAlert = async (row: any) => {
  await handleAlertApi({ id: row.id });
  ElMessage.success("已处理");
  fetchList();
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => { fetchList(); });
</script>
