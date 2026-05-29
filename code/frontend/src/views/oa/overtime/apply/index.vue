<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的加班记录</span>
              <div class="flex items-center gap-2">
                <el-button type="success" size="small" @click="handleExport">导出</el-button>
                <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
              </div>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <template #empty>
              <el-empty description="暂无加班记录" :image-size="60" />
            </template>
            <el-table-column label="开始时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" min-width="140">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="时长(h)" width="80" align="center">
              <template #default="{ row }">{{ row.hours || "-" }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
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

      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交加班申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="加班日期" prop="overtimeDate">
              <el-date-picker v-model="form.overtimeDate" type="date" placeholder="请选择加班日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="请选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
            <el-form-item label="加班时长" prop="hours">
              <el-input v-model.number="form.hours" type="number" placeholder="请输入加班时长（小时）" :min="0.5" :step="0.5">
                <template #append>小时</template>
              </el-input>
            </el-form-item>
            <el-form-item label="加班原因" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入加班原因" maxlength="200" show-word-limit />
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
      <ApprovalTimeline v-if="detailRow?.id" business-type="overtime" :business-id="detailRow.id" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { withdrawApplication, urgeTask } from "@/api/workflow";
import { submitOvertime, getOvertimePage } from "@/api/overtime";
import { useUserStore } from "@/store/user";
import { formatTime, formatStatusText, formatStatusTagType } from "@/utils/format";
import { downloadFile } from "@/utils/download";

const userStore = useUserStore();

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getOvertimePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("获取加班记录失败");
  } finally {
    loading.value = false;
  }
};

// --- 详情 ---
const detailVisible = ref(false);
const detailRow = ref<any>(null);
const showDetail = (row: any) => {
  detailRow.value = row;
  detailVisible.value = true;
};

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive({ overtimeDate: "", startTime: "", endTime: "", hours: undefined as number | undefined, reason: "" });
const rules = reactive<FormRules>({
  overtimeDate: [{ required: true, message: "请选择加班日期", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }],
  hours: [{ required: true, message: "请输入加班时长", trigger: "blur" }],
  reason: [{ required: true, message: "请输入加班原因", trigger: "blur" }]
});

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    await submitOvertime({ empId: userStore.userInfo?.empId, ...form });
    ElMessage.success("加班申请已提交");
    resetForm();
    pageNum.value = 1;
    fetchList();
  } catch (e) {
    ElMessage.error("提交加班申请失败");
  } finally {
    submitting.value = false;
  }
};

const resetForm = () => { formRef.value?.resetFields(); };

const handleWithdraw = async (row: any) => {
  try {
    await ElMessageBox.confirm("确定要撤回此申请吗？", "撤回确认", { type: "warning" });
    await withdrawApplication({ businessType: "overtime", businessId: row.id });
    ElMessage.success("申请已撤回");
    fetchList();
  } catch {}
};

const handleUrge = async (row: any) => {
  try {
    await urgeTask({ businessType: "overtime", businessId: row.id });
    ElMessage.success("已发送催办提醒");
  } catch {}
};

const handleExport = async () => {
  try {
    await downloadFile("/api/overtime/export", "加班数据.xlsx");
    ElMessage.success("导出成功");
  } catch {
    ElMessage.error("导出失败");
  }
};

onMounted(() => { fetchList(); });
</script>
