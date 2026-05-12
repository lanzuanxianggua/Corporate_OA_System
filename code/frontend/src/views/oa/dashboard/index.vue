<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getDashboardStats } from "@/api/oa/statistics";

defineOptions({ name: "OaDashboard" });

const stats = ref<any>({});
const loading = ref(true);

onMounted(async () => {
  try {
    const res = await getDashboardStats({ period: "today" });
    if (res.data) {
      stats.value = res.data;
    }
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="dashboard-container" v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>员工总数</template>
          <div class="stat-number">{{ stats.employeeTotal || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日出勤</template>
          <div class="stat-number" style="color: #67C23A">{{ stats.attendance?.clockedIn || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>待审批请假</template>
          <div class="stat-number" style="color: #E6A23C">{{ stats.leave?.pending || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日迟到</template>
          <div class="stat-number" style="color: #F56C6C">{{ stats.attendance?.late || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>部门分布</template>
          <div v-if="stats.departmentDistribution?.length">
            <div v-for="dept in stats.departmentDistribution" :key="dept.name" class="dept-bar">
              <span class="dept-name">{{ dept.name }}</span>
              <el-progress :percentage="dept.value" :stroke-width="16" />
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>请假统计</template>
          <div v-if="stats.leave?.byType" class="leave-stats">
            <div class="leave-stat-item">
              <span>请假总数</span>
              <strong>{{ stats.leave.total || 0 }}</strong>
            </div>
            <div class="leave-stat-item">
              <span>已通过</span>
              <strong style="color: #67C23A">{{ stats.leave.approved || 0 }}</strong>
            </div>
            <div class="leave-stat-item">
              <span>待审批</span>
              <strong style="color: #E6A23C">{{ stats.leave.pending || 0 }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>出勤趋势（近7天）</template>
      <div v-if="stats.attendanceTrend?.length" class="trend-list">
        <div v-for="item in stats.attendanceTrend" :key="item.date" class="trend-item">
          <span class="trend-date">{{ item.date }}</span>
          <span class="trend-rate">{{ item.rate }}%</span>
        </div>
      </div>
      <el-empty v-else description="暂无数据" />
    </el-card>
  </div>
</template>

<style scoped>
.stat-number { font-size: 36px; font-weight: bold; text-align: center; padding: 20px 0; }
.dept-bar { margin-bottom: 12px; }
.dept-name { display: inline-block; width: 80px; font-size: 14px; }
.leave-stats { display: flex; gap: 32px; justify-content: center; padding: 20px; }
.leave-stat-item { text-align: center; }
.leave-stat-item span { display: block; font-size: 13px; color: #999; margin-bottom: 8px; }
.leave-stat-item strong { font-size: 24px; }
.trend-list { display: flex; gap: 16px; flex-wrap: wrap; }
.trend-item { display: flex; gap: 8px; padding: 8px 16px; background: #f5f7fa; border-radius: 4px; }
.trend-date { color: #666; }
.trend-rate { color: #409EFF; font-weight: bold; }
</style>
