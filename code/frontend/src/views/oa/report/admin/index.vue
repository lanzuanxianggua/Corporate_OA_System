<script setup lang="ts">
import { ref, onMounted } from "vue";
import {
  getAdminAttendanceSummary,
  getAdminDeptCompare,
  getAdminAttendanceTrend,
  getAdminLeaveAnalysis,
  getAdminEmployeeRanking,
  getAdminTodayOverview
} from "@/api/oa/report";

defineOptions({ name: "OaReportAdmin" });

const currentMonth = ref(new Date().toISOString().slice(0, 7));
const summary = ref<any>({});
const deptCompare = ref<any[]>([]);
const trend = ref<any[]>([]);
const leaveAnalysis = ref<any[]>([]);
const ranking = ref<any[]>([]);
const todayOverview = ref<any>({});
const rankType = ref("best");
const loading = ref(true);

async function loadData() {
  loading.value = true;
  try {
    const [summaryRes, deptRes, trendRes, leaveRes, rankRes, todayRes] = await Promise.allSettled([
      getAdminAttendanceSummary({ month: currentMonth.value }),
      getAdminDeptCompare({ month: currentMonth.value }),
      getAdminAttendanceTrend({ month: currentMonth.value, months: 12 }),
      getAdminLeaveAnalysis({ month: currentMonth.value }),
      getAdminEmployeeRanking({ month: currentMonth.value, type: rankType.value }),
      getAdminTodayOverview()
    ]);
    if (summaryRes.status === "fulfilled" && summaryRes.value.data) summary.value = summaryRes.value.data;
    if (deptRes.status === "fulfilled" && deptRes.value.data) deptCompare.value = deptRes.value.data;
    if (trendRes.status === "fulfilled" && trendRes.value.data) trend.value = trendRes.value.data;
    if (leaveRes.status === "fulfilled" && leaveRes.value.data) leaveAnalysis.value = leaveRes.value.data;
    if (rankRes.status === "fulfilled" && rankRes.value.data) ranking.value = rankRes.value.data;
    if (todayRes.status === "fulfilled" && todayRes.value.data) todayOverview.value = todayRes.value.data;
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);

function onMonthChange() {
  loadData();
}

async function onRankTypeChange() {
  try {
    const res = await getAdminEmployeeRanking({ month: currentMonth.value, type: rankType.value });
    if (res.data) ranking.value = res.data;
  } catch (e) { /* ignore */ }
}
</script>

<template>
  <div class="admin-report" v-loading="loading">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据报表</span>
          <el-date-picker v-model="currentMonth" type="month" value-format="YYYY-MM" @change="onMonthChange" />
        </div>
      </template>
    </el-card>

    <!-- 今日概览 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ todayOverview.totalEmployees || 0 }}</div>
            <div class="stat-label">总员工数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #67C23A">{{ todayOverview.clockedIn || 0 }}</div>
            <div class="stat-label">今日打卡</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #F56C6C">{{ todayOverview.notClockedIn || 0 }}</div>
            <div class="stat-label">未打卡</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #E6A23C">{{ todayOverview.late || 0 }}</div>
            <div class="stat-label">迟到</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度汇总 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>全员考勤汇总</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="总记录数">{{ summary.totalRecords || 0 }}</el-descriptions-item>
            <el-descriptions-item label="平均出勤率">{{ summary.avgAttendanceRate || 0 }}%</el-descriptions-item>
            <el-descriptions-item label="正常">
              <el-tag type="success">{{ summary.normalCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="迟到">
              <el-tag type="warning">{{ summary.lateCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="早退">
              <el-tag type="warning">{{ summary.earlyLeaveCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="缺勤">
              <el-tag type="danger">{{ summary.absentCount || 0 }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>部门出勤对比</template>
          <div v-if="deptCompare.length">
            <div v-for="dept in deptCompare" :key="dept.deptName" class="dept-bar">
              <span class="dept-name">{{ dept.deptName }}</span>
              <el-progress :percentage="dept.rate" :stroke-width="14" style="flex:1; margin-left:8px" />
              <span class="dept-rate">{{ dept.rate }}%</span>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>出勤趋势</template>
          <div v-if="trend.length" class="trend-list">
            <div v-for="item in trend" :key="item.month" class="trend-item">
              <span>{{ item.month }}</span>
              <el-progress :percentage="item.rate" :stroke-width="10" style="flex:1; margin-left:8px" />
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>请假分析</template>
          <div v-if="leaveAnalysis.length">
            <div v-for="item in leaveAnalysis" :key="item.type" class="leave-item">
              <span>{{ item.type }}</span>
              <el-tag type="warning">{{ item.count }}次</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>员工出勤排名</span>
          <el-radio-group v-model="rankType" @change="onRankTypeChange">
            <el-radio-button value="best">最佳</el-radio-button>
            <el-radio-button value="worst">最差</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="ranking" stripe empty-text="暂无数据">
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column prop="empName" label="员工" />
        <el-table-column prop="normalDays" label="正常天数" />
        <el-table-column prop="totalDays" label="总天数" />
        <el-table-column prop="rate" label="出勤率">
          <template #default="{ row }">
            <el-progress :percentage="row.rate" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 32px; font-weight: bold; }
.stat-label { font-size: 13px; color: #999; margin-top: 8px; }
.dept-bar { display: flex; align-items: center; margin-bottom: 12px; }
.dept-name { width: 80px; font-size: 14px; }
.dept-rate { width: 50px; text-align: right; font-size: 13px; margin-left: 8px; }
.trend-list { display: flex; flex-direction: column; gap: 8px; }
.trend-item { display: flex; align-items: center; }
.leave-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
</style>
