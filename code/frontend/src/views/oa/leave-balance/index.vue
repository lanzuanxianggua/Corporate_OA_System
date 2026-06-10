<template>
  <div class="h-full">
    <el-card shadow="never" class="leave-balance-card h-full">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">假期余额</span>
          <span class="text-sm text-[var(--oa-muted)]">{{ userStore.isAdmin() ? "全员假期余额" : "我的假期余额" }}</span>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        height="100%"
        class="leave-balance-table"
        :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }"
      >
        <template #empty>
          <el-empty description="暂无假期余额记录" :image-size="72" />
        </template>
        <el-table-column prop="empName" label="员工" width="100" />
        <el-table-column prop="leaveType" label="假期类型" width="100">
          <template #default="{ row }">{{ leaveTypeMap[row.leaveType] || "其他" }}</template>
        </el-table-column>
        <el-table-column prop="totalDays" label="总额(天)" width="100" align="center" />
        <el-table-column prop="usedDays" label="已用(天)" width="100" align="center" />
        <el-table-column label="剩余(天)" width="100" align="center">
          <template #default="{ row }">
            <span :class="(row.totalDays - row.usedDays) <= 0 ? 'text-red-500' : ''">{{ row.totalDays - row.usedDays }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年度" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>

      <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getMyBalances, getBalancePage } from "@/api/leaveBalance";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const leaveTypeMap: Record<number, string> = { 1: "事假", 2: "病假", 3: "年假", 4: "婚假", 5: "丧假", 6: "产假" };

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res = userStore.isAdmin()
      ? await getBalancePage({ pageNum: pageNum.value, pageSize: pageSize.value })
      : await getMyBalances();
    const data: any = res;
    tableData.value = data.data?.list || data.data?.records || data.data || [];
    total.value = data.data?.total ?? tableData.value.length;
  } finally {
    loading.value = false;
  }
};

onMounted(() => { fetchList(); });
</script>

<style scoped>
.leave-balance-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: calc(100% - 57px);
  min-height: 0;
}

.leave-balance-table {
  flex: 1;
  min-height: 0;
  width: 100%;
}
</style>
