<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">我的请假</span>
          <el-button type="primary" size="small" @click="openCreateDialog">新建请假申请</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="mb-3">
        <el-form-item label="请假类型">
          <el-select v-model="filters.leaveType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in LEAVE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="opt in LEAVE_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div v-loading="loading">
        <el-empty v-if="!loading && list.length === 0" description="暂无请假记录" :image-size="80" />
        <el-table v-else :data="list" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
          <el-table-column prop="leaveType" label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ leaveTypeLabel(row.leaveType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="empName" label="申请人" width="100" />
          <el-table-column prop="deptName" label="部门" min-width="120" show-overflow-tooltip />
          <el-table-column label="开始日期" width="120">
            <template #default="{ row }">{{ formatDate(row.startDate) }}</template>
          </el-table-column>
          <el-table-column label="结束日期" width="120">
            <template #default="{ row }">{{ formatDate(row.endDate) }}</template>
          </el-table-column>
          <el-table-column label="天数" width="80" align="center">
            <template #default="{ row }">{{ row.totalDays ?? "-" }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="事由" min-width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="(LEAVE_STATUS_TAG_V2[row.status || ''] || 'info') as any" size="small" effect="light">
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
              <el-popconfirm
                v-if="row.status === 'PENDING'"
                title="确定要撤回此申请吗？"
                @confirm="onRevoke(row)"
              >
                <template #reference>
                  <el-button type="warning" link size="small">撤回</el-button>
                </template>
              </el-popconfirm>
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

    <LeaveFormDialog v-model:visible="dialogVisible" @success="onSubmitSuccess" />

    <el-drawer v-model="detailVisible" title="请假详情" size="560px" :close-on-click-modal="false">
      <div v-if="currentRow" v-loading="detailLoading">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="请假类型">{{ leaveTypeLabel(currentRow.leaveType) }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentRow.empName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentRow.deptName || "-" }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ formatDate(currentRow.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ formatDate(currentRow.endDate) }}</el-descriptions-item>
          <el-descriptions-item label="天数">{{ currentRow.totalDays ?? "-" }} 天</el-descriptions-item>
          <el-descriptions-item label="事由">{{ currentRow.reason || "-" }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(LEAVE_STATUS_TAG_V2[currentRow.status || ''] || 'info') as any" size="small">
              {{ statusLabel(currentRow.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(currentRow.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>审批历史</el-divider>
        <ApprovalTimeline business-type="hr_leave" :business-id="currentRow.id || 0" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import LeaveFormDialog from "@/views/hr-leave/LeaveFormDialog.vue";
import {
  hrLeaveApi,
  LEAVE_TYPE_MAP_V2,
  LEAVE_TYPE_OPTIONS,
  LEAVE_STATUS_MAP_V2,
  LEAVE_STATUS_OPTIONS,
  LEAVE_STATUS_TAG_V2
} from "@/api/hr-leave";
import type { HrLeaveDetail, HrLeaveVO } from "@/api/hr-leave";

const loading = ref(false);
const list = ref<HrLeaveVO[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const filters = reactive<{ leaveType?: string; status?: string }>({
  leaveType: undefined,
  status: undefined
});

const dialogVisible = ref(false);

const detailVisible = ref(false);
const detailLoading = ref(false);
const currentRow = ref<HrLeaveDetail | null>(null);

const leaveTypeLabel = (type?: string) => (type ? LEAVE_TYPE_MAP_V2[type] || type : "-");
const statusLabel = (status?: string) => (status ? LEAVE_STATUS_MAP_V2[status] || status : "-");
const formatDate = (s?: string) => (s ? s.substring(0, 10) : "-");
const formatTime = (s?: string) => (s ? s.replace("T", " ").substring(0, 16) : "-");

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await hrLeaveApi.listMy({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      leaveType: filters.leaveType,
      status: filters.status
    });
    list.value = res?.list || [];
    total.value = res?.total || 0;
  } catch {
    ElMessage.error("获取请假列表失败");
  } finally {
    loading.value = false;
  }
};

const onSearch = () => {
  pageNum.value = 1;
  fetchList();
};

const onReset = () => {
  filters.leaveType = undefined;
  filters.status = undefined;
  pageNum.value = 1;
  fetchList();
};

const openCreateDialog = () => {
  dialogVisible.value = true;
};

const onSubmitSuccess = () => {
  dialogVisible.value = false;
  pageNum.value = 1;
  fetchList();
};

const viewDetail = async (row: HrLeaveVO) => {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const data = await hrLeaveApi.getDetail(row.id);
    currentRow.value = data;
  } catch {
    ElMessage.error("获取请假详情失败");
  } finally {
    detailLoading.value = false;
  }
};

const onRevoke = async (row: HrLeaveVO) => {
  if (!row.id) return;
  try {
    await hrLeaveApi.revoke(row.id);
    ElMessage.success("请假已撤回");
    fetchList();
  } catch {
    ElMessage.error("撤回失败");
  }
};

// Keep the constants referenced (so tree-shaking does not drop them)
void LEAVE_STATUS_OPTIONS;
void LEAVE_TYPE_OPTIONS;

onMounted(fetchList);
</script>
