<template>
  <section class="oa-analytics-page">
    <header class="oa-page-header">
      <div>
        <div class="oa-eyebrow">Personal Report</div>
        <h1 class="oa-page-title">个人考勤报表</h1>
        <p class="oa-page-subtitle">按日、周、月、年查看个人出勤、异常与请假情况</p>
      </div>
      <div class="oa-header-actions">
        <el-radio-group v-model="period" @change="handlePeriodChange">
          <el-radio-button value="today">本日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="year">本年</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="period === 'month'"
          v-model="month"
          type="month"
          placeholder="选择月份"
          format="YYYY年MM月"
          value-format="YYYY-MM"
          :clearable="false"
          @change="fetchAllData" />
        <el-date-picker
          v-if="period === 'year'"
          v-model="year"
          type="year"
          placeholder="选择年份"
          format="YYYY年"
          value-format="YYYY"
          :clearable="false"
          @change="fetchAllData" />
      </div>
    </header>

    <div class="oa-stat-grid">
      <article v-for="item in statsCards" :key="item.label" class="oa-stat-card">
        <div class="oa-stat-icon" :style="{ color: item.color, backgroundColor: item.bgColor }">
          <el-icon :size="22"><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="oa-stat-label">{{ item.label }}</div>
          <div class="oa-stat-value">{{ statsData[item.key] }}</div>
          <div class="oa-stat-note">{{ item.note }}</div>
        </div>
      </article>
    </div>

    <div class="oa-grid">
      <article class="oa-panel oa-col-8">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">出勤趋势</h2>
            <p class="oa-panel-subtitle">当前筛选周期下的正常出勤变化</p>
          </div>
        </div>
        <div ref="trendChartRef" class="oa-chart"></div>
      </article>

      <article class="oa-panel oa-col-4">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">请假统计</h2>
            <p class="oa-panel-subtitle">请假类型占比</p>
          </div>
        </div>
        <div ref="leaveChartRef" class="oa-chart"></div>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">月度对比</h2>
            <p class="oa-panel-subtitle">本期与上期关键考勤指标对照</p>
          </div>
        </div>
        <div ref="compareChartRef" class="oa-chart"></div>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">出勤率统计</h2>
            <p class="oa-panel-subtitle">正常出勤占应出勤天数比例</p>
          </div>
        </div>
        <div ref="rateChartRef" class="oa-chart"></div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref } from "vue";
import type { Component } from "vue";
import dayjs from "dayjs";
import * as echarts from "@/utils/echarts";
import { CircleCheck, CircleClose, Timer, WarningFilled } from "@element-plus/icons-vue";
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
import {
  getPersonalAttendanceSummary,
  getPersonalAttendanceTrend,
  getPersonalLeaveSummary,
  getPersonalMonthlyCompare
} from "@/api/report";

type Period = "today" | "week" | "month" | "year";
type StatKey = "normalDays" | "lateDays" | "earlyLeaveDays" | "absentDays";

interface StatCard {
  label: string;
  key: StatKey;
  note: string;
  icon: Component;
  color: string;
  bgColor: string;
}

const period = ref<Period>("month");
const month = ref(dayjs().format("YYYY-MM"));
const year = ref(dayjs().format("YYYY"));
const trendChartRef = ref<HTMLDivElement>();
const leaveChartRef = ref<HTMLDivElement>();
const compareChartRef = ref<HTMLDivElement>();
const rateChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];

const statsData = reactive<Record<StatKey, number>>({
  normalDays: 0,
  lateDays: 0,
  earlyLeaveDays: 0,
  absentDays: 0
});

const statsCards: StatCard[] = [
  { label: "出勤天数", key: "normalDays", note: "正常出勤记录", icon: CircleCheck, color: "#2563eb", bgColor: "#eff6ff" },
  { label: "迟到次数", key: "lateDays", note: "迟到异常记录", icon: WarningFilled, color: "#d97706", bgColor: "#fff7ed" },
  { label: "早退次数", key: "earlyLeaveDays", note: "早退异常记录", icon: Timer, color: "#dc2626", bgColor: "#fef2f2" },
  { label: "缺勤天数", key: "absentDays", note: "缺勤异常记录", icon: CircleClose, color: "#7c3aed", bgColor: "#f5f3ff" }
];

function getQueryMonth() {
  if (period.value === "today" || period.value === "week") return dayjs().format("YYYY-MM");
  if (period.value === "month") return month.value;
  if (period.value === "year") return `${year.value}-01`;
  return dayjs().format("YYYY-MM");
}

function getTrendMonths() {
  if (period.value === "today") return 7;
  if (period.value === "week") return 4;
  if (period.value === "year") return 12;
  return 6;
}

function handlePeriodChange() {
  fetchAllData();
}

function destroyCharts() {
  charts.forEach((chart) => chart.dispose());
  charts.length = 0;
}

async function fetchAllData() {
  destroyCharts();
  const queryMonth = getQueryMonth();
  try {
    const response: any = await getPersonalAttendanceSummary(queryMonth, period.value);
    if (response.data) {
      statsData.normalDays = Number(response.data.normalDays) || 0;
      statsData.lateDays = Number(response.data.lateDays) || 0;
      statsData.earlyLeaveDays = Number(response.data.earlyLeaveDays) || 0;
      statsData.absentDays = Number(response.data.absentDays) || 0;
    }
  } catch {}

  await nextTick();
  await Promise.all([
    initTrendChart(),
    initLeaveChart(queryMonth),
    initCompareChart(queryMonth),
    initRateChart(queryMonth)
  ]);
}

async function initTrendChart() {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getPersonalAttendanceTrend(getTrendMonths(), period.value);
    const data = (response.data || []) as any[];
    if (!data.length) {
      chart.setOption(emptyChartOption("暂无出勤趋势数据"));
      return;
    }

    chart.setOption({
      textStyle: chartTextStyle(),
      color: chartPalette,
      tooltip: axisTooltip(),
      grid: chartGrid(36),
      xAxis: { type: "category", data: data.map((item) => item.month || item.date || ""), ...axisStyle() },
      yAxis: { type: "value", name: "天数", minInterval: 1, ...axisStyle() },
      series: [
        {
          name: "正常出勤",
          type: "line",
          data: data.map((item) => Number(item.normalDays || item.days || item.count) || 0),
          smooth: true,
          symbolSize: 7,
          lineStyle: { width: 3, color: "#2563eb" },
          itemStyle: { color: "#2563eb" },
          areaStyle: { color: createGradient("rgba(37, 99, 235, .24)", "rgba(37, 99, 235, .04)") }
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("趋势数据加载失败"));
  }
}

async function initLeaveChart(queryMonth: string) {
  if (!leaveChartRef.value) return;
  const chart = echarts.init(leaveChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getPersonalLeaveSummary(queryMonth);
    const data = ((response.data || []) as any[])
      .map((item) => ({ name: item.type || item.name || "未知类型", value: Number(item.count || item.value) || 0 }))
      .filter((item) => item.value > 0);

    if (!data.length) {
      chart.setOption(emptyChartOption("暂无请假数据"));
      return;
    }

    chart.setOption({
      textStyle: chartTextStyle(),
      color: chartPalette,
      tooltip: itemTooltip("{b}<br/>{c} 次 ({d}%)"),
      legend: { bottom: 0, icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: chartMutedColor() } },
      series: [
        {
          type: "pie",
          radius: ["48%", "70%"],
          center: ["50%", "44%"],
          padAngle: 3,
          itemStyle: { borderColor: chartBorderColor(), borderWidth: 4, borderRadius: 8 },
          label: { color: chartTextColor(), fontWeight: 650 },
          data
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("请假数据加载失败"));
  }
}

async function initCompareChart(queryMonth: string) {
  if (!compareChartRef.value) return;
  const chart = echarts.init(compareChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getPersonalMonthlyCompare(queryMonth);
    const data = response.data || {};
    chart.setOption({
      textStyle: chartTextStyle(),
      color: ["#2563eb", "#94a3b8"],
      tooltip: axisTooltip(),
      legend: { top: 0, right: 0, textStyle: { color: chartMutedColor() } },
      grid: chartGrid(42),
      xAxis: { type: "category", data: ["正常出勤", "迟到", "缺勤"], ...axisStyle() },
      yAxis: { type: "value", minInterval: 1, ...axisStyle() },
      series: [
        {
          name: "本期",
          type: "bar",
          barWidth: 22,
          data: [data.currentMonthNormal || 0, data.currentMonthLate || 0, data.currentMonthAbsent || 0],
          itemStyle: { borderRadius: [6, 6, 0, 0] }
        },
        {
          name: "上期",
          type: "bar",
          barWidth: 22,
          data: [data.lastMonthNormal || 0, data.lastMonthLate || 0, data.lastMonthAbsent || 0],
          itemStyle: { borderRadius: [6, 6, 0, 0] }
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("月度对比数据加载失败"));
  }
}

async function initRateChart(queryMonth: string) {
  if (!rateChartRef.value) return;
  const chart = echarts.init(rateChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getPersonalAttendanceSummary(queryMonth, period.value);
    const data = response.data || {};
    const total = Number(data.totalDays) || 0;
    const normal = Number(data.normalDays) || 0;
    const rate = total > 0 ? Number(((normal / total) * 100).toFixed(1)) : 0;
    chart.setOption({
      textStyle: chartTextStyle(),
      tooltip: itemTooltip("出勤率<br/>{c}%"),
      series: [
        {
          type: "gauge",
          startAngle: 210,
          endAngle: -30,
          min: 0,
          max: 100,
          radius: "88%",
          progress: { show: true, width: 16, roundCap: true, itemStyle: { color: "#2563eb" } },
          axisLine: { lineStyle: { width: 16, color: [[1, chartBorderColor()]] } },
          axisTick: { show: false },
          splitLine: { show: false },
          axisLabel: { show: false },
          pointer: { show: false },
          detail: { valueAnimation: true, formatter: "{value}%", fontSize: 30, fontWeight: 760, color: chartTextColor() },
          title: { offsetCenter: [0, "34%"], color: chartMutedColor(), fontSize: 13 },
          data: [{ value: rate, name: "出勤率" }]
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("出勤率数据加载失败"));
  }
}

function handleResize() {
  charts.forEach((chart) => chart.resize());
}

function handleThemeChange() {
  fetchAllData();
}

onMounted(() => {
  fetchAllData();
  window.addEventListener("resize", handleResize);
  window.addEventListener("oa-theme-change", handleThemeChange);
});

onUnmounted(() => {
  destroyCharts();
  window.removeEventListener("resize", handleResize);
  window.removeEventListener("oa-theme-change", handleThemeChange);
});
</script>
