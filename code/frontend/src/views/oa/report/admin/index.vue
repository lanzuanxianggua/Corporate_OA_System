<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <el-date-picker v-model="month" type="month" placeholder="选择月份" format="YYYY年MM月" value-format="YYYY-MM" @change="fetchAllData" />
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

    <el-card class="mb-5">
      <template #header><span class="font-medium">全员出勤趋势（近12个月）</span></template>
      <div ref="trendChartRef" style="height: 300px"></div>
    </el-card>

    <el-row :gutter="20" class="mb-5">
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">部门出勤对比</span></template>
          <div ref="deptChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="font-medium">请假分析</span></template>
          <div ref="leaveChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-medium">员工出勤排名</span>
          <el-radio-group v-model="rankType" size="small" @change="fetchRanking">
            <el-radio-button value="best">最佳排名</el-radio-button>
            <el-radio-button value="worst">最差排名</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="rankingList" stripe>
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <span :style="{ color: rankColors[$index] || '#303133', fontWeight: $index < 3 ? 'bold' : 'normal' }">
              {{ $index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="员工姓名" prop="empName" />
        <el-table-column label="部门" prop="deptName" />
        <el-table-column label="出勤率">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.rate || 0)" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from "vue";
import dayjs from "dayjs";
import * as echarts from "echarts";
import {
  getAdminAttendanceSummary, getAdminAttendanceTrend,
  getAdminDeptCompare, getAdminLeaveAnalysis, getAdminEmployeeRanking
} from "@/api/report";

const month = ref(dayjs().format("YYYY-MM"));
const trendChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const leaveChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];
const rankType = ref("best");
const rankingList = ref<any[]>([]);
const rankColors = ["#FFD700", "#C0C0C0", "#CD7F32"];

const statsCards = reactive([
  { label: "总记录数", value: "0", icon: "User", color: "#409EFF", bg: "#e6f7ff" },
  { label: "平均出勤率", value: "0%", icon: "TrendCharts", color: "#67C23A", bg: "#f6ffed" },
  { label: "迟到人次", value: "0", icon: "WarningFilled", color: "#E6A23C", bg: "#fff7e6" },
  { label: "缺勤人次", value: "0", icon: "CircleClose", color: "#F56C6C", bg: "#fef0f0" }
]);

const fetchAllData = async () => {
  try {
    const r: any = await getAdminAttendanceSummary(month.value);
    if (r.data) {
      statsCards[0].value = String(r.data.totalRecords || 0);
      statsCards[1].value = (r.data.avgAttendanceRate != null ? r.data.avgAttendanceRate.toFixed(1) + "%" : "0%");
      statsCards[2].value = String(r.data.lateCount || 0);
      statsCards[3].value = String(r.data.absentCount || 0);
    }
  } catch {}
  await nextTick();
  initTrendChart();
  initDeptChart();
  initLeaveChart();
  fetchRanking();
};

const initTrendChart = async () => {
  if (!trendChartRef.value) return;
  try {
    const r: any = await getAdminAttendanceTrend(month.value, 12);
    const chart = echarts.init(trendChartRef.value); charts.push(chart);
    const data = r.data || [];
    const normalCountMap: Record<string, number> = {};
    (data as any[]).forEach((d: any) => { normalCountMap[d.month || d.date || ""] = d.normalCount || 0; });
    chart.setOption({
      tooltip: { trigger: "axis", formatter: (params: any) => {
        const p = params[0];
        return `${p.name}<br/>出勤率: ${p.value}%<br/>正常次数: ${normalCountMap[p.name] || 0}`;
      } },
      xAxis: { type: "category", data: data.map((d: any) => d.month || d.date || "") },
      yAxis: { type: "value", name: "出勤率(%)", max: 100 },
      series: [{ type: "line", data: data.map((d: any) => d.rate || 0), areaStyle: { opacity: 0.3, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: "#409EFF" }, { offset: 1, color: "rgba(64,158,255,0.1)" }]) }, smooth: true, itemStyle: { color: "#409EFF" }, lineStyle: { width: 3 } }]
    });
  } catch {}
};

const initDeptChart = async () => {
  if (!deptChartRef.value) return;
  try {
    const r: any = await getAdminDeptCompare(month.value);
    const chart = echarts.init(deptChartRef.value); charts.push(chart);
    const data = r.data || [];
    chart.setOption({
      tooltip: { trigger: "axis" },
      xAxis: { type: "category", data: data.map((d: any) => d.deptName || d.name || "") },
      yAxis: { type: "value", name: "出勤率%", max: 100 },
      series: [{ type: "bar", data: data.map((d: any) => d.rate || d.value || 0), itemStyle: { color: "#409EFF" } }]
    });
  } catch {}
};

const initLeaveChart = async () => {
  if (!leaveChartRef.value) return;
  try {
    const r: any = await getAdminLeaveAnalysis(month.value);
    const chart = echarts.init(leaveChartRef.value); charts.push(chart);
    chart.setOption({
      tooltip: { trigger: "item", formatter: "{b}: {c}次 ({d}%)" },
      legend: { bottom: 0, type: "scroll" },
      series: [{
        type: "pie", radius: ["35%", "60%"],
        label: { formatter: "{b}\n{c}次" },
        data: (r.data || []).map((d: any) => {
          const typeMap: Record<string, string> = { "0": "事假", "1": "病假", "2": "年假", "3": "婚假", "4": "产假", "5": "其他" };
          return { name: typeMap[d.type] || d.type || "未知", value: d.count || d.value || 0 };
        }),
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: "rgba(0,0,0,0.2)" } }
      }]
    });
  } catch {}
};

const fetchRanking = async () => {
  try {
    const r: any = await getAdminEmployeeRanking(month.value, rankType.value);
    if (r.data) rankingList.value = r.data;
  } catch {}
};

const handleResize = () => charts.forEach((c) => c.resize());
onMounted(() => { fetchAllData(); window.addEventListener("resize", handleResize); });
onUnmounted(() => { charts.forEach((c) => c.dispose()); window.removeEventListener("resize", handleResize); });
</script>
