<template>
  <div>
    <div class="flex items-center gap-3 mb-4 flex-wrap">
      <el-input v-model="searchName" placeholder="搜索员工姓名" clearable class="w-48" @keyup.enter="fetchData" />
      <el-select v-model="statusFilter" placeholder="考勤状态" clearable class="w-32">
        <el-option label="正常" :value="0" />
        <el-option label="迟到" :value="1" />
        <el-option label="早退" :value="2" />
        <el-option label="缺勤" :value="3" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" size="default" style="width: 260px" />
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card>
      <el-table :data="attendanceList" stripe v-loading="loading">
        <el-table-column label="工号" prop="empId" width="100" />
        <el-table-column label="姓名" prop="empName" width="100" />
        <el-table-column label="日期" prop="workDate" width="120" />
        <el-table-column label="上班时间">
          <template #default="{ row }">{{ row.clockIn ? row.clockIn.substring(11, 19) : "-" }}</template>
        </el-table-column>
        <el-table-column label="下班时间">
          <template #default="{ row }">{{ row.clockOut ? row.clockOut.substring(11, 19) : "-" }}</template>
        </el-table-column>
        <el-table-column label="工作时长">
          <template #default="{ row }">{{ calcWorkHours(row.clockIn, row.clockOut) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
      </el-table>

      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchData"
          @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAttendanceAdminPage } from "@/api/attendance";

const searchName = ref("");
const statusFilter = ref<number | "">("");
const dateRange = ref<string[]>([]);
const attendanceList = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: "正常", type: "success" },
  1: { text: "迟到", type: "warning" },
  2: { text: "早退", type: "warning" },
  3: { text: "缺勤", type: "danger" },
  4: { text: "请假", type: "info" }
};
const statusText = (s?: number) => statusMap[s ?? -1]?.text || "未打卡";
const statusType = (s?: number) => (statusMap[s ?? -1]?.type || "info") as any;

const calcWorkHours = (clockIn?: string, clockOut?: string) => {
  if (!clockIn || !clockOut) return "-";
  const diff = new Date(clockOut).getTime() - new Date(clockIn).getTime();
  return (diff / (1000 * 60 * 60)).toFixed(1) + "h";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    };
    if (searchName.value) params.empName = searchName.value;
    if (statusFilter.value !== "") params.status = statusFilter.value;
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res: any = await getAttendanceAdminPage(params);
    if (res.data) {
      attendanceList.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } catch {
    attendanceList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  searchName.value = "";
  statusFilter.value = "";
  dateRange.value = [];
  pageNum.value = 1;
  fetchData();
};

onMounted(fetchData);
</script>
