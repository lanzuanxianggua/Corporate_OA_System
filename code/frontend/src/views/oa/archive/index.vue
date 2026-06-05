<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">员工档案</span>
          <el-input v-model="searchKey" placeholder="搜索员工姓名" style="width: 240px" clearable @clear="fetchList" @keyup.enter="fetchList">
            <template #append>
              <el-button @click="fetchList">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="empName" label="姓名" min-width="100" />
        <el-table-column prop="empNo" label="工号" min-width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="hireDate" label="入职日期" min-width="110" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDrawer(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" title="员工档案详情" size="450px">
      <template v-if="archiveData">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="姓名">{{ archiveData.empName }}</el-descriptions-item>
          <el-descriptions-item label="工号">{{ archiveData.empNo }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ archiveData.deptName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ archiveData.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ archiveData.email }}</el-descriptions-item>
          <el-descriptions-item label="入职日期">{{ archiveData.entryDate || archiveData.hireDate || "-" }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ archiveData.idCard || "-" }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ archiveData.education || "-" }}</el-descriptions-item>
          <el-descriptions-item label="紧急联系人">{{ archiveData.emergencyContact || "-" }}</el-descriptions-item>
          <el-descriptions-item label="紧急联系电话">{{ archiveData.emergencyPhone || "-" }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ archiveData.remark || "-" }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Search } from "@element-plus/icons-vue";
import { getArchivePage, getArchive } from "@/api/empArchive";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getArchivePage({ pageNum: pageNum.value, pageSize: pageSize.value, searchKey: searchKey.value || undefined });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const drawerVisible = ref(false);
const archiveData = ref<any>(null);

const openDrawer = async (row: any) => {
  try {
    const res: any = await getArchive(row.id || row.empId);
    archiveData.value = { ...row, ...(res.data || {}) };
  } catch {
    archiveData.value = row;
  }
  drawerVisible.value = true;
};

onMounted(() => { fetchList(); });
</script>
