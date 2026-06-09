<template>
  <section class="oa-analytics-page">
    <header class="oa-page-header">
      <div>
        <div class="oa-eyebrow">Admin Report</div>
        <h1 class="oa-page-title">管理考勤报表</h1>
        <p class="oa-page-subtitle">面向管理者的全员出勤、部门对比、请假与排名分析</p>
      </div>
      <div class="oa-header-actions">
        <el-date-picker
          v-model="month"
          type="month"
          placeholder="选择月份"
          format="YYYY年MM月"
          value-format="YYYY-MM"
          :clearable="false"
          @change="fetchAllData"
        />
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
      <article class="oa-panel oa-col-12">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">全员出勤趋势</h2>
            <p class="oa-panel-subtitle">近 12 个月全员出勤率变化</p>
          </div>
        </div>
        <div ref="trendChartRef" class="oa-chart oa-chart-tall"></div>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">部门出勤对比</h2>
            <p class="oa-panel-subtitle">各部门月度出勤率</p>
          </div>
        </div>
        <div ref="deptChartRef" class="oa-chart"></div>
      </article>

      <article class="oa-panel oa-col-6">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">请假分析</h2>
            <p class="oa-panel-subtitle">不同请假类型占比</p>
          </div>
        </div>
        <div ref="leaveChartRef" class="oa-chart"></div>
      </article>

      <article class="oa-panel oa-col-12">
        <div class="oa-panel-header">
          <div>
            <h2 class="oa-panel-title">员工出勤排名</h2>
            <p class="oa-panel-subtitle">按员工出勤率查看最佳或待跟进名单</p>
          </div>
          <el-radio-group v-model="rankType" size="small" @change="fetchRanking">
            <el-radio-button value="best">最佳排名</el-radio-button>
            <el-radio-button value="worst">待跟进排名</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="rankingList" stripe>
          <el-table-column label="排名" width="90">
            <template #default="{ $index }">
              <span class="rank-index" :style="{ color: rankColors[$index] || '#111827' }">
                {{ $index + 1 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="员工姓名" prop="empName" min-width="140" />
          <el-table-column label="部门" prop="deptName" min-width="140" />
          <el-table-column label="出勤率" min-width="220">
            <template #default="{ row }">
              <el-progress
                :percentage="Number(row.rate || 0)"
                :stroke-width="10"
                :color="Number(row.rate || 0) >= 90 ? '#059669' : Number(row.rate || 0) >= 75 ? '#d97706' : '#dc2626'"
              />
            </template>
          </el-table-column>
        </el-table>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref } from "vue";
import type { Component } from "vue";
import dayjs from "dayjs";
import * as echarts from "echarts";
import { CircleClose, TrendCharts, User, WarningFilled } from "@element-plus/icons-vue";
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
import {
  getAdminAttendanceSummary,
  getAdminAttendanceTrend,
  getAdminDeptCompare,
  getAdminEmployeeRanking,
  getAdminLeaveAnalysis
} from "@/api/report";

interface StatCard {
  label: string;
  value: string;
  note: string;
  icon: Component;
  color: string;
  bgColor: string;
}

const month = ref(dayjs().format("YYYY-MM"));
const trendChartRef = ref<HTMLDivElement>();
const deptChartRef = ref<HTMLDivElement>();
const leaveChartRef = ref<HTMLDivElement>();
const charts: echarts.ECharts[] = [];
const rankType = ref("best");
const rankingList = ref<any[]>([]);
const rankColors = ["#d97706", "#64748b", "#a16207"];

const statsCards = reactive<StatCard[]>([
  { label: "总记录数", value: "0", note: "当月考勤记录", icon: User, color: "#2563eb", bgColor: "#eff6ff" },
  { label: "平均出勤率", value: "0%", note: "全员平均水平", icon: TrendCharts, color: "#059669", bgColor: "#ecfdf5" },
  { label: "迟到人次", value: "0", note: "迟到异常数量", icon: WarningFilled, color: "#d97706", bgColor: "#fff7ed" },
  { label: "缺勤人次", value: "0", note: "缺勤异常数量", icon: CircleClose, color: "#dc2626", bgColor: "#fef2f2" }
]);

function destroyCharts() {
  charts.forEach((chart) => chart.dispose());
  charts.length = 0;
}

async function fetchAllData() {
  destroyCharts();
  try {
    const response: any = await getAdminAttendanceSummary(month.value);
    if (response.data) {
      statsCards[0].value = String(response.data.totalRecords || 0);
      statsCards[1].value = response.data.avgAttendanceRate != null ? `${Number(response.data.avgAttendanceRate).toFixed(1)}%` : "0%";
      statsCards[2].value = String(response.data.lateCount || 0);
      statsCards[3].value = String(response.data.absentCount || 0);
    }
  } catch {}

  await nextTick();
  await Promise.all([initTrendChart(), initDeptChart(), initLeaveChart(), fetchRanking()]);
}

async function initTrendChart() {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getAdminAttendanceTrend(month.value, 12);
    const data = (response.data || []) as any[];
    if (!data.length) {
      chart.setOption(emptyChartOption("暂无全员出勤趋势数据"));
      return;
    }

    const normalCountMap: Record<string, number> = {};
    data.forEach((item) => {
      normalCountMap[item.month || item.date || ""] = Number(item.normalCount) || 0;
    });

    chart.setOption({
      textStyle: chartTextStyle(),
      color: chartPalette,
      tooltip: {
        ...axisTooltip(),
        formatter: (params: any) => {
          const item = params[0];
          return `${item.name}<br/>出勤率：${item.value}%<br/>正常次数：${normalCountMap[item.name] || 0}`;
        }
      },
      grid: chartGrid(42),
      xAxis: { type: "category", data: data.map((item) => item.month || item.date || ""), ...axisStyle() },
      yAxis: { type: "value", name: "出勤率(%)", max: 100, ...axisStyle() },
      series: [
        {
          name: "出勤率",
          type: "line",
          data: data.map((item) => Number(item.rate) || 0),
          smooth: true,
          symbolSize: 7,
          lineStyle: { width: 3, color: "#2563eb" },
          itemStyle: { color: "#2563eb" },
          areaStyle: { color: createGradient("rgba(37, 99, 235, .24)", "rgba(37, 99, 235, .04)") }
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("全员趋势加载失败"));
  }
}

async function initDeptChart() {
  if (!deptChartRef.value) return;
  const chart = echarts.init(deptChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getAdminDeptCompare(month.value);
    const data = (response.data || []) as any[];
    if (!data.length) {
      chart.setOption(emptyChartOption("暂无部门数据"));
      return;
    }

    chart.setOption({
      textStyle: chartTextStyle(),
      color: chartPalette,
      tooltip: axisTooltip(),
      grid: chartGrid(42),
      xAxis: { type: "category", data: data.map((item) => item.deptName || item.name || ""), ...axisStyle() },
      yAxis: { type: "value", name: "出勤率(%)", max: 100, ...axisStyle() },
      series: [
        {
          name: "出勤率",
          type: "bar",
          barWidth: 26,
          data: data.map((item) => Number(item.rate || item.value) || 0),
          itemStyle: { color: "#2563eb", borderRadius: [6, 6, 0, 0] },
          label: { show: true, position: "top", color: "#111827", fontWeight: 700, formatter: "{c}%" }
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("部门对比加载失败"));
  }
}

async function initLeaveChart() {
  if (!leaveChartRef.value) return;
  const chart = echarts.init(leaveChartRef.value);
  charts.push(chart);

  try {
    const response: any = await getAdminLeaveAnalysis(month.value);
    const typeMap: Record<string, string> = {
      "0": "事假",
      "1": "病假",
      "2": "年假",
      "3": "婚假",
      "4": "产假",
      "5": "其他"
    };
    const data = ((response.data || []) as any[])
      .map((item) => ({
        name: typeMap[item.type] || item.type || item.name || "未知类型",
        value: Number(item.count || item.value) || 0
      }))
      .filter((item) => item.value > 0);

    if (!data.length) {
      chart.setOption(emptyChartOption("暂无请假数据"));
      return;
    }

    chart.setOption({
      textStyle: chartTextStyle(),
      color: chartPalette,
      tooltip: itemTooltip("{b}<br/>{c} 次 ({d}%)"),
      legend: { bottom: 0, type: "scroll", icon: "circle", itemWidth: 8, itemHeight: 8, textStyle: { color: "#6b7280" } },
      series: [
        {
          type: "pie",
          radius: ["48%", "70%"],
          center: ["50%", "44%"],
          padAngle: 3,
          itemStyle: { borderColor: "#fff", borderWidth: 4, borderRadius: 8 },
          label: { formatter: "{b}\n{c}次", color: "#374151", fontWeight: 650 },
          data
        }
      ]
    });
  } catch {
    chart.setOption(emptyChartOption("请假分析加载失败"));
  }
}

async function fetchRanking() {
  try {
    const response: any = await getAdminEmployeeRanking(month.value, rankType.value);
    rankingList.value = response.data || [];
  } catch {}
}

function handleResize() {
  charts.forEach((chart) => chart.resize());
}

onMounted(() => {
  fetchAllData();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  destroyCharts();
  window.removeEventListener("resize", handleResize);
});
</script>

<style scoped>
.rank-index {
  font-size: 15px;
  font-weight: 760;
  font-variant-numeric: tabular-nums;
}
</style>
