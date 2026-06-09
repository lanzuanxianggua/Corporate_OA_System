<template>
  <div class="h-[560px] border border-gray-200 rounded-lg overflow-hidden relative">
    <VueFlow v-model:nodes="flowNodes" v-model:edges="flowEdges" :default-viewport="{ zoom: 0.9, x: 80, y: 50 }" fit-view-on-init class="bg-gray-50" @connect="onConnect">
      <Background />
      <Controls position="bottom-right" />
    </VueFlow>

    <div class="absolute top-2 left-2 z-10 flex gap-1">
      <el-button size="small" @click="addStartNode">开始</el-button>
      <el-button size="small" @click="addApprovalNode">审批</el-button>
      <el-button size="small" @click="addGatewayNode('exclusive')">排他网关</el-button>
      <el-button size="small" @click="addGatewayNode('parallel')">并行网关</el-button>
      <el-button size="small" @click="addEndNode">结束</el-button>
    </div>

    <div v-if="selectedNode" class="absolute top-2 right-2 z-10 w-72 bg-white border rounded-lg shadow-lg p-3">
      <div class="flex justify-between items-center mb-2">
        <span class="text-sm font-medium">节点配置</span>
        <el-button size="small" text @click="selectedNode = null">X</el-button>
      </div>
      <el-form label-width="70px" size="small">
        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="基础信息" name="base">
            <el-form-item label="名称">
              <el-input v-model="selectedNode.data.label" @update:model-value="syncFromFlow" />
            </el-form-item>
            <template v-if="selectedNode.data.nodeType === 'approval'">
              <el-form-item label="审批人类型">
                <el-select v-model="selectedNode.data.assigneeType" class="w-full" @change="(v: any) => handleAssigneeTypeChange(selectedNode.data, v)">
                  <el-option label="按角色(本部门)" value="role" />
                  <el-option label="按角色(全公司)" value="role_global" />
                  <el-option label="指定人员" value="specific" />
                  <el-option label="直属上级" value="dept_manager" />
                  <el-option label="角色链" value="role_chain" />
                </el-select>
              </el-form-item>
              <el-form-item label="审批人">
                <template v-if="selectedNode.data.assigneeType === 'specific'">
                  <el-select v-model="selectedNode.data.assigneeValue" filterable remote :remote-method="searchEmp" :loading="empLoading" class="w-full" @change="syncFromFlow">
                    <el-option v-for="e in empOpts" :key="e.id" :label="e.empName" :value="String(e.id)" />
                  </el-select>
                </template>
                <template v-else-if="['role','role_global'].includes(selectedNode.data.assigneeType)">
                  <el-select v-model="selectedNode.data.assigneeValue" class="w-full" @change="syncFromFlow">
                    <el-option v-for="r in ROLE_KEYS" :key="r.value" :label="r.label" :value="r.value" />
                  </el-select>
                </template>
                <template v-else-if="selectedNode.data.assigneeType === 'role_chain'">
                  <el-input v-model="selectedNode.data.assigneeValue" placeholder='["DEPT_MANAGER","DIRECTOR","GM"]' @blur="syncFromFlow" />
                </template>
                <template v-else-if="selectedNode.data.assigneeType === 'dept_manager'">
                  <span class="text-gray-400 text-xs">自动取发起人直属上级</span>
                </template>
              </el-form-item>
              <el-form-item label="多人审批">
                <el-switch v-model="selectedNode.data.multiEnabled" @change="syncFromFlow" />
              </el-form-item>
              <template v-if="selectedNode.data.multiEnabled">
                <el-form-item label="方式">
                  <el-radio-group v-model="selectedNode.data.multiType" @change="syncFromFlow">
                    <el-radio value="countersign">会签</el-radio>
                    <el-radio value="orsign">或签</el-radio>
                  </el-radio-group>
                </el-form-item>
              </template>
              <el-form-item label="超时(h)">
                <el-input-number v-model="selectedNode.data.timeoutHours" :min="0" :max="720" @change="syncFromFlow" />
              </el-form-item>
              <el-form-item v-if="selectedNode.data.timeoutHours > 0" label="超时动作">
                <el-select v-model="selectedNode.data.timeoutAction" class="w-full" @change="syncFromFlow">
                  <el-option label="仅通知" value="notify_only" />
                  <el-option label="自动通过" value="auto_approve" />
                  <el-option label="自动驳回" value="auto_reject" />
                  <el-option label="上报" value="escalate" />
                </el-select>
              </el-form-item>
            </template>
          </el-collapse-item>

          <el-collapse-item
            v-if="selectedNode.data.nodeType === 'approval' && selectedNode.data.timeoutAction === 'escalate'"
            title="超时升级目标" name="escalate">
            <el-form-item label="升级类型">
              <el-select v-model="selectedNode.data.escalateTo.type" class="w-full" @change="(v: any) => handleEscalateTypeChange(selectedNode.data.escalateTo, v)">
                <el-option label="指定员工" value="specific" />
                <el-option label="角色(同部门)" value="role" />
                <el-option label="角色(全局)" value="role_global" />
                <el-option label="直属上级" value="dept_manager" />
                <el-option label="角色链" value="role_chain" />
              </el-select>
            </el-form-item>
            <el-form-item label="升级目标">
              <template v-if="selectedNode.data.escalateTo?.type === 'specific'">
                <el-select v-model="selectedNode.data.escalateTo.value" filterable remote :remote-method="searchEmp" :loading="empLoading" class="w-full" @change="syncFromFlow">
                  <el-option v-for="e in empOpts" :key="e.id" :label="e.empName" :value="String(e.id)" />
                </el-select>
              </template>
              <template v-else-if="['role','role_global'].includes(selectedNode.data.escalateTo?.type)">
                <el-select v-model="selectedNode.data.escalateTo.value" class="w-full" @change="syncFromFlow">
                  <el-option v-for="r in ROLE_KEYS" :key="r.value" :label="r.label" :value="r.value" />
                </el-select>
              </template>
              <template v-else-if="selectedNode.data.escalateTo?.type === 'role_chain'">
                <el-input v-model="selectedNode.data.escalateTo.value" placeholder='["DEPT_MANAGER","DIRECTOR","GM"]' @blur="syncFromFlow" />
              </template>
              <template v-else>
                <span class="text-gray-400 text-xs">自动取发起人直属上级</span>
              </template>
            </el-form-item>
          </el-collapse-item>

          <el-collapse-item
            v-if="selectedNode.data.nodeType === 'approval'"
            title="4 维条件路由" name="routing">
            <div class="flex justify-between items-center mb-2">
              <span class="text-xs text-gray-500">满足条件时跳转(顺序敏感,第一条命中即匹配)</span>
              <el-button size="small" type="primary" plain @click="addRoutingRule">
                <el-icon><Plus /></el-icon> 添加条件
              </el-button>
            </div>
            <div v-for="(rule, idx) in selectedNode.data.routingRules" :key="rule.id" class="border rounded p-2 mb-2 bg-gray-50">
              <el-form-item label="字段" label-width="50px">
                <el-select v-model="rule.field" placeholder="选择维度" @change="(v: any) => handleRoutingFieldChange(rule, v)">
                  <el-option-group label="4 维条件">
                    <el-option label="金额阈值" value="amount" />
                    <el-option label="天数阈值" value="days" />
                    <el-option label="职级阈值" value="level" />
                    <el-option label="部门级别" value="deptLevel" />
                  </el-option-group>
                  <el-option-group label="自定义">
                    <el-option label="加班小时" value="hours" />
                    <el-option label="自定义字段..." value="__custom__" />
                  </el-option-group>
                </el-select>
                <el-input v-if="rule.field === '__custom__' && !rule.customApplied" v-model="rule.customField" placeholder="如 items.length" class="mt-1" size="small" @blur="() => { applyCustomField(rule); syncFromFlow() }" />
                <el-tag v-else-if="rule.field === '__custom__' && rule.customField" class="mt-1" size="small">{{ rule.customField }}</el-tag>
              </el-form-item>
              <el-form-item label="操作符" label-width="50px">
                <el-select v-model="rule.op" @change="syncFromFlow">
                  <el-option v-for="o in ['<','<=','>','>=','==','!=']" :key="o" :label="o" :value="o" />
                </el-select>
              </el-form-item>
              <el-form-item label="阈值" label-width="50px">
                <el-input-number v-model="rule.value" :min="0" controls-position="right" @change="syncFromFlow" />
              </el-form-item>
              <el-form-item label="跳转至" label-width="50px">
                <el-select v-model="rule.skipTo" placeholder="选择目标审批节点" @change="syncFromFlow">
                  <el-option v-for="n in approvalNodeOptions" :key="n.nodeId" :label="`${n.nodeId}(${n.nodeName})`" :value="n.nodeId" />
                </el-select>
              </el-form-item>
              <el-form-item label="备选" label-width="50px">
                <el-select v-model="rule.jumpTo" clearable placeholder="可选" @change="syncFromFlow">
                  <el-option v-for="n in approvalNodeOptions" :key="n.nodeId" :label="n.nodeName" :value="n.nodeId" />
                </el-select>
              </el-form-item>
              <div class="text-right">
                <el-button size="small" type="danger" link @click="removeRoutingRule(idx)">
                  <el-icon><Delete /></el-icon> 删除该行
                </el-button>
              </div>
            </div>
            <div v-if="!selectedNode.data.routingRules?.length" class="text-xs text-gray-400 text-center py-2">
              暂无条件,点击右上"添加条件"开始配置
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from "vue";
import { VueFlow, useVueFlow } from "@vue-flow/core";
import { Background } from "@vue-flow/background";
import { Controls } from "@vue-flow/controls";
import { Plus, Delete } from "@element-plus/icons-vue";
import "@vue-flow/core/dist/style.css";
import "@vue-flow/core/dist/theme-default.css";
import "@vue-flow/controls/dist/style.css";
import request from "@/utils/request";

const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{ (e: "update:modelValue", val: string): void }>();

const { addNodes, addEdges, onNodeClick, onEdgeDoubleClick, removeEdges } = useVueFlow();

let nodeIdCounter = 1;
const nextId = () => `n${nodeIdCounter++}`;

const flowNodes = ref<any[]>([]);
const flowEdges = ref<any[]>([]);
const selectedNode = ref<any>(null);
const empLoading = ref(false);
const empOpts = ref<any[]>([]);

// ===== 4 维条件路由 + 超时升级目标 =====
const activeCollapse = ref<string[]>(["base", "escalate", "routing"]);

const approvalNodeOptions = computed(() =>
  flowNodes.value
    .filter(n => n.data?.nodeType === "approval" && n.id !== selectedNode.value?.id)
    .map(n => ({ nodeId: n.id, nodeName: n.data?.label ?? n.id }))
);

const ROLE_KEYS = [
  { label: "部门经理", value: "DEPT_MANAGER" },
  { label: "总监", value: "DIRECTOR" },
  { label: "总经理", value: "GM" },
  { label: "人事专员", value: "HR" },
  { label: "财务专员", value: "FINANCE" },
];

function ensureApprovalDefaults(data: any) {
  if (!data || data.nodeType !== "approval") return;
  data.routingRules ??= [];
  if (data.timeoutAction === "escalate" && !data.escalateTo) {
    data.escalateTo = { type: "role_global", value: "" };
  }
}

function defaultAssigneeValue(type: string) {
  return type === "dept_manager" ? "dept_manager" : "";
}

function handleAssigneeTypeChange(data: any, type: string) {
  data.assigneeValue = defaultAssigneeValue(type);
  syncFromFlow();
}

function handleEscalateTypeChange(escalateTo: any, type: string) {
  if (!escalateTo) return;
  escalateTo.value = defaultAssigneeValue(type);
  syncFromFlow();
}

function addRoutingRule() {
  if (!selectedNode.value) return;
  ensureApprovalDefaults(selectedNode.value.data);
  selectedNode.value.data.routingRules.push({
    id: crypto.randomUUID(),
    field: "amount",
    op: ">",
    value: 0,
    skipTo: "",
    jumpTo: "",
  });
  syncFromFlow();
}

function removeRoutingRule(idx: number) {
  if (!selectedNode.value) return;
  selectedNode.value.data.routingRules.splice(idx, 1);
  syncFromFlow();
}

function stripContextPrefix(field: string) {
  return field?.startsWith("context.") ? field.slice("context.".length) : field;
}

function isKnownRoutingField(field: string) {
  return ["amount", "days", "level", "deptLevel", "hours"].includes(field);
}

function handleRoutingFieldChange(rule: any, value: string) {
  if (value === "__custom__") {
    rule.customField = "";
    rule.customApplied = false;
  } else {
    delete rule.customField;
    delete rule.customApplied;
  }
  syncFromFlow();
}

function applyCustomField(rule: any) {
  if (rule.field === "__custom__" && rule.customField) {
    rule.customField = stripContextPrefix(String(rule.customField).trim());
    rule.customApplied = true;
    syncFromFlow();
  }
}

function getRoutingField(rule: any) {
  if (rule.field === "__custom__") {
    return stripContextPrefix(String(rule.customField || "").trim());
  }
  return stripContextPrefix(String(rule.field || "").trim());
}

function parseRoutingWhen(when: string) {
  const fallback = { field: "amount", op: ">", value: 0 };
  if (!when) return fallback;
  const match = String(when).trim().match(/^(.+?)\s*(<=|>=|==|!=|<|>)\s*(.+)$/);
  if (!match) return fallback;
  const field = stripContextPrefix(match[1].trim());
  const rawValue = match[3].trim();
  const unquotedValue = rawValue.replace(/^["']|["']$/g, "");
  const numericValue = Number(unquotedValue);
  return {
    field: field || fallback.field,
    op: match[2],
    value: Number.isFinite(numericValue) ? numericValue : unquotedValue,
  };
}

const searchEmp = async (q: string) => {
  if (!q) return;
  empLoading.value = true;
  try {
    const res: any = await request.get("/api/employee/page", { params: { pageNum: 1, pageSize: 20, empName: q } });
    empOpts.value = res.data?.list || [];
  } finally { empLoading.value = false; }
};

const addStartNode = () => {
  addNodes([{ id: nextId(), type: "input", position: { x: 250, y: 20 }, data: { label: "开始", nodeType: "start" } }]);
};

const addEndNode = () => {
  addNodes([{ id: nextId(), type: "output", position: { x: 250, y: 500 }, data: { label: "结束", nodeType: "end" } }]);
};

const addApprovalNode = () => {
  const id = nextId();
  const data: any = { label: "审批节点", nodeType: "approval", assigneeType: "role", assigneeValue: "", multiEnabled: false, multiType: "countersign", timeoutHours: 0, timeoutAction: "notify_only" };
  ensureApprovalDefaults(data);
  addNodes([{ id, position: { x: 200, y: 200 + flowNodes.value.length * 80 }, data }]);
};

const addGatewayNode = (gwType: string) => {
  const id = nextId();
  addNodes([{ id, position: { x: 250, y: 250 + flowNodes.value.length * 60 }, data: { label: gwType === "exclusive" ? "排他网关" : "并行网关", nodeType: "gateway", gatewayType: gwType } }]);
};

const onConnect = (params: any) => {
  addEdges([{ ...params, animated: true, label: "", data: { condition: null } }]);
};

onNodeClick(({ node }: any) => { selectedNode.value = node; });

onEdgeDoubleClick(({ edge }: any) => { removeEdges([edge]); });

// Serialize flow -> JSON schema
const syncFromFlow = () => { emitSchema(); };

const emitSchema = () => {
  const nodes = flowNodes.value.map((n: any) => {
    const data = n.data || {};
    if (data.nodeType === "approval") {
      const out: any = {
        nodeId: n.id,
        nodeType: data.nodeType,
        name: data.label,
        nodeName: data.label,  // V1010 bug-4: dual-write for backend compat
        assigneeType: data.assigneeType,
        assigneeValue: data.assigneeType === "dept_manager" && !data.assigneeValue ? "dept_manager" : data.assigneeValue,
      };
      if (data.multiEnabled) out.multiType = data.multiType;
      if (data.timeoutHours > 0) {
        out.timeoutHours = data.timeoutHours;
        out.timeoutAction = data.timeoutAction;
        if (data.timeoutAction === "escalate" && data.escalateTo) {
          out.escalateTo = { type: data.escalateTo.type, value: data.escalateTo.value };
        }
      }
      if (data.routingRules?.length) {
        const routingRules = data.routingRules.map((r: any) => {
          const field = getRoutingField(r);
          if (!field) return null;
          const obj: any = {
            when: `${field} ${r.op} ${r.value}`,
            skipTo: r.skipTo,
          };
          if (r.jumpTo) obj.jumpTo = r.jumpTo;
          return obj;
        }).filter(Boolean);
        if (routingRules.length) out.routingRules = routingRules;
      }
      return out;
    }
    const base: any = { nodeId: n.id, nodeType: data.nodeType, name: data.label, nodeName: data.label };
    if (data.nodeType === "gateway") base.gatewayType = data.gatewayType;
    return base;
  });
  const edges = flowEdges.value.map((e: any) => {
    const edge: any = { source: e.source, target: e.target, sourceId: e.source, targetId: e.target };
    if (e.data?.condition) edge.condition = e.data.condition;
    return edge;
  });
  emit("update:modelValue", JSON.stringify({ schemaVersion: 2, nodes, edges }));
};

// Parse initial modelValue
watch(() => props.modelValue, (val) => {
  if (!val) return;
  try {
    const parsed = JSON.parse(val);
    if (parsed.nodes && parsed.edges) {
      const typeMap: Record<string, string> = { start: "input", end: "output" };
      flowNodes.value = parsed.nodes.map((n: any, i: number) => {
        const data: any = {
          label: n.nodeName || n.name,
          nodeType: n.nodeType,
          assigneeType: n.assigneeType || "role",
          assigneeValue: n.assigneeValue || "",
          multiEnabled: !!n.multiType,
          multiType: n.multiType || "countersign",
          timeoutHours: n.timeoutHours || 0,
          timeoutAction: n.timeoutAction || "notify_only",
          gatewayType: n.gatewayType || "exclusive",
        };
        if (n.escalateTo) data.escalateTo = { type: n.escalateTo.type, value: n.escalateTo.value };
        if (Array.isArray(n.routingRules)) {
          data.routingRules = n.routingRules.map((r: any) => {
            const parsedWhen = parseRoutingWhen(r.when);
            const field = stripContextPrefix(parsedWhen.field);
            const knownField = isKnownRoutingField(field);
            return {
              id: crypto.randomUUID(),
              field: knownField ? field : "__custom__",
              customField: knownField ? undefined : field,
              customApplied: !knownField,
              op: parsedWhen.op,
              value: parsedWhen.value,
              skipTo: r.skipTo || "",
              jumpTo: r.jumpTo || "",
            };
          });
        }
        ensureApprovalDefaults(data);
        return {
          id: n.nodeId,
          type: typeMap[n.nodeType] || "default",
          position: { x: 200, y: i * 100 + 20 },
          data,
        };
      });
      flowEdges.value = parsed.edges.map((e: any) => ({
        id: `${e.source || e.sourceId}-${e.target || e.targetId}`,
        source: e.source || e.sourceId,
        target: e.target || e.targetId,
        animated: true,
        data: { condition: e.condition || null }
      }));
    }
  } catch { /* ignore */ }
}, { immediate: true });

// 切换超时动作时,escalate<->非 escalate 互转兜底
watch(
  () => selectedNode.value?.data?.timeoutAction,
  (val, old) => {
    if (!selectedNode.value) return;
    if (val === "escalate" && !selectedNode.value.data.escalateTo) {
      selectedNode.value.data.escalateTo = { type: "role_global", value: "" };
    }
    if (old === "escalate" && val !== "escalate") {
      delete selectedNode.value.data.escalateTo;
    }
    syncFromFlow();
  }
);

watch([flowNodes, flowEdges], () => emitSchema(), { deep: true });
</script>
