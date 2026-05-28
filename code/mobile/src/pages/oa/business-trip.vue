<template>
  <view class="container">
    <view class="card">
      <text class="section-title">出差申请</text>

      <view class="form-item">
        <text class="form-label">目的地</text>
        <input class="form-input" v-model="form.destination" placeholder="请输入出差目的地" />
      </view>

      <view class="form-item">
        <text class="form-label">开始日期</text>
        <picker mode="date" @change="onStartDateChange">
          <view class="form-value picker-value">
            {{ form.startDate || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">结束日期</text>
        <picker mode="date" @change="onEndDateChange">
          <view class="form-value picker-value">
            {{ form.endDate || '请选择' }}
            <text class="picker-arrow">&#9654;</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="form-label">出差事由</text>
        <textarea class="form-textarea" v-model="form.reason" placeholder="请输入出差事由" :maxlength="200" />
      </view>
    </view>

    <button class="submit-btn" :disabled="submitting" @click="handleSubmit">
      <text v-if="!submitting">提交申请</text>
      <text v-else>提交中...</text>
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { submitBusinessTrip } from "@/api/common";

const form = ref({
  destination: "",
  startDate: "",
  endDate: "",
  reason: ""
});

const submitting = ref(false);

const onStartDateChange = (e: any) => { form.value.startDate = e.detail.value; };
const onEndDateChange = (e: any) => { form.value.endDate = e.detail.value; };

const handleSubmit = async () => {
  const { destination, startDate, endDate, reason } = form.value;
  if (!destination || !startDate || !endDate || !reason) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  if (new Date(endDate) < new Date(startDate)) {
    uni.showToast({ title: "结束日期不能早于开始日期", icon: "none" });
    return;
  }
  if (destination.trim().length < 2) {
    uni.showToast({ title: "目的地至少2个字", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await submitBusinessTrip({ destination, startDate, endDate, reason });
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
