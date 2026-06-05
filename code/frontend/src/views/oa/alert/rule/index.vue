<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">预警规则</span>
          <el-button type="primary" @click="openDialog()">新增规则</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="100" />
        <el-table-column prop="condition" label="触发条件" min-width="150" show-overflow-tooltip />
        <el-table-column prop="level" label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'warning' : row.level === 2 ? 'danger' : 'info'" size="small">
              {{ row.level === 1 ? "警告" : row.level === 2 ? "严重" : "提示" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
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
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑规则' : '新增规则'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-input v-model="form.ruleType" placeholder="请输入规则类型" />
        </el-form-item>
        <el-form-item label="触发条件" prop="condition">
          <el-input v-model="form.condition" placeholder="请输入触发条件" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio :value="0">提示</el-radio>
            <el-radio :value="1">警告</el-radio>
            <el-radio :value="2">严重</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="通知方式">
          <el-input v-model="form.notifyType" placeholder="如：email, sms" />
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
import { getAlertRulePage, addAlertRule, updateAlertRule, deleteAlertRule } from "@/api/alert";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getAlertRulePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleToggle = async (row: any) => {
  try {
    await updateAlertRule({ id: row.id, enabled: row.enabled });
    ElMessage.success(row.enabled ? "已启用" : "已禁用");
  } catch {
    row.enabled = !row.enabled;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  id: undefined as number | undefined, ruleName: "", ruleType: "",
  condition: "", level: 1, notifyType: ""
});
const rules = reactive<FormRules>({
  ruleName: [{ required: true, message: "请输入规则名称", trigger: "blur" }],
  ruleType: [{ required: true, message: "请输入规则类型", trigger: "blur" }],
  condition: [{ required: true, message: "请输入触发条件", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, ruleName: row.ruleName, ruleType: row.ruleType, condition: row.condition, level: row.level ?? 1, notifyType: row.notifyType || "" });
  } else {
    Object.assign(form, { id: undefined, ruleName: "", ruleType: "", condition: "", level: 1, notifyType: "" });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) await updateAlertRule(form);
    else await addAlertRule(form);
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deleteAlertRule(id);
  ElMessage.success("删除成功");
  fetchList();
};

onMounted(() => { fetchList(); });
</script>
