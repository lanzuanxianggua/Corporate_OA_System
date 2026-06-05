<template>
  <div>
    <!-- 假期余额卡片 -->
    <el-row :gutter="16" class="mb-5">
      <el-col
        v-for="item in balanceList"
        :key="item.leaveType"
        :span="8"
        :lg="6"
      >
        <el-card shadow="hover" :body-style="{ padding: '16px' }">
          <div class="text-center">
            <div class="text-sm text-[#909399] mb-1">
              {{ LEAVE_TYPE_MAP[item.leaveType] || "未知" }}
            </div>
            <div class="text-3xl font-bold text-[#409EFF] my-2">
              {{ item.remainDays }}
            </div>
            <el-progress
              :percentage="calcPercent(item)"
              :stroke-width="6"
              :show-text="false"
            />
            <div class="text-xs text-[#909399] mt-1">
              已用 {{ item.usedDays }} / 总计 {{ item.totalDays }} 天
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="mb-4" :body-style="{ padding: '16px 20px' }">
      <el-form :model="queryParams" inline>
        <el-form-item label="请假类型">
          <el-select
            v-model="queryParams.leaveType"
            clearable
            placeholder="全部"
            style="width: 120px"
          >
            <el-option
              v-for="(label, key) in LEAVE_TYPE_MAP"
              :key="key"
              :label="label"
              :value="Number(key)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            clearable
            placeholder="全部"
            style="width: 110px"
          >
            <el-option
              v-for="(label, key) in LEAVE_STATUS_MAP"
              :key="key"
              :label="label"
              :value="Number(key)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item style="float: right">
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新建请假
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 请假列表 -->
    <el-card :body-style="{ padding: '0' }">
      <el-table
        :data="leaveList"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column
          label="申请单号"
          prop="id"
          width="80"
          align="center"
        />
        <el-table-column label="请假类型" width="100" align="center">
          <template #default="{ row }">
            {{ LEAVE_TYPE_MAP[row.leaveType] || "未知" }}
          </template>
        </el-table-column>
        <el-table-column label="开始日期" prop="startDate" width="120" />
        <el-table-column label="结束日期" prop="endDate" width="120" />
        <el-table-column label="天数" prop="days" width="70" align="center" />
        <el-table-column label="请假原因" min-width="160">
          <template #default="{ row }">
            <span class="text-[#606266]">{{ row.reason || "-" }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(LEAVE_STATUS_TAG[row.status as number] || 'info') as any" effect="plain">
              {{ LEAVE_STATUS_MAP[row.status] || "未知" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row as unknown as LeaveVO)">
              详情
            </el-button>
            <el-button
              link
              type="warning"
              size="small"
              v-if="row.status === 0"
              @click="handleRevoke(row as unknown as LeaveVO)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end px-5 py-4">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新建请假对话框 -->
    <el-dialog
      v-model="createVisible"
      title="新建请假"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="leaveFormRef"
        :model="leaveForm"
        :rules="leaveRules"
        label-width="90px"
      >
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="leaveForm.leaveType" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="(label, key) in LEAVE_TYPE_MAP"
              :key="key"
              :label="label"
              :value="Number(key)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="leaveForm.startDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="(d: Date) => d && d < new Date(new Date().toDateString())"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="leaveForm.endDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="(d: Date) => {
              if (!leaveForm.startDate) return d < new Date(new Date().toDateString())
              return d < new Date(leaveForm.startDate)
            }"
          />
        </el-form-item>
        <el-form-item label="请假原因" prop="reason">
          <el-input
            v-model="leaveForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入请假原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLeave">
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="请假详情"
      width="560px"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号" :span="1">
            {{ detailData.id }}
          </el-descriptions-item>
          <el-descriptions-item label="请假类型" :span="1">
            {{ LEAVE_TYPE_MAP[detailData.leaveType] || "未知" }}
          </el-descriptions-item>
          <el-descriptions-item label="开始日期" :span="1">
            {{ detailData.startDate }}
          </el-descriptions-item>
          <el-descriptions-item label="结束日期" :span="1">
            {{ detailData.endDate }}
          </el-descriptions-item>
          <el-descriptions-item label="天数" :span="1">
            {{ detailData.days }} 天
          </el-descriptions-item>
          <el-descriptions-item label="状态" :span="1">
            <el-tag
              :type="(LEAVE_STATUS_TAG[detailData.status] || 'info') as any"
              effect="plain"
            >
              {{ LEAVE_STATUS_MAP[detailData.status] || "未知" }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请员工" :span="1">
            {{ detailData.empName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="所属部门" :span="1">
            {{ detailData.deptName || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="请假原因" :span="2">
            {{ detailData.reason || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">
            {{ detailData.createTime || "-" }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间" :span="2">
            {{ detailData.updateTime || "-" }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <div v-else class="text-center text-[#909399] py-8">加载中...</div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search } from "@element-plus/icons-vue";
import {
  leaveApi,
  LEAVE_TYPE_MAP,
  LEAVE_STATUS_MAP,
  LEAVE_STATUS_TAG,
} from "@/api/hr-leave";
import type {
  LeaveVO,
  LeaveBalanceVO,
  LeaveCreateDTO,
} from "@/api/hr-leave";
import type { FormInstance, FormRules } from "element-plus";

// ── State ────────────────────────────────────────────────────────────────────

const loading = ref(false);
const submitting = ref(false);
const leaveList = ref<LeaveVO[]>([]);
const balanceList = ref<LeaveBalanceVO[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  leaveType: undefined as number | undefined,
  status: undefined as number | undefined,
});

// ── Dialogs ──────────────────────────────────────────────────────────────────

const createVisible = ref(false);
const detailVisible = ref(false);
const detailData = ref<LeaveVO | null>(null);
const leaveFormRef = ref<FormInstance>();
const leaveForm = reactive<LeaveCreateDTO>({
  leaveType: 1,
  startDate: "",
  endDate: "",
  reason: "",
});

const leaveRules: FormRules = {
  leaveType: [{ required: true, message: "请选择请假类型", trigger: "change" }],
  startDate: [{ required: true, message: "请选择开始日期", trigger: "change" }],
  endDate: [{ required: true, message: "请选择结束日期", trigger: "change" }],
  reason: [
    { required: true, message: "请输入请假原因", trigger: "blur" },
    { min: 2, message: "原因至少2个字符", trigger: "blur" },
  ],
};

// ── Helpers ──────────────────────────────────────────────────────────────────

function calcPercent(item: LeaveBalanceVO | Record<string, any>): number {
  if (!item.totalDays) return 0;
  return Math.round((item.usedDays / item.totalDays) * 100);
}

// ── Data fetching ────────────────────────────────────────────────────────────

async function fetchList() {
  loading.value = true;
  try {
    const res = await leaveApi.listMy(queryParams);
    leaveList.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: any) {
    ElMessage.error(error?.message || "获取请假列表失败");
  } finally {
    loading.value = false;
  }
}

async function fetchBalances() {
  try {
    balanceList.value = await leaveApi.getBalances();
  } catch {
    // silently ignore — balances are secondary content
  }
}

function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.pageSize = 10;
  queryParams.leaveType = undefined;
  queryParams.status = undefined;
  fetchList();
}

// ── Create ───────────────────────────────────────────────────────────────────

function showCreateDialog() {
  leaveForm.leaveType = 1;
  leaveForm.startDate = "";
  leaveForm.endDate = "";
  leaveForm.reason = "";
  createVisible.value = true;
}

async function submitLeave() {
  if (!leaveFormRef.value) return;
  try {
    await leaveFormRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await leaveApi.create(leaveForm);
    ElMessage.success("请假申请已提交");
    createVisible.value = false;
    fetchList();
    fetchBalances();
  } catch (error: any) {
    ElMessage.error(error?.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

// ── Detail ───────────────────────────────────────────────────────────────────

async function viewDetail(row: LeaveVO) {
  detailVisible.value = true;
  detailData.value = null;
  try {
    detailData.value = await leaveApi.getDetail(row.id);
  } catch (error: any) {
    ElMessage.error(error?.message || "获取详情失败");
  }
}

// ── Revoke ───────────────────────────────────────────────────────────────────

async function handleRevoke(row: LeaveVO) {
  try {
    await ElMessageBox.confirm(
      `确定要撤回申请单 #${row.id} 吗？`,
      "撤回确认",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }
    );
    await leaveApi.revoke(row.id);
    ElMessage.success("已撤回");
    fetchList();
    fetchBalances();
  } catch {
    // cancelled or error
  }
}

// ── Lifecycle ────────────────────────────────────────────────────────────────

onMounted(() => {
  fetchBalances();
  fetchList();
});
</script>
