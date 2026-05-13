<template>
  <div class="logs-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
          <el-input v-model="searchModule" placeholder="搜索操作模块" style="width: 200px" />
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="content" label="操作内容" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationTime" label="操作时间" width="160" />
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const searchModule = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(5);

const tableData = ref([
  { operator: "admin", ip: "192.168.1.100", module: "用户管理", content: "新增用户: zhangsan", status: "成功", operationTime: "2026-05-13 10:00" },
  { operator: "admin", ip: "192.168.1.100", module: "角色管理", content: "修改角色: 管理员", status: "成功", operationTime: "2026-05-13 11:30" },
  { operator: "张三", ip: "192.168.1.101", module: "考勤管理", content: "提交请假申请", status: "成功", operationTime: "2026-05-13 14:00" },
  { operator: "李四", ip: "192.168.1.102", module: "公告管理", content: "发布公告: 端午节放假通知", status: "成功", operationTime: "2026-05-13 15:30" }
]);
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