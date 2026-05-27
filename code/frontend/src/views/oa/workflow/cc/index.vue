<template>
  <div class="h-full">
    <el-card shadow="never" class="h-full">
      <template #header>
        <span class="text-base font-semibold">我的抄送</span>
      </template>
      <el-table :data="tableData" v-loading="loading" stripe size="small">
        <el-table-column label="流程类型" prop="businessType" width="120" />
        <el-table-column label="抄送时间" width="180">
          <template #default="{ row }">{{ row.createTime?.replace('T', ' ').substring(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'warning'" size="small">
              {{ row.status === '1' ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === '0'" type="primary" link size="small" @click="markRead(row)">已读</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" background small @change="fetchList" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getMyCcRecords, readCcRecord } from "@/api/workflow";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getMyCcRecords({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || res.data?.records || [];
    total.value = res.data?.total || 0;
  } catch {} finally {
    loading.value = false;
  }
};

const markRead = async (row: any) => {
  await readCcRecord(row.id);
  ElMessage.success("已标记为已读");
  fetchList();
};

onMounted(() => { fetchList(); });
</script>
