<template>
  <div>
    <div class="text-center mb-8">
      <div class="text-5xl font-bold text-[var(--oa-text)] font-mono">{{ currentTime }}</div>
      <div class="text-base text-[var(--oa-subtle)] mt-2">{{ currentDate }}</div>
    </div>

    <el-row :gutter="20" class="mb-6">
      <el-col :xs="24" :md="12">
        <div class="bg-[var(--oa-surface)] rounded-lg p-6" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 rounded-full bg-[var(--oa-surface-muted)] flex items-center justify-center">
              <el-icon :size="24" color="#E6A23C"><Sunny /></el-icon>
            </div>
            <span class="text-lg font-medium">上班打卡</span>
          </div>
          <template v-if="todayData?.clockIn">
            <div class="flex items-center gap-2 text-2xl font-bold text-[var(--oa-success)]">
              <el-icon><CircleCheck /></el-icon>
              {{ todayData.clockIn.substring(11, 19) }}
            </div>
          </template>
          <template v-else>
            <el-button type="primary" size="large" :loading="clockingIn" :disabled="clockedInDone" @click="handleClockIn">点击打卡</el-button>
          </template>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="bg-[var(--oa-surface)] rounded-lg p-6" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 rounded-full bg-[var(--oa-surface-muted)] flex items-center justify-center">
              <el-icon :size="24" color="var(--oa-primary)"><Moon /></el-icon>
            </div>
            <span class="text-lg font-medium">下班打卡</span>
          </div>
          <template v-if="todayData?.clockOut">
            <div class="flex items-center gap-2 text-2xl font-bold text-[var(--oa-success)]">
              <el-icon><CircleCheck /></el-icon>
              {{ todayData.clockOut.substring(11, 19) }}
            </div>
          </template>
          <template v-else>
            <el-button type="primary" size="large" :loading="clockingOut" :disabled="clockedOutDone" @click="handleClockOut">点击打卡</el-button>
          </template>
        </div>
      </el-col>
    </el-row>

    <!-- 考勤历史记录 -->
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">考勤记录</span>
          <div class="flex items-center gap-3">
            <el-radio-group v-model="historyPeriod" size="small" @change="handlePeriodChange">
              <el-radio-button value="day">本日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
            <el-date-picker v-if="historyPeriod === 'day'" v-model="queryDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" size="small" style="width:150px" @change="fetchHistory" />
          </div>
        </div>
      </template>
      <el-table :data="historyList" stripe style="width: 100%">
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
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
      </el-table>
      <el-empty v-if="historyList.length === 0" description="暂无考勤记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import dayjs from "dayjs";
import isoWeek from "dayjs/plugin/isoWeek";
import { ElMessage } from "element-plus";
import { getTodayAttendance, getAttendanceHistory, clockIn as clockInApi, clockOut as clockOutApi } from "@/api/attendance";
import type { Attendance } from "@/types/api";

dayjs.extend(isoWeek);

const currentTime = ref(dayjs().format("HH:mm:ss"));
const currentDate = ref(dayjs().format("YYYY年MM月DD日 dddd"));
const todayData = ref<Attendance | null>(null);
const clockingIn = ref(false);
const clockingOut = ref(false);
const clockedInDone = ref(false);
const clockedOutDone = ref(false);
let timer: number;

const historyPeriod = ref("day");
const queryDate = ref(dayjs().format("YYYY-MM-DD"));
const historyList = ref<Attendance[]>([]);

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: "正常", type: "success" },
  1: { text: "迟到", type: "warning" },
  2: { text: "早退", type: "warning" },
  3: { text: "缺勤", type: "danger" },
  4: { text: "请假", type: "info" }
};
const statusText = (s?: number) => statusMap[s ?? -1]?.text || "未打卡";
const statusType = (s?: number): "success" | "warning" | "danger" | "info" => {
  const map: Record<number, "success" | "warning" | "danger" | "info"> = {
    0: "success", 1: "warning", 2: "warning", 3: "danger", 4: "info"
  };
  return map[s ?? -1] || "info";
};

const calcWorkHours = (clockIn?: string, clockOut?: string) => {
  if (!clockIn || !clockOut) return "-";
  const diff = new Date(clockOut).getTime() - new Date(clockIn).getTime();
  return (diff / (1000 * 60 * 60)).toFixed(1) + "小时";
};

const getDateRange = () => {
  const today = dayjs();
  if (historyPeriod.value === "day") {
    const d = queryDate.value || today.format("YYYY-MM-DD");
    return { start: d, end: d };
  }
  if (historyPeriod.value === "week") {
    const monday = today.isoWeekday(1);
    return { start: monday.format("YYYY-MM-DD"), end: today.format("YYYY-MM-DD") };
  }
  return { start: today.startOf("month").format("YYYY-MM-DD"), end: today.format("YYYY-MM-DD") };
};

const fetchHistory = async () => {
  const { start, end } = getDateRange();
  try {
    const r = await getAttendanceHistory(start, end);
    if (r.data) historyList.value = r.data;
  } catch { historyList.value = []; }
};

const handlePeriodChange = () => {
  if (historyPeriod.value === "day") queryDate.value = dayjs().format("YYYY-MM-DD");
  fetchHistory();
};

const fetchToday = async () => {
  try {
    const r = await getTodayAttendance();
    if (r.data) {
      todayData.value = r.data;
      if (r.data.clockIn) clockedInDone.value = true;
      if (r.data.clockOut) clockedOutDone.value = true;
    }
  } catch {}
};

const handleClockIn = async () => {
  clockingIn.value = true;
  try {
    await clockInApi();
    ElMessage.success("上班打卡成功");
    clockedInDone.value = true;
  } catch {
    // interceptor already shows the error
  } finally {
    clockingIn.value = false;
    await fetchToday();
  }
};

const handleClockOut = async () => {
  clockingOut.value = true;
  try {
    await clockOutApi();
    ElMessage.success("下班打卡成功");
    clockedOutDone.value = true;
  } catch {
    // interceptor already shows the error
  } finally {
    clockingOut.value = false;
    await fetchToday();
  }
};

onMounted(() => {
  fetchToday();
  fetchHistory();
  timer = window.setInterval(() => { currentTime.value = dayjs().format("HH:mm:ss"); }, 1000);
});
onUnmounted(() => clearInterval(timer));
</script>
