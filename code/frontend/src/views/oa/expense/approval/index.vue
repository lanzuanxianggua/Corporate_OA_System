<template>
  <div class="h-full">
    <el-card shadow="never">
      <!-- 顶部筛选 -->
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">经费审批</span>
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
        <el-table-column prop="title" label="申请标题" min-width="100" show-overflow-tooltip />
        <el-table-column label="费用类别" width="90">
          <template #default="{ row }">{{ categoryMap[row.category] || "-" }}</template>
        </el-table-column>
        <el-table-column label="申请金额" width="110" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
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
          <el-descriptions-item label="申请标题">{{ currentRow.title }}</el-descriptions-item>
          <el-descriptions-item label="费用类别">{{ categoryMap[currentRow.category] || "-" }}</el-descriptions-item>
          <el-descriptions-item label="申请金额">{{ formatAmount(currentRow.amount) }}</el-descriptions-item>
          <el-descriptions-item label="费用说明" :span="2">{{ currentRow.description }}</el-descriptions-item>
        </el-descriptions>

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
import { getExpensePage, approveExpense } from "@/api/expense";

const categoryMap: Record<number, string> = { 1: "差旅费", 2: "办公用品", 3: "招待费", 4: "其他" };

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
    const res: any = await getExpensePage(params);
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
  approving.value = true;
  try {
    await approveExpense({
      id: currentRow.value.id,
      status: approveAction.value,
      remark: approveRemark.value
    });
    ElMessage.success(approveAction.value === 1 ? "已通过该经费申请" : "已拒绝该经费申请");
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

const formatAmount = (amount?: number) => {
  if (amount == null) return "-";
  return `￥${amount.toFixed(2)}`;
};

onMounted(() => {
  fetchList();
});
</script>
