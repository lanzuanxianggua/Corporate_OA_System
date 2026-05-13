<template>
  <div>
    <div class="text-center mb-8">
      <div class="text-5xl font-bold text-[#303133] font-mono">{{ currentTime }}</div>
      <div class="text-base text-[#909399] mt-2">{{ currentDate }}</div>
    </div>

    <el-row :gutter="20" class="mb-6">
      <el-col :span="12">
        <div class="bg-white rounded-lg p-6" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 rounded-full bg-[#fff7e6] flex items-center justify-center">
              <el-icon :size="24" color="#E6A23C"><Sunny /></el-icon>
            </div>
            <span class="text-lg font-medium text-[#303133]">上班打卡</span>
          </div>
          <template v-if="todayData?.clockInTime">
            <div class="flex items-center gap-2 text-2xl font-bold text-[#67C23A]">
              <el-icon><CircleCheck /></el-icon>
              {{ todayData.clockInTime?.substring(11, 19) }}
            </div>
            <div class="text-sm text-[#909399] mt-1">已打卡</div>
          </template>
          <template v-else>
            <el-button type="primary" size="large" :loading="clockingIn" @click="handleClockIn">
              点击打卡
            </el-button>
          </template>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="bg-white rounded-lg p-6" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 rounded-full bg-[#e6f7ff] flex items-center justify-center">
              <el-icon :size="24" color="#409EFF"><Moon /></el-icon>
            </div>
            <span class="text-lg font-medium text-[#303133]">下班打卡</span>
          </div>
          <template v-if="todayData?.clockOutTime">
            <div class="flex items-center gap-2 text-2xl font-bold text-[#67C23A]">
              <el-icon><CircleCheck /></el-icon>
              {{ todayData.clockOutTime?.substring(11, 19) }}
            </div>
            <div class="text-sm text-[#909399] mt-1">已打卡</div>
          </template>
          <template v-else>
            <el-button type="primary" size="large" :loading="clockingOut" @click="handleClockOut">
              点击打卡
            </el-button>
          </template>
        </div>
      </el-col>
    </el-row>

    <el-card>
      <template #header><span class="font-medium">今日考勤记录</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="上班时间">
          {{ todayData?.clockInTime?.substring(11, 19) || "未打卡" }}
        </el-descriptions-item>
        <el-descriptions-item label="下班时间">
          {{ todayData?.clockOutTime?.substring(11, 19) || "未打卡" }}
        </el-descriptions-item>
        <el-descriptions-item label="考勤状态">
          <el-tag :type="statusType(todayData?.status)">
            {{ statusText(todayData?.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import dayjs from "dayjs";
import { ElMessage } from "element-plus";
import { getTodayAttendance, clockIn, clockOut } from "@/api/attendance";

const currentTime = ref(dayjs().format("HH:mm:ss"));
const currentDate = ref(dayjs().format("YYYY年MM月DD日 dddd"));
const todayData = ref<any>(null);
const clockingIn = ref(false);
const clockingOut = ref(false);
let timer: number;

const statusMap: Record<string, { text: string; type: string }> = {
  normal: { text: "正常", type: "success" },
  late: { text: "迟到", type: "warning" },
  early: { text: "早退", type: "warning" },
  absent: { text: "缺勤", type: "danger" },
  unclocked: { text: "未打卡", type: "info" }
};

const statusText = (status?: string) => statusMap[status || "unclocked"]?.text || "未打卡";
const statusType = (status?: string) => statusMap[status || "unclocked"]?.type || "info" as const;

const fetchData = async () => {
  try {
    const res: any = await getTodayAttendance();
    if (res.data) todayData.value = res.data;
  } catch {
    // ignore
  }
};

const handleClockIn = async () => {
  clockingIn.value = true;
  try {
    await clockIn();
    ElMessage.success("上班打卡成功");
    await fetchData();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clockingIn.value = false;
  }
};

const handleClockOut = async () => {
  clockingOut.value = true;
  try {
    await clockOut();
    ElMessage.success("下班打卡成功");
    await fetchData();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clockingOut.value = false;
  }
};

onMounted(() => {
  fetchData();
  timer = window.setInterval(() => {
    currentTime.value = dayjs().format("HH:mm:ss");
  }, 1000);
});

onUnmounted(() => clearInterval(timer));
</script>
