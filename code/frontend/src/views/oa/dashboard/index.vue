<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <el-radio-group v-model="period" @change="fetchData">
        <el-radio-button value="today">今日</el-radio-button>
        <el-radio-button value="week">本周</el-radio-button>
        <el-radio-button value="month">本月</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="6">
        <div class="bg-white rounded-lg p-5 flex items-center gap-4" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="w-14 h-14 rounded-lg flex items-center justify-center" :style="{ backgroundColor: item.bg }">
            <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">{{ item.value }}</span>
            <span class="text-sm text-[#909399] mt-1">{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">出勤趋势（近7天）</span></template>
          <div ref="barChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">部门人员分布</span></template>
          <div ref="pieChartRef" style="height: 300px"></div>
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
const barChartRef = ref<HTMLDivElement>();
const pieChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];

const statsCards = reactive([
  { label: "总人数", value: "0", icon: "User", color: "#409EFF", bg: "#e6f7ff" },
  { label: "已打卡", value: "0", icon: "CircleCheck", color: "#67C23A", bg: "#f6ffed" },
  { label: "请假人数", value: "0", icon: "Document", color: "#E6A23C", bg: "#fff7e6" },
  { label: "迟到人数", value: "0", icon: "Warning", color: "#9254de", bg: "#f9f0ff" }
]);

const fetchData = async () => {
  try {
    const r: any = await getDashboardStats(period.value);
    if (r.data) {
      statsCards[0].value = String(r.data.employeeTotal || 0);
      const att = r.data.attendance || {};
      statsCards[1].value = String(att.clockedIn || 0);
      const lv = r.data.leave || {};
      statsCards[2].value = String(lv.total || 0);
      statsCards[3].value = String(att.late || 0);
    }
  } catch {}
  await nextTick();
  initBarChart();
  initPieChart();
};

let cachedData: any = null;

const initBarChart = () => {
  if (!barChartRef.value) return;
  const chart = echarts.init(barChartRef.value); charts.push(chart);
  const trend = cachedData?.attendanceTrend || [];
  chart.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: trend.map((d: any) => d.date || "") },
    yAxis: { type: "value", name: "出勤率(%)" },
    series: [{ type: "bar", data: trend.map((d: any) => d.rate || 0), itemStyle: { color: "#409EFF" } }]
  });
};

const initPieChart = () => {
  if (!pieChartRef.value) return;
  const chart = echarts.init(pieChartRef.value); charts.push(chart);
  const dept = cachedData?.departmentDistribution || [];
  chart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0 },
    series: [{ type: "pie", radius: ["40%", "65%"], data: dept.map((d: any) => ({ value: d.value, name: d.name })) }]
  });
};

const fetchAllData = async () => {
  try {
    const r: any = await getDashboardStats(period.value);
    if (r.data) {
      cachedData = r.data;
      statsCards[0].value = String(r.data.employeeTotal || 0);
      const att = r.data.attendance || {};
      statsCards[1].value = String(att.clockedIn || 0);
      const lv = r.data.leave || {};
      statsCards[2].value = String(lv.total || 0);
      statsCards[3].value = String(att.late || 0);
    }
  } catch {}
  await nextTick();
  initBarChart();
  initPieChart();
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { charts.forEach((c) => c.dispose()); window.removeEventListener("resize", handleResize); });
</script>
