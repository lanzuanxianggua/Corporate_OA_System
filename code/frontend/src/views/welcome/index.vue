<template>
  <section class="oa-analytics-page">
    <header class="oa-page-header">
      <div>
        <div class="oa-eyebrow">Home</div>
        <h1 class="oa-page-title">{{ greeting }}，{{ userStore.userInfo?.empName || "用户" }}</h1>
        <p class="oa-page-subtitle">{{ currentDate }}</p>
      </div>
      <div class="oa-header-actions">
        <el-button type="primary" :icon="DocumentAdd" @click="router.push('/oa/leave/apply')">
          请假申请
        </el-button>
        <el-button :icon="Calendar" @click="router.push('/oa/schedule/index')">
          我的日程
        </el-button>
      </div>
    </header>

    <div class="oa-stat-grid">
      <article v-for="item in statsCards" :key="item.label" class="oa-stat-card">
        <div class="oa-stat-icon" :style="{ color: item.color, backgroundColor: item.bgColor }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="oa-stat-label">{{ item.label }}</div>
          <div class="oa-stat-value">{{ item.value }}</div>
          <div class="oa-stat-note">{{ item.note }}</div>
        </div>
      </article>
    </div>

    <div class="oa-grid">
      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">今日考勤</h2>
            <p class="oa-panel-subtitle">展示当天上下班打卡与考勤状态</p>
          </div>
          <el-tag :type="statusTagType" effect="light">{{ statusText }}</el-tag>
        </div>

        <div v-if="todayAtt" class="oa-micro-grid">
          <div class="oa-kpi-box">
            <div class="oa-kpi-label">上班打卡</div>
            <div class="oa-kpi-value">{{ formatClock(todayAtt.clockIn) }}</div>
            <div class="oa-kpi-note">{{ todayAtt.clockIn ? "已记录" : "未打卡" }}</div>
          </div>
          <div class="oa-kpi-box">
            <div class="oa-kpi-label">下班打卡</div>
            <div class="oa-kpi-value">{{ formatClock(todayAtt.clockOut) }}</div>
            <div class="oa-kpi-note">{{ todayAtt.clockOut ? "已记录" : "未打卡" }}</div>
          </div>
          <div class="oa-kpi-box">
            <div class="oa-kpi-label">考勤状态</div>
            <div class="oa-kpi-value">{{ statusText }}</div>
            <div class="oa-kpi-note">按系统规则计算</div>
          </div>
          <div class="oa-kpi-box">
            <div class="oa-kpi-label">工作台入口</div>
            <div class="oa-kpi-value">OA</div>
            <div class="oa-kpi-note">日常办公汇总</div>
          </div>
        </div>
        <el-empty v-else description="今日暂无考勤记录" :image-size="64" />
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">快捷入口</h2>
            <p class="oa-panel-subtitle">常用办公功能聚合</p>
          </div>
        </div>
        <div class="oa-action-grid">
          <button
            v-for="item in quickEntries"
            :key="item.label"
            type="button"
            class="oa-action-tile"
            @click="router.push(item.path)"
          >
            <el-icon :size="20" :color="item.color"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </article>

      <article class="oa-panel oa-col-4">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">本月出勤概览</h2>
            <p class="oa-panel-subtitle">个人出勤状态统计</p>
          </div>
        </div>
        <div class="oa-micro-grid month-grid">
          <div v-for="item in monthCards" :key="item.label" class="oa-kpi-box">
            <div class="oa-kpi-label">{{ item.label }}</div>
            <div class="oa-kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
            <div class="oa-kpi-note">{{ item.note }}</div>
          </div>
        </div>
      </article>

      <article class="oa-panel oa-col-8">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">最近公告</h2>
            <p class="oa-panel-subtitle">公告与通知动态</p>
          </div>
          <el-button type="primary" link @click="router.push('/oa/notice/list')">查看更多</el-button>
        </div>
        <div v-if="noticeList.length">
          <div v-for="item in noticeList" :key="item.id" class="oa-list-item notice-item" @click="handleViewNotice(item)">
            <div>
              <div class="notice-title">
                {{ item.title }}
                <el-tag v-if="item.noticeType === 1" type="danger" size="small">公告</el-tag>
                <el-tag v-else type="info" size="small">通知</el-tag>
              </div>
              <div class="notice-time">{{ item.createTime }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无公告" :image-size="64" />
      </article>
    </div>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title" width="600px">
      <div class="notice-time">{{ currentNotice?.createTime }}</div>
      <el-divider />
      <div class="notice-content">{{ currentNotice?.content }}</div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { Component } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import {
  Bell,
  Calendar,
  ChatDotRound,
  CircleCheck,
  Clock,
  Document,
  DocumentAdd,
  Message
} from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { getTodayAttendance } from "@/api/attendance";
import { getNoticePage } from "@/api/notice";
import { getUnreadCount } from "@/api/message";
import { getSchedulePage } from "@/api/schedule";
import { getLeavePage } from "@/api/leave";
import { getPersonalAttendanceSummary } from "@/api/report";
import type { Attendance, Notice } from "@/types/api";

interface StatCard {
  label: string;
  value: number;
  note: string;
  icon: Component;
  color: string;
  bgColor: string;
}

const router = useRouter();
const userStore = useUserStore();

const dashboardData = reactive({ clockedIn: 0, leaveTotal: 0, unreadMessage: 0, todaySchedule: 0 });
const todayAtt = ref<Attendance | null>(null);
const monthStats = reactive({ normalDays: 0, lateDays: 0, earlyLeaveDays: 0, absentDays: 0 });
const noticeList = ref<Notice[]>([]);
const noticeDialogVisible = ref(false);
const currentNotice = ref<Notice | null>(null);

const weekNames = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return "早上好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const currentDate = computed(() => `${dayjs().format("YYYY年MM月DD日")} ${weekNames[new Date().getDay()]}`);

const statusText = computed(() => {
  const status = todayAtt.value?.status;
  if (status === 0) return "正常";
  if (status === 1) return "迟到";
  if (status === 2) return "早退";
  if (status === 3) return "缺勤";
  if (status === 4) return "请假";
  return "未打卡";
});

const statusTagType = computed(() => {
  const status = todayAtt.value?.status;
  if (status === 0) return "success";
  if (status === 1) return "warning";
  if (status === 2 || status === 3) return "danger";
  return "info";
});

const statsCards = computed<StatCard[]>(() => [
  {
    label: "今日出勤",
    value: dashboardData.clockedIn,
    note: dashboardData.clockedIn ? "已完成上班打卡" : "等待打卡记录",
    icon: CircleCheck,
    color: "#2563eb",
    bgColor: "#eff6ff"
  },
  {
    label: "待审请假",
    value: dashboardData.leaveTotal,
    note: "当前待处理申请",
    icon: Document,
    color: "#d97706",
    bgColor: "#fff7ed"
  },
  {
    label: "未读消息",
    value: dashboardData.unreadMessage,
    note: "消息中心未读",
    icon: Message,
    color: "#dc2626",
    bgColor: "#fef2f2"
  },
  {
    label: "今日日程",
    value: dashboardData.todaySchedule,
    note: "当天计划事项",
    icon: Clock,
    color: "#7c3aed",
    bgColor: "#f5f3ff"
  }
]);

const quickEntries = [
  { label: "请假申请", icon: DocumentAdd, color: "#2563eb", path: "/oa/leave/apply" },
  { label: "公告通知", icon: Bell, color: "#d97706", path: "/oa/notice/list" },
  { label: "消息中心", icon: ChatDotRound, color: "#7c3aed", path: "/oa/message/list" },
  { label: "我的日程", icon: Calendar, color: "#059669", path: "/oa/schedule/index" }
];

const monthCards = computed(() => [
  { label: "正常出勤", value: monthStats.normalDays, note: "按天统计", color: "#2563eb" },
  { label: "迟到", value: monthStats.lateDays, note: "异常次数", color: "#d97706" },
  { label: "早退", value: monthStats.earlyLeaveDays, note: "异常次数", color: "#dc2626" },
  { label: "缺勤", value: monthStats.absentDays, note: "异常天数", color: "#7c3aed" }
]);

function formatClock(value?: string) {
  return value ? value.substring(11, 16) : "--:--";
}

function handleViewNotice(item: Notice) {
  currentNotice.value = item;
  noticeDialogVisible.value = true;
}

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
      dashboardData.todaySchedule = res.data.list.filter((item) => (item.startTime || "").startsWith(today)).length;
    }
  } catch {}

  try {
    const res = await getPersonalAttendanceSummary(dayjs().format("YYYY-MM"));
    if (res.data) {
      monthStats.normalDays = Number(res.data.normalDays) || 0;
      monthStats.lateDays = Number(res.data.lateDays) || 0;
      monthStats.earlyLeaveDays = Number(res.data.earlyLeaveDays) || 0;
      monthStats.absentDays = Number(res.data.absentDays) || 0;
    }
  } catch {}

  try {
    const res = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (res.data?.list) noticeList.value = res.data.list;
  } catch {}
});
</script>

<style scoped>
.month-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.notice-item {
  cursor: pointer;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 650;
}

.notice-time {
  margin-top: 4px;
  color: var(--oa-subtle);
  font-size: 12px;
}

.notice-content {
  color: #374151;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .month-grid {
    grid-template-columns: 1fr;
  }
}
</style>
