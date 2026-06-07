<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <el-radio-group v-model="period" @change="handlePeriodChange">
          <el-radio-button value="today">本日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="year">本年</el-radio-button>
        </el-radio-group>
        <el-date-picker v-if="period === 'month'" v-model="month" type="month" placeholder="选择月份" format="YYYY年MM月" value-format="YYYY-MM" @change="fetchAllData" />
        <el-date-picker v-if="period === 'year'" v-model="year" type="year" placeholder="选择年份" format="YYYY年" value-format="YYYY" @change="fetchAllData" />
      </div>
    </div>

    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="6">
        <div class="bg-white rounded-lg p-5 flex items-center gap-4" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="w-14 h-14 rounded-lg flex items-center justify-center" :style="{ backgroundColor: item.bg }">
            <span class="text-xl font-bold" :style="{ color: item.color }">{{ statsData[item.key] }}</span>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">{{ statsData[item.key] }}</span>
            <span class="text-sm text-[#909399] mt-1">{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-5">
      <el-col :span="16">
        <el-card>
          <template #header><span class="font-medium">出勤趋势</span></template>
          <div ref="trendChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span class="font-medium">请假统计</span></template>
          <div ref="leaveChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">月度对比</span></template>
          <div ref="compareChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">出勤率统计</span></template>
          <div ref="rateChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from "vue";
import dayjs from "dayjs";
import {
  getPersonalAttendanceSummary, getPersonalAttendanceTrend,
  getPersonalLeaveSummary, getPersonalMonthlyCompare
} from "@/api/report";
import { init, type ECharts } from "@/utils/echarts";

const period = ref("month");
const month = ref(dayjs().format("YYYY-MM"));
const year = ref(dayjs().format("YYYY"));
const trendChartRef = ref<HTMLDivElement>();
const leaveChartRef = ref<HTMLDivElement>();
const compareChartRef = ref<HTMLDivElement>();
const rateChartRef = ref<HTMLDivElement>();
const charts: ECharts[] = [];

const statsData = reactive({ normalDays: 0, lateDays: 0, earlyLeaveDays: 0, absentDays: 0 });

const statsCards = [
  { label: "出勤天数", key: "normalDays" as const, color: "#409EFF", bg: "#e6f7ff" },
  { label: "迟到次数", key: "lateDays" as const, color: "#E6A23C", bg: "#fff7e6" },
  { label: "早退次数", key: "earlyLeaveDays" as const, color: "#F56C6C", bg: "#fef0f0" },
  { label: "缺勤天数", key: "absentDays" as const, color: "#9254de", bg: "#f9f0ff" }
];

const getQueryMonth = () => {
  if (period.value === "today" || period.value === "week") return dayjs().format("YYYY-MM");
  if (period.value === "month") return month.value;
  if (period.value === "year") return year.value + "-01";
  return dayjs().format("YYYY-MM");
};

const getTrendMonths = () => {
  if (period.value === "today") return 7;
  if (period.value === "week") return 4;
  if (period.value === "month") return 6;
  if (period.value === "year") return 12;
  return 6;
};

const handlePeriodChange = () => {
  destroyCharts();
  fetchAllData();
};

const destroyCharts = () => {
  charts.forEach((c) => c.dispose());
  charts.length = 0;
};

const fetchAllData = async () => {
  const qMonth = getQueryMonth();
  try {
    const r: any = await getPersonalAttendanceSummary(qMonth, period.value);
    if (r.data) {
      statsData.normalDays = r.data.normalDays || 0;
      statsData.lateDays = r.data.lateDays || 0;
      statsData.earlyLeaveDays = r.data.earlyLeaveDays || 0;
      statsData.absentDays = r.data.absentDays || 0;
    }
  } catch {}
  await nextTick();
  initTrendChart();
  initLeaveChart(qMonth);
  initCompareChart(qMonth);
  initRateChart(qMonth);
};

const initTrendChart = async () => {
  if (!trendChartRef.value) return;
  try {
    const r: any = await getPersonalAttendanceTrend(getTrendMonths(), period.value);
    const chart = init(trendChartRef.value);
    charts.push(chart);
    chart.setOption({
      tooltip: { trigger: "axis" },
      xAxis: { type: "category", data: (r.data || []).map((d: any) => d.month || d.date || "") },
      yAxis: { type: "value", name: "天数" },
      series: [{ type: "line", data: (r.data || []).map((d: any) => d.normalDays || d.days || d.count || 0), areaStyle: { opacity: 0.3 }, smooth: true, itemStyle: { color: "#409EFF" } }]
    });
  } catch {}
};

const initLeaveChart = async (qMonth: string) => {
  if (!leaveChartRef.value) return;
  try {
    const r: any = await getPersonalLeaveSummary(qMonth);
    const chart = init(leaveChartRef.value);
    charts.push(chart);
    chart.setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0 },
      series: [{ type: "pie", radius: ["40%", "65%"], data: (r.data || []).map((d: any) => ({ name: d.type || d.name || "", value: d.count || d.value || 0 })) }]
    });
  } catch {}
};

const initCompareChart = async (qMonth: string) => {
  if (!compareChartRef.value) return;
  try {
    const r: any = await getPersonalMonthlyCompare(qMonth);
    const chart = init(compareChartRef.value);
    charts.push(chart);
    const d = r.data || {};
    chart.setOption({
      tooltip: { trigger: "axis" },
      legend: { data: ["本期", "上期"] },
      xAxis: { type: "category", data: ["正常出勤", "迟到", "缺勤"] },
      yAxis: { type: "value" },
      series: [
        { name: "本期", type: "bar", data: [d.currentMonthNormal || 0, d.currentMonthLate || 0, d.currentMonthAbsent || 0], itemStyle: { color: "#409EFF" } },
        { name: "上期", type: "bar", data: [d.lastMonthNormal || 0, d.lastMonthLate || 0, d.lastMonthAbsent || 0], itemStyle: { color: "#909399" } }
      ]
    });
  } catch {}
};

const initRateChart = async (qMonth: string) => {
  if (!rateChartRef.value) return;
  try {
    const r: any = await getPersonalAttendanceSummary(qMonth, period.value);
    const chart = init(rateChartRef.value);
    charts.push(chart);
    const data = r.data || {};
    const total = data.totalDays || 0;
    const normal = data.normalDays || 0;
    const rate = total > 0 ? ((normal / total) * 100).toFixed(1) : 0;
    chart.setOption({
      tooltip: { trigger: "item" },
      series: [{
        type: "gauge",
        startAngle: 220,
        endAngle: -40,
        min: 0,
        max: 100,
        progress: { show: true, width: 14 },
        axisLine: { lineStyle: { width: 14 } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: { show: false },
        detail: { valueAnimation: true, formatter: (val: number) => `${val.toFixed(1)}%`, fontSize: 22, fontWeight: "bold", offsetCenter: [0, "10%"], color: "#303133" },
        data: [{ value: Number(rate), name: "出勤率" }],
        title: { show: true, offsetCenter: [0, "40%"], fontSize: 13, color: "#909399" }
      }]
    });
  } catch {}
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { destroyCharts(); window.removeEventListener("resize", handleResize); });
</script>
