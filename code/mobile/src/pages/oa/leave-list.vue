<template>
  <view class="container">
    <view class="card" v-for="item in list" :key="item.id">
      <view class="flex-between">
        <text class="item-type">{{ formatLeaveType(item.leaveType) }}</text>
        <text :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
      </view>
      <view class="item-dates mt-20">
        <text class="text-gray">{{ item.startDate }} ~ {{ item.endDate }}</text>
        <text class="text-gray ml-20">{{ item.days }}天</text>
      </view>
      <text class="item-reason text-gray mt-20">{{ item.reason }}</text>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无请假记录</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getLeavePage } from "@/api/leave";
import { STATUS_MAP, STATUS_CLASS_MAP, LEAVE_TYPE_MAP } from "@/utils/constants";

const list = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const finished = ref(false);

const statusText = (status: number) => STATUS_MAP[status] ?? "未知";
const statusClass = (status: number) => STATUS_CLASS_MAP[status] ?? "text-gray";
const formatLeaveType = (type: number) => LEAVE_TYPE_MAP[type] || "请假";

const fetchList = async () => {
  if (loading.value || finished.value) return;
  loading.value = true;
  try {
    const res: any = await getLeavePage({ pageNum: pageNum.value, pageSize: 20 });
    const records = res.data?.list || [];
    if (pageNum.value === 1) {
      list.value = records;
    } else {
      list.value.push(...records);
    }
    if (records.length < 20) finished.value = true;
    else pageNum.value++;
  } catch {
    // silently handle
  } finally {
    loading.value = false;
  }
};

onShow(fetchList);
</script>

<style scoped>
.item-type {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}
.item-dates {
  display: flex;
  align-items: center;
}
.item-reason {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty {
  display: flex;
  justify-content: center;
  padding: 100rpx 0;
}
.load-more {
  display: flex;
  justify-content: center;
  padding: 20rpx 0;
}
.ml-20 { margin-left: 20rpx; }
</style>
