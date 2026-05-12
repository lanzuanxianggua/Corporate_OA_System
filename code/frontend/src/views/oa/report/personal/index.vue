<script setup lang="ts">
import { ref, onMounted } from "vue";
import {
  getPersonalAttendanceSummary,
  getPersonalAttendanceTrend,
  getPersonalLeaveSummary,
  getPersonalMonthlyCompare
} from "@/api/oa/report";

defineOptions({ name: "OaReportPersonal" });

const currentMonth = ref(new Date().toISOString().slice(0, 7));
const summary = ref<any>({});
const trend = ref<any[]>([]);
const leaveSummary = ref<any[]>([]);
const compare = ref<any>({});
const loading = ref(true);

async function loadData() {
  loading.value = true;
  try {
    const [summaryRes, trendRes, leaveRes, compareRes] = await Promise.allSettled([
      getPersonalAttendanceSummary({ month: currentMonth.value }),
      getPersonalAttendanceTrend({ months: 6 }),
      getPersonalLeaveSummary({ month: currentMonth.value }),
      getPersonalMonthlyCompare({ month: currentMonth.value })
    ]);
    if (summaryRes.status === "fulfilled" && summaryRes.value.data) summary.value = summaryRes.value.data;
    if (trendRes.status === "fulfilled" && trendRes.value.data) trend.value = trendRes.value.data;
    if (leaveRes.status === "fulfilled" && leaveRes.value.data) leaveSummary.value = leaveRes.value.data;
    if (compareRes.status === "fulfilled" && compareRes.value.data) compare.value = compareRes.value.data;
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);

function onMonthChange() {
  loadData();
}
</script>

<template>
  <div class="personal-report" v-loading="loading">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人报表</span>
          <el-date-picker v-model="currentMonth" type="month" value-format="YYYY-MM" @change="onMonthChange" />
        </div>
      </template>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #67C23A">{{ summary.normalDays || 0 }}</div>
            <div class="stat-label">正常出勤（天）</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #E6A23C">{{ summary.lateDays || 0 }}</div>
            <div class="stat-label">迟到（天）</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #F56C6C">{{ summary.absentDays || 0 }}</div>
            <div class="stat-label">缺勤（天）</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" style="color: #409EFF">{{ summary.attendanceRate || 0 }}%</div>
            <div class="stat-label">出勤率</div>
          </div>
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
              <el-progress :percentage="item.rate" :stroke-width="12" style="flex: 1; margin-left: 8px" />
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>请假统计</template>
          <div v-if="leaveSummary.length">
            <div v-for="item in leaveSummary" :key="item.type" class="leave-item">
              <span>{{ item.type }}</span>
              <el-tag type="warning">{{ item.count }}次</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px" v-if="compare.currentMonthNormal !== undefined">
      <template #header>月度对比</template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="">{{ currentMonth }}</el-descriptions-item>
        <el-descriptions-item label="正常">{{ compare.currentMonthNormal }}天</el-descriptions-item>
        <el-descriptions-item label="迟到">{{ compare.currentMonthLate }}天</el-descriptions-item>
        <el-descriptions-item label="上月"></el-descriptions-item>
        <el-descriptions-item label="正常">{{ compare.lastMonthNormal }}天</el-descriptions-item>
        <el-descriptions-item label="迟到">{{ compare.lastMonthLate }}天</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 32px; font-weight: bold; }
.stat-label { font-size: 13px; color: #999; margin-top: 8px; }
.trend-list { display: flex; flex-direction: column; gap: 12px; }
.trend-item { display: flex; align-items: center; }
.leave-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
</style>
