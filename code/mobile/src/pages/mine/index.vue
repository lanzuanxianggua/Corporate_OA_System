<template>
  <view class="container">
    <view class="profile-card card">
      <view class="avatar-large">{{ userStore.displayName.charAt(0) }}</view>
      <text class="profile-name">{{ userStore.displayName }}</text>
      <text class="text-gray">{{ userStore.userInfo.empCode || '' }}</text>
    </view>

    <view class="card">
      <view class="menu-item" @click="navigate('/pages/oa/leave-list')">
        <text>请假记录</text><text class="text-gray">></text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/schedule')">
        <text>我的日程</text><text class="text-gray">></text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/document')">
        <text>文档中心</text><text class="text-gray">></text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/notice-list')">
        <text>公告通知</text><text class="text-gray">></text>
      </view>
    </view>

    <view class="card">
      <view class="menu-item" @click="handleLogout">
        <text class="text-danger">退出登录</text><text class="text-gray">></text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from "@/store/user";

const userStore = useUserStore();

const navigate = (url: string) => {
  uni.navigateTo({ url });
};

const handleLogout = async () => {
  const [, res] = await uni.showModal({ title: "提示", content: "确定要退出登录吗？" }) as any;
  if (res?.confirm) {
    await userStore.logout();
  }
};
</script>

<style scoped>
.profile-card {
  display: flex; flex-direction: column; align-items: center;
  padding: 48rpx 24rpx;
}
.avatar-large {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  background: #409EFF; color: #fff; display: flex;
  align-items: center; justify-content: center;
  font-size: 48rpx; font-weight: bold; margin-bottom: 20rpx;
}
.profile-name { font-size: 36rpx; font-weight: 600; margin-bottom: 8rpx; }
.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 0; border-bottom: 1rpx solid #f2f3f5;
}
.menu-item:last-child { border-bottom: none; }
</style>
