<template>
  <section class="oa-analytics-page">
    <header class="oa-page-header">
      <div>
        <div class="oa-eyebrow">Workbench</div>
        <h1 class="oa-page-title">{{ greeting }}，{{ userStore.userInfo?.empName || "用户" }}</h1>
        <p class="oa-page-subtitle">{{ currentDate }}</p>
      </div>
      <div class="oa-header-actions">
        <div class="work-clock">
          <strong>{{ currentTime }}</strong>
          <span>{{ currentWeekday }}</span>
        </div>
        <el-button type="primary" :icon="DocumentAdd" @click="router.push('/oa/leave/apply')">
          请假申请
        </el-button>
        <el-button :icon="Calendar" @click="router.push('/oa/schedule/index')">
          我的日程
        </el-button>
        <el-button :icon="ChatDotRound" @click="router.push('/oa/message/list')">
          消息中心
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
            <p class="oa-panel-subtitle">上下班打卡和工时状态</p>
          </div>
          <el-tag :type="statusTagType" effect="light">{{ statusLabel }}</el-tag>
        </div>

        <div v-if="todayAtt" class="attendance-grid">
          <div class="oa-kpi-box">
            <div class="oa-kpi-label">上班打卡</div>
            <template v-if="todayAtt.clockIn">
              <div class="oa-kpi-value success-text">
                <el-icon><CircleCheck /></el-icon>
                {{ formatClock(todayAtt.clockIn) }}
              </div>
              <div class="oa-kpi-note">已完成上班打卡</div>
            </template>
            <template v-else>
              <el-button type="primary" :loading="clockingIn" @click="handleClockIn">上班打卡</el-button>
            </template>
          </div>

          <div class="oa-kpi-box">
            <div class="oa-kpi-label">下班打卡</div>
            <template v-if="todayAtt.clockOut">
              <div class="oa-kpi-value success-text">
                <el-icon><CircleCheck /></el-icon>
                {{ formatClock(todayAtt.clockOut) }}
              </div>
              <div class="oa-kpi-note">已完成下班打卡</div>
            </template>
            <template v-else>
              <el-button type="warning" :loading="clockingOut" @click="handleClockOut">下班打卡</el-button>
            </template>
          </div>
        </div>

        <div v-if="todayAtt?.clockIn && todayAtt?.clockOut" class="work-hours">
          工作时长：{{ calcWorkHours(todayAtt.clockIn, todayAtt.clockOut) }}
        </div>

        <el-empty v-if="!todayAtt" description="今日暂无考勤记录" :image-size="64">
          <el-button type="primary" :loading="clockingIn" @click="handleClockIn">立即打卡</el-button>
        </el-empty>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">本月出勤概览</h2>
            <p class="oa-panel-subtitle">正常、迟到、早退、缺勤占比</p>
          </div>
          <el-button type="primary" link @click="router.push('/oa/report/personal')">查看报表</el-button>
        </div>
        <div class="oa-micro-grid">
          <div v-for="item in monthCards" :key="item.label" class="oa-kpi-box">
            <div class="oa-kpi-label">{{ item.label }}</div>
            <div class="oa-kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
            <div class="oa-kpi-note">{{ item.note }}</div>
          </div>
        </div>
        <div class="attendance-rate">
          <div>
            <span>出勤率</span>
            <strong>{{ attendanceRate }}%</strong>
          </div>
          <el-progress
            :percentage="attendanceRate"
            :stroke-width="10"
            :show-text="false"
            :color="attendanceRate >= 90 ? '#059669' : attendanceRate >= 70 ? '#d97706' : '#dc2626'" />
        </div>
      </article>

      <article class="oa-panel oa-col-12">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">快捷入口</h2>
            <p class="oa-panel-subtitle">覆盖考勤、请假、文档、报表与消息</p>
          </div>
        </div>
        <div class="oa-action-grid quick-grid">
          <button
            v-for="item in quickEntries"
            :key="item.label"
            type="button"
            class="oa-action-tile"
            @click="router.push(item.path)"
          >
            <span class="quick-icon" :style="{ color: item.color, backgroundColor: item.bgColor }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </span>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">最新公告</h2>
            <p class="oa-panel-subtitle">组织公告与通知</p>
          </div>
          <el-button type="primary" link @click="router.push('/oa/notice/list')">更多</el-button>
        </div>
        <div v-if="noticeList.length">
          <div v-for="item in noticeList" :key="item.id" class="oa-list-item list-link" @click="handleViewNotice(item)">
            <div>
              <div class="list-title">
                <el-tag v-if="item.noticeType === 1" type="danger" size="small">公告</el-tag>
                <el-tag v-else type="info" size="small">通知</el-tag>
                {{ item.title }}
              </div>
              <div class="list-time">{{ item.createTime }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无公告" :image-size="64" />
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">今日日程</h2>
            <p class="oa-panel-subtitle">当天待办安排</p>
          </div>
          <el-button type="primary" link @click="router.push('/oa/schedule/index')">更多</el-button>
        </div>
        <div v-if="scheduleList.length">
          <div v-for="item in scheduleList" :key="item.id" class="oa-list-item">
            <div>
              <div class="list-title">
                <el-icon color="#2563eb"><Clock /></el-icon>
                {{ item.title }}
              </div>
              <div class="list-time">
                {{ formatClock(item.startTime) }} - {{ formatClock(item.endTime) }}
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="今日无日程" :image-size="64" />
      </article>
    </div>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title" width="600px">
      <div class="list-time">{{ currentNotice?.createTime }}</div>
      <el-divider />
      <div class="notice-content">{{ currentNotice?.content }}</div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import type { Component } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import { ElMessage } from "element-plus";
import {
  Bell,
  Calendar,
  ChatDotRound,
  CircleCheck,
  Clock,
  DataAnalysis,
  Document,
  DocumentAdd,
  Folder,
  Message,
  Sunny,
  TrendCharts
} from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { getTodayAttendance, clockIn, clockOut } from "@/api/attendance";
import { getNoticePage } from "@/api/notice";
import { getLeavePage } from "@/api/leave";
import { getUnreadCount } from "@/api/message";
import { getSchedulePage } from "@/api/schedule";
import { getPersonalAttendanceSummary } from "@/api/report";

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
const currentTime = ref(dayjs().format("HH:mm:ss"));
const weekNames = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];
let timer = 0;

const todayAtt = ref<any>(null);
const clockingIn = ref(false);
const clockingOut = ref(false);
const monthStats = reactive({ normalDays: 0, lateDays: 0, earlyDays: 0, absentDays: 0 });
const noticeList = ref<any[]>([]);
const scheduleList = ref<any[]>([]);
const noticeDialogVisible = ref(false);
const currentNotice = ref<any>(null);

const currentDate = computed(() => `${dayjs().format("YYYY年MM月DD日")} ${currentWeekday.value}`);
const currentWeekday = computed(() => weekNames[new Date().getDay()]);

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 12) return "早上好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

const attendanceRate = computed(() => {
  const total = monthStats.normalDays + monthStats.lateDays + monthStats.earlyDays + monthStats.absentDays;
  if (total === 0) return 0;
  return Math.round((monthStats.normalDays / total) * 100);
});

const statsCards = reactive<StatCard[]>([
  { label: "今日出勤", value: 0, note: "上班打卡状态", icon: CircleCheck, color: "#2563eb", bgColor: "#eff6ff" },
  { label: "待审请假", value: 0, note: "待审批申请", icon: Document, color: "#d97706", bgColor: "#fff7ed" },
  { label: "未读消息", value: 0, note: "消息中心未读", icon: Message, color: "#dc2626", bgColor: "#fef2f2" },
  { label: "今日日程", value: 0, note: "当天日程数量", icon: Clock, color: "#7c3aed", bgColor: "#f5f3ff" }
]);

const quickEntries = [
  { label: "考勤打卡", icon: Sunny, color: "#d97706", bgColor: "#fff7ed", path: "/oa/attendance/clock" },
  { label: "请假申请", icon: DocumentAdd, color: "#2563eb", bgColor: "#eff6ff", path: "/oa/leave/apply" },
  { label: "公告通知", icon: Bell, color: "#dc2626", bgColor: "#fef2f2", path: "/oa/notice/list" },
  { label: "文档中心", icon: Folder, color: "#059669", bgColor: "#ecfdf5", path: "/oa/document/list" },
  { label: "消息中心", icon: ChatDotRound, color: "#7c3aed", bgColor: "#f5f3ff", path: "/oa/message/list" },
  { label: "我的日程", icon: Calendar, color: "#0891b2", bgColor: "#ecfeff", path: "/oa/schedule/index" },
  { label: "个人报表", icon: DataAnalysis, color: "#d97706", bgColor: "#fff7ed", path: "/oa/report/personal" },
  { label: "数据看板", icon: TrendCharts, color: "#dc2626", bgColor: "#fef2f2", path: "/oa/dashboard" }
];

const monthCards = computed(() => [
  { label: "正常", value: monthStats.normalDays, note: "正常出勤", color: "#2563eb" },
  { label: "迟到", value: monthStats.lateDays, note: "迟到次数", color: "#d97706" },
  { label: "早退", value: monthStats.earlyDays, note: "早退次数", color: "#dc2626" },
  { label: "缺勤", value: monthStats.absentDays, note: "缺勤天数", color: "#7c3aed" }
]);

const statusLabel = computed(() => {
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

function formatClock(value?: string) {
  return value ? value.substring(11, 16) : "--:--";
}

function calcWorkHours(clockInValue?: string, clockOutValue?: string) {
  if (!clockInValue || !clockOutValue) return "-";
  const diff = new Date(clockOutValue).getTime() - new Date(clockInValue).getTime();
  return `${(diff / (1000 * 60 * 60)).toFixed(1)} 小时`;
}

async function handleClockIn() {
  clockingIn.value = true;
  try {
    await clockIn();
    ElMessage.success("上班打卡成功");
    await fetchToday();
    await fetchStats();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clockingIn.value = false;
  }
}

async function handleClockOut() {
  clockingOut.value = true;
  try {
    await clockOut();
    ElMessage.success("下班打卡成功");
    await fetchToday();
    await fetchStats();
  } catch (error: any) {
    ElMessage.error(error.message || "打卡失败");
  } finally {
    clockingOut.value = false;
  }
}

function handleViewNotice(item: any) {
  currentNotice.value = item;
  noticeDialogVisible.value = true;
}

async function fetchToday() {
  try {
    const res: any = await getTodayAttendance();
    if (res.data) todayAtt.value = res.data;
  } catch {}
}

async function fetchStats() {
  try {
    const res: any = await getUnreadCount();
    if (res.data !== undefined) statsCards[2].value = Number(res.data) || 0;
  } catch {}

  try {
    const today = dayjs().format("YYYY-MM-DD");
    const res: any = await getSchedulePage({ pageNum: 1, pageSize: 100 });
    if (res.data?.list) {
      const todayItems = res.data.list.filter((item: any) => (item.startTime || "").startsWith(today));
      statsCards[3].value = todayItems.length;
    }
  } catch {}

  try {
    const res: any = await getLeavePage({ pageNum: 1, pageSize: 1, status: 0 });
    if (res.data?.total !== undefined) statsCards[1].value = Number(res.data.total) || 0;
  } catch {}

  try {
    const res: any = await getTodayAttendance();
    if (res.data) statsCards[0].value = res.data.clockIn ? 1 : 0;
  } catch {}
}

async function fetchMonthStats() {
  try {
    const res: any = await getPersonalAttendanceSummary(dayjs().format("YYYY-MM"));
    if (res.data) {
      monthStats.normalDays = Number(res.data.normalDays) || 0;
      monthStats.lateDays = Number(res.data.lateDays) || 0;
      monthStats.earlyDays = Number(res.data.earlyLeaveDays) || 0;
      monthStats.absentDays = Number(res.data.absentDays) || 0;
    }
  } catch {}
}

async function fetchNotice() {
  try {
    const res: any = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (res.data?.list) noticeList.value = res.data.list;
  } catch {}
}

async function fetchSchedule() {
  try {
    const today = dayjs().format("YYYY-MM-DD");
    const res: any = await getSchedulePage({ pageNum: 1, pageSize: 10 });
    if (res.data?.list) {
      scheduleList.value = res.data.list.filter((item: any) => (item.startTime || "").startsWith(today));
    }
  } catch {}
}

onMounted(() => {
  fetchToday();
  fetchStats();
  fetchMonthStats();
  fetchNotice();
  fetchSchedule();
  timer = window.setInterval(() => {
    currentTime.value = dayjs().format("HH:mm:ss");
  }, 1000);
});

onUnmounted(() => clearInterval(timer));
</script>

<style scoped>
.work-clock {
  display: grid;
  gap: 2px;
  min-width: 108px;
  padding: 8px 12px;
  border: 1px solid var(--oa-border-soft);
  border-radius: 8px;
  background: var(--oa-surface-soft);
  text-align: right;
}

.work-clock strong {
  color: var(--oa-text);
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.work-clock span {
  color: var(--oa-muted);
  font-size: 12px;
}

.attendance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.success-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--oa-success);
}

.work-hours {
  margin-top: 12px;
  color: var(--oa-muted);
  font-size: 13px;
  text-align: center;
}

.attendance-rate {
  margin-top: 16px;
}

.attendance-rate > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--oa-muted);
  font-size: 13px;
}

.attendance-rate strong {
  color: var(--oa-text);
  font-variant-numeric: tabular-nums;
}

.quick-grid {
  grid-template-columns: repeat(8, minmax(0, 1fr));
}

.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 8px;
}

.list-link {
  cursor: pointer;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--oa-text);
  font-size: 14px;
  font-weight: 650;
}

.list-time {
  margin-top: 4px;
  color: var(--oa-subtle);
  font-size: 12px;
}

.notice-content {
  color: var(--oa-text-soft);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

@media (max-width: 1280px) {
  .quick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .work-clock {
    text-align: left;
  }

  .attendance-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
