<template>
  <view class="container">
    <view
      class="card"
      v-for="item in list"
      :key="item.id"
      @click="handleRead(item)"
    >
      <view class="flex-between">
        <text class="msg-sender">{{ item.senderName || item.sender || '系统' }}</text>
        <text :class="item.isRead ? 'text-gray' : 'text-primary'">
          {{ item.isRead ? '已读' : '未读' }}
        </text>
      </view>
      <text class="msg-content mt-20">{{ item.content }}</text>
      <text class="text-gray mt-20">{{ item.createTime }}</text>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无消息</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getMessagePage, markAsRead } from "@/api/message";

const list = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const finished = ref(false);

const fetchList = async () => {
  if (loading.value || finished.value) return;
  loading.value = true;
  try {
    const res: any = await getMessagePage({ page: page.value, pageSize: 20 });
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

const handleRead = async (item: any) => {
  if (item.isRead) return;
  try {
    await markAsRead(item.id);
    item.isRead = true;
  } catch {}
};

onShow(fetchList);
</script>

<style scoped>
.msg-sender {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}
.msg-content {
  display: block;
  font-size: 28rpx;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty { display: flex; justify-content: center; padding: 100rpx 0; }
.load-more { display: flex; justify-content: center; padding: 20rpx 0; }
</style>
