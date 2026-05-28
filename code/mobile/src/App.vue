<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";

onLaunch(() => {
  // Navigation interceptor for auth guard
  const authInterceptor = {
    invoke(args: any) {
      const token = uni.getStorageSync("token");
      const url: string = args.url || "";
      // Allow navigation to login page without token
      if (!token && !url.includes("/pages/login/")) {
        uni.reLaunch({ url: "/pages/login/index" });
        return false;
      }
      return true;
    }
  };

  uni.addInterceptor("navigateTo", authInterceptor);
  uni.addInterceptor("redirectTo", authInterceptor);
  uni.addInterceptor("reLaunch", authInterceptor);
  uni.addInterceptor("switchTab", authInterceptor);
});
</script>

<style>
page {
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  font-size: 28rpx;
  color: #303133;
}

.container {
  padding: 24rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 20rpx;
  color: #303133;
}

.text-gray {
  color: #909399;
  font-size: 24rpx;
}

.text-primary {
  color: #409EFF;
}

.text-success {
  color: #67C23A;
}

.text-danger {
  color: #F56C6C;
}

.text-warning {
  color: #E6A23C;
}

.flex-row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-20 { margin-top: 20rpx; }
.mb-20 { margin-bottom: 20rpx; }
</style>
