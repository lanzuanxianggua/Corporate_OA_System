<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <el-date-picker v-model="month" type="month" placeholder="选择月份" format="YYYY年MM月" value-format="YYYY-MM" @change="fetchData" />
    </div>
    <el-card>
      <el-table :data="attendanceList" stripe style="width: 100%">
        <el-table-column label="日期" prop="workDate" />
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
        <el-table-column label="备注" prop="remark" />
      </el-table>
      <el-empty v-if="attendanceList.length === 0" description="暂无考勤记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import dayjs from "dayjs";
import { getAttendanceHistory } from "@/api/attendance";

const month = ref(dayjs().format("YYYY-MM"));
const attendanceList = ref<any[]>([]);

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: "正常", type: "success" },
  1: { text: "迟到", type: "warning" },
  2: { text: "早退", type: "warning" },
  3: { text: "缺勤", type: "danger" },
  5: { text: "休假", type: "" },
  6: { text: "出差", type: "primary" }
};
const statusText = (s?: number) => statusMap[s ?? -1]?.text || "未打卡";
const statusType = (s?: number) => (statusMap[s ?? -1]?.type || "info") as any;

const calcWorkHours = (clockIn?: string, clockOut?: string) => {
  if (!clockIn || !clockOut) return "-";
  const diff = new Date(clockOut).getTime() - new Date(clockIn).getTime();
  const hours = diff / (1000 * 60 * 60);
  return hours.toFixed(1) + "小时";
};

const fetchData = async () => {
  try {
    const startDate = dayjs(month.value).startOf("month").format("YYYY-MM-DD");
    const endDate = dayjs(month.value).endOf("month").format("YYYY-MM-DD");
    const res: any = await getAttendanceHistory(startDate, endDate);
    attendanceList.value = res.data || [];
  } catch {
    attendanceList.value = [];
  }
};

onMounted(fetchData);
</script>
