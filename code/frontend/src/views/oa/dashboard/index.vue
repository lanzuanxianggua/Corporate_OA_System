<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-bold text-[#303133]">数据看板</span>
      <el-radio-group v-model="period" @change="fetchAllData">
        <el-radio-button value="today">今日</el-radio-button>
        <el-radio-button value="week">本周</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="16" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="4">
        <div class="bg-white rounded-lg p-4" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="text-sm text-[#909399] mb-1">{{ item.label }}</div>
          <div class="text-2xl font-bold" :style="{ color: item.color }">{{ item.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-5">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">近7天出勤率趋势</span></template>
          <div ref="trendChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">部门人员分布</span></template>
          <div ref="deptChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header><span class="font-medium">考勤状态分布</span></template>
          <div ref="attStatusChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span class="font-medium">请假类型统计</span></template>
          <div ref="leaveTypeChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span class="font-medium">今日打卡概览</span></template>
          <div ref="clockInChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from "vue";
import * as echarts from "echarts";
import { getDashboardStats } from "@/api/statistics";

const period = ref("today");
const trendChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const attStatusChartRef = ref<HTMLDivElement>();
const leaveTypeChartRef = ref<HTMLDivElement>();
const clockInChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];
let cachedData: any = null;

const statsCards = reactive([
  { label: "总人数", value: "0", color: "#409EFF" },
  { label: "已打卡", value: "0", color: "#67C23A" },
  { label: "未打卡", value: "0", color: "#909399" },
  { label: "迟到", value: "0", color: "#E6A23C" },
  { label: "早退", value: "0", color: "#F56C6C" },
  { label: "请假", value: "0", color: "#9254de" }
]);

const destroyCharts = () => {
  charts.forEach((c) => c.dispose());
  charts.length = 0;
};

const fetchAllData = async () => {
  destroyCharts();
  try {
    const r: any = await getDashboardStats(period.value);
    if (r.data) {
      cachedData = r.data;
      const att = r.data.attendance || {};
      const lv = r.data.leave || {};
      statsCards[0].value = String(r.data.employeeTotal || 0);
      statsCards[1].value = String(att.clockedIn || 0);
      statsCards[2].value = String(att.notClockedIn || 0);
      statsCards[3].value = String(att.late || 0);
      statsCards[4].value = String(att.earlyLeave || 0);
      statsCards[5].value = String(lv.total || 0);
    }
  } catch {}
  await nextTick();
  initTrendChart();
  initDeptChart();
  initAttStatusChart();
  initLeaveTypeChart();
  initClockInChart();
};

const initTrendChart = () => {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  charts.push(chart);
  const trend = cachedData?.attendanceTrend || [];
  chart.setOption({
    tooltip: { trigger: "axis", formatter: "{b}: {c}%" },
    xAxis: { type: "category", data: trend.map((d: any) => d.date || "") },
    yAxis: { type: "value", name: "出勤率(%)", max: 100 },
    series: [{
      type: "line", data: trend.map((d: any) => d.rate || 0),
      areaStyle: { opacity: 0.3, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "#409EFF" }, { offset: 1, color: "rgba(64,158,255,0.1)" }]) },
      smooth: true, itemStyle: { color: "#409EFF" }, lineStyle: { width: 3 }
    }]
  });
};

const initDeptChart = () => {
  if (!deptChartRef.value) return;
  const chart = echarts.init(deptChartRef.value);
  charts.push(chart);
  const dept = cachedData?.departmentDistribution || [];
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c}人 ({d}%)" },
    legend: { bottom: 0, type: "scroll" },
    series: [{
      type: "pie", radius: ["35%", "60%"],
      label: { formatter: "{b}\n{d}%" },
      data: dept.map((d: any) => ({ value: d.value || d.count || 0, name: d.name || "" })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,0.2)" } }
    }]
  });
};

const initAttStatusChart = () => {
  if (!attStatusChartRef.value) return;
  const chart = echarts.init(attStatusChartRef.value);
  charts.push(chart);
  const att = cachedData?.attendance || {};
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c}人 ({d}%)" },
    legend: { bottom: 0 },
    color: ["#67C23A", "#E6A23C", "#F56C6C", "#909399"],
    series: [{
      type: "pie", radius: "60%",
      data: [
        { value: att.clockedIn || 0, name: "正常出勤" },
        { value: att.late || 0, name: "迟到" },
        { value: att.earlyLeave || 0, name: "早退" },
        { value: att.absent || 0, name: "缺勤" }
      ].filter(d => d.value > 0)
    }]
  });
};

const initLeaveTypeChart = () => {
  if (!leaveTypeChartRef.value) return;
  const chart = echarts.init(leaveTypeChartRef.value);
  charts.push(chart);
  const lv = cachedData?.leave || {};
  const byType = lv.byType || {};
  const data = Object.entries(byType).map(([name, value]) => ({ name, value: value as number }));
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c}次 ({d}%)" },
    legend: { bottom: 0 },
    series: [{
      type: "pie", radius: ["35%", "60%"],
      data: data.length > 0 ? data : [{ name: "暂无数据", value: 1 }],
      label: { formatter: data.length > 0 ? "{b}\n{c}次" : "" }
    }]
  });
};

const initClockInChart = () => {
  if (!clockInChartRef.value) return;
  const chart = echarts.init(clockInChartRef.value);
  charts.push(chart);
  const att = cachedData?.attendance || {};
  const total = cachedData?.employeeTotal || 0;
  const clockedIn = att.clockedIn || 0;
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c}人" },
    series: [{
      type: "gauge", startAngle: 200, endAngle: -20,
      min: 0, max: total || 100,
      progress: { show: true, width: 16 },
      axisLine: { lineStyle: { width: 16 } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      detail: { valueAnimation: true, formatter: `{c}/${total}`, fontSize: 20, offsetCenter: [0, "0%"] },
      data: [{ value: clockedIn, name: "已打卡" }],
      title: { offsetCenter: [0, "30%"], fontSize: 14, color: "#909399" }
    }]
  });
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { destroyCharts(); window.removeEventListener("resize", handleResize); });
</script>
