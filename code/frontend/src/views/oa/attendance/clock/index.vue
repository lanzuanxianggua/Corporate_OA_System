<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import { clockIn, clockOut, getTodayStatus } from "@/api/oa/attendance";

defineOptions({ name: "OaAttendanceClock" });

const now = ref(new Date());
const todayStatus = ref<any>({});
const loading = ref(false);
const clockInLoading = ref(false);
const clockOutLoading = ref(false);

let timer: ReturnType<typeof setInterval> | null = null;

/** 格式化时间 HH:mm:ss */
function formatTime(date: Date): string {
  const h = String(date.getHours()).padStart(2, "0");
  const m = String(date.getMinutes()).padStart(2, "0");
  const s = String(date.getSeconds()).padStart(2, "0");
  return `${h}:${m}:${s}`;
}

/** 格式化日期 YYYY-MM-DD 星期X */
function formatDate(date: Date): string {
  const y = date.getFullYear();
  const mo = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
  return `${y}-${mo}-${d} 星期${weekdays[date.getDay()]}`;
}

/** 加载今日考勤状态 */
async function loadTodayStatus() {
  try {
    const res = await getTodayStatus();
    if (res.data) {
      todayStatus.value = res.data;
    }
  } catch {
    // 静默处理
  }
}

/** 上班打卡 */
async function handleClockIn() {
  clockInLoading.value = true;
  try {
    const res = await clockIn();
    if (res.code === 200 || res.code === 0) {
      ElMessage.success("上班打卡成功");
      await loadTodayStatus();
    } else {
      ElMessage.error(res.message || "打卡失败");
    }
  } catch {
    ElMessage.error("打卡请求异常");
  } finally {
    clockInLoading.value = false;
  }
}

/** 下班打卡 */
async function handleClockOut() {
  clockOutLoading.value = true;
  try {
    const res = await clockOut();
    if (res.code === 200 || res.code === 0) {
      ElMessage.success("下班打卡成功");
      await loadTodayStatus();
    } else {
      ElMessage.error(res.message || "打卡失败");
    }
  } catch {
    ElMessage.error("打卡请求异常");
  } finally {
    clockOutLoading.value = false;
  }
}

/** 判断是否已上班打卡 */
const hasClockedIn = ref(false);
/** 判断是否已下班打卡 */
const hasClockedOut = ref(false);

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);
  loadTodayStatus();
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<template>
  <div class="attendance-clock-container">
    <el-card shadow="hover" class="clock-card">
      <template #header>
        <div class="card-header">
          <span>考勤打卡</span>
        </div>
      </template>

      <!-- 当前时间显示 -->
      <div class="time-display">
        <div class="current-date">{{ formatDate(now) }}</div>
        <div class="current-time">{{ formatTime(now) }}</div>
      </div>

      <!-- 打卡按钮 -->
      <div class="clock-buttons">
        <el-button
          type="primary"
          size="large"
          :loading="clockInLoading"
          :disabled="!!todayStatus.clockIn"
          @click="handleClockIn"
          class="clock-btn"
        >
          {{ todayStatus.clockIn ? "已签到" : "上班打卡" }}
        </el-button>
        <el-button
          type="primary"
          size="large"
          :loading="clockOutLoading"
          :disabled="!todayStatus.clockIn || !!todayStatus.clockOut"
          @click="handleClockOut"
          class="clock-btn"
        >
          {{ todayStatus.clockOut ? "已签退" : "下班打卡" }}
        </el-button>
      </div>
    </el-card>

    <!-- 今日考勤状态 -->
    <el-card shadow="hover" class="status-card" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>今日考勤状态</span>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="上班打卡时间">
          {{ todayStatus.clockIn || "未打卡" }}
        </el-descriptions-item>
        <el-descriptions-item label="下班打卡时间">
          {{ todayStatus.clockOut || "未打卡" }}
        </el-descriptions-item>
        <el-descriptions-item label="考勤状态">
          <el-tag
            v-if="todayStatus.status"
            :type="
              todayStatus.status === '正常'
                ? 'success'
                : todayStatus.status === '迟到'
                  ? 'warning'
                  : todayStatus.status === '早退'
                    ? 'warning'
                    : todayStatus.status === '缺勤'
                      ? 'danger'
                      : 'info'
            "
          >
            {{ todayStatus.status }}
          </el-tag>
          <span v-else>暂无记录</span>
        </el-descriptions-item>
        <el-descriptions-item label="工作时长">
          {{ todayStatus.workHours || "--" }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.attendance-clock-container {
  padding: 16px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.time-display {
  text-align: center;
  padding: 32px 0;
}

.current-date {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.current-time {
  font-size: 56px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 4px;
  font-variant-numeric: tabular-nums;
}

.clock-buttons {
  display: flex;
  justify-content: center;
  gap: 40px;
  padding: 16px 0 32px;
}

.clock-btn {
  width: 160px;
  height: 56px;
  font-size: 18px;
  border-radius: 28px;
}

.status-card :deep(.el-descriptions) {
  margin-top: 8px;
}
</style>
