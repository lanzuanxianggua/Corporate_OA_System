<template>
  <div class="clock-container">
    <div class="clock-display">
      <div class="time">{{ currentTime }}</div>
      <div class="date">{{ currentDate }}</div>
    </div>

    <el-row :gutter="20" class="clock-cards">
      <el-col :span="12">
        <el-card class="clock-card">
          <div class="clock-content">
            <div class="clock-icon" style="background-color: #fff7e6">
              <el-icon size="32" color="#E6A23C"><Sunrise /></el-icon>
            </div>
            <div class="clock-info">
              <span class="clock-title">上班打卡</span>
              <span class="clock-time" v-if="attendance.clockInTime">
                <el-icon color="#67C23A"><CircleCheck /></el-icon>
                {{ formatTime(attendance.clockInTime) }}
              </span>
              <span class="clock-time no-clock" v-else>--:--</span>
            </div>
            <el-button
              v-if="!attendance.clockInTime"
              type="primary"
              size="large"
              @click="handleClockIn"
              :loading="clocking"
            >
              点击打卡
            </el-button>
            <el-tag v-else type="success" size="large">已打卡</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="clock-card">
          <div class="clock-content">
            <div class="clock-icon" style="background-color: #e6f7ff">
              <el-icon size="32" color="#409EFF"><Sunset /></el-icon>
            </div>
            <div class="clock-info">
              <span class="clock-title">下班打卡</span>
              <span class="clock-time" v-if="attendance.clockOutTime">
                <el-icon color="#67C23A"><CircleCheck /></el-icon>
                {{ formatTime(attendance.clockOutTime) }}
              </span>
              <span class="clock-time no-clock" v-else>--:--</span>
            </div>
            <el-button
              v-if="attendance.clockInTime && !attendance.clockOutTime"
              type="primary"
              size="large"
              @click="handleClockOut"
              :loading="clocking"
            >
              点击打卡
            </el-button>
            <el-tag v-else-if="attendance.clockOutTime" type="success" size="large">已打卡</el-tag>
            <el-tag v-else type="info" size="large">请先上班打卡</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="record-card">
      <template #header>
        <span>今日考勤记录</span>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="上班时间">{{ attendance.clockInTime ? formatTime(attendance.clockInTime) : '--:--' }}</el-descriptions-item>
        <el-descriptions-item label="下班时间">{{ attendance.clockOutTime ? formatTime(attendance.clockOutTime) : '--:--' }}</el-descriptions-item>
        <el-descriptions-item label="工作时长">{{ workHours }}</el-descriptions-item>
        <el-descriptions-item label="考勤状态">
          <el-tag :type="statusType">{{ statusText }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import { getTodayAttendance, clockIn, clockOut } from "@/api/attendance";

const currentTime = ref(dayjs().format("HH:mm:ss"));
const currentDate = ref(dayjs().format("YYYY年MM月DD日 dddd"));
let timeInterval: number;

const attendance = reactive({
  clockInTime: "",
  clockOutTime: ""
});
const clocking = ref(false);

const formatTime = (timeStr: string) => {
  if (!timeStr) return "--:--";
  const time = dayjs(timeStr);
  return time.isValid() ? time.format("HH:mm") : timeStr;
};

const workHours = computed(() => {
  if (!attendance.clockInTime || !attendance.clockOutTime) return "--";
  const start = dayjs(attendance.clockInTime);
  const end = dayjs(attendance.clockOutTime);
  const diff = end.diff(start, "hour", true);
  return `${diff.toFixed(1)}小时`;
});

const statusType = computed(() => {
  if (!attendance.clockInTime) return "info";
  if (dayjs(attendance.clockInTime).hour() > 9) return "warning";
  return "success";
});

const statusText = computed(() => {
  if (!attendance.clockInTime) return "未打卡";
  if (dayjs(attendance.clockInTime).hour() > 9) return "迟到";
  return "正常";
});

const loadTodayAttendance = async () => {
  try {
    const res: any = await getTodayAttendance();
    if (res.data) {
      attendance.clockInTime = res.data.clockInTime || "";
      attendance.clockOutTime = res.data.clockOutTime || "";
    }
  } catch (error) {
    console.error("获取今日考勤失败", error);
  }
};

const handleClockIn = async () => {
  try {
    clocking.value = true;
    await clockIn();
    ElMessage.success("上班打卡成功");
    await loadTodayAttendance();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clocking.value = false;
  }
};

const handleClockOut = async () => {
  try {
    clocking.value = true;
    await clockOut();
    ElMessage.success("下班打卡成功");
    await loadTodayAttendance();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clocking.value = false;
  }
};

onMounted(() => {
  timeInterval = window.setInterval(() => {
    currentTime.value = dayjs().format("HH:mm:ss");
    currentDate.value = dayjs().format("YYYY年MM月DD日 dddd");
  }, 1000);
  loadTodayAttendance();
});

onUnmounted(() => {
  clearInterval(timeInterval);
});
</script>

<style scoped lang="scss">
.clock-container {
  max-width: 1000px;
  margin: 0 auto;
}

.clock-display {
  text-align: center;
  margin-bottom: 32px;

  .time {
    font-size: 64px;
    font-weight: bold;
    color: #303133;
    font-family: "DIN Alternate", "Helvetica Neue", Arial, sans-serif;
  }

  .date {
    font-size: 18px;
    color: #909399;
    margin-top: 8px;
  }
}

.clock-cards {
  margin-bottom: 20px;
}

.clock-card {
  .clock-content {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 20px;
  }
}

.clock-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clock-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .clock-title {
    font-size: 16px;
    color: #606266;
  }

  .clock-time {
    font-size: 28px;
    font-weight: bold;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 8px;

    &.no-clock {
      color: #c0c4cc;
    }
  }
}

.record-card {
  margin-top: 20px;
}
</style>