<template>
  <view class="container">
    <view class="card">
      <text class="section-title">今日考勤</text>
      <view class="flex-between mt-20" v-if="today">
        <view class="clock-block">
          <text class="text-gray">上班</text>
          <text class="clock-time">{{ formatClock(today.clockIn) || '--:--' }}</text>
        </view>
        <view class="clock-block">
          <text class="text-gray">下班</text>
          <text class="clock-time">{{ formatClock(today.clockOut) || '--:--' }}</text>
        </view>
      </view>
    </view>

    <button class="clock-btn" :class="clockedIn && !clockedOut ? 'btn-out' : 'btn-in'" :disabled="clocking" @click="handleClock">
      {{ clocking ? '打卡中...' : (clockedIn && !clockedOut ? '下班打卡' : '上班打卡') }}
    </button>
    <text class="text-gray clock-hint">{{ currentTime }}</text>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getTodayAttendance, clockIn, clockOut } from "@/api/attendance";

const today = ref<any>(null);
const clockedIn = ref(false);
const clockedOut = ref(false);
const clocking = ref(false);
const currentTime = ref("");
let timer: ReturnType<typeof setInterval> | null = null;

/** Format backend clock datetime to "HH:mm" */
const formatClock = (dt: string) => {
  if (!dt) return "";
  const parts = dt.split(" ");
  return parts.length > 1 ? parts[1].substring(0, 5) : dt.substring(0, 5);
};

const updateTime = () => {
  const now = new Date();
  currentTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
};

const fetchToday = async () => {
  try {
    const res: any = await getTodayAttendance();
    today.value = res.data;
    clockedIn.value = !!res.data?.clockIn;
    clockedOut.value = !!res.data?.clockOut;
  } catch {
    // silently handle
  }
};

const handleClock = async () => {
  if (clocking.value) return;
  clocking.value = true;
  try {
    if (clockedIn.value && !clockedOut.value) {
      await clockOut();
      uni.showToast({ title: "下班打卡成功", icon: "success" });
    } else {
      await clockIn();
      uni.showToast({ title: "上班打卡成功", icon: "success" });
    }
    fetchToday();
  } catch {
    // Error toast handled by request interceptor
  } finally {
    clocking.value = false;
  }
};

onMounted(() => { updateTime(); timer = setInterval(updateTime, 1000); });
onUnmounted(() => { if (timer) clearInterval(timer); });
onShow(fetchToday);
</script>

<style scoped>
.clock-block { flex: 1; text-align: center; }
.clock-time { display: block; font-size: 48rpx; font-weight: bold; color: #303133; margin-top: 16rpx; }
.clock-btn {
  width: 300rpx; height: 300rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 60rpx auto 20rpx; font-size: 36rpx; font-weight: bold;
  color: #fff; border: none;
}
.btn-in { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.btn-out { background: linear-gradient(135deg, #E6A23C, #f0c78a); }
.clock-hint { display: block; text-align: center; font-size: 28rpx; }
</style>
