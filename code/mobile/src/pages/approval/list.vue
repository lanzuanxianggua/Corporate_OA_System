<template>
  <view class="container">
    <view class="tabs">
      <view :class="['tab', activeTab === 0 ? 'tab-active' : '']" @click="activeTab = 0; fetchList()">待审批</view>
      <view :class="['tab', activeTab === 1 ? 'tab-active' : '']" @click="activeTab = 1; fetchList()">已审批</view>
    </view>

    <!-- Pending -->
    <template v-if="activeTab === 0">
      <view class="card" v-for="item in pendingList" :key="item.id" @click="goDetail(item)">
        <view class="flex-between">
          <text class="item-title">{{ item.businessType || '审批' }}</text>
          <text class="tag tag-primary">待处理</text>
        </view>
        <view class="mt-20">
          <text class="text-gray">{{ item.nodeName || '-' }}</text>
        </view>
        <view class="mt-10 flex-between">
          <text class="text-gray text-sm">{{ formatTime(item.createTime) }}</text>
        </view>
      </view>
    </template>

    <!-- Done -->
    <template v-if="activeTab === 1">
      <view class="card" v-for="item in historyList" :key="item.id">
        <view class="flex-between">
          <text class="item-title">{{ item.businessType || '审批' }}</text>
          <text :class="['tag', item.status === '1' ? 'tag-success' : item.status === '2' ? 'tag-danger' : 'tag-info']">
            {{ statusMap[item.status] || '已处理' }}
          </text>
        </view>
        <view class="mt-20">
          <text class="text-gray">{{ item.nodeName || '-' }} · {{ item.assigneeName || '-' }}</text>
        </view>
        <view class="mt-10">
          <text class="text-gray text-sm">{{ item.remark || '' }}</text>
        </view>
        <view class="mt-10">
          <text class="text-gray text-sm">{{ formatTime(item.actionTime) }}</text>
        </view>
      </view>
    </template>

    <view v-if="!loading && ((activeTab === 0 && pendingList.length === 0) || (activeTab === 1 && historyList.length === 0))" class="empty">
      <text class="text-gray">暂无数据</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getPendingTasks, getApprovalHistory } from "@/api/workflow";
import { STATUS_MAP } from "@/utils/constants";

const activeTab = ref(0);
const pendingList = ref<any[]>([]);
const historyList = ref<any[]>([]);
const loading = ref(false);

const statusMap: Record<string, string> = STATUS_MAP as unknown as Record<string, string>;

const fetchList = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 0) {
      const res: any = await getPendingTasks({ pageNum: 1, pageSize: 50 });
      pendingList.value = res.data?.list || [];
    } else {
      const types = ["leave", "trip", "outing", "purchase", "expense", "overtime", "loan"];
      const all: any[] = [];
      for (const t of types) {
        try {
          const res: any = await getApprovalHistory({ businessType: t, businessId: 0 });
          all.push(...(res.data || []));
        } catch {}
      }
      historyList.value = all;
    }
  } finally {
    loading.value = false;
  }
};

const goDetail = (item: any) => {
  uni.navigateTo({ url: `/pages/approval/detail?instanceId=${item.instanceId}&taskId=${item.id}&businessType=${item.businessType || ''}` });
};

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

onMounted(fetchList);
onShow(fetchList);
</script>

<style scoped>
.tabs { display: flex; background: #fff; border-radius: 16rpx; margin-bottom: 20rpx; overflow: hidden; }
.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 30rpx; color: #909399; position: relative; }
.tab-active { color: #409EFF; font-weight: 600; }
.tab-active::after { content: ''; position: absolute; bottom: 0; left: 30%; width: 40%; height: 4rpx; background: #409EFF; border-radius: 2rpx; }
.item-title { font-size: 30rpx; font-weight: 500; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 6rpx; }
.tag-primary { background: #ecf5ff; color: #409EFF; }
.tag-success { background: #f0f9eb; color: #67C23A; }
.tag-danger { background: #fef0f0; color: #F56C6C; }
.tag-info { background: #f4f4f5; color: #909399; }
.empty { text-align: center; padding: 100rpx 0; }
.text-sm { font-size: 24rpx; }
</style>
