<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAttendancePage } from "@/api/oa/attendance";

defineOptions({ name: "OaAttendanceRecord" });

interface AttendanceRow {
  id: number;
  workDate: string;
  clockIn: string;
  clockOut: string;
  status: string;
}

const loading = ref(false);
const tableData = ref<AttendanceRow[]>([]);
const total = ref(0);

const pagination = ref({
  currentPage: 1,
  pageSize: 10
});

const dateRange = ref<[string, string] | null>(null);

/** 获取状态标签类型 */
function getStatusType(status: string): "" | "success" | "warning" | "danger" | "info" {
  const map: Record<string, "" | "success" | "warning" | "danger" | "info"> = {
    正常: "success",
    迟到: "warning",
    早退: "warning",
    缺勤: "danger"
  };
  return map[status] || "info";
}

/** 加载数据 */
async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNum: pagination.value.currentPage,
      pageSize: pagination.value.pageSize
    };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await getAttendancePage(params);
    if (res.data) {
      tableData.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } catch {
    // 静默处理
  } finally {
    loading.value = false;
  }
}

/** 分页大小变更 */
function handleSizeChange(val: number) {
  pagination.value.pageSize = val;
  pagination.value.currentPage = 1;
  loadData();
}

/** 当前页变更 */
function handleCurrentChange(val: number) {
  pagination.value.currentPage = val;
  loadData();
}

/** 日期范围变更 */
function handleDateChange() {
  pagination.value.currentPage = 1;
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="attendance-record-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>个人考勤记录</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          clearable
          style="width: 360px"
        />
      </div>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        empty-text="暂无考勤记录"
      >
        <el-table-column
          prop="workDate"
          label="工作日期"
          width="140"
          align="center"
        />
        <el-table-column
          prop="clockIn"
          label="上班打卡"
          width="160"
          align="center"
        >
          <template #default="{ row }">
            {{ row.clockIn || "--" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="clockOut"
          label="下班打卡"
          width="160"
          align="center"
        >
          <template #default="{ row }">
            {{ row.clockOut || "--" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="考勤状态"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.status"
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ row.status }}
            </el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.attendance-record-container {
  padding: 16px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
