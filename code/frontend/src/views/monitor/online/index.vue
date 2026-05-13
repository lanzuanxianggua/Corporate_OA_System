<template>
  <div class="online-container">
    <el-card>
      <template #header>
        <span>在线用户</span>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="ip" label="登录IP" width="150" />
        <el-table-column prop="dept" label="部门" width="120" />
        <el-table-column prop="browser" label="浏览器" />
        <el-table-column prop="loginTime" label="登录时间" width="160" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleForceLogout(row)">强制下线</el-button>
          </template>
        </el-table-column>
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
import { ElMessage, ElMessageBox } from "element-plus";
import { getOnlineLogs, forceLogout } from "@/api/monitor";

const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableData = ref<any[]>([]);

const loadData = async () => {
  try {
    loading.value = true;
    const res: any = await getOnlineLogs({ page: currentPage.value, pageSize: pageSize.value });
    if (res.data?.list) {
      tableData.value = res.data.list;
      total.value = res.data.total;
    }
  } catch (error) {
    console.error("获取在线用户失败", error);
  } finally {
    loading.value = false;
  }
};

const handleForceLogout = (row: any) => {
  ElMessageBox.confirm(`确定要强制下线用户 "${row.username}" 吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(async () => {
    try {
      await forceLogout(row.id);
      ElMessage.success("操作成功");
      loadData();
    } catch (error) {
      console.error("强制下线失败", error);
    }
  });
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.online-container {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>