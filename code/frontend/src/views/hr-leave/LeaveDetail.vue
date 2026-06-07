<template>
  <div class="h-full">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">请假详情</span>
          <el-button text @click="goBack">返回</el-button>
        </div>
      </template>

      <template v-if="leave">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="请假 ID">{{ leave.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="(LEAVE_STATUS_TAG_V2[leave.status || ''] || 'info') as any" size="small">
              {{ statusLabel(leave.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="请假类型">
            {{ leaveTypeLabel(leave.leaveType) }}
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ leave.empName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ leave.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="天数">{{ leave.totalDays ?? '-' }} 天</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ formatDate(leave.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ formatDate(leave.endDate) }}</el-descriptions-item>
          <el-descriptions-item label="事由" :span="2">{{ leave.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(leave.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="流程实例 ID">{{ leave.wfInstanceId ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>审批历史</el-divider>
        <ApprovalTimeline business-type="hr_leave" :business-id="leave.id || 0" />
      </template>

      <el-empty v-else-if="!loading" description="未找到请假记录" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import ApprovalTimeline from "@/components/ApprovalTimeline.vue";
import {
  hrLeaveApi,
  LEAVE_TYPE_MAP_V2,
  LEAVE_STATUS_MAP_V2,
  LEAVE_STATUS_TAG_V2
} from "@/api/hr-leave";
import type { HrLeaveDetail } from "@/api/hr-leave";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const leave = ref<HrLeaveDetail | null>(null);

const leaveTypeLabel = (type?: string) => (type ? LEAVE_TYPE_MAP_V2[type] || type : "-");
const statusLabel = (status?: string) => (status ? LEAVE_STATUS_MAP_V2[status] || status : "-");
const formatDate = (s?: string) => (s ? s.substring(0, 10) : "-");
const formatTime = (s?: string) => (s ? s.replace("T", " ").substring(0, 16) : "-");

const fetchDetail = async (id: number) => {
  loading.value = true;
  try {
    const data = await hrLeaveApi.getDetail(id);
    leave.value = data;
  } catch {
    ElMessage.error("获取请假详情失败");
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push({ name: "HrLeaveMy" });
  }
};

const resolveId = () => {
  const raw = route.params.id;
  const id = Number(Array.isArray(raw) ? raw[0] : raw);
  return Number.isFinite(id) && id > 0 ? id : 0;
};

onMounted(() => {
  const id = resolveId();
  if (id > 0) {
    fetchDetail(id);
  } else {
    ElMessage.warning("无效的请假 ID");
  }
});

watch(
  () => route.params.id,
  () => {
    const id = resolveId();
    if (id > 0) {
      fetchDetail(id);
    }
  }
);
</script>
