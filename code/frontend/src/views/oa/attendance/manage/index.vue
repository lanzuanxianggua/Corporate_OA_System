<template>
  <div class="manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考勤管理</span>
          <div class="search-form">
            <el-input v-model="searchName" placeholder="搜索员工姓名" style="width: 200px; margin-right: 10px" />
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="margin-right: 10px"
            />
            <el-select v-model="status" placeholder="状态" style="width: 120px; margin-right: 10px">
              <el-option label="全部" value="" />
              <el-option label="正常" value="正常" />
              <el-option label="迟到" value="迟到" />
              <el-option label="早退" value="早退" />
              <el-option label="缺勤" value="缺勤" />
            </el-select>
            <el-button type="primary">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="empCode" label="工号" width="100" />
        <el-table-column prop="empName" label="姓名" width="120" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="morningIn" label="上班时间" width="120" />
        <el-table-column prop="morningOut" label="下班时间" width="120" />
        <el-table-column prop="workHours" label="工作时长" width="100" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
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
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const searchName = ref("");
const dateRange = ref([]);
const status = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(156);

const tableData = ref([
  { empCode: "E001", empName: "张三", deptName: "技术部", morningIn: "08:55", morningOut: "18:30", workHours: "9.5小时", status: "正常" },
  { empCode: "E002", empName: "李四", deptName: "市场部", morningIn: "09:10", morningOut: "18:00", workHours: "8.8小时", status: "迟到" },
  { empCode: "E003", empName: "王五", deptName: "人事部", morningIn: "08:50", morningOut: "18:20", workHours: "9.5小时", status: "正常" },
  { empCode: "E004", empName: "赵六", deptName: "财务部", morningIn: "08:45", morningOut: "17:50", workHours: "9.1小时", status: "早退" },
  { empCode: "E005", empName: "钱七", deptName: "技术部", morningIn: "-", morningOut: "-", workHours: "-", status: "缺勤" },
]);

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    "正常": "success",
    "迟到": "warning",
    "早退": "warning",
    "缺勤": "danger"
  };
  return map[status] || "info";
};
</script>

<style scoped lang="scss">
.manage-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  .search-form {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>