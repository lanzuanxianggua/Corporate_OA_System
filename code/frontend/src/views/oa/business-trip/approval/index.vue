<template>
  <div class="h-full">
    <el-card shadow="never">
      <!-- 顶部筛选 -->
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">出差审批</span>
          <el-radio-group v-model="statusFilter" @change="handleFilterChange">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待审批</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 审批列表 -->
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="empName" label="申请人" width="90" />
        <el-table-column prop="destination" label="目的地" width="100" show-overflow-tooltip />
        <el-table-column prop="purpose" label="事由" min-width="120" show-overflow-tooltip />
        <el-table-column label="时间范围" min-width="180">
          <template #default="{ row }">
            <div class="text-xs leading-5">
              <div>{{ formatTime(row.startTime) }}</div>
              <div>至 {{ formatTime(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="天数" width="60" align="center">
          <template #default="{ row }">{{ calcDays(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(statusTagType(row.status) as any)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="success" size="small" plain @click="openApproveDialog(row, 1)">通过</el-button>
              <el-button type="danger" size="small" plain @click="openApproveDialog(row, 2)">拒绝</el-button>
            </template>
            <span v-else class="text-[#c0c4cc] text-xs">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <!-- 审批弹窗 -->
    <el-dialog v-model="dialogVisible" :title="approveAction === 1 ? '审批通过' : '审批拒绝'" width="520px" :close-on-click-modal="false">
      <template v-if="currentRow">
        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="申请人">{{ currentRow.empName }}</el-descriptions-item>
          <el-descriptions-item label="目的地">{{ currentRow.destination }}</el-descriptions-item>
          <el-descriptions-item label="出差天数">{{ calcDays(currentRow.startTime, currentRow.endTime) }} 天</el-descriptions-item>
          <el-descriptions-item label="出发时间" :span="2">{{ formatTime(currentRow.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="返回时间" :span="2">{{ formatTime(currentRow.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="出差事由" :span="2">{{ currentRow.purpose }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">审批进度</el-divider>
        <ApprovalTimeline v-if="currentRow?.id" business-type="trip" :business-id="currentRow.id" />

        <el-form label-position="top">
          <el-form-item label="审批备注">
            <el-input v-model="approveRemark" type="textarea" :rows="3" placeholder="请输入审批备注（可选）" maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="approveAction === 1 ? 'success' : 'danger'" :loading="approving" @click="handleApprove">确认{{ approveAction === 1 ? "通过" : "拒绝" }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { getBusinessTripPage, approveBusinessTrip } from "@/api/businessTrip";

const calcDays = (startTime?: string, endTime?: string) => {
  if (!startTime || !endTime) return "-";
  const diff = new Date(endTime).getTime() - new Date(startTime).getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

// --- 列表 ---
const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref<number | undefined>(undefined);

const fetchList = async () => {
  loading.value = true;
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value };
    if (statusFilter.value !== undefined) params.status = statusFilter.value;
    const res: any = await getBusinessTripPage(params);
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pageNum.value = 1;
  fetchList();
};

// --- 审批 ---
const dialogVisible = ref(false);
const approving = ref(false);
const currentRow = ref<any>(null);
const approveAction = ref(1);
const approveRemark = ref("");

const openApproveDialog = (row: any, action: number) => {
  currentRow.value = row;
  approveAction.value = action;
  approveRemark.value = "";
  dialogVisible.value = true;
};

const handleApprove = async () => {
  if (!currentRow.value?.id) return;
  if (approveAction.value === 2 && (!approveRemark.value || !approveRemark.value.trim())) {
    ElMessage.warning("驳回时必须填写原因");
    return;
  }
  approving.value = true;
  try {
    await approveBusinessTrip({
      id: currentRow.value.id,
      status: approveAction.value,
      remark: approveRemark.value
    });
    ElMessage.success(approveAction.value === 1 ? "已通过该出差申请" : "已拒绝该出差申请");
    dialogVisible.value = false;
    fetchList();
  } catch {
    // error handled by interceptor
  } finally {
    approving.value = false;
  }
};

// --- 工具函数 ---
const statusText = (status?: number) => {
  const map: Record<number, string> = { 0: "待审批", 1: "已通过", 2: "已拒绝" };
  return map[status ?? -1] || "未知";
};

const statusTagType = (status?: number) => {
  const map: Record<number, string> = { 0: "warning", 1: "success", 2: "danger" };
  return map[status ?? -1] || "info";
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => {
  fetchList();
});
</script>
