<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-input v-model="searchModule" placeholder="搜索操作模块" clearable class="w-48" />
      <el-button type="primary" @click="fetchData">查询</el-button>
    </div>
    <el-card>
      <template #header><span class="font-medium">操作日志</span></template>
      <el-table :data="logList" stripe style="width: 100%">
        <el-table-column label="操作人" prop="username" />
        <el-table-column label="IP地址" prop="ip" />
        <el-table-column label="操作模块" prop="module" />
        <el-table-column label="操作内容" prop="summary" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "成功" : "失败" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" prop="operatingTime" width="180" />
      </el-table>
      <div class="flex justify-end mt-4">
        <OaPagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getOperationLogs } from "@/api/monitor";
import type { OperationLog } from "@/types/api";

const searchModule = ref("");
const logList = ref<OperationLog[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchData = async () => {
  try {
    const r: any = await getOperationLogs({ page: page.value, pageSize: pageSize.value, module: searchModule.value || undefined });
    if (r.data?.list) { logList.value = r.data.list; total.value = r.data.total || 0; }
    else if (r.data?.records) { logList.value = r.data.records; total.value = r.data.total || 0; }
  } catch {}
};

onMounted(fetchData);
</script>
