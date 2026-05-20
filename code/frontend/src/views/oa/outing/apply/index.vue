<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <!-- 左列：外出记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的外出记录</span>
              <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="location" label="外出地点" min-width="100" show-overflow-tooltip />
            <el-table-column prop="reason" label="事由" min-width="120" show-overflow-tooltip />
            <el-table-column label="开始时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="light">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background small @change="fetchList" />
          </div>
        </el-card>
      </el-col>

      <!-- 右列：提交外出申请 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交外出申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="外出事由" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入外出事由" maxlength="200" show-word-limit />
            </el-form-item>

            <el-form-item label="外出地点" prop="location">
              <el-input v-model="form.location" placeholder="请输入外出地点" maxlength="50" />
            </el-form-item>

            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>

            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="请选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" :disabled-date="(d: Date) => disableEndDate(d)" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getOutingPage, submitOuting } from "@/api/outing";
import { useUserStore } from "@/store/user";

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
    const res: any = await getOutingPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

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

// --- 表单 ---
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  reason: "",
  location: "",
  startTime: "",
  endTime: ""
});

const rules = reactive<FormRules>({
  reason: [{ required: true, message: "请输入外出事由", trigger: "blur" }],
  location: [{ required: true, message: "请输入外出地点", trigger: "blur" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }]
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
    await submitOuting({
      empId: userStore.userInfo?.empId,
      reason: form.reason,
      location: form.location,
      startTime: form.startTime,
      endTime: form.endTime
    });
    ElMessage.success("外出申请已提交");
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

onMounted(() => {
  fetchList();
});
</script>
