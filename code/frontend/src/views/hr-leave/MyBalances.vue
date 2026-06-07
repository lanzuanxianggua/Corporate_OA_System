<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">我的假期余额</span>
          <div class="flex items-center gap-2">
            <el-button type="primary" size="small" :loading="loading" @click="fetchBalances">刷新</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && balances.length === 0" description="暂无假期余额" :image-size="80" />
        <el-row v-else :gutter="20">
          <el-col v-for="b in balances" :key="b.id ?? b.leaveType" :xs="24" :sm="12" :md="8" :lg="6" class="mb-4">
            <el-card shadow="hover" class="balance-card">
              <div class="flex items-center justify-between">
                <span class="text-base font-semibold text-[#303133]">{{ leaveTypeLabel(b.leaveType) }}</span>
                <el-tag :type="balanceStatusType(b.status)" size="small">{{ balanceStatusLabel(b.status) }}</el-tag>
              </div>
              <div class="mt-2 text-3xl font-bold text-[#409eff] leading-none">
                {{ b.remainingDays ?? 0 }}
                <span class="text-sm font-normal text-[#909399]">天剩余</span>
              </div>
              <el-progress
                class="mt-3"
                :percentage="usagePercent(b)"
                :stroke-width="8"
                :color="usagePercent(b) > 80 ? '#f56c6c' : usagePercent(b) > 50 ? '#e6a23c' : '#67c23a'"
              />
              <div class="mt-2 text-xs text-[#909399] flex justify-between">
                <span>总额度 {{ b.totalDays ?? 0 }} 天</span>
                <span>已用 {{ b.usedDays ?? 0 }} 天</span>
              </div>
              <div v-if="b.frozenDays && b.frozenDays > 0" class="mt-1 text-xs text-[#e6a23c]">
                冻结中(审批中) {{ b.frozenDays }} 天
              </div>
              <div v-if="b.year" class="mt-1 text-xs text-[#c0c4cc]">年度 {{ b.year }}</div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { hrLeaveApi, LEAVE_TYPE_MAP_V2, LEAVE_STATUS_MAP_V2 } from "@/api/hr-leave";
import type { HrLeaveBalance } from "@/api/hr-leave";

const loading = ref(false);
const balances = ref<HrLeaveBalance[]>([]);

const fetchBalances = async () => {
  loading.value = true;
  try {
    const data = await hrLeaveApi.getBalances();
    balances.value = Array.isArray(data) ? data : [];
  } catch {
    ElMessage.error("获取假期余额失败");
  } finally {
    loading.value = false;
  }
};

const leaveTypeLabel = (type?: string) => (type ? LEAVE_TYPE_MAP_V2[type] || type : "其他");

const balanceStatusLabel = (status?: string) => {
  if (!status) return "正常";
  if (status === "ACTIVE") return "正常";
  if (status === "FROZEN") return "冻结";
  if (status === "DEPLETED") return "已用完";
  return status;
};

const balanceStatusType = (status?: string): "success" | "warning" | "danger" | "info" => {
  if (status === "ACTIVE") return "success";
  if (status === "FROZEN") return "warning";
  if (status === "DEPLETED") return "danger";
  return "info";
};

const usagePercent = (b: HrLeaveBalance) => {
  const total = Number(b.totalDays ?? 0);
  const used = Number(b.usedDays ?? 0);
  if (total <= 0) return 0;
  return Math.min(100, Math.round((used / total) * 100));
};

// Silence unused warning for shared map (kept for future extension)
void LEAVE_STATUS_MAP_V2;

onMounted(fetchBalances);
</script>

<style scoped>
.balance-card {
  border-radius: 8px;
}
</style>
