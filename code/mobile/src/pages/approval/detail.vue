<template>
  <view class="container">
    <!-- Approval chain timeline -->
    <view class="card">
      <text class="section-title">审批进度</text>
      <view v-for="(record, idx) in chain" :key="idx" class="timeline-item">
        <view class="timeline-dot" :class="'dot-' + getDotClass(record.approveStatus)"></view>
        <view class="timeline-content">
          <text class="timeline-title">{{ record.nodeName || '审批' }}</text>
          <text class="text-gray">{{ record.assigneeName || '-' }}</text>
          <view class="flex-row mt-10" v-if="record.approveStatus !== undefined && record.approveStatus !== null">
            <text :class="'tag-sm tag-' + getDotClass(record.approveStatus)">{{ getStatusLabel(record.approveStatus) }}</text>
            <text class="text-gray ml-10" v-if="record.remark">{{ record.remark }}</text>
          </view>
          <text class="text-gray text-sm" v-if="record.approveTime">{{ formatTime(record.approveTime) }}</text>
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
    <view class="action-bar secondary" v-if="taskId && canApprove">
      <button class="btn-transfer" @click="showTransferDialog">转办</button>
      <button class="btn-return" @click="showReturnDialog">退回</button>
    </view>

    <!-- Reject dialog -->
    <view class="mask" v-if="rejectVisible" @click="rejectVisible = false">
      <view class="dialog" @click.stop>
        <text class="section-title">驳回原因</text>
        <textarea v-model="remark" placeholder="请输入驳回原因" class="dialog-textarea" />
        <view class="dialog-btns">
          <button class="dialog-btn cancel" @click="rejectVisible = false">取消</button>
          <button class="dialog-btn danger" :disabled="submitting" @click="doReject">确认驳回</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getApprovalChain, handleTask } from "@/api/workflow";
import { STATUS_MAP } from "@/utils/constants";

const chain = ref<any[]>([]);
const taskId = ref("");
const businessType = ref("");
const canApprove = ref(false);
const remark = ref("");
const submitting = ref(false);
const rejectVisible = ref(false);

const fetchChain = async (bt: string, bid: string) => {
  try {
    const res: any = await getApprovalChain({ businessType: bt, businessId: Number(bid) });
    chain.value = res.data || [];
  } catch {
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
    rejectVisible.value = false;
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
  rejectVisible.value = true;
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
.action-bar.secondary { padding-top: 0; }
.btn-approve { flex: 1; height: 80rpx; line-height: 80rpx; background: #67C23A; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-approve[disabled] { background: #a0d88a; color: #fff; }
.btn-reject { flex: 1; height: 80rpx; line-height: 80rpx; background: #F56C6C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-reject[disabled] { background: #f89898; color: #fff; }
.btn-transfer { flex: 1; height: 80rpx; line-height: 80rpx; background: #E6A23C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-return { flex: 1; height: 80rpx; line-height: 80rpx; background: #909399; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }

/* Dialog */
.mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 999; }
.dialog { width: 80%; background: #fff; border-radius: 16rpx; padding: 32rpx; }
.dialog-textarea { width: 100%; height: 180rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx; padding: 16rpx 20rpx; font-size: 28rpx; box-sizing: border-box; margin-top: 16rpx; }
.dialog-btns { display: flex; gap: 20rpx; margin-top: 24rpx; }
.dialog-btn { flex: 1; height: 76rpx; line-height: 76rpx; font-size: 28rpx; border-radius: 8rpx; border: none; }
.dialog-btn.cancel { background: #f4f4f5; color: #909399; }
.dialog-btn.danger { background: #F56C6C; color: #fff; }
.dialog-btn.danger[disabled] { background: #f89898; }
</style>
