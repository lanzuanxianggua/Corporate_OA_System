<template>
  <view class="container">
    <!-- Approval chain timeline -->
    <view class="card">
      <text class="section-title">审批进度</text>
      <view v-for="(record, idx) in chain" :key="idx" class="timeline-item">
        <view class="timeline-dot" :class="'dot-' + getDotClass(record.approveStatus)"></view>
        <view class="timeline-content" v-if="idx < chain.length - 1">
          <text class="timeline-title">{{ record.nodeName || '审批' }}</text>
          <text class="text-gray">{{ record.assigneeName || '-' }}</text>
          <view class="flex-row mt-10" v-if="record.approveStatus !== undefined">
            <text :class="'tag-sm tag-' + getDotClass(record.approveStatus)">{{ getStatusLabel(record.approveStatus) }}</text>
            <text class="text-gray ml-10" v-if="record.remark">{{ record.remark }}</text>
          </view>
          <text class="text-gray text-sm" v-if="record.approveTime">{{ formatTime(record.approveTime) }}</text>
        </view>
        <view class="timeline-content" v-else>
          <text class="timeline-title">{{ record.nodeName || '审批' }}</text>
          <text class="text-gray">{{ record.assigneeName || '-' }}</text>
        </view>
      </view>
      <view v-if="chain.length === 0" class="empty-sm"><text class="text-gray">暂无审批记录</text></view>
    </view>

    <!-- Action buttons (if task is pending) -->
    <view class="action-bar" v-if="taskId && canApprove">
      <button class="btn-reject" :disabled="submitting" @click="showRejectDialog">驳回</button>
      <button class="btn-approve" :disabled="submitting" @click="doApprove">
        <text v-if="!submitting">通过</text>
        <text v-else>提交中...</text>
      </button>
    </view>
    <view class="action-bar" v-if="taskId && canApprove">
      <button class="btn-transfer" @click="showTransferDialog">转办</button>
      <button class="btn-return" @click="showReturnDialog">退回</button>
    </view>

    <!-- Reject dialog -->
    <uni-popup ref="rejectPopup" type="dialog">
      <view class="popup-content">
        <text class="section-title">驳回原因</text>
        <textarea v-model="remark" placeholder="请输入驳回原因" class="popup-textarea" />
        <view class="popup-btns">
          <button class="btn-cancel" @click="rejectPopup?.close()">取消</button>
          <button class="btn-danger" :disabled="submitting" @click="doReject">
            <text v-if="!submitting">确认驳回</text>
            <text v-else>提交中...</text>
          </button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getApprovalChain, handleTask } from "@/api/workflow";
import { STATUS_MAP } from "@/utils/constants";

const rejectPopup = ref<any>(null);
const chain = ref<any[]>([]);
const taskId = ref("");
const businessType = ref("");
const canApprove = ref(false);
const remark = ref("");
const submitting = ref(false);

const fetchChain = async (bt: string, bid: string) => {
  try {
    const res: any = await getApprovalChain({ businessType: bt, businessId: Number(bid) });
    chain.value = res.data || [];
  } catch (e: any) {
    uni.showToast({ title: "加载审批记录失败", icon: "none" });
  }
};

const doApprove = async () => {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await handleTask({ taskId: Number(taskId.value), status: 1, remark: "" });
    uni.showToast({ title: "已通过", icon: "success" });
    canApprove.value = false;
    setTimeout(() => uni.navigateBack(), 1000);
  } catch (e: any) {
    uni.showToast({ title: e.message || "审批操作失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
};

const doReject = async () => {
  if (!remark.value.trim()) {
    uni.showToast({ title: "请输入驳回原因", icon: "none" });
    return;
  }
  if (submitting.value) return;
  submitting.value = true;
  try {
    await handleTask({ taskId: Number(taskId.value), status: 2, remark: remark.value });
    uni.showToast({ title: "已驳回", icon: "success" });
    rejectPopup.value?.close();
    canApprove.value = false;
    setTimeout(() => uni.navigateBack(), 1000);
  } catch (e: any) {
    uni.showToast({ title: e.message || "驳回操作失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
};

const showRejectDialog = () => {
  remark.value = "";
  rejectPopup.value?.open();
};
const showTransferDialog = () => { uni.showToast({ title: "转办功能请在PC端操作", icon: "none" }); };
const showReturnDialog = () => { uni.showToast({ title: "退回功能请在PC端操作", icon: "none" }); };

const getDotClass = (status: number) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  if (status === 4) return "warning";
  return "primary";
};

const getStatusLabel = (status: number) => STATUS_MAP[status] || "已处理";

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

onLoad((query) => {
  const instanceId = query?.instanceId || "";
  const tid = query?.taskId || "";
  businessType.value = query?.businessType || "";
  const bid = query?.businessId || "";

  // taskId is passed directly from the list page
  if (tid) {
    taskId.value = String(tid);
    canApprove.value = true;
  }

  if (businessType.value && bid) fetchChain(businessType.value, bid);
});
</script>

<style scoped>
.timeline-item { display: flex; padding-left: 30rpx; position: relative; padding-bottom: 30rpx; }
.timeline-item::before { content: ''; position: absolute; left: 11rpx; top: 24rpx; bottom: 0; width: 2rpx; background: #e4e7ed; }
.timeline-item:last-child::before { display: none; }
.timeline-dot { width: 24rpx; height: 24rpx; border-radius: 50%; position: absolute; left: 0; top: 4rpx; }
.dot-success { background: #67C23A; }
.dot-danger { background: #F56C6C; }
.dot-warning { background: #E6A23C; }
.dot-primary { background: #409EFF; }
.timeline-content { margin-left: 20rpx; flex: 1; }
.timeline-title { font-size: 28rpx; font-weight: 500; display: block; }
.tag-sm { font-size: 20rpx; padding: 2rpx 8rpx; border-radius: 4rpx; }
.tag-success { background: #f0f9eb; color: #67C23A; }
.tag-danger { background: #fef0f0; color: #F56C6C; }
.tag-warning { background: #fdf6ec; color: #E6A23C; }
.tag-primary { background: #ecf5ff; color: #409EFF; }
.empty-sm { text-align: center; padding: 40rpx 0; }
.text-sm { font-size: 24rpx; }

.action-bar { display: flex; gap: 20rpx; padding: 20rpx 0; }
.btn-approve { flex: 1; height: 80rpx; line-height: 80rpx; background: #67C23A; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-approve[disabled] { background: #a0d88a; color: #fff; }
.btn-reject { flex: 1; height: 80rpx; line-height: 80rpx; background: #F56C6C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-reject[disabled] { background: #f89898; color: #fff; }
.btn-transfer { flex: 1; height: 80rpx; line-height: 80rpx; background: #E6A23C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-return { flex: 1; height: 80rpx; line-height: 80rpx; background: #909399; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
</style>
