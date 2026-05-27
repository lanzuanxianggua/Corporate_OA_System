<template>
  <div v-loading="loading">
    <el-timeline v-if="records.length > 0">
      <el-timeline-item
        v-for="record in records"
        :key="record.id"
        :type="getTimelineType(record.approveStatus)"
        :timestamp="formatTime(record.approveTime)"
      >
        <div class="text-sm font-medium">{{ record.nodeName || '审批' }}</div>
        <div class="text-xs text-gray-500">审批人：{{ record.assigneeName || '-' }}</div>
        <div class="mt-1 flex items-center gap-2">
          <el-tag :type="getStatusTagType(record.approveStatus)" size="small">
            {{ getStatusLabel(record.approveStatus) }}
          </el-tag>
          <span v-if="record.remark" class="text-xs text-gray-500">{{ record.remark }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>

    <template v-if="pendingTasks.length > 0">
      <div class="text-xs text-gray-400 mb-2 mt-2" v-if="records.length > 0">当前待审批</div>
      <el-timeline>
        <el-timeline-item
          v-for="task in pendingTasks"
          :key="task.id"
          type="primary"
          :hollow="true"
        >
          <div class="text-sm font-medium">{{ task.nodeName || '审批' }}</div>
          <div class="text-xs text-gray-500">审批人：{{ task.assigneeName || '-' }}</div>
          <div class="mt-1 flex items-center gap-2">
            <el-tag type="info" size="small">等待审批中</el-tag>
            <el-tag v-if="task.multiType === 'countersign'" type="warning" size="small">会签</el-tag>
            <el-tag v-else-if="task.multiType === 'orsign'" type="warning" size="small">或签</el-tag>
          </div>
        </el-timeline-item>
      </el-timeline>
    </template>

    <el-empty v-if="records.length === 0 && pendingTasks.length === 0" description="暂无审批记录" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { getApprovalChain, getApprovalHistory } from "@/api/workflow";

const props = defineProps<{
  businessType: string;
  businessId: number | string;
}>();

const records = ref<any[]>([]);
const pendingTasks = ref<any[]>([]);
const loading = ref(false);

const fetchData = async () => {
  if (!props.businessId) return;
  loading.value = true;
  try {
    const [chainRes, historyRes]: any[] = await Promise.all([
      getApprovalChain({
        businessType: props.businessType,
        businessId: Number(props.businessId)
      }),
      getApprovalHistory({
        businessType: props.businessType,
        businessId: Number(props.businessId)
      })
    ]);
    records.value = chainRes.data || [];
    const allTasks: any[] = historyRes.data || [];
    pendingTasks.value = allTasks.filter((t: any) => t.status === "0");
  } finally {
    loading.value = false;
  }
};

const getTimelineType = (status: number) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  if (status === 4) return "warning";
  if (status === 3) return "info";
  return "primary";
};

const getStatusTagType = (status: number) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  if (status === 4) return "warning";
  return "info";
};

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    1: "已通过",
    2: "已驳回",
    3: "已转办",
    4: "已撤回",
    5: "已退回"
  };
  return map[status] || "已处理";
};

const formatTime = (time: string) => {
  if (!time) return "";
  return time.replace("T", " ");
};

onMounted(fetchData);
watch(() => props.businessId, fetchData);
</script>
