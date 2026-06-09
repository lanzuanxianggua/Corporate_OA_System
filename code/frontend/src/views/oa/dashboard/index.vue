<template>
  <section class="oa-analytics-page">
    <header class="oa-page-header">
      <div>
        <div class="oa-eyebrow">OA Operations</div>
        <h1 class="oa-page-title">数据看板</h1>
        <p class="oa-page-subtitle">以所选日期出勤为核心，汇总缺勤、请假、迟到、审批与组织结构指标</p>
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
          @change="fetchDashboardData"
        />
        <el-button :icon="Refresh" circle :loading="loading" @click="fetchDashboardData" />
      </div>
    </header>

    <div v-loading="loading" element-loading-text="正在加载看板数据...">
      <div class="oa-stat-grid">
        <article v-for="item in metricCards" :key="item.label" class="oa-stat-card">
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

      <div class="oa-micro-grid dashboard-insights">
        <div v-for="item in insightCards" :key="item.label" class="oa-kpi-box">
          <div class="insight-line">
            <span class="oa-signal-dot" :style="{ backgroundColor: item.color }"></span>
            <span class="oa-kpi-label">{{ item.label }}</span>
          </div>
          <div class="oa-kpi-value compact-value">{{ item.value }}</div>
        </div>
      </div>

      <div class="oa-grid">
        <article class="oa-panel oa-col-6">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}出勤健康度</h2>
              <p class="oa-panel-subtitle">已打卡、待打卡、迟到、早退、缺勤与请假状态</p>
            </div>
            <el-tag :type="attendanceTagType" effect="light">{{ attendanceRate }}%</el-tag>
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

        <article class="oa-panel oa-col-6">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}出勤构成</h2>
              <p class="oa-panel-subtitle">出勤、请假、缺勤和待打卡占比</p>
            </div>
          </div>
          <div ref="attendanceMixChartRef" class="oa-chart"></div>
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

        <article class="oa-panel oa-col-6">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">{{ dateScopeLabel }}请假分析</h2>
              <p class="oa-panel-subtitle">当前日期范围内请假类型占比</p>
            </div>
          </div>
          <div ref="leaveTypeChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-6">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">部门人力结构</h2>
              <p class="oa-panel-subtitle">组织规模分布与资源集中度</p>
            </div>
          </div>
          <div ref="deptChartRef" class="oa-chart"></div>
        </article>

        <article class="oa-panel oa-col-12">
          <div class="oa-panel-header">
            <div>
              <h2 class="oa-panel-title">审批与流动</h2>
              <p class="oa-panel-subtitle">待审批、月度请假、新增员工与出差活跃度</p>
            </div>
          </div>
          <div ref="operationsChartRef" class="oa-chart"></div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import type { Component } from "vue";
import * as echarts from "echarts";
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
  chartGrid,
  chartPalette,
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
}

interface MetricCard {
  label: string;
  value: string;
  note: string;
  color: string;
  bgColor: string;
  icon: Component;
}

interface InsightCard {
  label: string;
  value: string;
  color: string;
}

interface ChartItem {
  name: string;
  value: number;
  extra?: string;
}

const loading = ref(false);
const dashboardData = ref<DashboardData>({});
const selectedDate = ref(formatDate(new Date()));
const attendanceGaugeRef = ref<HTMLDivElement>();
const attendanceMixChartRef = ref<HTMLDivElement>();
const attendanceRankChartRef = ref<HTMLDivElement>();
const absenceRankChartRef = ref<HTMLDivElement>();
const leaveTypeChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const operationsChartRef = ref<HTMLDivElement>();
const lateRankChartRef = ref<HTMLDivElement>();
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
const leaveRate = computed(() => {
  if (!totalRequired.value) return 0;
  return Math.round((onLeaveToday.value / totalRequired.value) * 100);
});
const exceptionTotal = computed(() => lateToday.value + earlyLeaveToday.value + absentToday.value);

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
    color: "#059669",
    bgColor: "#ecfdf5",
    icon: CircleCheck
  },
  {
    label: "待打卡",
    value: formatNumber(notClockedToday.value),
    note: "尚未形成有效打卡",
    color: "#64748b",
    bgColor: "#f8fafc",
    icon: Timer
  },
  {
    label: `${dateScopeLabel.value}缺勤`,
    value: formatNumber(absentToday.value),
    note: `缺勤率 ${absenceRate.value}%`,
    color: "#dc2626",
    bgColor: "#fef2f2",
    icon: WarningFilled
  },
  {
    label: `${dateScopeLabel.value}请假`,
    value: formatNumber(onLeaveToday.value),
    note: `占应出勤 ${leaveRate.value}%`,
    color: "#7c3aed",
    bgColor: "#f5f3ff",
    icon: Timer
  },
  {
    label: `${dateScopeLabel.value}异常`,
    value: formatNumber(exceptionTotal.value),
    note: `${formatNumber(lateToday.value)} 迟到 / ${formatNumber(earlyLeaveToday.value)} 早退`,
    color: "#d97706",
    bgColor: "#fff7ed",
    icon: DataAnalysis
  },
  {
    label: "待审批",
    value: formatNumber(pendingApprovals.value),
    note: "需要管理者处理",
    color: "#be123c",
    bgColor: "#fff1f2",
    icon: WarningFilled
  },
  {
    label: "本月新增",
    value: formatNumber(Number(dashboardData.value.newEmployeesThisMonth) || 0),
    note: `本月出差 ${formatNumber(Number(dashboardData.value.businessTripCountThisMonth) || 0)}`,
    color: "#0f766e",
    bgColor: "#f0fdfa",
    icon: TrendCharts
  }
]);

const insightCards = computed<InsightCard[]>(() => [
  { label: "审批积压", value: `${formatNumber(pendingApprovals.value)} 条待处理`, color: "#be123c" },
  { label: "请假审批", value: `${formatNumber(Number(dashboardData.value.leave?.pending) || 0)} 条待确认`, color: "#7c3aed" },
  { label: "月度请假", value: `${formatNumber(Number(dashboardData.value.leaveCountThisMonth) || 0)} 条记录`, color: "#2563eb" },
  { label: "月度出差", value: `${formatNumber(Number(dashboardData.value.businessTripCountThisMonth) || 0)} 条记录`, color: "#d97706" }
]);

const attendanceSignals = computed(() => [
  { label: "已打卡", value: formatNumber(clockedIn.value), color: "#059669" },
  { label: "待打卡", value: formatNumber(notClockedToday.value), color: "#64748b" },
  { label: "迟到", value: formatNumber(lateToday.value), color: "#d97706" },
  { label: "早退", value: formatNumber(earlyLeaveToday.value), color: "#dc2626" },
  { label: "缺勤", value: formatNumber(absentToday.value), color: "#be123c" },
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
  initAttendanceGauge();
  initAttendanceMixChart();
  initAttendanceRankChart();
  initAbsenceRankChart();
  initLateRankChart();
  initLeaveTypeChart();
  initDeptChart();
  initOperationsChart();
}

function initAttendanceGauge() {
  if (!attendanceGaugeRef.value) return;
  const chart = echarts.init(attendanceGaugeRef.value);
  charts.push(chart);
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
          itemStyle: { color: createGradient("#2563eb", "#059669", true) }
        },
        axisLine: { lineStyle: { width: 16, color: [[1, "#e5e7eb"]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: {
          valueAnimation: true,
          formatter: "{value}%",
          fontSize: 30,
          fontWeight: 760,
          color: "#111827",
          offsetCenter: [0, "-4%"]
        },
        title: { offsetCenter: [0, "30%"], color: "#6b7280", fontSize: 13 },
        data: [{ value: attendanceRate.value, name: `${dateScopeLabel.value}出勤率` }]
      }
    ]
  });
}

function initAttendanceMixChart() {
  if (!attendanceMixChartRef.value) return;
  const chart = echarts.init(attendanceMixChartRef.value);
  charts.push(chart);

  const data = [
    { name: "已打卡", value: clockedIn.value },
    { name: "待打卡", value: notClockedToday.value },
    { name: "请假", value: onLeaveToday.value },
    { name: "缺勤", value: absentToday.value }
  ].filter((item) => item.value > 0);

  if (!data.length) {
    chart.setOption(emptyChartOption("暂无出勤数据"));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: ["#059669", "#64748b", "#7c3aed", "#dc2626"],
    tooltip: itemTooltip("{b}<br/>{c} 人 ({d}%)"),
    legend: { bottom: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: "#6b7280" } },
    series: [
      {
        type: "pie",
        radius: ["50%", "72%"],
        center: ["50%", "44%"],
        padAngle: 3,
        minAngle: 5,
        itemStyle: { borderColor: "#fff", borderWidth: 4, borderRadius: 8 },
        label: { formatter: "{b}\n{c}人", color: "#374151", fontWeight: 650 },
        data
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
    colors: ["#059669", "#2563eb"],
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
    colors: ["#dc2626", "#d97706"]
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
    colors: ["#d97706", "#dc2626"]
  });
}

function initHorizontalRankChart(
  element: HTMLDivElement | undefined,
  source: ChartItem[],
  config: { emptyText: string; unit: string; colors: [string, string]; max?: number }
) {
  if (!element) return;
  const chart = echarts.init(element);
  charts.push(chart);

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
      axisLabel: { color: "#374151", fontWeight: 650 }
    },
    series: [
      {
        type: "bar",
        barWidth: 14,
        data: data.map((item) => item.value),
        label: { show: true, position: "right", color: "#111827", fontWeight: 700, formatter: `{c}${config.unit}` },
        itemStyle: { borderRadius: [0, 8, 8, 0], color: createGradient(config.colors[0], config.colors[1], true) }
      }
    ]
  });
}

function initLeaveTypeChart() {
  if (!leaveTypeChartRef.value) return;
  const chart = echarts.init(leaveTypeChartRef.value);
  charts.push(chart);

  const byType = dashboardData.value.leave?.byType || {};
  const data = Object.entries(byType)
    .map(([name, value]) => ({ name, value: Number(value) || 0 }))
    .filter((item) => item.value > 0);

  if (!data.length) {
    chart.setOption(emptyChartOption(`${dateScopeLabel.value}暂无请假`));
    return;
  }

  chart.setOption({
    textStyle: chartTextStyle(),
    color: chartPalette,
    tooltip: itemTooltip("{b}<br/>{c} 人 ({d}%)"),
    legend: { bottom: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: "#6b7280" } },
    series: [
      {
        type: "pie",
        radius: ["48%", "70%"],
        center: ["50%", "44%"],
        padAngle: 3,
        itemStyle: { borderColor: "#fff", borderWidth: 4, borderRadius: 8 },
        label: { formatter: "{b}\n{c}人", color: "#374151", fontWeight: 650 },
        data
      }
    ]
  });
}

function initDeptChart() {
  if (!deptChartRef.value) return;
  const chart = echarts.init(deptChartRef.value);
  charts.push(chart);

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
      axisLabel: { color: "#374151", fontWeight: 650 }
    },
    series: [
      {
        type: "bar",
        barWidth: 14,
        data: data.map((item) => item.value),
        label: { show: true, position: "right", color: "#111827", fontWeight: 700 },
        itemStyle: { borderRadius: [0, 8, 8, 0], color: createGradient("#2563eb", "#0891b2", true) }
      }
    ]
  });
}

function initOperationsChart() {
  if (!operationsChartRef.value) return;
  const chart = echarts.init(operationsChartRef.value);
  charts.push(chart);

  const data = [
    { name: "待审批", value: pendingApprovals.value, color: "#be123c" },
    { name: "本月请假", value: Number(dashboardData.value.leaveCountThisMonth) || 0, color: "#7c3aed" },
    { name: "新增员工", value: Number(dashboardData.value.newEmployeesThisMonth) || 0, color: "#0f766e" },
    { name: "出差申请", value: Number(dashboardData.value.businessTripCountThisMonth) || 0, color: "#d97706" }
  ];

  chart.setOption({
    textStyle: chartTextStyle(),
    tooltip: axisTooltip(),
    grid: chartGrid(38),
    xAxis: { type: "category", data: data.map((item) => item.name), ...axisStyle() },
    yAxis: { type: "value", minInterval: 1, ...axisStyle() },
    series: [
      {
        type: "bar",
        barWidth: 30,
        data: data.map((item) => ({
          value: item.value,
          itemStyle: { borderRadius: [8, 8, 0, 0], color: createGradient(item.color, "#cbd5e1") }
        })),
        label: { show: true, position: "top", color: "#111827", fontWeight: 700 }
      }
    ]
  });
}

function handleResize() {
  charts.forEach((chart) => chart.resize());
}

onMounted(() => {
  fetchDashboardData();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  clearCharts();
  window.removeEventListener("resize", handleResize);
});
</script>

<style scoped>
.dashboard-insights {
  margin: 16px 0;
}

.insight-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.compact-value {
  font-size: 17px;
}

.health-layout {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(190px, 0.72fr);
  align-items: center;
  gap: 14px;
}

.gauge-chart {
  height: 278px;
}

.rank-chart {
  height: 330px;
}

@media (max-width: 900px) {
  .health-layout {
    grid-template-columns: 1fr;
  }
}
</style>
