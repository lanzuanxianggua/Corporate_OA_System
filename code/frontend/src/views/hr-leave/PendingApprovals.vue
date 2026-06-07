<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">待我审批 - 请假</span>
          <el-tag size="small" type="info">仅展示 hr_leave 业务类型</el-tag>
        </div>
      </template>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="暂无待审批任务" :image-size="80" />
        <el-table v-else :data="list" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
          <el-table-column label="申请人" width="120">
            <template #default="{ row }">
              <span>{{ row.instance?.initiatorName || row.assigneeName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务标题" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.businessTitle || row.instance?.businessType || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="当前节点" min-width="120">
            <template #default="{ row }">{{ row.nodeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="到达时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" plain @click="openHandleDialog(row, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="openHandleDialog(row, 2)">驳回</el-button>
              <el-button type="primary" size="small" plain @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          small
          @change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="handleAction === 1 ? '审批通过' : '审批驳回'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item :label="handleAction === 2 ? '驳回原因' : '审批备注'">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="3"
            :placeholder="handleAction === 2 ? '请输入驳回原因' : '请输入审批备注（可选）'"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="handleAction === 1 ? 'success' : 'danger'" :loading="handling" @click="confirmHandle">
          确认
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="请假审批详情" size="600px" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailRow" :column="1" border>
          <el-descriptions-item label="申请人">{{ detailRow.empName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailRow.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请假类型">
            {{ leaveTypeLabel(detailRow.leaveType) }}
          </el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ formatDate(detailRow.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ formatDate(detailRow.endDate) }}</el-descriptions-item>
          <el-descriptions-item label="天数">{{ detailRow.totalDays ?? '-' }} 天</el-descriptions-item>
          <el-descriptions-item label="事由">{{ detailRow.reason || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>审批历史</el-divider>
        <ApprovalTimeline
          v-if="currentTask?.businessType"
          :business-type="currentTask.businessType"
          :business-id="currentTask.instanceId || 0"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { getPendingTasks, handleTask } from "@/api/workflow";
import { hrLeaveApi, LEAVE_TYPE_MAP_V2 } from "@/api/hr-leave";
import type { HrLeaveDetail } from "@/api/hr-leave";
import type { WorkflowTask } from "@/types/api";

const loading = ref(false);
const list = ref<WorkflowTask[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const dialogVisible = ref(false);
const handling = ref(false);
const handleAction = ref<1 | 2>(1);
const remark = ref("");
const currentTask = ref<WorkflowTask | null>(null);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailRow = ref<HrLeaveDetail | null>(null);

const leaveTypeLabel = (type?: string) => (type ? LEAVE_TYPE_MAP_V2[type] || type : "-");
const formatDate = (s?: string) => (s ? s.substring(0, 10) : "-");
const formatTime = (s?: string) => (s ? s.replace("T", " ").substring(0, 16) : "-");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getPendingTasks({ pageNum: pageNum.value, pageSize: pageSize.value });
    const all: WorkflowTask[] = res?.data?.list || [];
    list.value = all.filter(t => String(t.businessType || "").toLowerCase() === "hr_leave");
    total.value = list.value.length;
  } catch {
    ElMessage.error("获取待办任务失败");
  } finally {
    loading.value = false;
  }
};

const openHandleDialog = (row: WorkflowTask, action: 1 | 2) => {
  currentTask.value = row;
  handleAction.value = action;
  remark.value = "";
  dialogVisible.value = true;
};

const confirmHandle = async () => {
  if (!currentTask.value?.id) return;
  if (handleAction.value === 2 && !remark.value.trim()) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  handling.value = true;
  try {
    await handleTask({
      taskId: currentTask.value.id,
      status: handleAction.value,
      remark: remark.value
    });
    ElMessage.success(handleAction.value === 1 ? "已通过" : "已驳回");
    dialogVisible.value = false;
    fetchList();
  } catch {
    ElMessage.error("操作失败");
  } finally {
    handling.value = false;
  }
};

const openDetail = async (row: WorkflowTask) => {
  if (!row.id) return;
  currentTask.value = row;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    // 业务主键需通过 findPendingTask 拿到；这里退化为展示 task 自带字段
    detailRow.value = {
      id: row.instanceId,
      empName: row.instance?.initiatorName,
      deptName: undefined,
      leaveType: undefined,
      startDate: undefined,
      endDate: undefined,
      totalDays: undefined,
      reason: undefined
    };
    // 尝试从实例里获取业务主键
    if (row.instanceId) {
      try {
        const data = await hrLeaveApi.getDetail(row.instanceId);
        detailRow.value = { ...detailRow.value, ...data };
      } catch {
        /* 静默失败：审批员无业务查看权限时仍可展示 task 信息 */
      }
    }
  } finally {
    detailLoading.value = false;
  }
};

onMounted(fetchList);
</script>
