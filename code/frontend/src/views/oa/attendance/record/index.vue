<template>
  <div class="record-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考勤记录</span>
          <el-date-picker
            v-model="month"
            type="month"
            placeholder="选择月份"
            style="width: 200px"
          />
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="morningIn" label="上班时间" width="120" />
        <el-table-column prop="morningOut" label="下班时间" width="120" />
        <el-table-column prop="workHours" label="工作时长" width="120" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
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

const month = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(30);

const tableData = ref([
  { date: "2026-05-13", morningIn: "08:55", morningOut: "18:30", workHours: "9.5小时", status: "正常", remark: "-" },
  { date: "2026-05-12", morningIn: "08:50", morningOut: "18:25", workHours: "9.5小时", status: "正常", remark: "-" },
  { date: "2026-05-11", morningIn: "09:05", morningOut: "18:30", workHours: "9.4小时", status: "迟到", remark: "-" },
  { date: "2026-05-10", morningIn: "-", morningOut: "-", workHours: "-", status: "休息", remark: "周末" },
  { date: "2026-05-09", morningIn: "08:52", morningOut: "18:20", workHours: "9.5小时", status: "正常", remark: "-" },
]);

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    "正常": "success",
    "迟到": "warning",
    "早退": "warning",
    "缺勤": "danger",
    "休息": "info"
  };
  return map[status] || "info";
};
</script>

<style scoped lang="scss">
.record-container {
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