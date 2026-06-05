<script setup lang="ts">
import { ref, onMounted } from 'vue';

const stats = ref({
  pendingApprovals: 0,
  myLeaves: 0,
  balance: 0,
  notifications: 0
});

onMounted(async () => {
  // 占位: 真实实现调用 dashboardApi.getStats()
  // stats.value = await apiGet('/v1/dashboard/stats');
  stats.value = { pendingApprovals: 3, myLeaves: 5, balance: 12.5, notifications: 8 };
});
</script>

<template>
  <div class="dashboard">
    <h2>工作台</h2>
    <el-row :gutter="16" class="mt-16">
      <el-col :span="6">
        <el-card>
          <div class="stat-title">待审批</div>
          <div class="stat-value">{{ stats.pendingApprovals }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-title">我的请假</div>
          <div class="stat-value">{{ stats.myLeaves }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-title">假期余额(天)</div>
          <div class="stat-value">{{ stats.balance }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-title">未读消息</div>
          <div class="stat-value">{{ stats.notifications }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="scss" scoped>
.dashboard {
  .stat-title {
    font-size: 14px;
    color: #666;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 32px;
    font-weight: 600;
    color: #1890ff;
  }
}
</style>
