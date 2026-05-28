<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <!-- 左列：出差记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的出差记录</span>
              <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <template #empty>
              <el-empty description="暂无出差记录" :image-size="60" />
            </template>
            <el-table-column prop="destination" label="目的地" width="90" show-overflow-tooltip />
            <el-table-column prop="purpose" label="事由" min-width="100" show-overflow-tooltip />
            <el-table-column label="出发时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="返回时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="天数" width="60" align="center">
              <template #default="{ row }">{{ calcDays(row.startTime, row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="160" align="center">
              <template #default="{ row }">
                <el-tag :type="(formatStatusTagType(row.status) as any)" size="small" effect="light">{{ formatStatusText(row.status) }}</el-tag>
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

      <!-- 右列：提交出差申请 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交出差申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="目的地" prop="destination">
              <el-input v-model="form.destination" placeholder="请输入出差目的地" maxlength="50" />
            </el-form-item>

            <el-form-item label="出差事由" prop="purpose">
              <el-input v-model="form.purpose" type="textarea" :rows="4" placeholder="请输入出差事由" maxlength="200" show-word-limit />
            </el-form-item>

            <el-form-item label="出发时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择出发时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>

            <el-form-item label="返回时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="请选择返回时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="(d: Date) => disableEndDate(d)" />
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
      <ApprovalTimeline v-if="detailRow?.id" business-type="trip" :business-id="detailRow.id" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { withdrawApplication, urgeTask } from "@/api/workflow";
import { getBusinessTripPage, submitBusinessTrip } from "@/api/businessTrip";
import { useUserStore } from "@/store/user";
import { formatTime, formatStatusText, formatStatusTagType } from "@/utils/format";

const userStore = useUserStore();

// --- 列表 ---
const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getBusinessTripPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("获取出差记录失败");
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
  destination: "",
  purpose: "",
  startTime: "",
  endTime: ""
});

const rules = reactive<FormRules>({
  destination: [{ required: true, message: "请输入出差目的地", trigger: "blur" }],
  purpose: [{ required: true, message: "请输入出差事由", trigger: "blur" }],
  startTime: [{ required: true, message: "请选择出发时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择返回时间", trigger: "change" }]
});

const disableEndDate = (date: Date) => {
  if (!form.startTime) return false;
  return date < new Date(form.startTime);
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  if (form.startTime && form.endTime && form.startTime >= form.endTime) {
    ElMessage.warning("返回时间必须晚于出发时间");
    return;
  }

  submitting.value = true;
  try {
    await submitBusinessTrip({
      empId: userStore.userInfo?.empId,
      destination: form.destination,
      purpose: form.purpose,
      startTime: form.startTime,
      endTime: form.endTime
    });
    ElMessage.success("出差申请已提交");
    resetForm();
    pageNum.value = 1;
    fetchList();
  } catch (e) {
    ElMessage.error("提交出差申请失败");
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
    await withdrawApplication({ businessType: "trip", businessId: row.id });
    ElMessage.success("申请已撤回");
    fetchList();
  } catch {}
};

const handleUrge = async (row: any) => {
  try {
    await urgeTask({ businessType: "trip", businessId: row.id });
    ElMessage.success("已发送催办提醒");
  } catch {}
};

onMounted(() => {
  fetchList();
});
</script>
