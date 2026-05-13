<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-input v-model="searchUsername" placeholder="搜索用户名" clearable class="w-48" />
      <el-button type="primary" @click="fetchData">查询</el-button>
    </div>
    <el-card>
      <template #header><span class="font-medium">登录日志</span></template>
      <el-table :data="logList" stripe>
        <el-table-column label="用户名" prop="username" />
        <el-table-column label="IP地址" prop="ip" />
        <el-table-column label="登录地点" prop="address" />
        <el-table-column label="浏览器" prop="browser" />
        <el-table-column label="操作系统" prop="system" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="行为" prop="behavior" />
        <el-table-column label="登录时间" prop="loginTime" width="180" />
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getLoginLogs } from "@/api/monitor";

const searchUsername = ref("");
const logList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchData = async () => {
  try {
    const r: any = await getLoginLogs({ page: page.value, pageSize: pageSize.value, username: searchUsername.value || undefined });
    if (r.data?.list) { logList.value = r.data.list; total.value = r.data.total || 0; }
    else if (r.data?.records) { logList.value = r.data.records; total.value = r.data.total || 0; }
  } catch {}
};

onMounted(fetchData);
</script>
