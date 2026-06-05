<template>
  <div>
    <!-- 问候区域 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#303133] mb-2">
        {{ greeting }}，{{ userStore.userInfo?.empName || "用户" }}
      </h1>
      <p class="text-sm text-[#909399]">{{ currentDate }}</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="6">
        <div class="bg-white rounded-lg p-5 flex items-center gap-4" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="w-14 h-14 rounded-lg flex items-center justify-center" :style="{ backgroundColor: item.bgColor }">
            <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">{{ item.value }}</span>
            <span class="text-sm text-[#909399] mt-1">{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 今日考勤状态 + 快捷入口 -->
    <el-row :gutter="20" class="mb-5">
      <el-col :span="12">
        <el-card class="h-full">
          <template #header><span class="font-medium">今日考勤</span></template>
          <div v-if="todayAtt" class="flex items-center justify-around">
            <div class="text-center">
              <div class="text-xs text-[#909399] mb-2">上班打卡</div>
              <div class="text-lg font-bold" :class="todayAtt.clockIn ? 'text-[#67C23A]' : 'text-[#909399]'">
                {{ todayAtt.clockIn ? todayAtt.clockIn.substring(11, 16) : "未打卡" }}
              </div>
            </div>
            <el-divider direction="vertical" style="height:50px" />
            <div class="text-center">
              <div class="text-xs text-[#909399] mb-2">下班打卡</div>
              <div class="text-lg font-bold" :class="todayAtt.clockOut ? 'text-[#67C23A]' : 'text-[#909399]'">
                {{ todayAtt.clockOut ? todayAtt.clockOut.substring(11, 16) : "未打卡" }}
              </div>
            </div>
            <el-divider direction="vertical" style="height:50px" />
            <div class="text-center">
              <div class="text-xs text-[#909399] mb-2">考勤状态</div>
              <el-tag :type="statusTagType" size="large">{{ statusText }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="今日暂无考勤记录" :image-size="50" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="h-full">
          <template #header><span class="font-medium">快捷入口</span></template>
          <div class="grid grid-cols-4 gap-4">
            <div v-for="item in quickEntries" :key="item.label"
              class="flex flex-col items-center gap-2 py-3 cursor-pointer rounded-lg hover:bg-[#f5f7fa] transition-colors"
              @click="$router.push(item.path)">
              <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
              <span class="text-xs text-[#606266]">{{ item.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 本月个人统计 -->
    <el-row :gutter="20" class="mb-5">
      <el-col :span="8">
        <el-card class="h-full">
          <template #header><span class="font-medium">本月出勤概览</span></template>
          <div class="grid grid-cols-2 gap-4 text-center">
            <div class="p-3 bg-[#f0f9ff] rounded-lg">
              <div class="text-2xl font-bold text-[#409EFF]">{{ monthStats.normalDays }}</div>
              <div class="text-xs text-[#909399] mt-1">正常出勤</div>
            </div>
            <div class="p-3 bg-[#fff7e6] rounded-lg">
              <div class="text-2xl font-bold text-[#E6A23C]">{{ monthStats.lateDays }}</div>
              <div class="text-xs text-[#909399] mt-1">迟到</div>
            </div>
            <div class="p-3 bg-[#fef0f0] rounded-lg">
              <div class="text-2xl font-bold text-[#F56C6C]">{{ monthStats.earlyLeaveDays }}</div>
              <div class="text-xs text-[#909399] mt-1">早退</div>
            </div>
            <div class="p-3 bg-[#f9f0ff] rounded-lg">
              <div class="text-2xl font-bold text-[#9254de]">{{ monthStats.absentDays }}</div>
              <div class="text-xs text-[#909399] mt-1">缺勤</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="h-full">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">最近公告</span>
              <el-button type="primary" link @click="$router.push('/oa/notice/list')">查看更多</el-button>
            </div>
          </template>
          <div v-if="noticeList.length > 0">
            <div v-for="item in noticeList" :key="item.id"
              class="py-3 border-b border-[#ebeef5] last:border-b-0 cursor-pointer hover:text-[#409EFF]"
              @click="handleViewNotice(item)">
              <div class="text-sm font-medium text-[#303133] flex items-center gap-2 mb-1">
                {{ item.title }}
                <el-tag v-if="item.noticeType === 1" type="danger" size="small">公告</el-tag>
              </div>
              <div class="text-xs text-[#909399]">{{ item.createTime }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title" width="600px">
      <div class="mb-4 text-sm text-[#909399]">{{ currentNotice?.createTime }}</div>
      <el-divider />
      <div class="text-sm text-[#303133] leading-6">{{ currentNotice?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import dayjs from "dayjs";
import { useUserStore } from "@/store/user";
import { getTodayAttendance } from "@/api/attendance";
import { getNoticePage } from "@/api/notice";
import { getUnreadCount } from "@/api/message";
import { getSchedulePage } from "@/api/schedule";
import { getLeavePage } from "@/api/leave";
import { getPersonalAttendanceSummary } from "@/api/report";
import type { Attendance, Notice } from "@/types/api";

const userStore = useUserStore();

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 12) return "早上好";
  if (h < 18) return "下午好";
  return "晚上好";
});
const currentDate = computed(() => dayjs().format("YYYY年MM月DD日 dddd"));

const dashboardData = reactive({ clockedIn: 0, leaveTotal: 0, unreadMessage: 0, todaySchedule: 0 });
const todayAtt = ref<Attendance | null>(null);
const monthStats = reactive({ normalDays: 0, lateDays: 0, earlyLeaveDays: 0, absentDays: 0 });

const statusText = computed(() => {
  if (!todayAtt.value) return "未打卡";
  const s = todayAtt.value.status;
  if (s === 0) return "正常";
  if (s === 1) return "迟到";
  if (s === 2) return "早退";
  if (s === 3) return "缺勤";
  return "未知";
});
const statusTagType = computed(() => {
  if (!todayAtt.value) return "info";
  const s = todayAtt.value.status;
  if (s === 0) return "success";
  if (s === 1) return "warning";
  if (s === 2) return "danger";
  return "info";
});

const statsCards = computed(() => [
  { label: "今日出勤", value: dashboardData.clockedIn, icon: "CircleCheck", color: "#409EFF", bgColor: "#e6f7ff" },
  { label: "待审请假", value: dashboardData.leaveTotal, icon: "Document", color: "#E6A23C", bgColor: "#fff7e6" },
  { label: "未读消息", value: dashboardData.unreadMessage, icon: "Message", color: "#F56C6C", bgColor: "#fef0f0" },
  { label: "今日日程", value: dashboardData.todaySchedule, icon: "Clock", color: "#9254de", bgColor: "#f9f0ff" }
]);

const quickEntries = [
  { label: "请假申请", icon: "DocumentAdd", color: "#409EFF", path: "/oa/leave/apply" },
  { label: "公告通知", icon: "Bell", color: "#E6A23C", path: "/oa/notice/list" },
  { label: "消息中心", icon: "ChatDotRound", color: "#9254de", path: "/oa/message/list" },
  { label: "我的日程", icon: "Calendar", color: "#67C23A", path: "/oa/schedule/index" }
];

const noticeList = ref<Notice[]>([]);
const noticeDialogVisible = ref(false);
const currentNotice = ref<Notice | null>(null);

const handleViewNotice = async (item: Notice) => {
  currentNotice.value = item;
  noticeDialogVisible.value = true;
};

onMounted(async () => {
  try {
    const res = await getTodayAttendance();
    if (res.data) {
      todayAtt.value = res.data;
      dashboardData.clockedIn = res.data.clockIn ? 1 : 0;
    }
  } catch {}
  try {
    const res = await getLeavePage({ pageNum: 1, pageSize: 1, status: 0 } as any);
    if (res.data?.total !== undefined) dashboardData.leaveTotal = res.data.total;
  } catch {}
  try {
    const res = await getUnreadCount();
    if (res.data !== undefined) dashboardData.unreadMessage = res.data;
  } catch {}
  try {
    const today = dayjs().format("YYYY-MM-DD");
    const res = await getSchedulePage({ pageNum: 1, pageSize: 100 });
    if (res.data?.list) {
      dashboardData.todaySchedule = res.data.list.filter((s) =>
        (s.startTime || "").startsWith(today)
      ).length;
    }
  } catch {}
  try {
    const res = await getPersonalAttendanceSummary(dayjs().format("YYYY-MM"));
    if (res.data) {
      monthStats.normalDays = (res.data as any).normalDays || 0;
      monthStats.lateDays = (res.data as any).lateDays || 0;
      monthStats.earlyLeaveDays = (res.data as any).earlyLeaveDays || 0;
      monthStats.absentDays = (res.data as any).absentDays || 0;
    }
  } catch {}
  try {
    const res = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (res.data?.list) noticeList.value = res.data.list;
  } catch {}
});
</script>
