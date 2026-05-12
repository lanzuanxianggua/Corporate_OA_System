<script setup lang="ts">
import { ref, onMounted } from "vue";
import { http } from "@/utils/http";
import { getAttendanceManage } from "@/api/oa/attendance";

defineOptions({ name: "OaAttendanceManage" });

interface AttendanceRow {
  id: number;
  empName: string;
  deptName: string;
  workDate: string;
  clockIn: string;
  clockOut: string;
  status: string;
  ip: string;
  address: string;
}

interface DeptOption {
  deptId: number;
  deptName: string;
}

const loading = ref(false);
const tableData = ref<AttendanceRow[]>([]);
const total = ref(0);
const deptList = ref<DeptOption[]>([]);

const pagination = ref({
  currentPage: 1,
  pageSize: 10
});

const queryParams = ref<{
  deptId: number | string;
  dateRange: [string, string] | null;
  empName: string;
}>({
  deptId: "",
  dateRange: null,
  empName: ""
});

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

/** 加载部门列表 */
async function loadDeptList() {
  try {
    const res = await http.request<{ data: DeptOption[] }>(
      "get",
      "/api/dept/list"
    );
    if (res.data) {
      deptList.value = res.data;
    }
  } catch {
    // 静默处理
  }
}

/** 加载考勤数据 */
async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNum: pagination.value.currentPage,
      pageSize: pagination.value.pageSize
    };
    if (queryParams.value.deptId !== "") {
      params.deptId = queryParams.value.deptId;
    }
    if (queryParams.value.empName) {
      params.empName = queryParams.value.empName;
    }
    if (queryParams.value.dateRange && queryParams.value.dateRange.length === 2) {
      params.startDate = queryParams.value.dateRange[0];
      params.endDate = queryParams.value.dateRange[1];
    }
    const res = await getAttendanceManage(params);
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

/** 搜索 */
function handleSearch() {
  pagination.value.currentPage = 1;
  loadData();
}

/** 重置 */
function handleReset() {
  queryParams.value = {
    deptId: "",
    dateRange: null,
    empName: ""
  };
  pagination.value.currentPage = 1;
  loadData();
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

onMounted(() => {
  loadDeptList();
  loadData();
});
</script>

<template>
  <div class="attendance-manage-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>考勤管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="filter-bar">
        <el-select
          v-model="queryParams.deptId"
          placeholder="选择部门"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="dept in deptList"
            :key="dept.deptId"
            :label="dept.deptName"
            :value="dept.deptId"
          />
        </el-select>

        <el-input
          v-model="queryParams.empName"
          placeholder="员工姓名"
          clearable
          style="width: 180px"
        />

        <el-date-picker
          v-model="queryParams.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 360px"
        />

        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        empty-text="暂无考勤数据"
      >
        <el-table-column
          prop="empName"
          label="员工姓名"
          width="120"
          align="center"
        />
        <el-table-column
          prop="workDate"
          label="工作日期"
          width="130"
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
          width="100"
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
        <el-table-column
          prop="ip"
          label="打卡IP"
          min-width="140"
        >
          <template #default="{ row }">
            {{ row.ip || "--" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="address"
          label="打卡地址"
          min-width="200"
        >
          <template #default="{ row }">
            {{ row.address || "--" }}
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
.attendance-manage-container {
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
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
