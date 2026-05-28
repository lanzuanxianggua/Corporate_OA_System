<template>
  <view class="container">
    <view class="card" v-if="notice">
      <text class="notice-title">{{ notice.title }}</text>
      <view class="notice-meta mt-20">
        <text class="text-gray">{{ notice.author || '系统管理员' }}</text>
        <text class="text-gray ml-20">{{ formatTime(notice.createTime) }}</text>
      </view>
    </view>

    <view class="card" v-if="notice">
      <rich-text :nodes="notice.content || ''"></rich-text>
    </view>

    <view class="empty" v-if="!loading && !notice">
      <text class="text-gray">公告不存在或已删除</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getNoticeById, markNoticeAsRead } from "@/api/notice";

const notice = ref<any>(null);
const loading = ref(true);

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

onLoad(async (options: any) => {
  const id = Number(options?.id);
  if (!id) { loading.value = false; return; }
  try {
    const res: any = await getNoticeById(id);
    notice.value = res.data;
    markNoticeAsRead(id).catch(() => {});
  } catch {
    // silently handle
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.notice-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #303133;
}
.notice-meta {
  display: flex;
  align-items: center;
}
.empty { display: flex; justify-content: center; padding: 100rpx 0; }
.ml-20 { margin-left: 20rpx; }
</style>
