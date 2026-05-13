<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <el-date-picker v-model="month" type="month" placeholder="选择月份" format="YYYY年MM月" value-format="YYYY-MM" @change="fetchAllData" />
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

    <el-card class="mb-5">
      <template #header><span class="font-medium">出勤趋势（近6个月）</span></template>
      <div ref="trendChartRef" style="height: 300px"></div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">请假统计</span></template>
          <div ref="leaveChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">月度对比</span></template>
          <div ref="compareChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from "vue";
import dayjs from "dayjs";
import * as echarts from "echarts";
import {
  getPersonalAttendanceSummary, getPersonalAttendanceTrend,
  getPersonalLeaveSummary, getPersonalMonthlyCompare
} from "@/api/report";

const month = ref(dayjs().format("YYYY-MM"));
const trendChartRef = ref<HTMLDivElement>();
const leaveChartRef = ref<HTMLDivElement>();
const compareChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];

const statsData = reactive({ normalDays: 0, lateDays: 0, earlyLeaveDays: 0, absentDays: 0 });

const statsCards = [
  { label: "出勤天数", key: "normalDays" as const, color: "#409EFF", bg: "#e6f7ff" },
  { label: "迟到次数", key: "lateDays" as const, color: "#E6A23C", bg: "#fff7e6" },
  { label: "早退次数", key: "earlyLeaveDays" as const, color: "#F56C6C", bg: "#fef0f0" },
  { label: "缺勤天数", key: "absentDays" as const, color: "#9254de", bg: "#f9f0ff" }
];

const fetchAllData = async () => {
  try {
    const r: any = await getPersonalAttendanceSummary(month.value);
    if (r.data) {
      statsData.normalDays = r.data.normalDays || 0;
      statsData.lateDays = r.data.lateDays || 0;
      statsData.earlyLeaveDays = r.data.earlyLeaveDays || 0;
      statsData.absentDays = r.data.absentDays || 0;
    }
  } catch {}
  await nextTick();
  initTrendChart();
  initLeaveChart();
  initCompareChart();
};

const initTrendChart = async () => {
  if (!trendChartRef.value) return;
  try {
    const r: any = await getPersonalAttendanceTrend(6);
    const chart = echarts.init(trendChartRef.value);
    charts.push(chart);
    chart.setOption({
      tooltip: { trigger: "axis" },
      xAxis: { type: "category", data: (r.data || []).map((d: any) => d.month || d.date) },
      yAxis: { type: "value", name: "天数" },
      series: [{ type: "line", data: (r.data || []).map((d: any) => d.days || d.count || 0), areaStyle: { opacity: 0.3 }, smooth: true, itemStyle: { color: "#409EFF" } }]
    });
  } catch {}
};

const initLeaveChart = async () => {
  if (!leaveChartRef.value) return;
  try {
    const r: any = await getPersonalLeaveSummary(month.value);
    const chart = echarts.init(leaveChartRef.value);
    charts.push(chart);
    chart.setOption({
      tooltip: { trigger: "item" },
      series: [{ type: "pie", radius: "60%", data: (r.data || []).map((d: any) => ({ name: d.type || d.name, value: d.count || d.value || 0 })), emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: "rgba(0,0,0,0.5)" } } }]
    });
  } catch {}
};

const initCompareChart = async () => {
  if (!compareChartRef.value) return;
  try {
    const r: any = await getPersonalMonthlyCompare(month.value);
    const chart = echarts.init(compareChartRef.value);
    charts.push(chart);
    const d = r.data || {};
    chart.setOption({
      tooltip: { trigger: "axis" },
      legend: { data: ["本月", "上月"] },
      xAxis: { type: "category", data: ["正常出勤", "迟到", "缺勤"] },
      yAxis: { type: "value" },
      series: [
        { name: "本月", type: "bar", data: [d.currentMonthNormal || 0, d.currentMonthLate || 0, d.currentMonthAbsent || 0], itemStyle: { color: "#409EFF" } },
        { name: "上月", type: "bar", data: [d.lastMonthNormal || 0, d.lastMonthLate || 0, d.lastMonthAbsent || 0], itemStyle: { color: "#909399" } }
      ]
    });
  } catch {}
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { charts.forEach((c) => c.dispose()); window.removeEventListener("resize", handleResize); });
</script>
