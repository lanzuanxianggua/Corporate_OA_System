<template>
  <view class="container">
    <view class="card">
      <text class="section-title">请假申请</text>

      <!-- Leave type -->
      <view class="form-item">
        <text class="form-label">请假类型</text>
        <picker :range="leaveTypeLabels" @change="onTypeChange">
          <view class="form-value picker-value">
            {{ selectedLeaveTypeLabel || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <!-- Start date -->
      <view class="form-item">
        <text class="form-label">开始日期</text>
        <picker mode="date" @change="onStartDateChange">
          <view class="form-value picker-value">
            {{ form.startDate || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <!-- End date -->
      <view class="form-item">
        <text class="form-label">结束日期</text>
        <picker mode="date" @change="onEndDateChange">
          <view class="form-value picker-value">
            {{ form.endDate || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <!-- Days -->
      <view class="form-item">
        <text class="form-label">请假天数</text>
        <input class="form-input" type="digit" v-model="form.days" placeholder="请输入天数" />
      </view>

      <!-- Reason -->
      <view class="form-item">
        <text class="form-label">请假原因</text>
        <textarea class="form-textarea" v-model="form.reason" placeholder="请输入请假原因" :maxlength="200" />
      </view>
    </view>

    <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
      <text v-if="!submitting">提交申请</text>
      <text v-else>提交中...</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { submitLeave } from "@/api/leave";
import { LEAVE_TYPE_OPTIONS } from "@/utils/constants";

/** Labels for picker (skip index 0 placeholder) */
const leaveTypeLabels = LEAVE_TYPE_OPTIONS.slice(1);
/** Maps picker index (0-based) to backend leaveType value (1-based string) */
const leaveTypeValueMap = ["1", "2", "3", "4", "5", "6", "7"];

const form = ref({
  leaveType: "0",
  startDate: "",
  endDate: "",
  days: "",
  reason: ""
});

const submitting = ref(false);

const selectedLeaveTypeLabel = computed(() => {
  if (form.value.leaveType === "0") return "";
  return LEAVE_TYPE_OPTIONS[Number(form.value.leaveType)] || "";
});

const onTypeChange = (e: any) => {
  const pickerIdx = Number(e.detail.value);
  form.value.leaveType = leaveTypeValueMap[pickerIdx];
};

const onStartDateChange = (e: any) => {
  form.value.startDate = e.detail.value;
  calcDays();
};

const onEndDateChange = (e: any) => {
  form.value.endDate = e.detail.value;
  calcDays();
};

const calcDays = () => {
  const { startDate, endDate } = form.value;
  if (startDate && endDate) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    form.value.days = diff > 0 ? String(diff) : "";
  }
};

const handleSubmit = async () => {
  const { leaveType, startDate, endDate, days, reason } = form.value;
  if (leaveType === "0") {
    uni.showToast({ title: "请选择请假类型", icon: "none" });
    return;
  }
  if (!startDate || !endDate || !days || !reason) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  if (new Date(endDate) < new Date(startDate)) {
    uni.showToast({ title: "结束日期不能早于开始日期", icon: "none" });
    return;
  }
  const leaveDays = Number(days);
  if (isNaN(leaveDays) || leaveDays < 0.5) {
    uni.showToast({ title: "请假天数不能少于0.5天", icon: "none" });
    return;
  }
  if (reason.trim().length < 2) {
    uni.showToast({ title: "请假原因至少2个字", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    // Backend expects startTime/endTime as full datetime strings, leaveType as String
    await submitLeave({
      leaveType,
      startTime: startDate + " 09:00:00",
      endTime: endDate + " 18:00:00",
      days: leaveDays,
      reason
    });
    uni.showToast({ title: "提交成功", icon: "success" });
    setTimeout(() => uni.navigateBack(), 1500);
  } catch {
    // Error toast handled by request interceptor
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.form-item { margin-bottom: 28rpx; }
.form-label { display: block; font-size: 28rpx; color: #606266; margin-bottom: 8rpx; }
.form-input {
  width: 100%; height: 76rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.form-textarea {
  width: 100%; height: 180rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 16rpx 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.picker-value {
  display: flex; justify-content: space-between; align-items: center;
  height: 76rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; color: #303133;
}
.picker-arrow { font-size: 22rpx; color: #c0c4cc; transform: rotate(90deg); }
.submit-btn {
  margin-top: 40rpx; background: #409EFF; color: #fff; border: none;
  border-radius: 8rpx; font-size: 30rpx; height: 88rpx; line-height: 88rpx;
}
.submit-btn[disabled] { background: #a0cfff; color: #fff; }
</style>
