<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">我的薪资</span>
      </template>

      <div class="mb-4">
        <el-date-picker v-model="month" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="fetchList" />
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="salaryMonth" label="月份" width="100" align="center" />
        <el-table-column prop="baseSalary" label="基本工资" min-width="120" align="right">
          <template #default="{ row }">{{ row.baseSalary?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column prop="postSalary" label="岗位工资" min-width="120" align="right">
          <template #default="{ row }">{{ row.postSalary?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column prop="meritSalary" label="绩效工资" min-width="120" align="right">
          <template #default="{ row }">{{ row.meritSalary?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column prop="allowance" label="津贴" min-width="100" align="right">
          <template #default="{ row }">{{ row.allowance?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column prop="deduction" label="扣款" min-width="100" align="right">
          <template #default="{ row }">{{ row.deduction?.toFixed(2) || "-" }}</template>
        </el-table-column>
        <el-table-column label="实发工资" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-[#409EFF]">{{ row.actualAmount?.toFixed(2) || "-" }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getMySalary } from "@/api/salary";

const loading = ref(false);
const tableData = ref<any[]>([]);
const month = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getMySalary({ month: month.value || undefined });
    tableData.value = res.data?.list || res.data || [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => { fetchList(); });
</script>
