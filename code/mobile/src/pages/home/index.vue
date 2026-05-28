<template>
  <view class="container">
    <!-- User greeting -->
    <view class="greeting card">
      <view class="flex-row">
        <view class="avatar">{{ userStore.displayName.charAt(0) }}</view>
        <view class="ml-20">
          <text class="greeting-name">{{ userStore.displayName }}</text>
          <text class="greeting-time text-gray">{{ greeting }}</text>
        </view>
      </view>
    </view>

    <!-- Quick actions -->
    <view class="card">
      <text class="section-title">快捷入口</text>
      <view class="quick-grid">
        <view class="quick-item" v-for="item in quickActions" :key="item.path" @click="navigate(item.path)">
          <view class="quick-icon" :style="{ background: item.bg }">
            <text :style="{ color: item.color, fontSize: '36rpx' }">{{ item.icon }}</text>
          </view>
          <text class="quick-label">{{ item.label }}</text>
        </view>
      </view>
    </view>

    <!-- Today attendance -->
    <view class="card" v-if="loading">
      <view class="skeleton-block"></view>
      <view class="skeleton-block short"></view>
    </view>
    <view class="card" v-else-if="attendance">
      <text class="section-title">今日考勤</text>
      <view class="flex-between mt-20">
        <view>
          <text class="text-gray">上班打卡</text>
          <text class="clock-time">{{ attendance.clockInTime || '未打卡' }}</text>
        </view>
        <view>
          <text class="text-gray">下班打卡</text>
          <text class="clock-time">{{ attendance.clockOutTime || '未打卡' }}</text>
        </view>
      </view>
    </view>

    <!-- Todo stats -->
    <view class="card" @click="goTodo">
      <view class="flex-between">
        <text class="section-title" style="margin-bottom:0">待办事项</text>
        <text class="text-primary">{{ todoCount }} 项待处理 ></text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { getTodayAttendance } from "@/api/attendance";
import { getTodoCount } from "@/api/todo";

const userStore = useUserStore();
const attendance = ref<any>(null);
const todoCount = ref(0);
const loading = ref(true);

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 9) return "早上好";
  if (h < 12) return "上午好";
  if (h < 14) return "中午好";
  if (h < 18) return "下午好";
  return "晚上好";
});

const quickActions = [
  { label: "考勤打卡", icon: "📍", path: "/pages/oa/attendance", bg: "#ecf5ff", color: "#409EFF" },
  { label: "请假申请", icon: "📋", path: "/pages/oa/leave-apply", bg: "#f0f9eb", color: "#67C23A" },
  { label: "出差申请", icon: "✈️", path: "/pages/oa/business-trip", bg: "#fdf6ec", color: "#E6A23C" },
  { label: "外出申请", icon: "🚶", path: "/pages/oa/outing", bg: "#fef0f0", color: "#F56C6C" },
  { label: "加班申请", icon: "🌙", path: "/pages/oa/overtime", bg: "#f4f4f5", color: "#909399" },
  { label: "经费申请", icon: "💰", path: "/pages/oa/expense", bg: "#ecf5ff", color: "#409EFF" },
  { label: "采购申请", icon: "🛒", path: "/pages/oa/purchase", bg: "#f0f9eb", color: "#67C23A" },
  { label: "借支申请", icon: "💳", path: "/pages/oa/loan", bg: "#fdf6ec", color: "#E6A23C" },
  { label: "公告通知", icon: "📢", path: "/pages/oa/notice-list", bg: "#fef0f0", color: "#F56C6C" },
  { label: "消息中心", icon: "💬", path: "/pages/oa/message", bg: "#ecf5ff", color: "#409EFF" },
  { label: "我的日程", icon: "📅", path: "/pages/oa/schedule", bg: "#f0f9eb", color: "#67C23A" },
  { label: "文档中心", icon: "📁", path: "/pages/oa/document", bg: "#f4f4f5", color: "#909399" }
];

const navigate = (path: string) => {
  uni.navigateTo({ url: path });
};

const goTodo = () => {
  uni.switchTab({ url: "/pages/todo/index" });
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res: any = await getTodayAttendance();
    attendance.value = res.data || null;
  } catch {}
  try {
    const res: any = await getTodoCount();
    todoCount.value = res.data || 0;
  } catch {} finally {
    loading.value = false;
  }
};

onMounted(fetchData);
onShow(fetchData);
</script>

<style scoped>
.greeting { display: flex; align-items: center; }
.avatar {
  width: 80rpx; height: 80rpx; border-radius: 50%; background: #409EFF;
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; font-weight: bold;
}
.greeting-name { font-size: 32rpx; font-weight: 600; display: block; }
.greeting-time { font-size: 24rpx; display: block; margin-top: 4rpx; }
.quick-grid { display: flex; flex-wrap: wrap; gap: 20rpx; }
.quick-item { width: calc(25% - 16rpx); display: flex; flex-direction: column; align-items: center; gap: 8rpx; }
.quick-icon { width: 88rpx; height: 88rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; }
.quick-label { font-size: 24rpx; color: #606266; }
.clock-time { display: block; font-size: 32rpx; font-weight: 600; color: #303133; margin-top: 4rpx; }
.ml-20 { margin-left: 20rpx; }
.skeleton-block {
  height: 40rpx; border-radius: 8rpx; background: linear-gradient(90deg, #f2f2f2 25%, #e6e6e6 50%, #f2f2f2 75%);
  background-size: 200% 100%; animation: skeleton-loading 1.5s infinite; margin-bottom: 16rpx;
}
.skeleton-block.short { width: 60%; }
@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
