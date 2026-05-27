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
        <el-form-item label="名称">
          <el-input v-model="selectedNode.data.label" @update:model-value="syncFromFlow" />
        </el-form-item>
        <template v-if="selectedNode.data.nodeType === 'approval'">
          <el-form-item label="审批人类型">
            <el-select v-model="selectedNode.data.assigneeType" class="w-full" @change="syncFromFlow">
              <el-option label="按角色(本部门)" value="role" />
              <el-option label="按角色(全公司)" value="role_global" />
              <el-option label="指定人员" value="specific" />
            </el-select>
          </el-form-item>
          <el-form-item label="审批人">
            <el-select v-model="selectedNode.data.assigneeValue" filterable remote :remote-method="searchEmp" :loading="empLoading" class="w-full" @change="syncFromFlow">
              <el-option v-for="e in empOpts" :key="e.id" :label="e.empName" :value="String(e.id)" />
            </el-select>
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
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { VueFlow, useVueFlow } from "@vue-flow/core";
import { Background } from "@vue-flow/background";
import { Controls } from "@vue-flow/controls";
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
  addNodes([{ id, position: { x: 200, y: 200 + flowNodes.value.length * 80 }, data: { label: "审批节点", nodeType: "approval", assigneeType: "role", assigneeValue: "", multiEnabled: false, multiType: "countersign", timeoutHours: 0, timeoutAction: "notify_only" } }]);
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
    const base: any = { nodeId: n.id, nodeType: n.data.nodeType, name: n.data.label };
    if (n.data.nodeType === "approval") {
      base.assigneeType = n.data.assigneeType;
      base.assigneeValue = n.data.assigneeValue;
      if (n.data.multiEnabled) base.multiType = n.data.multiType;
      if (n.data.timeoutHours > 0) {
        base.timeoutHours = n.data.timeoutHours;
        base.timeoutAction = n.data.timeoutAction;
      }
    }
    if (n.data.nodeType === "gateway") base.gatewayType = n.data.gatewayType;
    return base;
  });
  const edges = flowEdges.value.map((e: any) => {
    const edge: any = { sourceId: e.source, targetId: e.target };
    if (e.data?.condition) edge.condition = e.data.condition;
    return edge;
  });
  emit("update:modelValue", JSON.stringify({ nodes, edges }));
};

// Parse initial modelValue
watch(() => props.modelValue, (val) => {
  if (!val) return;
  try {
    const parsed = JSON.parse(val);
    if (parsed.nodes && parsed.edges) {
      const typeMap: Record<string, string> = { start: "input", end: "output" };
      flowNodes.value = parsed.nodes.map((n: any, i: number) => ({
        id: n.nodeId,
        type: typeMap[n.nodeType] || "default",
        position: { x: 200, y: i * 100 + 20 },
        data: {
          label: n.name,
          nodeType: n.nodeType,
          assigneeType: n.assigneeType || "role",
          assigneeValue: n.assigneeValue || "",
          multiEnabled: !!n.multiType,
          multiType: n.multiType || "countersign",
          timeoutHours: n.timeoutHours || 0,
          timeoutAction: n.timeoutAction || "notify_only",
          gatewayType: n.gatewayType || "exclusive"
        }
      }));
      flowEdges.value = parsed.edges.map((e: any) => ({
        id: `${e.sourceId}-${e.targetId}`,
        source: e.sourceId,
        target: e.targetId,
        animated: true,
        data: { condition: e.condition || null }
      }));
    }
  } catch { /* ignore */ }
}, { immediate: true });

watch([flowNodes, flowEdges], () => emitSchema(), { deep: true });
</script>
