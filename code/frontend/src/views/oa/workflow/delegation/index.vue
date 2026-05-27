<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold">审批委托设置</span>
          <el-button type="primary" size="small" @click="showDialog = true">新增委托</el-button>
        </div>
      </template>

      <el-table :data="delegations" v-loading="loading" stripe size="small">
        <el-table-column label="代理人" prop="delegateToName" width="120" />
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">{{ row.startTime?.replace('T', ' ').substring(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">{{ row.endTime?.replace('T', ' ').substring(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '生效中' : '已取消' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === '0'" type="danger" link size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" title="新增审批委托" width="480px">
      <el-form label-width="80px">
        <el-form-item label="代理人">
          <el-select v-model="form.delegateToId" filterable remote :remote-method="searchEmployee" placeholder="搜索员工" style="width: 100%">
            <el-option v-for="emp in employeeOptions" :key="emp.id" :label="emp.empName" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getMyDelegations, setDelegation, cancelDelegation } from "@/api/workflow";
import request from "@/utils/request";

const loading = ref(false);
const delegations = ref<any[]>([]);
const showDialog = ref(false);
const employeeOptions = ref<any[]>([]);

const form = reactive({
  delegateToId: null as number | null,
  startTime: "",
  endTime: ""
});

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getMyDelegations();
    delegations.value = res.data || [];
  } catch {} finally {
    loading.value = false;
  }
};

const searchEmployee = async (query: string) => {
  if (!query) return;
  const res: any = await request.get("/api/employee/page", { params: { pageNum: 1, pageSize: 10, empName: query } });
  employeeOptions.value = res.data?.records || res.data?.list || [];
};

const handleSubmit = async () => {
  if (!form.delegateToId || !form.startTime || !form.endTime) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  await setDelegation(form as any);
  ElMessage.success("委托设置成功");
  showDialog.value = false;
  form.delegateToId = null;
  form.startTime = "";
  form.endTime = "";
  fetchList();
};

const handleCancel = async (row: any) => {
  try {
    await ElMessageBox.confirm("确定取消此委托？", "确认", { type: "warning" });
    await cancelDelegation(row.id);
    ElMessage.success("已取消");
    fetchList();
  } catch {}
};

onMounted(() => { fetchList(); });
</script>
