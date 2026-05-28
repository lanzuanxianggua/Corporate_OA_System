<template>
  <div class="workflow-designer">
    <div v-for="(node, idx) in nodes" :key="idx" class="node-wrapper">
      <el-card shadow="hover" class="node-card">
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm font-medium text-gray-500">节点 {{ idx + 1 }}</span>
          <div class="flex gap-1">
            <el-button
              :icon="ArrowUp"
              size="small"
              circle
              :disabled="idx === 0"
              @click="moveNode(idx, -1)"
            />
            <el-button
              :icon="ArrowDown"
              size="small"
              circle
              :disabled="idx === nodes.length - 1"
              @click="moveNode(idx, 1)"
            />
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              circle
              @click="removeNode(idx)"
            />
          </div>
        </div>

        <el-form label-width="80px" size="default">
          <el-form-item label="节点名称">
            <el-input v-model="node.nodeName" placeholder="如：部门主管审批" />
          </el-form-item>

          <el-form-item label="审批人类型">
            <el-select v-model="node.assigneeType" placeholder="选择类型" class="w-full" @change="onAssigneeTypeChange(node)">
              <el-option
                v-for="opt in assigneeTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="审批人">
            <el-select
              v-if="node.assigneeType === 'role' || node.assigneeType === 'role_global'"
              v-model="node.assigneeValue"
              placeholder="选择角色"
              class="w-full"
              filterable
            >
              <el-option
                v-for="role in roleList"
                :key="role.id"
                :label="role.roleName"
                :value="String(role.id)"
              />
            </el-select>
            <el-select
              v-else
              v-model="node.assigneeValue"
              placeholder="搜索并选择员工"
              class="w-full"
              filterable
              remote
              :remote-method="searchEmployee"
              :loading="empSearchLoading"
            >
              <el-option
                v-for="emp in empOptions"
                :key="emp.id"
                :label="emp.empName + ' (' + emp.empCode + ')'"
                :value="String(emp.id)"
              />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">多人审批</el-divider>

          <el-form-item label="启用多人">
            <el-switch v-model="node.multiType" active-value="countersign" inactive-value="" @change="(val: string | number | boolean) => { if (!val) { node.multiType = undefined; node.multiAssigneeIds = []; } }" />
          </el-form-item>

          <template v-if="node.multiType">
            <el-form-item label="审批方式">
              <el-radio-group v-model="node.multiType">
                <el-radio value="countersign">会签</el-radio>
                <el-radio value="orsign">或签</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="额外审批人">
              <el-select
                v-model="node.multiAssigneeIds"
                multiple
                filterable
                remote
                :remote-method="searchEmployee"
                :loading="empSearchLoading"
                placeholder="搜索并选择额外审批人"
                class="w-full"
              >
                <el-option
                  v-for="emp in empOptions"
                  :key="emp.id"
                  :label="emp.empName + ' (' + emp.empCode + ')'"
                  :value="emp.id"
                />
              </el-select>
            </el-form-item>
          </template>

          <el-divider content-position="left">抄送人</el-divider>

          <el-form-item label="抄送人">
            <el-select
              v-model="node.ccList"
              multiple
              filterable
              remote
              :remote-method="searchEmployee"
              :loading="empSearchLoading"
              placeholder="搜索并选择抄送人"
              class="w-full"
            >
              <el-option
                v-for="emp in ccEmpOptions"
                :key="emp.id"
                :label="emp.empName + ' (' + emp.empCode + ')'"
                :value="emp.id"
              />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">超时设置</el-divider>

          <el-form-item label="超时时长">
            <el-input-number v-model="node.timeoutHours" :min="0" :max="720" :step="1" placeholder="0表示不超时" class="w-full" />
            <span class="text-xs text-gray-400 ml-2">小时（0表示不超时）</span>
          </el-form-item>

          <el-form-item v-if="node.timeoutHours > 0" label="超时动作">
            <el-select v-model="node.timeoutAction" placeholder="选择超时动作" class="w-full">
              <el-option label="仅通知催办" value="notify_only" />
              <el-option label="自动通过" value="auto_approve" />
              <el-option label="自动驳回" value="auto_reject" />
              <el-option label="上报处理" value="escalate" />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">条件</el-divider>

          <div v-for="(cond, cIdx) in node.conditions" :key="cIdx" class="condition-row">
            <el-select v-model="cond.field" placeholder="字段" class="!w-28" style="width: 112px">
              <el-option
                v-for="f in conditionFields"
                :key="f.value"
                :label="f.label"
                :value="f.value"
              />
            </el-select>
            <el-select v-model="cond.operator" placeholder="运算" class="!w-20" style="width: 80px">
              <el-option
                v-for="op in operatorOptions"
                :key="op.value"
                :label="op.label"
                :value="op.value"
              />
            </el-select>
            <el-input-number v-model="cond.value" :controls="false" placeholder="值" class="flex-1" style="min-width: 80px" />
            <el-button type="danger" :icon="Delete" circle size="small" @click="removeCondition(idx, cIdx)" />
          </div>
          <el-button type="primary" link size="small" @click="addCondition(idx)">+ 添加条件</el-button>
        </el-form>
      </el-card>

      <div v-if="idx < nodes.length - 1" class="node-arrow">
        <el-icon size="24" color="#909399"><ArrowDown /></el-icon>
      </div>
    </div>

    <el-button type="primary" plain class="w-full mt-3" @click="addNode">+ 添加审批节点</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue";
import { ArrowUp, ArrowDown, Delete } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getRoles } from "@/api/system";
import request from "@/utils/request";

interface Condition {
  field: string;
  operator: string;
  value: number;
}

interface WorkflowNode {
  nodeIndex: number;
  nodeName: string;
  assigneeType: string;
  assigneeValue: string;
  multiType: string | undefined;
  multiAssigneeIds: number[];
  ccList: number[];
  timeoutHours: number;
  timeoutAction: string;
  conditions: Condition[];
}

const props = defineProps<{
  modelValue: string;
  processType: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", val: string): void;
}>();

const conditionFieldMap: Record<string, Array<{ label: string; value: string }>> = {
  leave: [{ label: "请假天数", value: "days" }],
  trip: [{ label: "出差天数", value: "days" }],
  outing: [{ label: "外出天数", value: "days" }],
  purchase: [
    { label: "采购金额", value: "amount" },
    { label: "采购数量", value: "quantity" }
  ],
  expense: [{ label: "报销金额", value: "amount" }],
  overtime: [{ label: "加班时长(小时)", value: "hours" }],
  loan: [{ label: "借款金额", value: "amount" }]
};

const assigneeTypeOptions = [
  { label: "按角色(本部门)", value: "role" },
  { label: "按角色(全公司)", value: "role_global" },
  { label: "指定人员", value: "specific" }
];

const operatorOptions = [
  { label: "<=", value: "<=" },
  { label: "<", value: "<" },
  { label: ">", value: ">" },
  { label: ">=", value: ">=" },
  { label: "==", value: "==" },
  { label: "!=", value: "!=" },
  { label: "等于", value: "equals" },
  { label: "不等于", value: "not_equals" },
  { label: "包含", value: "contains" },
  { label: "开头是", value: "starts_with" },
  { label: "在列表中", value: "in" }
];

const conditionFields = computed(() => {
  return conditionFieldMap[props.processType] || [];
});

const nodes = ref<WorkflowNode[]>([]);
const roleList = ref<any[]>([]);
const empOptions = ref<any[]>([]);
const ccEmpOptions = ref<any[]>([]);
const empSearchLoading = ref(false);

const parseModelValue = (val: string) => {
  if (!val) {
    nodes.value = [];
    return;
  }
  try {
    const parsed = JSON.parse(val);
    if (Array.isArray(parsed)) {
      nodes.value = parsed.map((n: any, i: number) => ({
        nodeIndex: n.nodeIndex ?? i,
        nodeName: n.nodeName || "",
        assigneeType: n.assigneeType || "role",
        assigneeValue: n.assigneeValue || "",
        multiType: n.multiType || undefined,
        multiAssigneeIds: Array.isArray(n.multiAssigneeIds) ? n.multiAssigneeIds : [],
        ccList: Array.isArray(n.ccList) ? n.ccList : [],
        timeoutHours: n.timeoutHours ?? 0,
        timeoutAction: n.timeoutAction || "notify_only",
        conditions: Array.isArray(n.conditions)
          ? n.conditions.map((c: any) => ({
              field: c.field || "",
              operator: c.operator || "<=",
              value: Number(c.value) || 0
            }))
          : []
      }));
    } else {
      nodes.value = [];
    }
  } catch {
    nodes.value = [];
  }
};

const validateNodes = (): boolean => {
  if (nodes.value.length === 0) {
    ElMessage.warning("请至少添加一个审批节点");
    return false;
  }
  for (let i = 0; i < nodes.value.length; i++) {
    const node = nodes.value[i];
    if (!node.nodeName.trim()) {
      ElMessage.warning(`节点 ${i + 1}: 请输入节点名称`);
      return false;
    }
    if (!node.assigneeType) {
      ElMessage.warning(`节点 ${i + 1}: 请选择审批人类型`);
      return false;
    }
  }
  return true;
};

const emitUpdate = () => {
  const output = nodes.value.map((n, i) => ({
    nodeIndex: i,
    nodeName: n.nodeName,
    assigneeType: n.assigneeType,
    assigneeValue: n.assigneeValue,
    multiType: n.multiType,
    multiAssigneeIds: n.multiAssigneeIds,
    ccList: n.ccList,
    timeoutHours: n.timeoutHours,
    timeoutAction: n.timeoutAction,
    conditions: n.conditions
  }));
  emit("update:modelValue", JSON.stringify(output));
};

watch(
  () => props.modelValue,
  (val) => parseModelValue(val),
  { immediate: true }
);

watch(
  nodes,
  () => emitUpdate(),
  { deep: true }
);

const createEmptyNode = (): WorkflowNode => ({
  nodeIndex: nodes.value.length,
  nodeName: "",
  assigneeType: "role",
  assigneeValue: "",
  multiType: undefined,
  multiAssigneeIds: [],
  ccList: [],
  timeoutHours: 0,
  timeoutAction: "notify_only",
  conditions: []
});

const addNode = () => {
  nodes.value.push(createEmptyNode());
};

const removeNode = (idx: number) => {
  nodes.value.splice(idx, 1);
};

const moveNode = (idx: number, direction: number) => {
  const target = idx + direction;
  if (target < 0 || target >= nodes.value.length) return;
  const list = [...nodes.value];
  const temp = list[idx];
  list[idx] = list[target];
  list[target] = temp;
  nodes.value = list;
};

const addCondition = (nodeIdx: number) => {
  nodes.value[nodeIdx].conditions.push({
    field: conditionFields.value[0]?.value || "",
    operator: "<=",
    value: 0
  });
};

const removeCondition = (nodeIdx: number, condIdx: number) => {
  nodes.value[nodeIdx].conditions.splice(condIdx, 1);
};

const onAssigneeTypeChange = (node: WorkflowNode) => {
  node.assigneeValue = "";
};

const fetchRoles = async () => {
  try {
    const res: any = await getRoles();
    roleList.value = res.data || [];
  } catch {
    roleList.value = [];
  }
};

const searchEmployee = async (query: string) => {
  if (!query) {
    empOptions.value = [];
    ccEmpOptions.value = [];
    return;
  }
  empSearchLoading.value = true;
  try {
    const res: any = await request.get("/api/employee/page", {
      params: { pageNum: 1, pageSize: 20, empName: query }
    });
    empOptions.value = res.data?.list || [];
    ccEmpOptions.value = res.data?.list || [];
  } catch {
    empOptions.value = [];
    ccEmpOptions.value = [];
  } finally {
    empSearchLoading.value = false;
  }
};

onMounted(() => {
  fetchRoles();
});

defineExpose({ validateNodes });
</script>

<style scoped>
.workflow-designer {
  width: 100%;
}

.node-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.node-card :deep(.el-card__body) {
  padding: 16px;
}

.node-arrow {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
