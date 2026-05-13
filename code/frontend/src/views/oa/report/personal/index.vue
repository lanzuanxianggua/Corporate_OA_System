<template>
  <div class="report-personal-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人报表</span>
          <el-date-picker v-model="month" type="month" placeholder="选择月份" style="width: 200px" />
        </div>
      </template>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #e6f7ff">
            <el-icon size="24" color="#409EFF"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">22</span>
            <span class="stat-label">出勤天数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #fff7e6">
            <el-icon size="24" color="#E6A23C"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">1</span>
            <span class="stat-label">迟到次数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #fff1f0">
            <el-icon size="24" color="#F56C6C"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">0</span>
            <span class="stat-label">早退次数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #f9f0ff">
            <el-icon size="24" color="#9254de"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">1</span>
            <span class="stat-label">请假天数</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="chart-card">
      <template #header>
        <span>出勤趋势</span>
      </template>
      <div ref="trendChartRef" style="height: 300px"></div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>请假统计</span>
          </template>
          <div ref="leaveChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>月度对比</span>
          </template>
          <div ref="compareChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import * as echarts from "echarts";

const month = ref("");
const trendChartRef = ref<HTMLElement>();
const leaveChartRef = ref<HTMLElement>();
const compareChartRef = ref<HTMLElement>();

onMounted(() => {
  initTrendChart();
  initLeaveChart();
  initCompareChart();
});

const initTrendChart = () => {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["出勤天数"] },
    xAxis: { type: "category", data: ["1月", "2月", "3月", "4月", "5月", "6月"] },
    yAxis: { type: "value", max: 31 },
    series: [{
      name: "出勤天数",
      type: "line",
      smooth: true,
      data: [22, 20, 23, 21, 22, 0],
      areaStyle: { color: "rgba(64, 158, 255, 0.2)" },
      lineStyle: { color: "#409EFF" },
      itemStyle: { color: "#409EFF" }
    }]
  });
};

const initLeaveChart = () => {
  if (!leaveChartRef.value) return;
  const chart = echarts.init(leaveChartRef.value);
  chart.setOption({
    tooltip: { trigger: "item" },
    series: [{
      type: "pie",
      radius: ["40%", "70%"],
      data: [
        { value: 1, name: "事假", itemStyle: { color: "#E6A23C" } },
        { value: 1, name: "病假", itemStyle: { color: "#67C23A" } },
        { value: 3, name: "年假", itemStyle: { color: "#409EFF" } }
      ]
    }]
  });
};

const initCompareChart = () => {
  if (!compareChartRef.value) return;
  const chart = echarts.init(compareChartRef.value);
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["本月", "上月"] },
    xAxis: { type: "category", data: ["正常出勤", "迟到", "缺勤"] },
    yAxis: { type: "value" },
    series: [
      { name: "本月", type: "bar", data: [20, 1, 0], itemStyle: { color: "#409EFF" } },
      { name: "上月", type: "bar", data: [19, 2, 0], itemStyle: { color: "#67C23A" } }
    ]
  });
};
</script>

<style scoped lang="scss">
.report-personal-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stats-row {
    margin: 20px 0;
  }

  .stat-card {
    background: #ffffff;
    border-radius: 8px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }

  .chart-card {
    margin-top: 20px;
  }
}
</style>