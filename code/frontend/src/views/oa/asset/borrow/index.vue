<template>
  <div class="h-full">
    <el-row :gutter="20" class="h-full">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="h-full">
          <template #header>
            <span class="text-base font-semibold text-[var(--oa-text)]">资产借用记录</span>
          </template>

          <el-table :data="tableData" v-loading="loading" stripe style="width: 100%" size="small" :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
            <el-table-column prop="assetName" label="资产名称" min-width="100" />
            <el-table-column prop="borrower" label="借用人" width="80" />
            <el-table-column prop="borrowTime" label="借用时间" min-width="120">
              <template #default="{ row }">{{ formatTime(row.borrowTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.returned ? 'success' : 'warning'" size="small">{{ row.returned ? "已归还" : "借用中" }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4 flex justify-end">
            <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>
            <span class="text-base font-semibold text-[var(--oa-text)]">申请借用</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="max-w-[460px]">
            <el-form-item label="资产" prop="assetId">
              <el-select v-model="form.assetId" placeholder="请选择资产" style="width: 100%" filterable>
                <el-option v-for="a in assets" :key="a.id" :label="`${a.assetName} (${a.assetCode})`" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="借用原因" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入借用原因" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleBorrow">申请借用</el-button>
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
import { getAssetPage, getBorrowPage, borrowAsset } from "@/api/asset";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const assets = ref<any[]>([]);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getBorrowPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const fetchAssets = async () => {
  try {
    const res: any = await getAssetPage({ pageNum: 1, pageSize: 200, status: "0" });
    assets.value = res.data?.list || [];
  } catch {}
};

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive({ assetId: undefined as number | undefined, remark: "" });
const rules = reactive<FormRules>({
  assetId: [{ required: true, message: "请选择资产", trigger: "change" }],
  remark: [{ required: true, message: "请输入借用原因", trigger: "blur" }]
});

const handleBorrow = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    await borrowAsset(form);
    ElMessage.success("借用申请已提交");
    formRef.value.resetFields();
    fetchList();
  } finally {
    submitting.value = false;
  }
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => { fetchList(); fetchAssets(); });
</script>
