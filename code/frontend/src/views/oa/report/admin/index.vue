<template>
  <div class="report-admin-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理员报表</span>
          <el-date-picker v-model="month" type="month" placeholder="选择月份" style="width: 200px" />
        </div>
      </template>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #e6f7ff">
            <el-icon size="24" color="#409EFF"><User /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">156</span>
            <span class="stat-label">总人数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #f6ffed">
            <el-icon size="24" color="#67C23A"><DataLine /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">91%</span>
            <span class="stat-label">平均出勤率</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #fff7e6">
            <el-icon size="24" color="#E6A23C"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">8</span>
            <span class="stat-label">请假人数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #fff1f0">
            <el-icon size="24" color="#F56C6C"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">3</span>
            <span class="stat-label">迟到人数</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card class="chart-card">
      <template #header>
        <span>全员出勤趋势</span>
      </template>
      <div ref="trendChartRef" style="height: 250px"></div>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>部门出勤对比</span>
          </template>
          <div ref="deptChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>请假分析</span>
          </template>
          <div ref="leaveChartRef" style="height: 250px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>员工出勤排名</span>
          <el-radio-group v-model="rankType">
            <el-radio-button label="best">最佳排名</el-radio-button>
            <el-radio-button label="worst">最差排名</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="rankList" stripe>
        <el-table-column prop="rank" label="排名" width="80">
          <template #default="{ $index }">
            <span :class="getRankClass($index)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="员工姓名" />
        <el-table-column prop="dept" label="部门" />
        <el-table-column prop="rate" label="出勤率" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import * as echarts from "echarts";

const month = ref("");
const rankType = ref("best");
const trendChartRef = ref<HTMLElement>();
const deptChartRef = ref<HTMLElement>();
const leaveChartRef = ref<HTMLElement>();

const rankList = ref([
  { rank: 1, name: "张三", dept: "技术部", rate: "100%" },
  { rank: 2, name: "李四", dept: "人事部", rate: "98%" },
  { rank: 3, name: "王五", dept: "财务部", rate: "97%" },
  { rank: 4, name: "赵六", dept: "市场部", rate: "95%" },
  { rank: 5, name: "钱七", dept: "技术部", rate: "93%" }
]);

const getRankClass = (index: number) => {
  if (index === 0) return "rank-gold";
  if (index === 1) return "rank-silver";
  if (index === 2) return "rank-bronze";
  return "";
};

onMounted(() => {
  initTrendChart();
  initDeptChart();
  initLeaveChart();
});

const initTrendChart = () => {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["出勤率"] },
    xAxis: { type: "category", data: ["1月", "2月", "3月", "4月", "5月"] },
    yAxis: { type: "value", max: 100, axisLabel: { formatter: "{value}%" } },
    series: [{
      name: "出勤率",
      type: "line",
      smooth: true,
      data: [92, 89, 94, 91, 91],
      areaStyle: { color: "rgba(103, 194, 58, 0.2)" },
      lineStyle: { color: "#67C23A" },
      itemStyle: { color: "#67C23A" }
    }]
  });
};

const initDeptChart = () => {
  if (!deptChartRef.value) return;
  const chart = echarts.init(deptChartRef.value);
  chart.setOption({
    tooltip: { trigger: "axis" },
    xAxis: { type: "category", data: ["技术部", "市场部", "人事部", "财务部"] },
    yAxis: { type: "value", max: 100, axisLabel: { formatter: "{value}%" } },
    series: [{
      type: "bar",
      data: [
        { value: 92, itemStyle: { color: "#409EFF" } },
        { value: 88, itemStyle: { color: "#E6A23C" } },
        { value: 95, itemStyle: { color: "#67C23A" } },
        { value: 90, itemStyle: { color: "#9254de" } }
      ]
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
        { value: 5, name: "事假", itemStyle: { color: "#E6A23C" } },
        { value: 3, name: "病假", itemStyle: { color: "#67C23A" } },
        { value: 8, name: "年假", itemStyle: { color: "#409EFF" } },
        { value: 2, name: "其他", itemStyle: { color: "#909399" } }
      ]
    }]
  });
};
</script>

<style scoped lang="scss">
.report-admin-container {
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

  .rank-gold {
    color: #ffd700;
    font-weight: bold;
  }

  .rank-silver {
    color: #c0c0c0;
    font-weight: bold;
  }

  .rank-bronze {
    color: #cd7f32;
    font-weight: bold;
  }
}
</style>