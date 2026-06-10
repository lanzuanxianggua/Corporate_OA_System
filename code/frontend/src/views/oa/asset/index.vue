<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">资产管理</span>
          <el-button type="primary" @click="openDialog()">新增资产</el-button>
        </div>
      </template>

      <div class="mb-4">
        <el-input v-model="searchKey" placeholder="搜索资产名称/编号" style="width: 280px" clearable @clear="fetchList" @keyup.enter="fetchList">
          <template #append>
            <el-button @click="fetchList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <el-table-column prop="assetCode" label="资产编号" width="120" />
        <el-table-column prop="assetName" label="资产名称" min-width="120" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : row.status === '1' ? 'warning' : 'danger'" size="small">
              {{ row.status === '0' ? "闲置" : row.status === '1' ? "借用中" : "维修" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无资产数据" />
        </template>
      </el-table>

      <div class="mt-4 flex justify-end">
        <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资产' : '新增资产'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="资产编号" prop="assetCode">
          <el-input v-model="form.assetCode" placeholder="请输入资产编号" />
        </el-form-item>
        <el-form-item label="资产名称" prop="assetName">
          <el-input v-model="form.assetName" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="请输入分类" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { getAssetPage, addAsset, updateAsset, deleteAsset } from "@/api/asset";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getAssetPage({ pageNum: pageNum.value, pageSize: pageSize.value, assetName: searchKey.value || undefined, assetCode: searchKey.value || undefined });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } catch {
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, assetCode: "", assetName: "", category: "", remark: "" });
const rules = reactive<FormRules>({
  assetCode: [{ required: true, message: "请输入资产编号", trigger: "blur" }],
  assetName: [{ required: true, message: "请输入资产名称", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, assetCode: row.assetCode, assetName: row.assetName, category: row.category || "", remark: row.remark || "" });
  } else {
    Object.assign(form, { id: undefined, assetCode: "", assetName: "", category: "", remark: "" });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) await updateAsset(form);
    else await addAsset(form);
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteAsset(id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(() => { fetchList(); });
</script>
