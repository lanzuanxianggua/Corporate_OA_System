<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-button type="primary" @click="fetchData">刷新</el-button>
    </div>
    <el-card>
      <template #header><span class="font-medium">系统日志</span></template>
      <el-table :data="logList" stripe>
        <el-table-column label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.level === 0 ? 'success' : 'danger'" size="small">{{ row.level === 0 ? "INFO" : "ERROR" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="请求路径" prop="url" show-overflow-tooltip />
        <el-table-column label="请求方法" prop="method" width="100" />
        <el-table-column label="IP地址" prop="ip" width="130" />
        <el-table-column label="模块" prop="module" width="120" />
        <el-table-column label="耗时" prop="takesTime" width="100" />
        <el-table-column label="请求时间" prop="requestTime" width="180" />
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getSystemLogs } from "@/api/monitor";

const logList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchData = async () => {
  try {
    const r: any = await getSystemLogs({ page: page.value, pageSize: pageSize.value });
    if (r.data?.list) { logList.value = r.data.list; total.value = r.data.total || 0; }
    else if (r.data?.records) { logList.value = r.data.records; total.value = r.data.total || 0; }
  } catch {}
};

onMounted(fetchData);
</script>
