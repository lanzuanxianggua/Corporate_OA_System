<template>
  <view class="container">
    <view class="card" v-for="todo in list" :key="todo.id" @click="goDetail(todo)">
      <view class="flex-between">
        <text class="todo-title">{{ todo.businessType || '审批任务' }}</text>
        <text class="text-gray text-sm">{{ formatTime(todo.createTime) }}</text>
      </view>
      <view class="mt-20">
        <text class="text-gray">{{ todo.nodeName || '待审批' }}</text>
        <text class="text-gray ml-20" v-if="todo.assigneeName">审批人: {{ todo.assigneeName }}</text>
      </view>
      <view class="mt-20" v-if="todo.multiType">
        <text class="tag tag-warning">{{ todo.multiType === 'countersign' ? '会签' : '或签' }}</text>
      </view>
    </view>
    <view v-if="!loading && list.length === 0" class="empty">
      <text class="text-gray">暂无待办</text>
    </view>
    <view v-if="hasMore" class="load-more text-gray" @click="loadMore">加载更多</view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { onShow, onPullDownRefresh } from "@dcloudio/uni-app";
import { getPendingTasks } from "@/api/workflow";

const list = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const hasMore = ref(false);

const fetchList = async (reset = false) => {
  if (reset) { pageNum.value = 1; list.value = []; }
  loading.value = true;
  try {
    const res: any = await getPendingTasks({ pageNum: pageNum.value, pageSize: 10 });
    const items = res.data?.list || [];
    list.value = reset ? items : [...list.value, ...items];
    hasMore.value = items.length >= 10;
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const loadMore = () => { pageNum.value++; fetchList(); };

const goDetail = (todo: any) => {
  uni.navigateTo({ url: `/pages/approval/detail?instanceId=${todo.instanceId}&taskId=${todo.id}&businessType=${todo.businessType || ''}` });
};

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

onMounted(() => fetchList(true));
onShow(() => fetchList(true));
onPullDownRefresh(() => fetchList(true));
</script>

<style scoped>
.todo-title { font-size: 30rpx; font-weight: 500; }
.tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
.tag-warning { background: #fdf6ec; color: #E6A23C; }
.empty { text-align: center; padding: 100rpx 0; }
.load-more { text-align: center; padding: 20rpx; font-size: 26rpx; }
.text-sm { font-size: 24rpx; }
</style>
