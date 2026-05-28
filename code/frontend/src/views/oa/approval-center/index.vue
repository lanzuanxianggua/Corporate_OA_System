<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">审批中心</span>
        </div>
      </template>

      <!-- Pending / History tabs -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审批" name="pending" />
        <el-tab-pane label="已审批" name="history" />
      </el-tabs>

      <!-- History type selector - shown above table when on history tab -->
      <div v-if="activeTab === 'history'" class="mb-4">
        <el-radio-group v-model="activeHistoryType" @change="fetchHistoryTasks">
          <el-radio-button v-for="t in historyTypes" :key="t" :value="t">
            {{ businessTypeLabel(t) }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- Pending tasks table -->
      <template v-if="activeTab === 'pending'">
        <el-table
          :data="pendingTasks"
          v-loading="loading"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        >
          <el-table-column label="申请人" width="90">
            <template #default="{ row }">
              {{ row.instance?.initiatorName || getInitiatorName(row) || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="业务类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">
                {{ businessTypeLabel(row.businessType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前节点" min-width="120">
            <template #default="{ row }">
              {{ row.nodeName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="success"
                size="small"
                plain
                @click="openDialog(row, 1)"
              >通过</el-button>
              <el-button
                type="danger"
                size="small"
                plain
                @click="openDialog(row, 2)"
              >拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="pendingPageNum"
            v-model:page-size="pendingPageSize"
            :total="pendingTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @change="fetchPendingTasks"
          />
        </div>
      </template>

      <!-- History table -->
      <template v-if="activeTab === 'history'">
        <el-table
          :data="historyTasks"
          v-loading="historyLoading"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        >
          <el-table-column label="申请人" width="90">
            <template #default="{ row }">
              {{ row.empName || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="业务类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">
                {{ businessTypeLabel(activeHistoryType) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- leave specific columns -->
          <template v-if="activeHistoryType === 'leave'">
            <el-table-column label="类型" width="70">
              <template #default="{ row }">
                {{ leaveTypeMap[row.leaveType] || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="时间范围" min-width="180">
              <template #default="{ row }">
                <div class="text-xs leading-5">
                  <div>{{ formatTime(row.startTime) }}</div>
                  <div>至 {{ formatTime(row.endTime) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
          </template>

          <!-- trip specific columns -->
          <template v-if="activeHistoryType === 'trip'">
            <el-table-column prop="destination" label="目的地" width="120" show-overflow-tooltip />
            <el-table-column prop="purpose" label="目的" min-width="120" show-overflow-tooltip />
            <el-table-column label="时间范围" min-width="180">
              <template #default="{ row }">
                <div class="text-xs leading-5">
                  <div>{{ formatTime(row.startTime) }}</div>
                  <div>至 {{ formatTime(row.endTime) }}</div>
                </div>
              </template>
            </el-table-column>
          </template>

          <!-- outing specific columns -->
          <template v-if="activeHistoryType === 'outing'">
            <el-table-column prop="destination" label="目的地" width="120" show-overflow-tooltip />
            <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间范围" min-width="180">
              <template #default="{ row }">
                <div class="text-xs leading-5">
                  <div>{{ formatTime(row.startTime) }}</div>
                  <div>至 {{ formatTime(row.endTime) }}</div>
                </div>
              </template>
            </el-table-column>
          </template>

          <!-- purchase specific columns -->
          <template v-if="activeHistoryType === 'purchase'">
            <el-table-column prop="itemName" label="物品名称" width="130" show-overflow-tooltip />
            <el-table-column prop="quantity" label="数量" width="70" align="center" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">{{ row.amount != null ? '¥' + row.amount : '-' }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
          </template>

          <!-- expense specific columns -->
          <template v-if="activeHistoryType === 'expense'">
            <el-table-column prop="title" label="标题" width="140" show-overflow-tooltip />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">{{ row.amount != null ? '¥' + row.amount : '-' }}</template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
          </template>

          <!-- overtime specific columns -->
          <template v-if="activeHistoryType === 'overtime'">
            <el-table-column label="加班日期" width="110">
              <template #default="{ row }">{{ row.overtimeDate || '-' }}</template>
            </el-table-column>
            <el-table-column label="时间段" min-width="180">
              <template #default="{ row }">
                <div class="text-xs leading-5">
                  <div>{{ formatTime(row.startTime) }}</div>
                  <div>至 {{ formatTime(row.endTime) }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="hours" label="时长(h)" width="80" align="center" />
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
          </template>

          <!-- loan specific columns -->
          <template v-if="activeHistoryType === 'loan'">
            <el-table-column label="借支金额" width="110" align="right">
              <template #default="{ row }">{{ row.loanAmount != null ? '¥' + row.loanAmount : '-' }}</template>
            </el-table-column>
            <el-table-column prop="loanReason" label="借支原因" min-width="200" show-overflow-tooltip />
          </template>

          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="(statusTagType(row.status) as any)" size="small" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" plain @click="openHistoryDetail(row)">
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="historyPageNum"
            v-model:page-size="historyPageSize"
            :total="historyTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @change="fetchHistoryTasks"
          />
        </div>
      </template>
    </el-card>

    <!-- Approval Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="approveAction === 1 ? '审批通过' : '审批拒绝'"
      width="600px"
      :close-on-click-modal="false"
    >
      <template v-if="currentTask">
        <!-- Business detail -->
        <div v-loading="detailLoading">
          <el-descriptions :column="2" border size="small" class="mb-4">
            <el-descriptions-item label="申请人">
              {{ currentTask.instance?.initiatorName || getInitiatorName(currentTask) || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="业务类型">
              {{ businessTypeLabel(currentTask.businessType) }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- Dynamic business fields from detail -->
          <el-descriptions v-if="businessDetail" :column="2" border size="small" class="mb-4">
            <el-descriptions-item label="申请人">{{ businessDetail.empName || '-' }}</el-descriptions-item>
            <template v-if="currentTask.businessType === 'leave'">
              <el-descriptions-item label="部门">{{ businessDetail.deptName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="请假类型">{{ leaveTypeMap[businessDetail.leaveType] || '-' }}</el-descriptions-item>
              <el-descriptions-item label="天数">{{ calcDays(businessDetail.startTime, businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="开始时间" :span="2">{{ formatTime(businessDetail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间" :span="2">{{ formatTime(businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ businessDetail.reason || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'trip'">
              <el-descriptions-item label="目的地">{{ businessDetail.destination || '-' }}</el-descriptions-item>
              <el-descriptions-item label="天数">{{ calcDays(businessDetail.startTime, businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="开始时间" :span="2">{{ formatTime(businessDetail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间" :span="2">{{ formatTime(businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="目的" :span="2">{{ businessDetail.purpose || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'outing'">
              <el-descriptions-item label="目的地">{{ businessDetail.destination || '-' }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatTime(businessDetail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatTime(businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ businessDetail.reason || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'purchase'">
              <el-descriptions-item label="物品">{{ businessDetail.itemName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="数量">{{ businessDetail.quantity ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="金额">{{ businessDetail.amount != null ? '¥' + businessDetail.amount : '-' }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ businessDetail.reason || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'expense'">
              <el-descriptions-item label="标题">{{ businessDetail.title || '-' }}</el-descriptions-item>
              <el-descriptions-item label="类别">{{ businessDetail.category || '-' }}</el-descriptions-item>
              <el-descriptions-item label="金额">{{ businessDetail.amount != null ? '¥' + businessDetail.amount : '-' }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ businessDetail.description || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'overtime'">
              <el-descriptions-item label="加班日期">{{ businessDetail.overtimeDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="时长">{{ businessDetail.hours ?? '-' }} 小时</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ formatTime(businessDetail.startTime) }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ formatTime(businessDetail.endTime) }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ businessDetail.reason || '-' }}</el-descriptions-item>
            </template>
            <template v-if="currentTask.businessType === 'loan'">
              <el-descriptions-item label="借支金额">{{ businessDetail.loanAmount != null ? '¥' + businessDetail.loanAmount : '-' }}</el-descriptions-item>
              <el-descriptions-item label="原因" :span="2">{{ businessDetail.loanReason || '-' }}</el-descriptions-item>
            </template>
          </el-descriptions>
        </div>

        <el-divider content-position="left">审批进度</el-divider>
        <ApprovalTimeline
          v-if="currentTask.businessType && businessDetail?.id"
          :business-type="currentTask.businessType"
          :business-id="businessDetail.id"
        />

        <el-form label-position="top" class="mt-4">
          <el-form-item label="审批备注">
            <el-input
              v-model="approveRemark"
              type="textarea"
              :rows="3"
              placeholder="请输入审批备注（拒绝时必填）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :type="approveAction === 1 ? 'success' : 'danger'"
          :loading="approving"
          @click="handleApprove"
        >
          确认{{ approveAction === 1 ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- History detail dialog -->
    <el-dialog
      v-model="historyDialogVisible"
      title="审批详情"
      width="600px"
    >
      <template v-if="historyDetail">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="申请人">{{ historyDetail.empName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(statusTagType(historyDetail.status) as any)" size="small">
              {{ statusText(historyDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <template v-if="activeHistoryType === 'leave'">
            <el-descriptions-item label="请假类型">{{ leaveTypeMap[historyDetail.leaveType] || '-' }}</el-descriptions-item>
            <el-descriptions-item label="天数">{{ calcDays(historyDetail.startTime, historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间" :span="2">{{ formatTime(historyDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间" :span="2">{{ formatTime(historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ historyDetail.reason || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'trip'">
            <el-descriptions-item label="目的地">{{ historyDetail.destination || '-' }}</el-descriptions-item>
            <el-descriptions-item label="天数">{{ calcDays(historyDetail.startTime, historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间" :span="2">{{ formatTime(historyDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间" :span="2">{{ formatTime(historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="目的" :span="2">{{ historyDetail.purpose || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'outing'">
            <el-descriptions-item label="目的地">{{ historyDetail.destination || '-' }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatTime(historyDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatTime(historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ historyDetail.reason || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'purchase'">
            <el-descriptions-item label="物品">{{ historyDetail.itemName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ historyDetail.quantity ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ historyDetail.amount != null ? '¥' + historyDetail.amount : '-' }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ historyDetail.reason || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'expense'">
            <el-descriptions-item label="标题">{{ historyDetail.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{ historyDetail.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="金额">{{ historyDetail.amount != null ? '¥' + historyDetail.amount : '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ historyDetail.description || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'overtime'">
            <el-descriptions-item label="加班日期">{{ historyDetail.overtimeDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时长">{{ historyDetail.hours ?? '-' }} 小时</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatTime(historyDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ formatTime(historyDetail.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ historyDetail.reason || '-' }}</el-descriptions-item>
          </template>
          <template v-if="activeHistoryType === 'loan'">
            <el-descriptions-item label="借支金额">{{ historyDetail.loanAmount != null ? '¥' + historyDetail.loanAmount : '-' }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{ historyDetail.loanReason || '-' }}</el-descriptions-item>
          </template>
        </el-descriptions>

        <el-divider content-position="left">审批进度</el-divider>
        <ApprovalTimeline
          v-if="activeHistoryType && historyDetail.id"
          :business-type="activeHistoryType"
          :business-id="historyDetail.id"
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { getPendingTasks, handleTask } from "@/api/workflow";
import { getLeavePage } from "@/api/leave";
import { getBusinessTripPage } from "@/api/businessTrip";
import { getOutingPage } from "@/api/outing";
import { getPurchasePage } from "@/api/purchase";
import { getExpensePage } from "@/api/expense";
import { getOvertimePage } from "@/api/overtime";
import { getLoanPage } from "@/api/loan";
import { LEAVE_TYPE_MAP } from "@/utils/constants";
import type { WorkflowTask } from "@/types/api";

const leaveTypeMap = LEAVE_TYPE_MAP;

// ── Business type mappings ─────────────────────────────────────────────
const businessTypeLabels: Record<string, string> = {
  leave: "请假",
  trip: "出差",
  outing: "外出",
  purchase: "采购",
  expense: "经费",
  overtime: "加班",
  loan: "借支"
};
const businessTypeLabel = (t?: string) => businessTypeLabels[t || ""] || t || "-";

// API endpoint per business type for fetching pages (history tab)
const historyFetchers: Record<string, (params: any) => Promise<any>> = {
  leave: getLeavePage,
  trip: getBusinessTripPage,
  outing: getOutingPage,
  purchase: getPurchasePage,
  expense: getExpensePage,
  overtime: getOvertimePage,
  loan: getLoanPage
};
const historyTypes = Object.keys(historyFetchers);

// ── Main tab ────────────────────────────────────────────────────────────
const activeTab = ref("pending");

// ── Pending tasks state ─────────────────────────────────────────────────
const loading = ref(false);
const pendingTasks = ref<WorkflowTask[]>([]);
const pendingPageNum = ref(1);
const pendingPageSize = ref(10);
const pendingTotal = ref(0);

const fetchPendingTasks = async () => {
  loading.value = true;
  try {
    const res: any = await getPendingTasks({
      pageNum: pendingPageNum.value,
      pageSize: pendingPageSize.value
    });
    const list: any[] = res.data?.list || [];
    pendingTasks.value = list;
    pendingTotal.value = res.data?.total || 0;
  } catch {
    /* handled by interceptor */
  } finally {
    loading.value = false;
  }
};

// ── History tasks state ─────────────────────────────────────────────────
const historyLoading = ref(false);
const historyTasks = ref<any[]>([]);
const activeHistoryType = ref("leave");
const historyPageNum = ref(1);
const historyPageSize = ref(10);
const historyTotal = ref(0);

const fetchHistoryTasks = async () => {
  historyLoading.value = true;
  historyPageNum.value = 1;
  try {
    const fetcher = historyFetchers[activeHistoryType.value];
    if (!fetcher) return;
    const res: any = await fetcher({
      pageNum: historyPageNum.value,
      pageSize: historyPageSize.value,
      status: undefined
    });
    const list: any[] = res.data?.list || [];
    // Only show records that have been through workflow (status != 0, handle both string and number)
    historyTasks.value = list.filter((r: any) => r.status !== 0 && r.status !== "0");
    historyTotal.value = res.data?.total || 0;
  } catch {
    /* handled by interceptor */
  } finally {
    historyLoading.value = false;
  }
};

const handleTabChange = () => {
  if (activeTab.value === "pending") {
    fetchPendingTasks();
  } else {
    fetchHistoryTasks();
  }
};

// ── Approval dialog state ───────────────────────────────────────────────
const dialogVisible = ref(false);
const approving = ref(false);
const detailLoading = ref(false);
const currentTask = ref<WorkflowTask | null>(null);
const businessDetail = ref<any>(null);
const approveAction = ref(1);
const approveRemark = ref("");

const openDialog = async (task: WorkflowTask, action: number) => {
  currentTask.value = task;
  approveAction.value = action;
  approveRemark.value = "";
  dialogVisible.value = true;

  // Fetch business detail
  businessDetail.value = null;
  const bt = task.businessType;
  const bid = task.instance?.businessId;
  if (!bt || !bid) return;

  detailLoading.value = true;
  try {
    const fetcher = historyFetchers[bt];
    if (!fetcher) return;
    // Use page API with empId filter from the task's instance initiatorId to narrow results
    const initiatorId = task.instance?.initiatorId;
    const res: any = await fetcher({
      pageNum: 1,
      pageSize: 50,
      empId: initiatorId || undefined
    });
    const list: any[] = res.data?.list || [];
    const detail = list.find((r: any) => r.id === bid);
    businessDetail.value = detail || { id: bid };
  } catch {
    businessDetail.value = { id: bid };
  } finally {
    detailLoading.value = false;
  }
};

const handleApprove = async () => {
  if (!currentTask.value?.id) return;
  if (
    approveAction.value === 2 &&
    (!approveRemark.value || !approveRemark.value.trim())
  ) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  approving.value = true;
  try {
    await handleTask({
      taskId: currentTask.value.id,
      status: approveAction.value,
      remark: approveRemark.value
    });
    ElMessage.success(approveAction.value === 1 ? "审批通过" : "已拒绝");
    dialogVisible.value = false;
    fetchPendingTasks();
  } catch {
    /* handled by interceptor */
  } finally {
    approving.value = false;
  }
};

// ── History detail dialog ───────────────────────────────────────────────
const historyDialogVisible = ref(false);
const historyDetail = ref<any>(null);

const openHistoryDetail = (row: any) => {
  historyDetail.value = row;
  historyDialogVisible.value = true;
};

// ── Helpers ─────────────────────────────────────────────────────────────
const getInitiatorName = (task: WorkflowTask) => {
  // Backend may populate initiatorName on the instance, or we show '-'
  return (task as any).instance?.initiatorName || "-";
};

const calcDays = (s?: string, e?: string) => {
  if (!s || !e) return "-";
  return Math.ceil((new Date(e).getTime() - new Date(s).getTime()) / 86400000);
};

const statusText = (s?: number | string) =>
  ({ 0: "待审批", 1: "已通过", 2: "已拒绝", 3: "已转办", 4: "已撤回", 5: "已退回" }[Number(s ?? -1)] || "未知");
const statusTagType = (s?: number | string) =>
  ({ 0: "warning", 1: "success", 2: "danger", 3: "info", 4: "info", 5: "warning" }[Number(s ?? -1)] || "info");
const formatTime = (t?: string) =>
  t ? t.replace("T", " ").substring(0, 16) : "-";

onMounted(() => fetchPendingTasks());
</script>
