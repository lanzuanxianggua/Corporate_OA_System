<template>
  <view class="container">
    <view class="card">
      <text class="section-title">请假申请</text>

      <!-- Leave type -->
      <view class="form-item">
        <text class="form-label">请假类型</text>
        <picker :range="leaveTypes" @change="onTypeChange">
          <view class="form-value picker-value">
            {{ leaveTypes[form.leaveType] || '请选择' }}
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

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">提交申请</button>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { submitLeave } from "@/api/leave";

const leaveTypes = ["事假", "病假", "年假", "婚假", "产假", "丧假"];

const form = ref({
  leaveType: 0,
  startDate: "",
  endDate: "",
  days: "",
  reason: ""
});

const submitting = ref(false);

const onTypeChange = (e: any) => {
  form.value.leaveType = e.detail.value;
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
  if (!startDate || !endDate || !days || !reason) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await submitLeave({
      leaveType: leaveTypes[leaveType],
      startDate,
      endDate,
      days: Number(days),
      reason
    });
    uni.showToast({ title: "提交成功", icon: "success" });
    setTimeout(() => uni.navigateBack(), 1500);
  } catch {
    uni.showToast({ title: "提交失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.form-item {
  margin-bottom: 28rpx;
}
.form-label {
  display: block;
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 8rpx;
}
.form-input {
  width: 100%;
  height: 76rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.form-textarea {
  width: 100%;
  height: 180rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}
.picker-value {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 76rpx;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  color: #303133;
}
.picker-arrow {
  font-size: 22rpx;
  color: #c0c4cc;
  transform: rotate(90deg);
}
.submit-btn {
  margin-top: 40rpx;
  background: #409EFF;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  font-size: 30rpx;
  height: 88rpx;
  line-height: 88rpx;
}
</style>
