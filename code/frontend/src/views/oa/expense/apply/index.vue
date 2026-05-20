<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <!-- 左列：经费记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的经费记录</span>
              <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="title" label="申请标题" min-width="100" show-overflow-tooltip />
            <el-table-column label="费用类别" width="90">
              <template #default="{ row }">{{ categoryMap[row.category] || "-" }}</template>
            </el-table-column>
            <el-table-column label="申请金额" width="100" align="right">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="120" show-overflow-tooltip />
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

      <!-- 右列：提交经费申请 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交经费申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="申请标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入申请标题" maxlength="50" />
            </el-form-item>

            <el-form-item label="费用类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择费用类别" style="width: 100%">
                <el-option v-for="(label, value) in categoryMap" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>

            <el-form-item label="申请金额" prop="amount">
              <el-input v-model.number="form.amount" type="number" placeholder="请输入申请金额">
                <template #prepend>￥</template>
              </el-input>
            </el-form-item>

            <el-form-item label="费用说明" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入费用说明" maxlength="200" show-word-limit />
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
import { getExpensePage, submitExpense } from "@/api/expense";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const categoryMap: Record<number, string> = { 1: "差旅费", 2: "办公用品", 3: "招待费", 4: "其他" };

// --- 列表 ---
const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getExpensePage({ pageNum: pageNum.value, pageSize: pageSize.value });
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

const formatAmount = (amount?: number) => {
  if (amount == null) return "-";
  return `￥${amount.toFixed(2)}`;
};

// --- 表单 ---
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  title: "",
  category: undefined as number | undefined,
  amount: undefined as number | undefined,
  description: ""
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: "请输入申请标题", trigger: "blur" }],
  category: [{ required: true, message: "请选择费用类别", trigger: "change" }],
  amount: [{ required: true, message: "请输入申请金额", trigger: "blur" }],
  description: [{ required: true, message: "请输入费用说明", trigger: "blur" }]
});

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  submitting.value = true;
  try {
    await submitExpense({
      empId: userStore.userInfo?.empId,
      title: form.title,
      category: form.category,
      amount: form.amount,
      description: form.description
    });
    ElMessage.success("经费申请已提交");
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
