<template>
  <div>
    <el-card>
      <template #header><span class="font-medium">在线用户</span></template>
      <el-table :data="userList" stripe style="width: 100%">
        <el-table-column label="用户名" prop="empName" />
        <el-table-column label="登录IP" prop="ip" />
        <el-table-column label="部门" prop="deptName" />
        <el-table-column label="浏览器" prop="browser" />
        <el-table-column label="登录时间" prop="loginTime" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleForceLogout(row)">强制下线</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-4">
        <OaPagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" @change="fetchData"  :page-sizes="[10, 20, 50]" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getOnlineLogs, forceLogout } from "@/api/monitor";

const userList = ref<any[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchData = async () => {
  try {
    const r: any = await getOnlineLogs({ page: page.value, pageSize: pageSize.value });
    if (r.data?.list) { userList.value = r.data.list; total.value = r.data.total || 0; }
    else if (r.data?.records) { userList.value = r.data.records; total.value = r.data.total || 0; }
  } catch {}
};

const handleForceLogout = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定强制下线用户 "${row.empName || row.username || ''}" ？`, "提示", { type: "warning" });
    await forceLogout(row.empId || row.id);
    ElMessage.success("操作成功");
    await fetchData();
  } catch {}
};

onMounted(fetchData);
</script>
