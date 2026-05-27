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
      <button class="btn-reject" @click="showRejectDialog">驳回</button>
      <button class="btn-approve" @click="doApprove">通过</button>
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
          <button class="btn-danger" @click="doReject">确认驳回</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getApprovalChain, handleTask, getPendingTasks } from "@/api/workflow";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const chain = ref<any[]>([]);
const taskId = ref("");
const businessType = ref("");
const canApprove = ref(false);
const remark = ref("");

const fetchChain = async (bt: string, bid: string) => {
  try {
    const res: any = await getApprovalChain({ businessType: bt, businessId: Number(bid) });
    chain.value = res.data || [];
  } catch {}
};

const checkCanApprove = async (instanceId: string, bt: string) => {
  try {
    const res: any = await getPendingTasks({ pageNum: 1, pageSize: 50 });
    const tasks = res.data?.list || [];
    const mine = tasks.find((t: any) => String(t.instanceId) === String(instanceId));
    if (mine) {
      taskId.value = String(mine.id);
      canApprove.value = true;
    }
  } catch {}
};

const doApprove = async () => {
  await handleTask({ taskId: taskId.value, status: 1, remark: "" });
  uni.showToast({ title: "已通过", icon: "success" });
  canApprove.value = false;
  setTimeout(() => uni.navigateBack(), 1000);
};

const doReject = async () => {
  if (!remark.value.trim()) {
    uni.showToast({ title: "请输入驳回原因", icon: "none" });
    return;
  }
  await handleTask({ taskId: taskId.value, status: 2, remark: remark.value });
  uni.showToast({ title: "已驳回", icon: "success" });
  canApprove.value = false;
  setTimeout(() => uni.navigateBack(), 1000);
};

const showRejectDialog = () => { remark.value = ""; };
const showTransferDialog = () => { uni.showToast({ title: "转办功能请在PC端操作", icon: "none" }); };
const showReturnDialog = () => { uni.showToast({ title: "退回功能请在PC端操作", icon: "none" }); };

const getDotClass = (status: number) => {
  if (status === 1) return "success";
  if (status === 2) return "danger";
  if (status === 4) return "warning";
  return "primary";
};

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 1: "已通过", 2: "已驳回", 3: "已转办", 4: "已撤回", 5: "已退回" };
  return map[status] || "已处理";
};

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

onLoad((query) => {
  const instanceId = query?.instanceId || "";
  businessType.value = query?.businessType || "";
  const bid = query?.businessId || "";
  if (businessType.value && bid) fetchChain(businessType.value, bid);
  if (instanceId) checkCanApprove(instanceId, businessType.value);
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
.btn-reject { flex: 1; height: 80rpx; line-height: 80rpx; background: #F56C6C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-transfer { flex: 1; height: 80rpx; line-height: 80rpx; background: #E6A23C; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn-return { flex: 1; height: 80rpx; line-height: 80rpx; background: #909399; color: #fff; border-radius: 12rpx; font-size: 30rpx; border: none; }
</style>
