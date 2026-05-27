<template>
  <view class="container">
    <view class="card">
      <text class="section-title">采购申请</text>

      <view class="form-item">
        <text class="form-label">物品名称</text>
        <input class="form-input" v-model="form.itemName" placeholder="请输入采购物品名称" />
      </view>

      <view class="form-item">
        <text class="form-label">数量</text>
        <input class="form-input" type="number" v-model="form.quantity" placeholder="请输入数量" />
      </view>

      <view class="form-item">
        <text class="form-label">预算金额</text>
        <input class="form-input" type="digit" v-model="form.amount" placeholder="请输入预算金额" />
      </view>

      <view class="form-item">
        <text class="form-label">申请原因</text>
        <textarea class="form-textarea" v-model="form.reason" placeholder="请输入采购原因" :maxlength="200" />
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">提交申请</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { submitPurchase } from "@/api/common";

const form = ref({
  itemName: "",
  quantity: "",
  amount: "",
  reason: ""
});

const submitting = ref(false);

const handleSubmit = async () => {
  const { itemName, quantity, amount, reason } = form.value;
  if (!itemName || !quantity || !amount || !reason) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await submitPurchase({ itemName, quantity: Number(quantity), amount: Number(amount), reason });
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
.form-input {
  width: 100%; height: 76rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.form-textarea {
  width: 100%; height: 180rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 16rpx 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.submit-btn {
  margin-top: 40rpx; background: #409EFF; color: #fff; border: none;
  border-radius: 8rpx; font-size: 30rpx; height: 88rpx; line-height: 88rpx;
}
</style>
