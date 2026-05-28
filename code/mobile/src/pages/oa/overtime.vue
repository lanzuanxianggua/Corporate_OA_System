<template>
  <view class="container">
    <view class="card">
      <text class="section-title">加班申请</text>

      <view class="form-item">
        <text class="form-label">加班日期</text>
        <picker mode="date" @change="onDateChange">
          <view class="form-value picker-value">
            {{ form.overtimeDate || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">开始时间</text>
        <picker mode="time" @change="onStartTimeChange">
          <view class="form-value picker-value">
            {{ form.startTime || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">结束时间</text>
        <picker mode="time" @change="onEndTimeChange">
          <view class="form-value picker-value">
            {{ form.endTime || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">加班原因</text>
        <textarea class="form-textarea" v-model="form.reason" placeholder="请输入加班原因" :maxlength="200" />
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">提交申请</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { submitOvertime } from "@/api/common";

const form = ref({
  overtimeDate: "",
  startTime: "",
  endTime: "",
  reason: ""
});

const submitting = ref(false);

const onDateChange = (e: any) => { form.value.overtimeDate = e.detail.value; };
const onStartTimeChange = (e: any) => { form.value.startTime = e.detail.value; };
const onEndTimeChange = (e: any) => { form.value.endTime = e.detail.value; };

const handleSubmit = async () => {
  const { overtimeDate, startTime, endTime, reason } = form.value;
  if (!overtimeDate || !startTime || !endTime || !reason) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  if (startTime >= endTime) {
    uni.showToast({ title: "结束时间必须晚于开始时间", icon: "none" });
    return;
  }
  if (reason.trim().length < 2) {
    uni.showToast({ title: "加班原因至少2个字", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await submitOvertime({ overtimeDate, startTime, endTime, reason });
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
.form-item { margin-bottom: 28rpx; }
.form-label { display: block; font-size: 28rpx; color: #606266; margin-bottom: 8rpx; }
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
</style>
