<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <el-input v-model="searchName" placeholder="搜索员工姓名" clearable class="w-48" />
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-select v-model="statusFilter" placeholder="状态" clearable class="w-32">
        <el-option label="全部" value="" />
        <el-option label="正常" :value="0" />
        <el-option label="迟到" :value="1" />
        <el-option label="早退" :value="2" />
        <el-option label="缺勤" :value="3" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
    </div>
    <el-card>
      <el-table :data="attendanceList" stripe>
        <el-table-column label="工号" prop="empId" width="80" />
        <el-table-column label="姓名" prop="empName" />
        <el-table-column label="部门" prop="deptName" />
        <el-table-column label="上班时间">
          <template #default="{ row }">{{ row.clockIn?.substring(11, 19) || "-" }}</template>
        </el-table-column>
        <el-table-column label="下班时间">
          <template #default="{ row }">{{ row.clockOut?.substring(11, 19) || "-" }}</template>
        </el-table-column>
        <el-table-column label="工作时长">
          <template #default="{ row }">{{ calcWorkHours(row.clockIn, row.clockOut) }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAttendancePage } from "@/api/attendance";

const searchName = ref("");
const dateRange = ref<[Date, Date] | null>(null);
const statusFilter = ref<number | "">("");
const attendanceList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: "正常", type: "success" },
  1: { text: "迟到", type: "warning" },
  2: { text: "早退", type: "warning" },
  3: { text: "缺勤", type: "danger" }
};
const statusText = (s?: number) => statusMap[s ?? -1]?.text || "未打卡";
const statusType = (s?: number) => (statusMap[s ?? -1]?.type || "info") as any;

const calcWorkHours = (clockIn?: string, clockOut?: string) => {
  if (!clockIn || !clockOut) return "-";
  const diff = new Date(clockOut).getTime() - new Date(clockIn).getTime();
  return (diff / (1000 * 60 * 60)).toFixed(1) + "h";
};

const fetchData = async () => {
  try {
    const params: any = { pageNum: page.value, pageSize: pageSize.value };
    if (statusFilter.value !== "") params.status = statusFilter.value;
    const r: any = await getAttendancePage(params);
    if (r.data?.list) { attendanceList.value = r.data.list; total.value = r.data.total || 0; }
  } catch {
    attendanceList.value = [];
  }
};

onMounted(fetchData);
</script>
