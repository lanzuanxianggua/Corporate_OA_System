<template>
  <section class="oa-analytics-page data-board-page">
    <header class="oa-page-header board-header">
      <div>
        <div class="oa-eyebrow">OA Operations</div>
        <h1 class="oa-page-title">数据看板</h1>
        <p class="oa-page-subtitle">从出勤、审批、组织、请假和出差维度观察当前运营状态</p>
      </div>
      <div class="oa-header-actions">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          :clearable="false"
          :editable="false"
          :prefix-icon="Calendar"
          @change="fetchDashboardData" />
        <el-button :icon="Refresh" circle :loading="loading" @click="fetchDashboardData" />
      </div>
    </header>

    <div v-loading="loading" element-loading-text="正在加载看板数据...">
      <div class="oa-stat-grid board-stat-grid">
        <article v-for="item in metricCards" :key="item.label" class="oa-stat-card board-stat-card">
          <div class="oa-stat-icon" :style="{ color: item.color, backgroundColor: item.bgColor }">
            <el-icon :size="22"><component :is="item.icon" /></el-icon>
          </div>
          <div class="min-w-0">
            <div class="oa-stat-label">{{ item.label }}</div>
            <div class="oa-stat-value">{{ item.value }}</div>
            <div class="oa-stat-note">{{ item.note }}</div>
          </div>
        </article>
      </div>

      <div class="board-insight-grid">
        <article v-for="item in riskIndicators" :key="item.label" class="board-insight" :class="`is-${item.level}`">
          <div class="insight-topline">
            <span class="insight-dot"></span>
            <span>{{ item.label }}</span>
          </div>
          <strong>{{ item.value }}</strong>
          <p>{{ item.note }}</p>
        </article>
      </div>

      <div class="oa-grid">
        <article class="oa-panel oa-col-12">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">日活跃员工与有效工时趋势</h2>
              <p class="oa-panel-subtitle">近 30 天活跃员工、有效工时与审批处理量</p>
            </div>
            <el-tag effect="light" type="success">30 天</el-tag>
          </div>
          <div ref="dailyTrendChartRef" class="oa-chart board-chart-large"></div>
        </article>

        <article class="oa-panel oa-col-12">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">办公活跃时段热力图</h2>
              <p class="oa-panel-subtitle">签到、签退与审批处理在 7×24 小时中的分布</p>
            </div>
          </div>
          <div ref="officeHeatmapChartRef" class="oa-chart board-chart-large board-heatmap-chart"></div>
        </article>

        <article class="oa-panel oa-col-8">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">审批业务分布</h2>
              <p class="oa-panel-subtitle">按业务类型拆分审批中、已通过、已拒绝和已撤回</p>
            </div>
          </div>
          <div ref="approvalBusinessChartRef" class="oa-chart board-chart-large"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">近 6 月运营趋势</h2>
              <p class="oa-panel-subtitle">审批发起、审批通过、请假与出差申请的月度变化</p>
            </div>
          </div>
          <div ref="monthlyTrendChartRef" class="oa-chart board-chart-large"></div>
        </article>

        <article class="oa-panel oa-col-5">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}出勤健康度</h2>
              <p class="oa-panel-subtitle">当前日期范围内的出勤完成情况</p>
            </div>
          </div>
          <div class="health-layout">
            <div ref="attendanceGaugeRef" class="oa-chart gauge-chart"></div>
            <div class="oa-signal-list">
              <div v-for="item in attendanceSignals" :key="item.label" class="oa-signal-item">
                <span class="oa-signal-dot" :style="{ backgroundColor: item.color }"></span>
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>
        </article>

        <article class="oa-panel oa-col-7">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}异常雷达</h2>
              <p class="oa-panel-subtitle">迟到、早退、缺勤、请假待审和审批积压</p>
            </div>
          </div>
          <div ref="exceptionRadarChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">审批状态结构</h2>
              <p class="oa-panel-subtitle">审批中、通过、拒绝与撤回占比</p>
            </div>
          </div>
          <div ref="approvalStatusChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">考勤状态结构</h2>
              <p class="oa-panel-subtitle">正常、迟到、早退、缺勤、请假和出差分布</p>
            </div>
          </div>
          <div ref="attendanceStatusChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}请假类型</h2>
              <p class="oa-panel-subtitle">不同请假类型的占比结构</p>
            </div>
          </div>
          <div ref="leaveTypeChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-7">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">部门负载矩阵</h2>
              <p class="oa-panel-subtitle">横轴为部门人数，纵轴为请假与出差负载</p>
            </div>
          </div>
          <div ref="departmentWorkloadChartRef" class="oa-chart board-chart-large"></div>
        </article>

        <article class="oa-panel oa-col-5">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">部门人力结构</h2>
              <p class="oa-panel-subtitle">组织规模分布与资源集中度</p>
            </div>
          </div>
          <div ref="deptChartRef" class="oa-chart board-chart-large"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">出勤率 Top10</h2>
              <p class="oa-panel-subtitle">正常出勤率最高的员工</p>
            </div>
          </div>
          <div ref="attendanceRankChartRef" class="oa-chart rank-chart"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">缺勤 Top10</h2>
              <p class="oa-panel-subtitle">应出勤但未形成有效出勤记录</p>
            </div>
          </div>
          <div ref="absenceRankChartRef" class="oa-chart rank-chart"></div>
        </article>

        <article class="oa-panel oa-col-4">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">迟到 Top10</h2>
              <p class="oa-panel-subtitle">迟到次数最高的员工</p>
            </div>
          </div>
          <div ref="lateRankChartRef" class="oa-chart rank-chart"></div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import type { Component } from "vue";
import * as echarts from "@/utils/echarts";
import {
  Calendar,
  CircleCheck,
  DataAnalysis,
  OfficeBuilding,
  Refresh,
  Timer,
  TrendCharts,
  WarningFilled
} from "@element-plus/icons-vue";
import {
  axisStyle,
  axisTooltip,
  chartBorderColor,
  chartGrid,
  chartMutedColor,
  chartPalette,
  chartTextColor,
  chartTextStyle,
  createGradient,
  emptyChartOption,
  itemTooltip
} from "@/utils/chartTheme";
import { getDashboardStats } from "@/api/statistics";

interface AttendanceStats {
  clockedIn?: number;
  late?: number;
  earlyLeave?: number;
  absent?: number;
  totalRequired?: number;
}

interface AttendanceRankingItem {
  empName?: string;
  rate?: number;
  normalDays?: number;
  totalDays?: number;
}

interface LateRankingItem {
  empName?: string;
  lateCount?: number;
}

interface AbsenceRankingItem {
  empName?: string;
  absentCount?: number;
  requiredDays?: number;
  clockedDays?: number;
  approvedLeaveDays?: number;
}

interface NameValueItem {
  name?: string;
  value?: number;
  status?: string | number;
}

interface DailyTrendItem {
  date?: string;
  clockedIn?: number;
  late?: number;
  earlyLeave?: number;
  leave?: number;
  absent?: number;
  attendanceRate?: number;
}

interface OfficeActivityTrendItem {
  date?: string;
  fullDate?: string;
  activeEmployees?: number;
  workHours?: number;
  approvalActions?: number;
}

interface OfficeActivityHeatmapItem {
  weekday?: number;
  hour?: number;
  events?: number;
  clockInEvents?: number;
  clockOutEvents?: number;
  approvalEvents?: number;
}

interface MonthlyTrendItem {
  month?: string;
  leave?: number;
  trip?: number;
  submitted?: number;
  approved?: number;
  rejected?: number;
}

interface ApprovalBusinessItem {
  name?: string;
  total?: number;
  pending?: number;
  approved?: number;
  rejected?: number;
  canceled?: number;
}

interface DepartmentWorkloadItem {
  name?: string;
  employeeCount?: number;
  clockedIn?: number;
  leaveCount?: number;
  tripCount?: number;
  load?: number;
  attendanceRate?: number;
}

interface RiskIndicator {
  label: string;
  value: string;
  level: "good" | "warning" | "danger";
  note: string;
}

interface DashboardData {
  employeeTotal?: number;
  attendance?: AttendanceStats;
  leave?: {
    total?: number;
    pending?: number;
    approved?: number;
    byType?: Record<string, number>;
  };
  departmentDistribution?: Array<{ name?: string; value?: number; count?: number }>;
  pendingApprovals?: number;
  leaveCountThisMonth?: number;
  businessTripCountThisMonth?: number;
  newEmployeesThisMonth?: number;
  lateRanking?: LateRankingItem[];
  attendanceRanking?: AttendanceRankingItem[];
  absenceRanking?: AbsenceRankingItem[];
  attendanceTrendDetailed?: DailyTrendItem[];
  officeActivityTrend?: OfficeActivityTrendItem[];
  officeActivityHeatmap?: OfficeActivityHeatmapItem[];
  attendanceStatusDistribution?: NameValueItem[];
  approvalFunnel?: NameValueItem[];
  approvalStatusDistribution?: NameValueItem[];
  approvalBusinessDistribution?: ApprovalBusinessItem[];
  monthlyOperationTrend?: MonthlyTrendItem[];
  departmentWorkload?: DepartmentWorkloadItem[];
  riskIndicators?: RiskIndicator[];
}

interface MetricCard {
  label: string;
  value: string;
  note: string;
  color: string;
  bgColor: string;
  icon: Component;
}

interface ChartItem {
  name: string;
  value: number;
  extra?: string;
}

const loading = ref(false);
const dashboardData = ref<DashboardData>({});
const selectedDate = ref(formatDate(new Date()));

const dailyTrendChartRef = ref<HTMLDivElement>();
const officeHeatmapChartRef = ref<HTMLDivElement>();
const approvalFunnelChartRef = ref<HTMLDivElement>();
const attendanceGaugeRef = ref<HTMLDivElement>();
const monthlyTrendChartRef = ref<HTMLDivElement>();
const approvalStatusChartRef = ref<HTMLDivElement>();
const attendanceStatusChartRef = ref<HTMLDivElement>();
const exceptionRadarChartRef = ref<HTMLDivElement>();
const approvalBusinessChartRef = ref<HTMLDivElement>();
const departmentWorkloadChartRef = ref<HTMLDivElement>();
const attendanceRankChartRef = ref<HTMLDivElement>();
const absenceRankChartRef = ref<HTMLDivElement>();
const lateRankChartRef = ref<HTMLDivElement>();
const leaveTypeChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];

const dateScopeLabel = computed(() => (selectedDate.value === formatDate(new Date()) ? "今日" : "所选日"));
const attendance = computed(() => dashboardData.value.attendance || {});
const employeeTotal = computed(() => Number(dashboardData.value.employeeTotal) || 0);
const totalRequired = computed(() => Number(attendance.value.totalRequired) || employeeTotal.value);
const clockedIn = computed(() => Number(attendance.value.clockedIn) || 0);
const lateToday = computed(() => Number(attendance.value.late) || 0);
const earlyLeaveToday = computed(() => Number(attendance.value.earlyLeave) || 0);
const absentToday = computed(() => Number(attendance.value.absent) || 0);
const onLeaveToday = computed(() => Number(dashboardData.value.leave?.total) || 0);
const pendingApprovals = computed(() => Number(dashboardData.value.pendingApprovals) || 0);
const notClockedToday = computed(() => Math.max(0, totalRequired.value - clockedIn.value - onLeaveToday.value - absentToday.value));
const attendanceRate = computed(() => {
  if (!totalRequired.value) return 0;
  return Math.min(100, Math.round((clockedIn.value / totalRequired.value) * 100));
});
const absenceRate = computed(() => {
  if (!totalRequired.value) return 0;
  return Math.round((absentToday.value / totalRequired.value) * 100);
});
const exceptionTotal = computed(() => lateToday.value + earlyLeaveToday.value + absentToday.value);

const approvalSummary = computed(() => {
  const status = dashboardData.value.approvalStatusDistribution || [];
  const valueByStatus = (code: string) => Number(status.find((item) => String(item.status) === code)?.value) || 0;
  const pending = valueByStatus("0");
  const approved = valueByStatus("1");
  const rejected = valueByStatus("2");
  const canceled = valueByStatus("3");
  const completed = approved + rejected + canceled;
  return {
    pending,
    approved,
    rejected,
    canceled,
    completed,
    total: pending + completed,
    passRate: completed > 0 ? Math.round((approved / completed) * 100) : 0
  };
});

const attendanceTagType = computed(() => {
  if (attendanceRate.value >= 90) return "success";
  if (attendanceRate.value >= 75) return "warning";
  return "danger";
});

const metricCards = computed<MetricCard[]>(() => [
  {
    label: "在职员工",
    value: formatNumber(employeeTotal.value),
    note: "当前启用账号人数",
    color: "#2563eb",
    bgColor: "#eff6ff",
    icon: OfficeBuilding
  },
  {
    label: `${dateScopeLabel.value}出勤率`,
    value: `${attendanceRate.value}%`,
    note: `${formatNumber(clockedIn.value)} / ${formatNumber(totalRequired.value)} 已打卡`,
    color: "#14b8a6",
    bgColor: "#f0fdfa",
    icon: CircleCheck
  },
  {
    label: `${dateScopeLabel.value}缺勤`,
    value: formatNumber(absentToday.value),
    note: `缺勤率 ${absenceRate.value}%`,
    color: "#ef4444",
    bgColor: "#fef2f2",
    icon: WarningFilled
  },
  {
    label: `${dateScopeLabel.value}异常`,
    value: formatNumber(exceptionTotal.value),
    note: `${formatNumber(lateToday.value)} 迟到 / ${formatNumber(earlyLeaveToday.value)} 早退`,
    color: "#f97316",
    bgColor: "#fff7ed",
    icon: DataAnalysis
  },
  {
    label: "审批积压",
    value: formatNumber(pendingApprovals.value),
    note: "来自 wf_task 待办任务",
    color: "#db2777",
    bgColor: "#fdf2f8",
    icon: Timer
  },
  {
    label: "审批通过率",
    value: `${approvalSummary.value.passRate}%`,
    note: `已完结 ${formatNumber(approvalSummary.value.completed)} 条`,
    color: "#16a34a",
    bgColor: "#f0fdf4",
    icon: TrendCharts
  },
  {
    label: "本月请假",
    value: formatNumber(Number(dashboardData.value.leaveCountThisMonth) || 0),
    note: `待确认 ${formatNumber(Number(dashboardData.value.leave?.pending) || 0)} 条`,
    color: "#7c3aed",
    bgColor: "#f5f3ff",
    icon: Timer
  },
  {
    label: "本月流动",
    value: formatNumber(Number(dashboardData.value.businessTripCountThisMonth) || 0),
    note: `新增员工 ${formatNumber(Number(dashboardData.value.newEmployeesThisMonth) || 0)}`,
    color: "#0891b2",
    bgColor: "#ecfeff",
    icon: TrendCharts
  }
]);

const riskIndicators = computed<RiskIndicator[]>(() => {
  const items = dashboardData.value.riskIndicators || [];
  if (items.length) return items;
  return [
    { label: "出勤健康", value: `${attendanceRate.value}%`, level: attendanceRate.value >= 90 ? "good" : "warning", note: "等待接口返回完整风险指标" },
    { label: "审批积压", value: `${pendingApprovals.value} 条`, level: pendingApprovals.value > 50 ? "danger" : "good", note: "当前待办任务数量" }
  ];
});

const attendanceSignals = computed(() => [
  { label: "已打卡", value: formatNumber(clockedIn.value), color: "#14b8a6" },
  { label: "待打卡", value: formatNumber(notClockedToday.value), color: "#64748b" },
  { label: "迟到", value: formatNumber(lateToday.value), color: "#f97316" },
  { label: "早退", value: formatNumber(earlyLeaveToday.value), color: "#ef4444" },
  { label: "缺勤", value: formatNumber(absentToday.value), color: "#db2777" },
  { label: "请假", value: formatNumber(onLeaveToday.value), color: "#7c3aed" }
]);

function formatNumber(value: number): string {
  return new Intl.NumberFormat("zh-CN").format(value);
}

function formatDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function clearCharts() {
  charts.forEach((chart) => chart.dispose());
  charts.length = 0;
}

async function fetchDashboardData() {
  loading.value = true;
  clearCharts();
  try {
    const response = await getDashboardStats("today", undefined, selectedDate.value);
    dashboardData.value = (response.data || {}) as DashboardData;
  } catch {
    dashboardData.value = {};
  } finally {
    loading.value = false;
  }

  await nextTick();
  initCharts();
}

function initCharts() {
  initDailyTrendChart();
  initOfficeHeatmapChart();
  initApprovalFunnelChart();
  initAttendanceGauge();
  initMonthlyTrendChart();
  initApprovalStatusChart();
  initAttendanceStatusChart();
  initExceptionRadarChart();
  initApprovalBusinessChart();
  initDepartmentWorkloadChart();
  initAttendanceRankChart();
  initAbsenceRankChart();
  initLateRankChart();
  initLeaveTypeChart();
  initDeptChart();
}

function createChart(element: HTMLDivElement | undefined) {
  if (!element) return undefined;
  const chart = echarts.init(element);
  charts.push(chart);
  return chart;
}

function initDailyTrendChart() {
  const chart = createChart(dailyTrendChartRef.value);
  if (!chart) return;
  const data = dashboardData.value.officeActivityTrend || [];
  if (!data.length) {
    chart.setOption(emptyChartOption("暂无办公活跃趋势"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: ["#2563eb", "#14b8a6", "#f97316"],
    tooltip: axisTooltip(),
    legend: { top: 0, right: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: chartMutedColor() } },
    grid: { top: 42, right: 52, bottom: 32, left: 42, containLabel: true },
    xAxis: { type: "category", data: data.map((item) => item.date || ""), ...axisStyle() },
    yAxis: [
      { type: "value", name: "人数/次数", minInterval: 1, ...axisStyle() },
      { type: "value", name: "工时(h)", nameGap: 10, axisLabel: { formatter: "{value}h", color: chartMutedColor() }, splitLine: { show: false } }
    ],
    series: [
      {
        name: "活跃员工",
        type: "line",
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 3 },
        areaStyle: { color: createGradient("rgba(37, 99, 235, 0.22)", "rgba(37, 99, 235, 0.02)") },
        data: data.map((item) => Number(item.activeEmployees) || 0)
      },
      {
        name: "有效工时",
        type: "line",
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 3 },
        areaStyle: { color: createGradient("rgba(20, 184, 166, 0.18)", "rgba(20, 184, 166, 0.02)") },
        data: data.map((item) => Number(item.workHours) || 0)
      },
      {
        name: "审批处理",
        type: "bar",
        barWidth: 14,
        data: data.map((item) => Number(item.approvalActions) || 0),
        itemStyle: { borderRadius: [6, 6, 0, 0], color: createGradient("#f97316", "#facc15") }
      }
    ]
  });
}

function heatmapCellPalette() {
  const isDark = typeof document !== "undefined" && document.documentElement.classList.contains("dark");
  return isDark
    ? ["#24182f", "#39204f", "#562b78", "#8140b8", "#c084fc"]
    : ["#fbf7ff", "#ead8ff", "#d1aaff", "#9f5ee5", "#581c87"];
}

function heatmapGridGapColor() {
  const isDark = typeof document !== "undefined" && document.documentElement.classList.contains("dark");
  return isDark ? "#171b24" : "#f8fafc";
}

function heatmapColorCap(values: number[]) {
  const positiveValues = values.filter((value) => value > 0).sort((a, b) => a - b);
  if (!positiveValues.length) return 1;
  const p88Index = Math.max(0, Math.ceil(positiveValues.length * 0.88) - 1);
  const p88 = positiveValues[p88Index] || positiveValues[positiveValues.length - 1] || 1;
  return Math.max(6, p88);
}

function initOfficeHeatmapChart() {
  const chart = createChart(officeHeatmapChartRef.value);
  if (!chart) return;
  const weekdays = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];
  const hours = Array.from({ length: 24 }, (_, index) => `${index}:00`);
  const source = dashboardData.value.officeActivityHeatmap || [];
  const eventValues = source.map((item) => Number(item.events) || 0);
  const colorCap = heatmapColorCap(eventValues);
  const totalEvents = source.reduce((sum, item) => sum + (Number(item.events) || 0), 0);
  const sourceBySlot = new Map(source.map((item) => [`${Number(item.weekday) || 0}-${Number(item.hour) || 0}`, item]));
  const data = weekdays.flatMap((_, weekdayIndex) =>
    hours.map((_, hourIndex) => {
      const item = sourceBySlot.get(`${weekdayIndex}-${hourIndex}`);
      return [
        hourIndex,
        weekdayIndex,
        Number(item?.events) || 0,
        Number(item?.clockInEvents) || 0,
        Number(item?.clockOutEvents) || 0,
        Number(item?.approvalEvents) || 0
      ];
    })
  );

  if (!totalEvents) {
    chart.setOption(emptyChartOption("暂无办公时段数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: {
      ...itemTooltip(),
      formatter: (params: unknown) => {
        const item = params as { data: number[] };
        return `${weekdays[item.data[1]]} ${item.data[0]}:00<br/>总事件：${item.data[2]}<br/>签到：${item.data[3]} / 签退：${item.data[4]}<br/>审批：${item.data[5]}`;
      }
    },
    grid: { top: 18, right: 18, bottom: 30, left: 48, containLabel: true },
    xAxis: {
      type: "category",
      data: hours,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      splitArea: { show: false },
      axisPointer: { show: false },
      axisLabel: {
        color: chartMutedColor(),
        interval: (index: number) => index % 3 === 0,
        fontSize: 10,
        margin: 12
      }
    },
    yAxis: {
      type: "category",
      data: weekdays,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      splitArea: { show: false },
      axisPointer: { show: false },
      axisLabel: { color: chartTextColor(), fontWeight: 650 }
    },
    visualMap: {
      min: 0,
      max: colorCap,
      show: false,
      calculable: false,
      inRange: { color: heatmapCellPalette() }
    },
    series: [
      {
        name: "办公事件",
        type: "heatmap",
        data,
        label: { show: false },
        emphasis: {
          itemStyle: {
            borderColor: heatmapGridGapColor(),
            borderWidth: 2,
            borderRadius: 5,
            shadowBlur: 0
          }
        },
        itemStyle: {
          borderColor: heatmapGridGapColor(),
          borderWidth: 2,
          borderRadius: 5
        }
      }
    ]
  });
}

function initApprovalFunnelChart() {
  const chart = createChart(approvalFunnelChartRef.value);
  if (!chart) return;
  const data = (dashboardData.value.approvalFunnel || [])
    .map((item) => ({ name: item.name || "未知", value: Number(item.value) || 0 }))
    .filter((item) => item.value > 0);
  if (!data.length) {
    chart.setOption(emptyChartOption("暂无审批数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: ["#2563eb", "#14b8a6", "#16a34a"],
    tooltip: itemTooltip("{b}<br/>{c} 条"),
    series: [
      {
        type: "funnel",
        sort: "none",
        left: "8%",
        top: 18,
        width: "84%",
        height: "78%",
        gap: 3,
        minSize: "34%",
        maxSize: "100%",
        label: { color: "#fff", fontWeight: 700, formatter: "{b}\n{c}" },
        labelLine: { show: false },
        itemStyle: { borderWidth: 0, borderRadius: 6 },
        data
      }
    ]
  });
}

function initAttendanceGauge() {
  const chart = createChart(attendanceGaugeRef.value);
  if (!chart) return;
  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: itemTooltip(`${dateScopeLabel.value}出勤率：{c}%`),
    series: [
      {
        type: "gauge",
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        radius: "88%",
        progress: {
          show: true,
          width: 16,
          roundCap: true,
          itemStyle: { color: createGradient("#2563eb", "#14b8a6", true) }
        },
        axisLine: { lineStyle: { width: 16, color: [[1, chartBorderColor()]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: true,
          formatter: "{value}%",
          fontSize: 30,
          fontWeight: 760,
          color: chartTextColor(),
          offsetCenter: [0, "-4%"]
        },
        title: { offsetCenter: [0, "30%"], color: chartMutedColor(), fontSize: 13 },
        data: [{ value: attendanceRate.value, name: `${dateScopeLabel.value}出勤率` }]
      }
    ]
  });
}

function initMonthlyTrendChart() {
  const chart = createChart(monthlyTrendChartRef.value);
  if (!chart) return;
  const data = dashboardData.value.monthlyOperationTrend || [];
  if (!data.length) {
    chart.setOption(emptyChartOption("暂无月度数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: ["#2563eb", "#16a34a", "#7c3aed", "#f97316", "#ef4444"],
    tooltip: axisTooltip(),
    legend: { top: 0, right: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: chartMutedColor() } },
    grid: { top: 42, right: 22, bottom: 30, left: 42, containLabel: true },
    xAxis: { type: "category", data: data.map((item) => item.month || ""), ...axisStyle() },
    yAxis: { type: "value", minInterval: 1, ...axisStyle() },
    series: [
      { name: "审批发起", type: "bar", barWidth: 18, data: data.map((item) => Number(item.submitted) || 0), itemStyle: { borderRadius: [6, 6, 0, 0] } },
      { name: "审批通过", type: "line", smooth: true, symbolSize: 7, lineStyle: { width: 3 }, data: data.map((item) => Number(item.approved) || 0) },
      { name: "请假", type: "bar", barWidth: 18, data: data.map((item) => Number(item.leave) || 0), itemStyle: { borderRadius: [6, 6, 0, 0] } },
      { name: "出差", type: "bar", barWidth: 18, data: data.map((item) => Number(item.trip) || 0), itemStyle: { borderRadius: [6, 6, 0, 0] } },
      { name: "拒绝", type: "line", smooth: true, symbolSize: 7, lineStyle: { width: 2, type: "dashed" }, data: data.map((item) => Number(item.rejected) || 0) }
    ]
  });
}

function initApprovalStatusChart() {
  initDonutChart(approvalStatusChartRef.value, dashboardData.value.approvalStatusDistribution || [], {
    emptyText: "暂无审批状态",
    tooltip: "{b}<br/>{c} 条 ({d}%)",
    colors: ["#2563eb", "#16a34a", "#ef4444", "#64748b", "#f97316"]
  });
}

function initAttendanceStatusChart() {
  initDonutChart(attendanceStatusChartRef.value, dashboardData.value.attendanceStatusDistribution || [], {
    emptyText: "暂无考勤状态",
    tooltip: "{b}<br/>{c} 条 ({d}%)",
    colors: chartPalette
  });
}

function initDonutChart(
  element: HTMLDivElement | undefined,
  source: NameValueItem[],
  config: { emptyText: string; tooltip: string; colors: string[] }
) {
  const chart = createChart(element);
  if (!chart) return;
  const data = source
    .map((item) => ({ name: item.name || "未知", value: Number(item.value) || 0 }))
    .filter((item) => item.value > 0);
  if (!data.length) {
    chart.setOption(emptyChartOption(config.emptyText));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: config.colors,
    tooltip: itemTooltip(config.tooltip),
    legend: { bottom: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: chartMutedColor() } },
    series: [
      {
        type: "pie",
        radius: ["48%", "70%"],
        center: ["50%", "43%"],
        padAngle: 3,
        minAngle: 4,
        itemStyle: { borderColor: chartBorderColor(), borderWidth: 4, borderRadius: 8 },
        label: { formatter: "{b}\n{c}", color: chartTextColor(), fontWeight: 650 },
        data
      }
    ]
  });
}

function initExceptionRadarChart() {
  const chart = createChart(exceptionRadarChartRef.value);
  if (!chart) return;
  const values = [
    { name: "迟到", value: lateToday.value },
    { name: "早退", value: earlyLeaveToday.value },
    { name: "缺勤", value: absentToday.value },
    { name: "请假待审", value: Number(dashboardData.value.leave?.pending) || 0 },
    { name: "审批积压", value: pendingApprovals.value }
  ];
  const max = Math.max(...values.map((item) => item.value), 5);

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: itemTooltip(),
    radar: {
      radius: "66%",
      center: ["50%", "52%"],
      splitNumber: 4,
      axisName: { color: chartMutedColor(), fontWeight: 650 },
      splitLine: { lineStyle: { color: chartBorderColor() } },
      splitArea: { areaStyle: { color: ["rgba(37, 99, 235, 0.04)", "rgba(20, 184, 166, 0.04)"] } },
      axisLine: { lineStyle: { color: chartBorderColor() } },
      indicator: values.map((item) => ({ name: item.name, max: Math.max(max, item.value) }))
    },
    series: [
      {
        type: "radar",
        data: [
          {
            value: values.map((item) => item.value),
            name: "异常指标",
            areaStyle: { color: "rgba(239, 68, 68, 0.18)" },
            lineStyle: { color: "#ef4444", width: 3 },
            itemStyle: { color: "#ef4444" }
          }
        ]
      }
    ]
  });
}

function initApprovalBusinessChart() {
  const chart = createChart(approvalBusinessChartRef.value);
  if (!chart) return;
  const data = (dashboardData.value.approvalBusinessDistribution || []).slice(0, 10).reverse();
  if (!data.length) {
    chart.setOption(emptyChartOption("暂无审批业务数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: ["#2563eb", "#16a34a", "#ef4444", "#64748b"],
    tooltip: axisTooltip(),
    legend: { top: 0, right: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: chartMutedColor() } },
    grid: { top: 42, right: 34, bottom: 18, left: 76, containLabel: true },
    xAxis: { type: "value", minInterval: 1, ...axisStyle() },
    yAxis: {
      type: "category",
      data: data.map((item) => item.name || "未知业务"),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: chartTextColor(), fontWeight: 650 }
    },
    series: [
      { name: "审批中", type: "bar", stack: "total", barWidth: 16, data: data.map((item) => Number(item.pending) || 0) },
      { name: "已通过", type: "bar", stack: "total", barWidth: 16, data: data.map((item) => Number(item.approved) || 0) },
      { name: "已拒绝", type: "bar", stack: "total", barWidth: 16, data: data.map((item) => Number(item.rejected) || 0) },
      { name: "已撤回", type: "bar", stack: "total", barWidth: 16, data: data.map((item) => Number(item.canceled) || 0), itemStyle: { borderRadius: [0, 8, 8, 0] } }
    ]
  });
}

function initDepartmentWorkloadChart() {
  const chart = createChart(departmentWorkloadChartRef.value);
  if (!chart) return;
  const data = (dashboardData.value.departmentWorkload || [])
    .map((item) => [
      Number(item.employeeCount) || 0,
      Number(item.load) || 0,
      Number(item.attendanceRate) || 0,
      item.name || "未分配部门",
      Number(item.leaveCount) || 0,
      Number(item.tripCount) || 0
    ])
    .filter((item) => Number(item[0]) > 0);
  if (!data.length) {
    chart.setOption(emptyChartOption("暂无部门负载"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: {
      ...itemTooltip(),
      formatter: (params: unknown) => {
        const item = params as { data: Array<number | string> };
        return `${item.data[3]}<br/>部门人数：${item.data[0]}<br/>请假：${item.data[4]} / 出差：${item.data[5]}<br/>出勤率：${item.data[2]}%`;
      }
    },
    grid: chartGrid(46),
    xAxis: { type: "value", name: "人数", minInterval: 1, ...axisStyle() },
    yAxis: { type: "value", name: "负载", minInterval: 1, ...axisStyle() },
    visualMap: {
      min: 0,
      max: 100,
      dimension: 2,
      orient: "horizontal",
      left: "center",
      bottom: 0,
      text: ["高出勤", "低出勤"],
      textStyle: { color: chartMutedColor() },
      inRange: { color: ["#ef4444", "#f97316", "#14b8a6"] }
    },
    series: [
      {
        type: "scatter",
        data,
        symbolSize: (value: Array<number | string>) => Math.max(18, Math.min(54, Number(value[1]) * 5 + 18)),
        label: { show: true, formatter: (params: unknown) => String((params as { data: Array<number | string> }).data[3]), color: chartTextColor(), fontWeight: 650 },
        itemStyle: { borderColor: chartBorderColor(), borderWidth: 2, opacity: 0.88 }
      }
    ]
  });
}

function initAttendanceRankChart() {
  const data = (dashboardData.value.attendanceRanking || [])
    .map((item) => ({
      name: item.empName || "未知员工",
      value: Number(item.rate) || 0,
      extra: `${Number(item.normalDays) || 0}/${Number(item.totalDays) || 0} 天`
    }))
    .filter((item) => item.value > 0)
    .slice(0, 10);

  initHorizontalRankChart(attendanceRankChartRef.value, data, {
    emptyText: `${dateScopeLabel.value}暂无出勤率排行`,
    unit: "%",
    colors: ["#14b8a6", "#2563eb"],
    max: 100
  });
}

function initAbsenceRankChart() {
  const data = (dashboardData.value.absenceRanking || [])
    .map((item) => ({
      name: item.empName || "未知员工",
      value: Number(item.absentCount) || 0,
      extra: `应到 ${Number(item.requiredDays) || 0} 天`
    }))
    .filter((item) => item.value > 0)
    .slice(0, 10);

  initHorizontalRankChart(absenceRankChartRef.value, data, {
    emptyText: `${dateScopeLabel.value}暂无缺勤记录`,
    unit: "次",
    colors: ["#ef4444", "#f97316"]
  });
}

function initLateRankChart() {
  const data = (dashboardData.value.lateRanking || [])
    .map((item) => ({ name: item.empName || "未知员工", value: Number(item.lateCount) || 0 }))
    .filter((item) => item.value > 0)
    .slice(0, 10);

  initHorizontalRankChart(lateRankChartRef.value, data, {
    emptyText: `${dateScopeLabel.value}暂无迟到记录`,
    unit: "次",
    colors: ["#f97316", "#ef4444"]
  });
}

function initHorizontalRankChart(
  element: HTMLDivElement | undefined,
  source: ChartItem[],
  config: { emptyText: string; unit: string; colors: [string, string]; max?: number }
) {
  const chart = createChart(element);
  if (!chart) return;

  const data = [...source].reverse();
  if (!data.length) {
    chart.setOption(emptyChartOption(config.emptyText));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: {
      ...axisTooltip(),
      formatter: (params: unknown) => {
        const [item] = params as Array<{ name: string; value: number; dataIndex: number }>;
        const extra = data[item.dataIndex]?.extra;
        return `${item.name}<br/>${item.value}${config.unit}${extra ? `<br/>${extra}` : ""}`;
      }
    },
    grid: { top: 8, right: 34, bottom: 18, left: 78, containLabel: true },
    xAxis: { type: "value", max: config.max, minInterval: config.unit === "%" ? undefined : 1, ...axisStyle() },
    yAxis: {
      type: "category",
      data: data.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: chartTextColor(), fontWeight: 650 }
    },
    series: [
      {
        type: "bar",
        barWidth: 14,
        data: data.map((item) => item.value),
        label: { show: true, position: "right", color: chartTextColor(), fontWeight: 700, formatter: `{c}${config.unit}` },
        itemStyle: { borderRadius: [0, 8, 8, 0], color: createGradient(config.colors[0], config.colors[1], true) }
      }
    ]
  });
}

function initLeaveTypeChart() {
  const byType = dashboardData.value.leave?.byType || {};
  const data = Object.entries(byType).map(([name, value]) => ({ name, value: Number(value) || 0 }));
  initDonutChart(leaveTypeChartRef.value, data, {
    emptyText: `${dateScopeLabel.value}暂无请假`,
    tooltip: "{b}<br/>{c} 人 ({d}%)",
    colors: ["#7c3aed", "#2563eb", "#14b8a6", "#f97316", "#db2777", "#64748b"]
  });
}

function initDeptChart() {
  const chart = createChart(deptChartRef.value);
  if (!chart) return;

  const data = (dashboardData.value.departmentDistribution || [])
    .map((item) => ({ name: item.name || "未分配", value: Number(item.value || item.count) || 0 }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
    .slice(0, 10)
    .reverse();

  if (!data.length) {
    chart.setOption(emptyChartOption("暂无部门数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: { ...axisTooltip(), formatter: "{b}<br/>{c} 人" },
    grid: { top: 12, right: 30, bottom: 20, left: 76, containLabel: true },
    xAxis: { type: "value", minInterval: 1, ...axisStyle() },
    yAxis: {
      type: "category",
      data: data.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: chartTextColor(), fontWeight: 650 }
    },
    series: [
      {
        type: "bar",
        barWidth: 14,
        data: data.map((item) => item.value),
        label: { show: true, position: "right", color: chartTextColor(), fontWeight: 700 },
        itemStyle: { borderRadius: [0, 8, 8, 0], color: createGradient("#2563eb", "#0891b2", true) }
      }
    ]
  });
}

function handleResize() {
  charts.forEach((chart) => chart.resize());
}

async function handleThemeChange() {
  clearCharts();
  await nextTick();
  initCharts();
}

onMounted(() => {
  fetchDashboardData();
  window.addEventListener("resize", handleResize);
  window.addEventListener("oa-theme-change", handleThemeChange);
});

onUnmounted(() => {
  clearCharts();
  window.removeEventListener("resize", handleResize);
  window.removeEventListener("oa-theme-change", handleThemeChange);
});
</script>

<style scoped>
.data-board-page {
  padding-bottom: 18px;
}

.board-header {
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent 36%),
    linear-gradient(315deg, rgba(20, 184, 166, 0.1), transparent 32%),
    var(--oa-surface);
}

.board-stat-grid {
  margin-bottom: 14px;
}

.board-stat-card {
  position: relative;
  overflow: hidden;
}

.board-insight-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.board-insight {
  min-height: 118px;
  padding: 13px;
  border: 1px solid var(--oa-border-soft);
  border-left: 4px solid #14b8a6;
  border-radius: 8px;
  background: var(--oa-surface);
  box-shadow: var(--oa-shadow);
}

.board-insight.is-warning {
  border-left-color: #f97316;
}

.board-insight.is-danger {
  border-left-color: #ef4444;
}

.insight-topline {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--oa-muted);
  font-size: 12px;
  font-weight: 700;
}

.insight-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #14b8a6;
}

.board-insight.is-warning .insight-dot {
  background: #f97316;
}

.board-insight.is-danger .insight-dot {
  background: #ef4444;
}

.board-insight strong {
  display: block;
  margin-top: 10px;
  color: var(--oa-text);
  font-size: 22px;
  line-height: 1.1;
  font-weight: 780;
  font-variant-numeric: tabular-nums;
}

.board-insight p {
  margin: 8px 0 0;
  color: var(--oa-muted);
  font-size: 12px;
  line-height: 1.5;
}

.board-chart-large {
  height: 338px;
}

.board-heatmap-chart {
  height: 360px;
}

.health-layout {
  display: grid;
  grid-template-columns: minmax(210px, 1fr) minmax(168px, 0.7fr);
  align-items: center;
  gap: 14px;
}

.gauge-chart {
  height: 286px;
}

.rank-chart {
  height: 330px;
}

@media (max-width: 1280px) {
  .board-insight-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .health-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .board-insight-grid {
    grid-template-columns: 1fr;
  }

  .board-chart-large {
    height: 280px;
  }

  .board-heatmap-chart {
    height: 310px;
  }
}
</style>
