<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">流程定义</span>
          <el-button type="primary" @click="openDialog()">新增定义</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="processName" label="流程名称" min-width="150" />
        <el-table-column prop="processKey" label="流程标识" min-width="120" />
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="processType" label="分类" min-width="100" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">{{ row.status === "0" ? "已激活" : "未激活" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批流程" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ nodeSummary(row.nodeConfig) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleActivate(row)">{{ row.status === "0" ? "停用" : "激活" }}</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无流程定义" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑流程定义' : '新增流程定义'" width="900px" :close-on-click-modal="false" top="5vh" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <div class="flex gap-4 mb-3">
          <el-form-item label="流程名称" prop="processName" class="flex-1">
            <el-input v-model="form.processName" placeholder="请输入流程名称" />
          </el-form-item>
          <el-form-item label="流程标识" prop="processKey" class="flex-1">
            <el-input v-model="form.processKey" placeholder="请输入流程标识" :disabled="isEdit" />
          </el-form-item>
        </div>
        <el-form-item label="分类" prop="processType">
          <el-select v-model="form.processType" placeholder="请选择分类" style="width: 200px">
            <el-option label="请假" value="leave" />
            <el-option label="出差" value="trip" />
            <el-option label="外出" value="outing" />
            <el-option label="采购" value="purchase" />
            <el-option label="经费" value="expense" />
            <el-option label="合同" value="contract" />
            <el-option label="加班" value="overtime" />
            <el-option label="借款" value="loan" />
          </el-select>
        </el-form-item>
        <el-form-item label="设计模式">
          <el-radio-group v-model="designMode">
            <el-radio value="simple">简单模式</el-radio>
            <el-radio value="visual">可视化模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批流程" prop="nodeConfig">
          <WorkflowDesigner v-if="designMode === 'simple'" ref="designerRef" v-model="form.nodeConfig" :process-type="form.processType" />
          <FlowDesigner v-else v-model="form.nodeConfig" />
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
import { getDefinitions, createDefinition, updateDefinition, activateDefinition } from "@/api/workflow";
import WorkflowDesigner from "@/components/WorkflowDesigner.vue";
import FlowDesigner from "@/components/flow-designer/FlowDesigner.vue";

const designMode = ref<"simple" | "visual">("simple");

const loading = ref(false);
const tableData = ref<any[]>([]);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getDefinitions({});
    tableData.value = res.data || [];
  } catch {
    ElMessage.error("获取流程定义失败");
  } finally {
    loading.value = false;
  }
};

const nodeSummary = (nodeConfig: string) => {
  try {
    const parsed = JSON.parse(nodeConfig);
    if (parsed.nodes && parsed.edges) {
      // Graph format
      return parsed.nodes.filter((n: any) => n.nodeType === "approval").map((n: any) => n.name).join(" -> ") || "无审批节点";
    }
    // Legacy flat array
    if (!parsed.length) return "无节点";
    return parsed.map((n: any) => n.nodeName).join(" -> ");
  } catch {
    return "配置错误";
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const isEdit = ref(false);
const editingId = ref<number | undefined>(undefined);
const formRef = ref<FormInstance>();
const designerRef = ref<InstanceType<typeof WorkflowDesigner>>();
const form = reactive({ processName: "", processKey: "", processType: "", nodeConfig: "" });
const rules = reactive<FormRules>({
  processName: [{ required: true, message: "请输入流程名称", trigger: "blur" }],
  processKey: [{ required: true, message: "请输入流程标识", trigger: "blur" }],
  processType: [{ required: true, message: "请选择分类", trigger: "change" }],
  nodeConfig: [{ required: true, message: "请配置审批流程", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  isEdit.value = !!row;
  if (row) {
    editingId.value = row.id;
    form.processName = row.processName || "";
    form.processKey = row.processKey || "";
    form.processType = row.processType || "";
    form.nodeConfig = row.nodeConfig || "[]";
  } else {
    editingId.value = undefined;
    form.processName = "";
    form.processKey = "";
    form.processType = "";
    form.nodeConfig = "";
    formRef.value?.resetFields();
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  if (designMode.value === "simple" && designerRef.value) {
    if (!designerRef.value.validateNodes()) return;
  }
  saving.value = true;
  try {
    if (isEdit.value && editingId.value) {
      await updateDefinition({ id: editingId.value, ...form });
      ElMessage.success("更新成功");
    } else {
      await createDefinition(form);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchList();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleActivate = async (row: any) => {
  try {
    await activateDefinition(row.id);
    ElMessage.success(row.status === "0" ? "已停用" : "已激活");
    fetchList();
  } catch {
    ElMessage.error("操作失败");
  }
};

onMounted(() => {
  fetchList();
});
</script>
