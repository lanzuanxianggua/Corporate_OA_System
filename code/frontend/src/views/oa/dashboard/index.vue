<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-bold text-[#303133]">数据看板</span>
      <el-radio-group v-model="period" @change="fetchAllData">
        <el-radio-button value="today">今日</el-radio-button>
        <el-radio-button value="week">本周</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
        <el-radio-button value="year">本年</el-radio-button>
      </el-radio-group>
      <el-date-picker v-if="period === 'year'" v-model="selectedYear" type="year" placeholder="选择年份" format="YYYY年" value-format="YYYY" @change="fetchAllData" />
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
          <template #header><span class="font-medium">{{ trendTitle }}</span></template>
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
          <template #header><span class="font-medium">请假统计</span></template>
          <div ref="leaveTypeChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span class="font-medium">{{ periodLabel }}打卡概览</span></template>
          <div ref="clockInChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-5">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">{{ periodLabel }}迟到排行</span></template>
          <div ref="lateRankChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">{{ periodLabel }}出勤排行</span></template>
          <div ref="attendanceRankChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from "vue";
import * as echarts from "echarts";
import { getDashboardStats } from "@/api/statistics";

const period = ref("today");
const selectedYear = ref(new Date().getFullYear().toString());
const trendChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const attStatusChartRef = ref<HTMLDivElement>();
const leaveTypeChartRef = ref<HTMLDivElement>();
const clockInChartRef = ref<HTMLDivElement>();
const lateRankChartRef = ref<HTMLDivElement>();
const attendanceRankChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];
const cachedData = ref<any>(null);

const trendTitle = computed(() => {
  if (period.value === "today") return "今日出勤率";
  if (period.value === "week") return "本周出勤率趋势";
  if (period.value === "year") return "年度出勤率";
  return "本月出勤率趋势";
});

const periodLabel = computed(() => {
  if (period.value === "today") return "今日";
  if (period.value === "week") return "本周";
  if (period.value === "year") return "本年";
  return "本月";
});

const statsCards = computed(() => {
  const d = cachedData.value;
  if (!d) {
    return [
      { label: "总人数", value: "0", color: "#409EFF" },
      { label: "今日已打卡", value: "0", color: "#67C23A" },
      { label: "未打卡", value: "0", color: "#909399" },
      { label: "今日迟到", value: "0", color: "#E6A23C" },
      { label: "今日早退", value: "0", color: "#F56C6C" },
      { label: "今日请假", value: "0", color: "#9254de" }
    ];
  }
  const pl = periodLabel.value;
  const att = d.attendance || {};
  const clockedIn = Number(att.clockedIn) || 0;
  const total = Number(d.employeeTotal) || 0;
  const notClocked = period.value === "today" ? Math.max(0, total - clockedIn) : (Number(att.absent) || 0);
  return [
    { label: "总人数", value: String(total), color: "#409EFF" },
    { label: `${pl}已打卡`, value: String(clockedIn), color: "#67C23A" },
    { label: period.value === "today" ? "未打卡" : `${pl}缺勤`, value: String(notClocked), color: "#909399" },
    { label: `${pl}迟到`, value: String(Number(att.late) || 0), color: "#E6A23C" },
    { label: `${pl}早退`, value: String(Number(att.earlyLeave) || 0), color: "#F56C6C" },
    { label: `${pl}请假`, value: String(Number((d.leave || {}).total) || 0), color: "#9254de" }
  ];
});

const destroyCharts = () => {
  charts.forEach((c) => c.dispose());
  charts.length = 0;
};

const fetchAllData = async () => {
  destroyCharts();
  try {
    const yearParam = period.value === 'year' ? Number(selectedYear.value) : undefined;
    const r: any = await getDashboardStats(period.value, yearParam);
    if (r.data) {
      cachedData.value = r.data;
    }
  } catch {}
  await nextTick();
  initTrendChart();
  initDeptChart();
  initAttStatusChart();
  initLeaveTypeChart();
  initClockInChart();
  initLateRankChart();
  initAttendanceRankChart();
};

const initTrendChart = () => {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  charts.push(chart);

  if (period.value === 'year') {
    const trend = cachedData.value?.yearlyAttendanceTrend || [];
    chart.setOption({
      tooltip: { trigger: "axis", formatter: "{b}: {c}%" },
      xAxis: { type: "category", data: trend.map((d: any) => d.month || "") },
      yAxis: { type: "value", name: "出勤率(%)", max: 100 },
      series: [{
        type: "bar",
        data: trend.map((d: any) => d.rate || 0),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "#409EFF" },
            { offset: 1, color: "#79bbff" }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: "40%"
      }]
    });
    return;
  }

  const trend = cachedData.value?.attendanceTrend || [];
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
  const dept = cachedData.value?.departmentDistribution || [];
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
  const att = cachedData.value?.attendance || {};
  chart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c}人次 ({d}%)" },
    legend: { bottom: 0 },
    color: ["#67C23A", "#E6A23C", "#F56C6C", "#909399"],
    series: [{
      type: "pie", radius: "60%",
      data: [
        { value: Number(att.clockedIn) || 0, name: "正常出勤" },
        { value: Number(att.late) || 0, name: "迟到" },
        { value: Number(att.earlyLeave) || 0, name: "早退" },
        { value: Number(att.absent) || 0, name: "缺勤" }
      ].filter(d => d.value > 0)
    }]
  });
};

const initLeaveTypeChart = () => {
  if (!leaveTypeChartRef.value) return;
  const chart = echarts.init(leaveTypeChartRef.value);
  charts.push(chart);
  const lv = cachedData.value?.leave || {};
  const byType = lv.byType || {};
  const names = Object.keys(byType);
  const values = Object.values(byType).map((v: any) => Number(v) || 0);

  if (names.length === 0) {
    chart.setOption({ title: { text: "暂无请假数据", left: "center", top: "center", textStyle: { color: "#c0c4cc", fontSize: 14 } } });
    return;
  }

  chart.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: names },
    yAxis: { type: "value", name: "次数" },
    series: [{
      type: "bar",
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "#9254de" },
          { offset: 1, color: "#b37feb" }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: "40%"
    }]
  });
};

const initClockInChart = () => {
  if (!clockInChartRef.value) return;
  const chart = echarts.init(clockInChartRef.value);
  charts.push(chart);
  const att = cachedData.value?.attendance || {};
  const clockedIn = Number(att.clockedIn) || 0;
  const totalRequired = Number(att.totalRequired) || 100;
  const unit = period.value === "today" ? "人" : "人次";
  chart.setOption({
    tooltip: { trigger: "item", formatter: `{b}: {c}${unit}` },
    series: [{
      type: "gauge", startAngle: 200, endAngle: -20,
      min: 0, max: totalRequired,
      progress: { show: true, width: 16 },
      axisLine: { lineStyle: { width: 16 } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      pointer: { show: false },
      detail: { valueAnimation: true, formatter: (val: number) => `${val}/${totalRequired}`, fontSize: 20, offsetCenter: [0, "0%"] },
      data: [{ value: clockedIn, name: "已打卡" }],
      title: { offsetCenter: [0, "30%"], fontSize: 14, color: "#909399" }
    }]
  });
};

const initLateRankChart = () => {
  if (!lateRankChartRef.value) return;
  const chart = echarts.init(lateRankChartRef.value);
  charts.push(chart);
  const ranking = cachedData.value?.lateRanking || [];
  if (ranking.length === 0) {
    chart.setOption({ title: { text: "暂无迟到记录", left: "center", top: "center", textStyle: { color: "#c0c4cc", fontSize: 14 } } });
    return;
  }
  const names = ranking.map((r: any) => r.empName || "").reverse();
  const values = ranking.map((r: any) => r.lateCount || 0).reverse();
  chart.setOption({
    tooltip: { trigger: "axis", formatter: "{b}: {c}次" },
    grid: { left: 80, right: 30, top: 10, bottom: 30 },
    xAxis: { type: "value", name: "迟到次数" },
    yAxis: { type: "category", data: names },
    series: [{
      type: "bar",
      data: values,
      itemStyle: { color: "#E6A23C", borderRadius: [0, 4, 4, 0] }
    }]
  });
};

const initAttendanceRankChart = () => {
  if (!attendanceRankChartRef.value) return;
  const chart = echarts.init(attendanceRankChartRef.value);
  charts.push(chart);
  const ranking = cachedData.value?.attendanceRanking || [];
  if (ranking.length === 0) {
    chart.setOption({ title: { text: "暂无出勤记录", left: "center", top: "center", textStyle: { color: "#c0c4cc", fontSize: 14 } } });
    return;
  }
  const names = ranking.map((r: any) => r.empName || "").reverse();
  const values = ranking.map((r: any) => r.rate || 0).reverse();
  chart.setOption({
    tooltip: { trigger: "axis", formatter: "{b}: {c}%" },
    grid: { left: 80, right: 30, top: 10, bottom: 30 },
    xAxis: { type: "value", name: "出勤率(%)", max: 100 },
    yAxis: { type: "category", data: names },
    series: [{
      type: "bar",
      data: values,
      itemStyle: { color: "#67C23A", borderRadius: [0, 4, 4, 0] }
    }]
  });
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { destroyCharts(); window.removeEventListener("resize", handleResize); });
</script>
