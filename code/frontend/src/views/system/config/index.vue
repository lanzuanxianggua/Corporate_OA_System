<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">参数配置</span>
          <el-button type="primary" @click="openDialog()">新增参数</el-button>
        </div>
      </template>

      <div class="mb-4">
        <el-input v-model="searchKey" placeholder="搜索参数名称/键名" style="width: 280px" clearable @clear="fetchList" @keyup.enter="fetchList">
          <template #append>
            <el-button @click="fetchList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <el-table-column prop="configName" label="参数名称" min-width="150" />
        <el-table-column prop="configKey" label="参数键名" min-width="150" />
        <el-table-column prop="configValue" label="参数值" min-width="120" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
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
      </el-table>

      <div class="mt-4 flex justify-end">
        <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑参数' : '新增参数'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入参数键名" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
import { getConfigPage, addConfig, updateConfig, deleteConfig } from "@/api/config";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getConfigPage({ pageNum: pageNum.value, pageSize: pageSize.value, searchKey: searchKey.value || undefined });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, configName: "", configKey: "", configValue: "", remark: "" });
const rules = reactive<FormRules>({
  configName: [{ required: true, message: "请输入参数名称", trigger: "blur" }],
  configKey: [{ required: true, message: "请输入参数键名", trigger: "blur" }],
  configValue: [{ required: true, message: "请输入参数值", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, configName: row.configName, configKey: row.configKey, configValue: row.configValue, remark: row.remark || "" });
  } else {
    Object.assign(form, { id: undefined, configName: "", configKey: "", configValue: "", remark: "" });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) {
      await updateConfig(form);
    } else {
      await addConfig(form);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deleteConfig(id);
  ElMessage.success("删除成功");
  fetchList();
};

onMounted(() => { fetchList(); });
</script>
