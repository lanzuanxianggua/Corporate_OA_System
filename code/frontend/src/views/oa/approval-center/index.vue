<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">审批中心</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待审批</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- Tabs for switching business types -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="(config, key) in approvalTypeConfigs"
          :key="key"
          :label="config.label"
          :name="key"
        />
      </el-tabs>

      <!-- Dynamic table -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <!-- Dynamic columns based on activeTab -->
        <el-table-column prop="empName" label="申请人" width="90" />

        <!-- leave specific columns -->
        <template v-if="activeTab === 'leave'">
          <el-table-column prop="deptName" label="部门" width="100" />
          <el-table-column label="类型" width="70">
            <template #default="{ row }">
              {{ leaveTypeMap[row.leaveType] || '其他' }}
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
          <el-table-column label="天数" width="60" align="center">
            <template #default="{ row }">
              {{ calcDays(row.startTime, row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column
            prop="reason"
            label="原因"
            min-width="120"
            show-overflow-tooltip
          />
        </template>

        <!-- trip specific columns -->
        <template v-if="activeTab === 'trip'">
          <el-table-column
            prop="destination"
            label="目的地"
            width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="purpose"
            label="目的"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column label="时间范围" min-width="180">
            <template #default="{ row }">
              <div class="text-xs leading-5">
                <div>{{ formatTime(row.startTime) }}</div>
                <div>至 {{ formatTime(row.endTime) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="天数" width="60" align="center">
            <template #default="{ row }">
              {{ calcDays(row.startTime, row.endTime) }}
            </template>
          </el-table-column>
        </template>

        <!-- outing specific columns -->
        <template v-if="activeTab === 'outing'">
          <el-table-column
            prop="destination"
            label="目的地"
            width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="reason"
            label="原因"
            min-width="150"
            show-overflow-tooltip
          />
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
        <template v-if="activeTab === 'purchase'">
          <el-table-column
            prop="itemName"
            label="物品名称"
            width="130"
            show-overflow-tooltip
          />
          <el-table-column
            prop="quantity"
            label="数量"
            width="70"
            align="center"
          />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column
            prop="reason"
            label="原因"
            min-width="150"
            show-overflow-tooltip
          />
        </template>

        <!-- expense specific columns -->
        <template v-if="activeTab === 'expense'">
          <el-table-column
            prop="title"
            label="标题"
            width="140"
            show-overflow-tooltip
          />
          <el-table-column prop="category" label="类别" width="90" />
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column
            prop="description"
            label="描述"
            min-width="150"
            show-overflow-tooltip
          />
        </template>

        <!-- overtime specific columns -->
        <template v-if="activeTab === 'overtime'">
          <el-table-column label="加班日期" width="110">
            <template #default="{ row }">{{ row.overtimeDate }}</template>
          </el-table-column>
          <el-table-column label="时间段" min-width="180">
            <template #default="{ row }">
              <div class="text-xs leading-5">
                <div>{{ formatTime(row.startTime) }}</div>
                <div>至 {{ formatTime(row.endTime) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            prop="hours"
            label="时长(h)"
            width="80"
            align="center"
          />
          <el-table-column
            prop="reason"
            label="原因"
            min-width="120"
            show-overflow-tooltip
          />
        </template>

        <!-- loan specific columns -->
        <template v-if="activeTab === 'loan'">
          <el-table-column label="借支金额" width="110" align="right">
            <template #default="{ row }">¥{{ row.loanAmount }}</template>
          </el-table-column>
          <el-table-column
            prop="loanReason"
            label="借支原因"
            min-width="200"
            show-overflow-tooltip
          />
        </template>

        <!-- Shared status + action columns -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(statusTagType(row.status) as any)" size="small" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
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
            <span v-else class="text-[#c0c4cc] text-xs">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="fetchList"
        />
      </div>
    </el-card>

    <!-- Approval Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="approveAction === 1 ? '审批通过' : '审批拒绝'"
      width="520px"
      :close-on-click-modal="false"
    >
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="申请人">{{
            currentRow.empName
          }}</el-descriptions-item>
          <template v-if="activeTab === 'leave'">
            <el-descriptions-item label="部门">{{
              currentRow.deptName
            }}</el-descriptions-item>
            <el-descriptions-item label="请假类型">{{
              leaveTypeMap[currentRow.leaveType] || '其他'
            }}</el-descriptions-item>
            <el-descriptions-item label="天数">
              {{ calcDays(currentRow.startTime, currentRow.endTime) }} 天
            </el-descriptions-item>
            <el-descriptions-item label="开始时间" :span="2">{{
              formatTime(currentRow.startTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="结束时间" :span="2">{{
              formatTime(currentRow.endTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{
              currentRow.reason
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'trip'">
            <el-descriptions-item label="目的地">{{
              currentRow.destination
            }}</el-descriptions-item>
            <el-descriptions-item label="天数">
              {{ calcDays(currentRow.startTime, currentRow.endTime) }} 天
            </el-descriptions-item>
            <el-descriptions-item label="开始时间" :span="2">{{
              formatTime(currentRow.startTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="结束时间" :span="2">{{
              formatTime(currentRow.endTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="目的" :span="2">{{
              currentRow.purpose
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'outing'">
            <el-descriptions-item label="目的地">{{
              currentRow.destination
            }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{
              formatTime(currentRow.startTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{
              formatTime(currentRow.endTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{
              currentRow.reason
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'purchase'">
            <el-descriptions-item label="物品">{{
              currentRow.itemName
            }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{
              currentRow.quantity
            }}</el-descriptions-item>
            <el-descriptions-item label="金额"
              >¥{{ currentRow.amount }}</el-descriptions-item
            >
            <el-descriptions-item label="原因" :span="2">{{
              currentRow.reason
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'expense'">
            <el-descriptions-item label="标题">{{
              currentRow.title
            }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{
              currentRow.category
            }}</el-descriptions-item>
            <el-descriptions-item label="金额"
              >¥{{ currentRow.amount }}</el-descriptions-item
            >
            <el-descriptions-item label="描述" :span="2">{{
              currentRow.description
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'overtime'">
            <el-descriptions-item label="加班日期">{{
              currentRow.overtimeDate
            }}</el-descriptions-item>
            <el-descriptions-item label="时长">
              {{ currentRow.hours }} 小时
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{
              formatTime(currentRow.startTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{
              formatTime(currentRow.endTime)
            }}</el-descriptions-item>
            <el-descriptions-item label="原因" :span="2">{{
              currentRow.reason
            }}</el-descriptions-item>
          </template>
          <template v-if="activeTab === 'loan'">
            <el-descriptions-item label="借支金额"
              >¥{{ currentRow.loanAmount }}</el-descriptions-item
            >
            <el-descriptions-item label="原因" :span="2">{{
              currentRow.loanReason
            }}</el-descriptions-item>
          </template>
        </el-descriptions>

        <el-divider content-position="left">审批进度</el-divider>
        <ApprovalTimeline
          v-if="currentRow?.id"
          :business-type="activeConfig.businessType"
          :business-id="currentRow.id"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { approvalTypeConfigs } from "@/api/approval-center";
import { LEAVE_TYPE_MAP } from "@/utils/constants";

const leaveTypeMap = LEAVE_TYPE_MAP;

const activeTab = ref("leave");
const activeConfig = computed(() => approvalTypeConfigs[activeTab.value]);

// List state
const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | undefined>(undefined);

// Dialog state
const dialogVisible = ref(false);
const approving = ref(false);
const currentRow = ref<any>(null);
const approveAction = ref(1);
const approveRemark = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== undefined) params.status = statusFilter.value;
    const res: any = await activeConfig.value.getPage(params);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    /* handled by interceptor */
  } finally {
    loading.value = false;
  }
};

const handleTabChange = () => {
  pageNum.value = 1;
  statusFilter.value = undefined;
  fetchList();
};
const handleFilterChange = () => {
  pageNum.value = 1;
  fetchList();
};

const openDialog = (row: any, action: number) => {
  currentRow.value = row;
  approveAction.value = action;
  approveRemark.value = "";
  dialogVisible.value = true;
};

const handleApprove = async () => {
  if (!currentRow.value?.id) return;
  if (
    approveAction.value === 2 &&
    (!approveRemark.value || !approveRemark.value.trim())
  ) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  approving.value = true;
  try {
    await activeConfig.value.approve({
      id: currentRow.value.id,
      status: approveAction.value,
      remark: approveRemark.value
    });
    ElMessage.success(approveAction.value === 1 ? "审批通过" : "已拒绝");
    dialogVisible.value = false;
    fetchList();
  } catch {
    /* handled by interceptor */
  } finally {
    approving.value = false;
  }
};

const calcDays = (s?: string, e?: string) => {
  if (!s || !e) return "-";
  return Math.ceil(
    (new Date(e).getTime() - new Date(s).getTime()) / 86400000
  );
};

const statusText = (s?: number) =>
  ({ 0: "待审批", 1: "已通过", 2: "已拒绝", 4: "已撤回" }[s ?? -1] || "未知");
const statusTagType = (s?: number) =>
  ({ 0: "warning", 1: "success", 2: "danger", 4: "info" }[s ?? -1] || "info");
const formatTime = (t?: string) =>
  t ? t.replace("T", " ").substring(0, 16) : "-";

onMounted(() => fetchList());
</script>
