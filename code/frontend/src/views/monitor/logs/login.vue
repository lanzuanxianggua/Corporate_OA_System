<template>
  <div class="logs-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>登录日志</span>
          <el-input v-model="searchUsername" placeholder="搜索用户名" style="width: 200px" clearable @change="loadData" />
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="address" label="登录地点" />
        <el-table-column prop="browser" label="浏览器" width="120" />
        <el-table-column prop="system" label="操作系统" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="behavior" label="行为" />
        <el-table-column prop="loginTime" label="登录时间" width="160" />
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getLoginLogs } from "@/api/monitor";

const loading = ref(false);
const searchUsername = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<any[]>([]);

const loadData = async () => {
  try {
    loading.value = true;
    const res: any = await getLoginLogs({
      page: currentPage.value,
      pageSize: pageSize.value,
      username: searchUsername.value || undefined
    });
    if (res.data?.list) {
      tableData.value = res.data.list;
      total.value = res.data.total;
    }
  } catch (error) {
    console.error("获取登录日志失败", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.logs-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>