<template>
  <view class="login-page">
    <view class="login-header">
      <text class="app-title">OA办公系统</text>
      <text class="app-subtitle">移动办公，随时随地</text>
    </view>
    <view class="login-form">
      <view class="form-item">
        <input v-model="form.username" placeholder="请输入用户名" class="form-input" />
      </view>
      <view class="form-item">
        <input v-model="form.password" type="safe-password" placeholder="请输入密码" class="form-input" />
      </view>
      <view class="form-item captcha-row" v-if="captchaUrl">
        <input v-model="form.captchaCode" placeholder="验证码" class="form-input captcha-input" />
        <image :src="captchaUrl" class="captcha-img" @click="fetchCaptcha" mode="aspectFit" />
      </view>
      <button class="login-btn" @click="handleLogin" :loading="loading">登 录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useUserStore } from "@/store/user";
import { getCaptcha } from "@/api/auth";

const userStore = useUserStore();
const loading = ref(false);
const captchaUrl = ref("");
const form = reactive({ username: "", password: "", captchaUuid: "", captchaCode: "" });

const fetchCaptcha = async () => {
  try {
    const res: any = await getCaptcha();
    captchaUrl.value = res.data?.img || "";
    form.captchaUuid = res.data?.uuid || "";
  } catch {}
};

const handleLogin = async () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: "请输入用户名和密码", icon: "none" });
    return;
  }
  loading.value = true;
  try {
    await userStore.login(form.username, form.password, form.captchaUuid, form.captchaCode);
    uni.switchTab({ url: "/pages/home/index" });
  } catch {
    fetchCaptcha();
  } finally {
    loading.value = false;
  }
};

onMounted(fetchCaptcha);
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 60rpx;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.app-title {
  font-size: 52rpx;
  font-weight: bold;
  color: #fff;
  display: block;
  margin-bottom: 16rpx;
}

.app-subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.login-form {
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 32rpx;
}

.form-input {
  height: 88rpx;
  border: 2rpx solid #dcdfe6;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 200rpx;
  height: 88rpx;
  border-radius: 12rpx;
}

.login-btn {
  margin-top: 20rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: #409EFF;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
  border: none;
}
</style>
