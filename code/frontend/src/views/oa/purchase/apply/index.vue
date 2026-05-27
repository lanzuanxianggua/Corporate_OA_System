<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <!-- 左列：采购记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="h-full">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="text-base font-semibold text-[#303133]">我的采购记录</span>
              <el-tag type="info" size="small">共 {{ total }} 条</el-tag>
            </div>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
            <el-table-column prop="itemName" label="采购物品" min-width="100" show-overflow-tooltip />
            <el-table-column label="数量" width="70" align="center">
              <template #default="{ row }">{{ row.quantity || "-" }}</template>
            </el-table-column>
            <el-table-column label="预估金额" width="100" align="right">
              <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
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

      <!-- 右列：提交采购申请 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[#303133]">提交采购申请</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="right" class="max-w-[560px]">
            <el-form-item label="采购物品" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请输入采购物品名称" maxlength="50" />
            </el-form-item>

            <el-form-item label="数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="9999" placeholder="请输入数量" style="width: 100%" />
            </el-form-item>

            <el-form-item label="预估金额" prop="amount">
              <el-input v-model.number="form.amount" type="number" placeholder="请输入预估金额">
                <template #prepend>￥</template>
              </el-input>
            </el-form-item>

            <el-form-item label="采购原因" prop="reason">
              <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入采购原因" maxlength="200" show-word-limit />
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
      <ApprovalTimeline v-if="detailRow?.id" business-type="purchase" :business-id="detailRow.id" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import { withdrawApplication, urgeTask } from "@/api/workflow";
import { getPurchasePage, submitPurchase } from "@/api/purchase";
import { useUserStore } from "@/store/user";
import { formatStatusText, formatStatusTagType } from "@/utils/format";

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
    const res: any = await getPurchasePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

const formatAmount = (amount?: number) => {
  if (amount == null) return "-";
  return `￥${amount.toFixed(2)}`;
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
  itemName: "",
  quantity: 1,
  amount: undefined as number | undefined,
  reason: ""
});

const rules = reactive<FormRules>({
  itemName: [{ required: true, message: "请输入采购物品名称", trigger: "blur" }],
  quantity: [{ required: true, message: "请输入数量", trigger: "change" }],
  amount: [{ required: true, message: "请输入预估金额", trigger: "blur" }],
  reason: [{ required: true, message: "请输入采购原因", trigger: "blur" }]
});

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  submitting.value = true;
  try {
    await submitPurchase({
      empId: userStore.userInfo?.empId,
      itemName: form.itemName,
      quantity: form.quantity,
      amount: form.amount,
      reason: form.reason
    });
    ElMessage.success("采购申请已提交");
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
    await withdrawApplication({ businessType: "purchase", businessId: row.id });
    ElMessage.success("申请已撤回");
    fetchList();
  } catch {}
};

const handleUrge = async (row: any) => {
  try {
    await urgeTask({ businessType: "purchase", businessId: row.id });
    ElMessage.success("已发送催办提醒");
  } catch {}
};

onMounted(() => {
  fetchList();
});
</script>
