<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">假期余额</span>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
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

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
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
    tableData.value = data.data?.list || data.data || [];
    total.value = data.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

onMounted(() => { fetchList(); });
</script>
