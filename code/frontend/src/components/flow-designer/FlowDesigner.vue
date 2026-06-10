<template>
  <div class="workflow-builder">
    <div class="builder-header">
      <div>
        <div class="builder-title">审批流程配置</div>
        <div class="builder-subtitle">按真实审批步骤配置，系统会自动生成可执行流程</div>
      </div>
      <div class="template-actions">
        <el-button plain @click="applyTemplate('standard')">常规审批</el-button>
        <el-button plain @click="applyTemplate('amount')">金额分级</el-button>
        <el-button plain @click="applyTemplate('duration')">时长分级</el-button>
      </div>
    </div>

    <div class="builder-layout">
      <section class="steps-panel">
        <div class="flow-preview">
          <div class="flow-point start">发起</div>
          <template v-for="(step, index) in approvalSteps" :key="step.id">
            <div class="flow-line" />
            <button
              type="button"
              class="flow-step"
              :class="{ active: selectedStepId === step.id }"
              @click="selectedStepId = step.id"
            >
              <span class="step-index">{{ index + 1 }}</span>
              <span class="step-name">{{ step.name || `第 ${index + 1} 步审批` }}</span>
            </button>
          </template>
          <div class="flow-line" />
          <div class="flow-point end">归档</div>
        </div>

        <div class="step-list">
          <div v-for="(step, index) in approvalSteps" :key="step.id" class="approval-step">
            <div class="step-header">
              <div>
                <div class="step-title">第 {{ index + 1 }} 步审批</div>
                <div class="step-meta">{{ approverSummary(step) }}</div>
              </div>
              <div class="step-actions">
                <el-tooltip content="上移" placement="top">
                  <el-button :icon="ArrowUp" circle :disabled="index === 0" @click="moveStep(index, -1)" />
                </el-tooltip>
                <el-tooltip content="下移" placement="top">
                  <el-button :icon="ArrowDown" circle :disabled="index === approvalSteps.length - 1" @click="moveStep(index, 1)" />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button :icon="Delete" circle type="danger" :disabled="approvalSteps.length === 1" @click="removeStep(index)" />
                </el-tooltip>
              </div>
            </div>

            <div class="step-fields">
              <el-form-item label="步骤名称">
                <el-input v-model="step.name" placeholder="例如 部门主管审批" @input="emitGraph" />
              </el-form-item>
              <el-form-item label="审批人">
                <div class="approver-row">
                  <el-select v-model="step.assigneeType" class="type-select" @change="() => handleAssigneeTypeChange(step)">
                    <el-option label="直属上级" value="dept_manager" />
                    <el-option label="本部门角色" value="role" />
                    <el-option label="全公司角色" value="role_global" />
                    <el-option label="指定员工" value="specific" />
                    <el-option label="角色链" value="role_chain" />
                  </el-select>
                  <el-select
                    v-if="step.assigneeType === 'specific'"
                    v-model="step.assigneeValue"
                    filterable
                    remote
                    :remote-method="searchEmp"
                    :loading="empLoading"
                    placeholder="搜索员工"
                    class="value-select"
                    @change="emitGraph"
                  >
                    <el-option v-for="employee in empOpts" :key="employee.id" :label="employee.empName" :value="String(employee.id)" />
                  </el-select>
                  <el-select
                    v-else-if="['role', 'role_global'].includes(step.assigneeType)"
                    v-model="step.assigneeValue"
                    placeholder="选择角色"
                    class="value-select"
                    @change="emitGraph"
                  >
                    <el-option v-for="role in ROLE_KEYS" :key="role.value" :label="role.label" :value="role.value" />
                  </el-select>
                  <el-input
                    v-else-if="step.assigneeType === 'role_chain'"
                    v-model="step.assigneeValue"
                    placeholder="[&quot;DEPT_MANAGER&quot;,&quot;DIRECTOR&quot;,&quot;GM&quot;]"
                    class="value-select"
                    @change="emitGraph"
                  />
                  <span v-else class="auto-assignee">自动取发起人的直属上级</span>
                </div>
              </el-form-item>
              <div class="option-grid">
                <el-form-item label="多人审批">
                  <div class="inline-option">
                    <el-switch v-model="step.multiEnabled" @change="emitGraph" />
                    <el-radio-group v-if="step.multiEnabled" v-model="step.multiType" @change="emitGraph">
                      <el-radio value="countersign">会签</el-radio>
                      <el-radio value="orsign">或签</el-radio>
                    </el-radio-group>
                  </div>
                </el-form-item>
                <el-form-item label="超时提醒">
                  <div class="timeout-row">
                    <el-input-number v-model="step.timeoutHours" :min="0" :max="720" controls-position="right" @change="emitGraph" />
                    <span>小时</span>
                  </div>
                </el-form-item>
              </div>
            </div>
          </div>
        </div>

        <el-button class="add-step-button" type="primary" plain :icon="Plus" @click="addStep">添加审批步骤</el-button>
      </section>

      <aside class="rules-panel">
        <div class="rules-header">
          <div>
            <div class="rules-title">分级审批规则</div>
            <div class="rules-subtitle">满足条件时追加更高层级审批</div>
          </div>
          <el-switch v-model="enableTierRules" @change="emitGraph" />
        </div>

        <div v-if="enableTierRules" class="rule-list">
          <div v-for="(rule, index) in tierRules" :key="rule.id" class="tier-rule">
            <div class="rule-title-row">
              <span>规则 {{ index + 1 }}</span>
              <el-button type="danger" link :icon="Delete" @click="removeRule(index)">删除</el-button>
            </div>
            <el-form-item label="触发条件">
              <div class="condition-row">
                <el-select v-model="rule.field" @change="emitGraph">
                  <el-option label="金额" value="amount" />
                  <el-option label="天数" value="days" />
                  <el-option label="小时" value="hours" />
                  <el-option label="职级" value="level" />
                </el-select>
                <el-select v-model="rule.op" class="operator-select" @change="emitGraph">
                  <el-option v-for="operator in OPERATORS" :key="operator" :label="operator" :value="operator" />
                </el-select>
                <el-input-number v-model="rule.value" :min="0" controls-position="right" @change="emitGraph" />
              </div>
            </el-form-item>
            <el-form-item label="追加审批">
              <el-input v-model="rule.name" placeholder="例如 总经理审批" @input="emitGraph" />
            </el-form-item>
            <el-form-item label="审批人">
              <div class="approver-row">
                <el-select v-model="rule.assigneeType" class="type-select" @change="() => handleRuleTypeChange(rule)">
                  <el-option label="全公司角色" value="role_global" />
                  <el-option label="本部门角色" value="role" />
                  <el-option label="直属上级" value="dept_manager" />
                  <el-option label="指定员工" value="specific" />
                </el-select>
                <el-select
                  v-if="rule.assigneeType === 'specific'"
                  v-model="rule.assigneeValue"
                  filterable
                  remote
                  :remote-method="searchEmp"
                  :loading="empLoading"
                  placeholder="搜索员工"
                  class="value-select"
                  @change="emitGraph"
                >
                  <el-option v-for="employee in empOpts" :key="employee.id" :label="employee.empName" :value="String(employee.id)" />
                </el-select>
                <el-select
                  v-else-if="['role', 'role_global'].includes(rule.assigneeType)"
                  v-model="rule.assigneeValue"
                  placeholder="选择角色"
                  class="value-select"
                  @change="emitGraph"
                >
                  <el-option v-for="role in ROLE_KEYS" :key="role.value" :label="role.label" :value="role.value" />
                </el-select>
                <span v-else class="auto-assignee">自动取发起人的直属上级</span>
              </div>
            </el-form-item>
          </div>

          <el-button class="add-rule-button" :icon="Plus" @click="addRule">添加分级规则</el-button>
        </div>

        <el-empty v-else description="未启用分级审批" :image-size="88" />

        <div class="summary-panel">
          <div class="summary-item">
            <span>基础审批</span>
            <strong>{{ approvalSteps.length }} 步</strong>
          </div>
          <div class="summary-item">
            <span>分级规则</span>
            <strong>{{ enableTierRules ? tierRules.length : 0 }} 条</strong>
          </div>
          <div class="summary-item">
            <span>流程版本</span>
            <strong>v3</strong>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { ArrowDown, ArrowUp, Delete, Plus } from "@element-plus/icons-vue";
import request from "@/utils/request";

interface ApprovalStep {
  id: string;
  name: string;
  assigneeType: string;
  assigneeValue: string;
  multiEnabled: boolean;
  multiType: "countersign" | "orsign";
  timeoutHours: number;
}

interface TierRule {
  id: string;
  field: string;
  op: string;
  value: number;
  name: string;
  assigneeType: string;
  assigneeValue: string;
}

const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{ (event: "update:modelValue", value: string): void }>();

const OPERATORS = [">", ">=", "<", "<=", "==", "!="];
const ROLE_KEYS = [
  { label: "部门经理", value: "DEPT_MANAGER" },
  { label: "总监", value: "DIRECTOR" },
  { label: "总经理", value: "GM" },
  { label: "人事专员", value: "HR" },
  { label: "财务专员", value: "FINANCE" },
];

const approvalSteps = ref<ApprovalStep[]>([]);
const tierRules = ref<TierRule[]>([]);
const enableTierRules = ref(false);
const selectedStepId = ref("");
const empLoading = ref(false);
const empOpts = ref<any[]>([]);
let isApplyingModel = false;
let idCounter = 1;

function nextId(prefix: string) {
  return `${prefix}_${Date.now()}_${idCounter++}`;
}

function createStep(name = "部门主管审批", assigneeType = "dept_manager", assigneeValue = "dept_manager"): ApprovalStep {
  return {
    id: nextId("approval"),
    name,
    assigneeType,
    assigneeValue,
    multiEnabled: false,
    multiType: "countersign",
    timeoutHours: 0,
  };
}

function createRule(field = "amount", value = 50000, name = "总经理审批", assigneeValue = "GM"): TierRule {
  return {
    id: nextId("tier"),
    field,
    op: ">",
    value,
    name,
    assigneeType: "role_global",
    assigneeValue,
  };
}

function applyTemplate(type: "standard" | "amount" | "duration") {
  if (type === "standard") {
    approvalSteps.value = [
      createStep("部门主管审批", "dept_manager", "dept_manager"),
      createStep("行政/人事复核", "role_global", "HR"),
    ];
    enableTierRules.value = false;
    tierRules.value = [];
  }
  if (type === "amount") {
    approvalSteps.value = [
      createStep("部门主管审批", "dept_manager", "dept_manager"),
      createStep("财务复核", "role_global", "FINANCE"),
    ];
    enableTierRules.value = true;
    tierRules.value = [
      createRule("amount", 5000, "总监审批", "DIRECTOR"),
      createRule("amount", 50000, "总经理审批", "GM"),
    ];
  }
  if (type === "duration") {
    approvalSteps.value = [
      createStep("部门主管审批", "dept_manager", "dept_manager"),
    ];
    enableTierRules.value = true;
    tierRules.value = [
      createRule("days", 7, "总监审批", "DIRECTOR"),
    ];
  }
  selectedStepId.value = approvalSteps.value[0]?.id || "";
  emitGraph();
}

function addStep() {
  const step = createStep(`第 ${approvalSteps.value.length + 1} 步审批`, "dept_manager", "dept_manager");
  approvalSteps.value.push(step);
  selectedStepId.value = step.id;
  emitGraph();
}

function removeStep(index: number) {
  approvalSteps.value.splice(index, 1);
  selectedStepId.value = approvalSteps.value[Math.max(0, index - 1)]?.id || "";
  emitGraph();
}

function moveStep(index: number, direction: -1 | 1) {
  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= approvalSteps.value.length) return;
  const [step] = approvalSteps.value.splice(index, 1);
  approvalSteps.value.splice(targetIndex, 0, step);
  selectedStepId.value = step.id;
  emitGraph();
}

function addRule() {
  tierRules.value.push(createRule());
  enableTierRules.value = true;
  emitGraph();
}

function removeRule(index: number) {
  tierRules.value.splice(index, 1);
  emitGraph();
}

function defaultAssigneeValue(type: string) {
  return type === "dept_manager" ? "dept_manager" : "";
}

function handleAssigneeTypeChange(step: ApprovalStep) {
  step.assigneeValue = defaultAssigneeValue(step.assigneeType);
  emitGraph();
}

function handleRuleTypeChange(rule: TierRule) {
  rule.assigneeValue = defaultAssigneeValue(rule.assigneeType);
  emitGraph();
}

function approverSummary(step: ApprovalStep) {
  if (step.assigneeType === "dept_manager") return "直属上级";
  if (step.assigneeType === "role_chain") return "角色链";
  const role = ROLE_KEYS.find(item => item.value === step.assigneeValue);
  if (role) return role.label;
  return step.assigneeValue || "未设置审批人";
}

async function searchEmp(keyword: string) {
  if (!keyword) return;
  empLoading.value = true;
  try {
    const res: any = await request.get("/api/employee/page", { params: { pageNum: 1, pageSize: 20, empName: keyword } });
    empOpts.value = res.data?.list || res.data?.records || [];
  } finally {
    empLoading.value = false;
  }
}

function normalizeAssigneeValue(type: string, value: string) {
  return type === "dept_manager" && !value ? "dept_manager" : value;
}

function emitGraph() {
  if (isApplyingModel) return;
  const graph = buildGraph();
  emit("update:modelValue", JSON.stringify(graph));
}

function buildGraph() {
  const nodes: any[] = [
    { nodeId: "start", nodeType: "start", name: "开始", nodeName: "开始" },
  ];
  const edges: any[] = [];
  let previousNodeId = "start";

  approvalSteps.value.forEach((step, index) => {
    const nodeId = step.id || `approval_${index + 1}`;
    const assigneeValue = normalizeAssigneeValue(step.assigneeType, step.assigneeValue);
    const node: any = {
      nodeId,
      nodeType: "approval",
      name: step.name || `第 ${index + 1} 步审批`,
      nodeName: step.name || `第 ${index + 1} 步审批`,
      approvalMode: step.multiEnabled ? step.multiType : "single",
      assigneeRule: {
        type: step.assigneeType,
        value: assigneeValue,
        roleKey: ["role", "role_global"].includes(step.assigneeType) ? assigneeValue : undefined,
        userId: step.assigneeType === "specific" ? assigneeValue : undefined,
      },
      assigneeType: step.assigneeType,
      assigneeValue,
    };
    if (step.multiEnabled) node.multiType = step.multiType;
    if (step.timeoutHours > 0) {
      node.timeoutHours = step.timeoutHours;
      node.timeoutAction = "notify_only";
    }
    nodes.push(node);
    edges.push(edge(previousNodeId, nodeId));
    previousNodeId = nodeId;
  });

  const activeRules = enableTierRules.value
    ? tierRules.value.filter(rule => rule.field && rule.op && rule.value !== null && rule.value !== undefined && rule.assigneeType)
    : [];

  if (activeRules.length > 0 && approvalSteps.value.length > 0) {
    const gatewayId = "gateway_tier_rules";
    const branches: any[] = [];
    nodes.push({
      nodeId: gatewayId,
      nodeType: "gateway",
      gatewayType: "exclusive",
      name: "分级审批判断",
      nodeName: "分级审批判断",
      branches,
    });
    edges.push(edge(previousNodeId, gatewayId));
    previousNodeId = gatewayId;

    activeRules.forEach((rule, index) => {
      const nodeId = `tier_approval_${index + 1}`;
      const assigneeValue = normalizeAssigneeValue(rule.assigneeType, rule.assigneeValue);
      branches.push({
        when: `${rule.field} ${rule.op} ${rule.value}`,
        to: nodeId,
      });
      nodes.push({
        nodeId,
        nodeType: "approval",
        name: rule.name || `分级审批 ${index + 1}`,
        nodeName: rule.name || `分级审批 ${index + 1}`,
        approvalMode: "single",
        assigneeRule: {
          type: rule.assigneeType,
          value: assigneeValue,
          roleKey: ["role", "role_global"].includes(rule.assigneeType) ? assigneeValue : undefined,
          userId: rule.assigneeType === "specific" ? assigneeValue : undefined,
        },
        assigneeType: rule.assigneeType,
        assigneeValue,
      });
      edges.push(edge(gatewayId, nodeId));
      edges.push(edge(nodeId, "end"));
    });
  }

  nodes.push({ nodeId: "end", nodeType: "end", name: "结束", nodeName: "结束" });
  edges.push(edge(previousNodeId, "end"));

  return { schemaVersion: 3, nodes, edges };
}

function edge(source: string, target: string) {
  return { source, target, sourceId: source, targetId: target };
}

function applyModelValue(value: string) {
  isApplyingModel = true;
  try {
    const parsed = value ? JSON.parse(value) : null;
    if (!parsed || !Array.isArray(parsed.nodes)) {
      applyTemplate("standard");
      return;
    }

    const approvalNodes = parsed.nodes.filter((node: any) => node.nodeType === "approval" && !String(node.nodeId).startsWith("tier_approval_"));
    approvalSteps.value = approvalNodes.length
      ? approvalNodes.map((node: any, index: number) => graphNodeToStep(node, index))
      : [createStep("部门主管审批", "dept_manager", "dept_manager")];

    const tierApprovalNodes = parsed.nodes.filter((node: any) => String(node.nodeId).startsWith("tier_approval_"));
    const gateway = parsed.nodes.find((node: any) => node.nodeId === "gateway_tier_rules");
    if (gateway && Array.isArray(gateway.branches) && tierApprovalNodes.length) {
      enableTierRules.value = true;
      tierRules.value = gateway.branches.map((branch: any, index: number) => {
        const approvalNode = tierApprovalNodes.find((node: any) => node.nodeId === branch.to) || tierApprovalNodes[index];
        return graphBranchToRule(branch, approvalNode, index);
      });
    } else {
      enableTierRules.value = false;
      tierRules.value = [];
    }
    selectedStepId.value = approvalSteps.value[0]?.id || "";
  } catch {
    approvalSteps.value = [createStep("部门主管审批", "dept_manager", "dept_manager")];
    tierRules.value = [];
    enableTierRules.value = false;
    selectedStepId.value = approvalSteps.value[0]?.id || "";
  } finally {
    isApplyingModel = false;
  }
}

function graphNodeToStep(node: any, index: number): ApprovalStep {
  const assigneeRule = node.assigneeRule || {};
  return {
    id: node.nodeId || nextId("approval"),
    name: node.nodeName || node.name || `第 ${index + 1} 步审批`,
    assigneeType: node.assigneeType || assigneeRule.type || "role",
    assigneeValue: node.assigneeValue || assigneeRule.value || assigneeRule.roleKey || assigneeRule.userId || "DEPT_MANAGER",
    multiEnabled: !!node.multiType,
    multiType: node.multiType || "countersign",
    timeoutHours: node.timeoutHours || 0,
  };
}

function graphBranchToRule(branch: any, node: any, index: number): TierRule {
  const parsed = parseCondition(branch.when);
  const assigneeRule = node?.assigneeRule || {};
  return {
    id: nextId("tier"),
    field: parsed.field,
    op: parsed.op,
    value: parsed.value,
    name: node?.nodeName || node?.name || `分级审批 ${index + 1}`,
    assigneeType: node?.assigneeType || assigneeRule.type || "role_global",
    assigneeValue: node?.assigneeValue || assigneeRule.value || assigneeRule.roleKey || assigneeRule.userId || "DIRECTOR",
  };
}

function parseCondition(condition: string) {
  const match = String(condition || "").match(/^(.+?)\s*(<=|>=|==|!=|<|>)\s*(.+)$/);
  if (!match) return { field: "amount", op: ">", value: 0 };
  return {
    field: match[1].trim(),
    op: match[2],
    value: Number(match[3]) || 0,
  };
}

watch(
  () => props.modelValue,
  value => applyModelValue(value),
  { immediate: true }
);

watch(
  [approvalSteps, tierRules, enableTierRules],
  () => emitGraph(),
  { deep: true }
);

if (!approvalSteps.value.length) {
  approvalSteps.value = [createStep("部门主管审批", "dept_manager", "dept_manager")];
  selectedStepId.value = approvalSteps.value[0].id;
}
</script>

<style scoped>
.workflow-builder {
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-surface);
  overflow: hidden;
}

.builder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--oa-border);
  background: var(--oa-surface-soft);
}

.builder-title,
.rules-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--oa-text);
}

.builder-subtitle,
.rules-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--oa-muted);
}

.template-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.builder-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  min-height: 560px;
}

.steps-panel {
  padding: 16px;
  border-right: 1px solid var(--oa-border);
  min-width: 0;
}

.flow-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  min-height: 72px;
  padding: 10px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-surface-soft);
}

.flow-point,
.flow-step {
  flex: 0 0 auto;
  height: 40px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  border: 1px solid var(--oa-border);
  background: var(--oa-surface);
  color: var(--oa-text);
}

.flow-point.start {
  border-color: rgba(22, 163, 74, 0.35);
}

.flow-point.end {
  border-color: rgba(99, 102, 241, 0.35);
}

.flow-step {
  cursor: pointer;
}

.flow-step.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
}

.flow-line {
  flex: 0 0 32px;
  height: 1px;
  background: var(--oa-border);
}

.step-index {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 12px;
}

.step-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.approval-step,
.tier-rule,
.summary-panel {
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: var(--oa-surface);
}

.approval-step {
  padding: 14px;
}

.step-header,
.rules-header,
.rule-title-row,
.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.step-title {
  font-weight: 700;
  color: var(--oa-text);
}

.step-meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--oa-muted);
}

.step-actions {
  display: flex;
  gap: 6px;
}

.step-fields {
  display: grid;
  gap: 4px;
  margin-top: 12px;
}

.approver-row,
.condition-row,
.timeout-row,
.inline-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.type-select {
  width: 150px;
  flex: 0 0 150px;
}

.value-select,
.editor-host {
  flex: 1;
  min-width: 0;
}

.auto-assignee {
  color: var(--oa-muted);
  font-size: 13px;
}

.option-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.add-step-button,
.add-rule-button {
  width: 100%;
  margin-top: 12px;
}

.rules-panel {
  padding: 16px;
  min-width: 0;
  background: var(--oa-surface-soft);
}

.rule-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.tier-rule {
  padding: 12px;
}

.rule-title-row {
  margin-bottom: 8px;
  font-weight: 700;
}

.condition-row {
  align-items: stretch;
}

.operator-select {
  width: 78px;
  flex: 0 0 78px;
}

.summary-panel {
  margin-top: 16px;
  padding: 10px 12px;
}

.summary-item {
  padding: 8px 0;
  color: var(--oa-muted);
  font-size: 13px;
}

.summary-item + .summary-item {
  border-top: 1px solid var(--oa-border);
}

.summary-item strong {
  color: var(--oa-text);
}

@media (max-width: 960px) {
  .builder-layout {
    grid-template-columns: 1fr;
  }

  .steps-panel {
    border-right: 0;
    border-bottom: 1px solid var(--oa-border);
  }

  .option-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .builder-header,
  .step-header,
  .approver-row,
  .condition-row {
    align-items: stretch;
    flex-direction: column;
  }

  .type-select,
  .operator-select {
    width: 100%;
    flex-basis: auto;
  }
}
</style>
