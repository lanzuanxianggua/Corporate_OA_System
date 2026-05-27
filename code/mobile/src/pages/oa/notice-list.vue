<template>
  <view class="container">
    <view class="card" v-for="item in list" :key="item.id" @click="goDetail(item.id)">
      <view class="flex-between">
        <text class="item-title">{{ item.title }}</text>
        <view v-if="!item.isRead" class="unread-dot"></view>
      </view>
      <view class="flex-between mt-20">
        <text class="text-gray">{{ item.author || '系统管理员' }}</text>
        <text class="text-gray">{{ item.createTime }}</text>
      </view>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无公告</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getNoticePage } from "@/api/notice";

const list = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const finished = ref(false);

const fetchList = async () => {
  if (loading.value || finished.value) return;
  loading.value = true;
  try {
    const res: any = await getNoticePage({ page: page.value, pageSize: 20 });
    const records = res.data?.records || res.data || [];
    if (page.value === 1) {
      list.value = records;
    } else {
      list.value.push(...records);
    }
    if (records.length < 20) finished.value = true;
    else page.value++;
  } catch {} finally {
    loading.value = false;
  }
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/oa/notice-detail?id=${id}` });
};

onShow(fetchList);
</script>

<style scoped>
.item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #F56C6C;
  margin-left: 12rpx;
  flex-shrink: 0;
}
.empty { display: flex; justify-content: center; padding: 100rpx 0; }
.load-more { display: flex; justify-content: center; padding: 20rpx 0; }
</style>
