<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <!-- 左列：我的请假记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的请假记录</span>
              <div class="flex items-center gap-2">
                <el-button type="success" size="small" @click="handleExport">导出</el-button>
                <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
              </div>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column label="类型" width="70">
              <template #default="{ row }">{{ leaveTypeMap[row.leaveType] || "其他" }}</template>
            </el-table-column>
            <el-table-column label="开始时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="天数" width="60" align="center">
              <template #default="{ row }">{{ calcDays(row.startTime, row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="原因" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reason || "-" }}</template>
            </el-table-column>
            <el-table-column label="状态" width="160" align="center">
              <template #default="{ row }">
                <el-tag :type="formatStatusTagType(row.status)" size="small" effect="light">{{ formatStatusText(row.status) }}</el-tag>
                <el-button v-if="row.status !== 0" type="info" link size="small" class="ml-1" @click="showDetail(row)">详情</el-button>
                <el-button v-if="row.status === 0" type="warning" link size="small" @click="handleWithdraw(row)">撤回</el-button>
                <el-button v-if="row.status === 0" type="info" link size="small" @click="handleUrge(row)">催办</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background small @change="fetchList" />
          </div>
        </el-card>
      </el-col>

      <!-- 右列：提交请假申请 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交请假申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="请假类型" prop="leaveType">
              <el-select v-model="form.leaveType" placeholder="请选择请假类型" style="width: 100%">
                <el-option v-for="(label, value) in leaveTypeMap" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>

            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>

            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="请选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="(d: Date) => disableEndDate(d)" />
            </el-form-item>

            <el-form-item label="请假原因" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入请假原因" maxlength="200" show-word-limit />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="detailVisible" title="审批详情" width="600px">
      <ApprovalTimeline v-if="detailRow?.id" business-type="leave" :business-id="detailRow.id" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { withdrawApplication, urgeTask } from "@/api/workflow";
import { getLeavePage, submitLeave } from "@/api/leave";
import { useUserStore } from "@/store/user";
import { downloadFile } from "@/utils/download";
import { LEAVE_TYPE_MAP } from "@/utils/constants";
import { formatTime, formatStatusText, formatStatusTagType } from "@/utils/format";

const userStore = useUserStore();
const leaveTypeMap = LEAVE_TYPE_MAP;

// --- 列表 ---
const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getLeavePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

const calcDays = (startTime?: string, endTime?: string) => {
  if (!startTime || !endTime) return "-";
  const diff = new Date(endTime).getTime() - new Date(startTime).getTime();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

// --- 详情 ---
const detailVisible = ref(false);
const detailRow = ref<any>(null);
const showDetail = (row: any) => {
  detailRow.value = row;
  detailVisible.value = true;
};

// --- 表单 ---
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  leaveType: undefined as number | undefined,
  startTime: "",
  endTime: "",
  reason: ""
});

const rules = reactive<FormRules>({
  leaveType: [{ required: true, message: "请选择请假类型", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }],
  reason: [{ required: true, message: "请输入请假原因", trigger: "blur" }]
});

const disableEndDate = (date: Date) => {
  if (!form.startTime) return false;
  return date < new Date(form.startTime);
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  if (form.startTime && form.endTime && form.startTime >= form.endTime) {
    ElMessage.warning("结束时间必须晚于开始时间");
    return;
  }

  submitting.value = true;
  try {
    await submitLeave({
      empId: userStore.userInfo?.empId,
      leaveType: form.leaveType,
      startTime: form.startTime,
      endTime: form.endTime,
      reason: form.reason
    });
    ElMessage.success("请假申请已提交");
    resetForm();
    pageNum.value = 1;
    fetchList();
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false;
  }
};

const resetForm = () => {
  formRef.value?.resetFields();
};

const handleWithdraw = async (row: any) => {
  try {
    await ElMessageBox.confirm("确定要撤回此申请吗？", "撤回确认", { type: "warning" });
    await withdrawApplication({ businessType: "leave", businessId: row.id });
    ElMessage.success("申请已撤回");
    fetchList();
  } catch {}
};

const handleUrge = async (row: any) => {
  try {
    await urgeTask({ businessType: "leave", businessId: row.id });
    ElMessage.success("已发送催办提醒");
  } catch {}
};

const handleExport = async () => {
  try {
    await downloadFile("/api/leave/export", "请假数据.xlsx");
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  }
};

onMounted(() => {
  fetchList();
});
</script>
