<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">我的待办任务</span>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="processName" label="流程名称" min-width="120" />
        <el-table-column prop="taskName" label="任务节点" min-width="100">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <span>{{ row.nodeName || row.taskName || '-' }}</span>
              <el-tag v-if="row.multiType === 'countersign'" type="warning" size="small">会签</el-tag>
              <el-tag v-else-if="row.multiType === 'orsign'" type="warning" size="small">或签</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="applicant" label="申请人" width="90" />
        <el-table-column prop="createTime" label="创建时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" plain @click="openHandleDialog(row, 1)">通过</el-button>
            <el-button type="danger" size="small" plain @click="openHandleDialog(row, 2)">驳回</el-button>
            <el-button type="warning" size="small" plain @click="openTransferDialog(row)">转办</el-button>
            <el-button type="info" size="small" plain @click="openReturnDialog(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="handleAction === 1 ? '审批通过' : '审批驳回'" width="500px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="审批备注">
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="请输入审批备注（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="handleAction === 1 ? 'success' : 'danger'" :loading="handling" @click="confirmHandle">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferVisible" title="转办任务" width="500px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="转办给">
          <el-select v-model="transferForm.toAssigneeId" filterable remote :remote-method="searchEmployee" placeholder="搜索员工" style="width: 100%">
            <el-option v-for="emp in transferEmpOptions" :key="emp.id" :label="emp.empName + ' (' + emp.empCode + ')'" :value="emp.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="转办原因">
          <el-input v-model="transferForm.reason" type="textarea" :rows="3" placeholder="请输入转办原因（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transfering" @click="confirmTransfer">确认转办</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="退回任务" width="500px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="退回目标">
          <el-select v-model="returnForm.returnTarget" placeholder="选择退回目标" style="width: 100%">
            <el-option label="退回给申请人" value="initiator" />
          </el-select>
        </el-form-item>
        <el-form-item label="退回原因" required>
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" placeholder="请输入退回原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="warning" :loading="returning" @click="confirmReturn">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getPendingTasks, handleTask, transferTask, returnTask } from "@/api/workflow";
import { getEmployeePage } from "@/api/employee";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getPendingTasks({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const handling = ref(false);
const currentRow = ref<any>(null);
const handleAction = ref(1);
const remark = ref("");

const openHandleDialog = (row: any, action: number) => {
  currentRow.value = row;
  handleAction.value = action;
  remark.value = "";
  dialogVisible.value = true;
};

const confirmHandle = async () => {
  if (!currentRow.value?.id) return;
  if (handleAction.value === 2 && (!remark.value || !remark.value.trim())) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  handling.value = true;
  try {
    await handleTask({ taskId: currentRow.value.id, status: handleAction.value, remark: remark.value });
    ElMessage.success(handleAction.value === 1 ? "已通过" : "已驳回");
    dialogVisible.value = false;
    fetchList();
  } finally {
    handling.value = false;
  }
};

// --- 转办 ---
const transferVisible = ref(false);
const transfering = ref(false);
const transferEmpOptions = ref<any[]>([]);
const transferForm = reactive({
  toAssigneeId: null as number | null,
  reason: ""
});

const openTransferDialog = (row: any) => {
  currentRow.value = row;
  transferForm.toAssigneeId = null;
  transferForm.reason = "";
  transferEmpOptions.value = [];
  transferVisible.value = true;
};

const searchEmployee = async (query: string) => {
  if (!query) return;
  const res: any = await getEmployeePage({ pageNum: 1, pageSize: 10, empName: query });
  transferEmpOptions.value = res.data?.records || res.data?.list || [];
};

const confirmTransfer = async () => {
  if (!currentRow.value?.id) return;
  if (!transferForm.toAssigneeId) {
    ElMessage.warning("请选择转办人");
    return;
  }
  transfering.value = true;
  try {
    await transferTask({ taskId: currentRow.value.id, toAssigneeId: transferForm.toAssigneeId, reason: transferForm.reason });
    ElMessage.success("已转办");
    transferVisible.value = false;
    fetchList();
  } finally {
    transfering.value = false;
  }
};

// --- 退回 ---
const returnVisible = ref(false);
const returning = ref(false);
const returnForm = reactive({
  returnTarget: "initiator",
  remark: ""
});

const openReturnDialog = (row: any) => {
  currentRow.value = row;
  returnForm.returnTarget = "initiator";
  returnForm.remark = "";
  returnVisible.value = true;
};

const confirmReturn = async () => {
  if (!currentRow.value?.id) return;
  if (!returnForm.remark.trim()) {
    ElMessage.warning("退回时必须填写原因");
    return;
  }
  returning.value = true;
  try {
    await returnTask({ taskId: currentRow.value.id, returnTarget: returnForm.returnTarget, remark: returnForm.remark });
    ElMessage.success("已退回");
    returnVisible.value = false;
    fetchList();
  } finally {
    returning.value = false;
  }
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => { fetchList(); });
</script>
